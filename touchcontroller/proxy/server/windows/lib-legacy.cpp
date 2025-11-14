#include "lib-legacy.hpp"
#include "event.hpp"
#include "input.hpp"
#include "protocol.hpp"

#include <cstring>
#include <deque>
#include <mutex>
#include <vector>
#include <algorithm>
#include <wchar.h>
#include <string>
#include <shlobj.h>
#include <windows.h>
#include <shellapi.h>

extern std::mutex g_event_queue_mutex;
extern std::deque<ProxyMessage> g_event_queue;

namespace {

static const wchar_t* osk_reg_path =
    L"Software\\Classes\\CLSID\\{054AAE20-4BEA-4347-8A35-64A533254A9D}"
    L"\\LocalServer32";
static const wchar_t* osk_window_class = L"IPTip_Main_Window";

// https://github.com/chromium/chromium/blob/1508d938a36d8b4e75e929e614731163542610a3/ui/base/ime/win/on_screen_keyboard_display_manager_tab_tip.cc#L304
static bool get_tab_tip_path(std::wstring& out_path) {
    HKEY hKey = nullptr;
    if (RegOpenKeyExW(HKEY_LOCAL_MACHINE, osk_reg_path, 0,
                      KEY_READ | KEY_WOW64_64KEY, &hKey) != ERROR_SUCCESS)
        return false;

    wchar_t path_buf[1024];
    DWORD path_len = sizeof(path_buf);
    LONG result = RegQueryValueExW(hKey, nullptr, nullptr, nullptr,
                                   (LPBYTE)path_buf, &path_len);
    RegCloseKey(hKey);
    if (result != ERROR_SUCCESS) return false;

    std::wstring raw_path(path_buf);
    std::transform(raw_path.begin(), raw_path.end(), raw_path.begin(), towlower);

    const std::wstring var = L"%commonprogramfiles%";
    size_t pos = raw_path.find(var);
    if (pos != std::wstring::npos) {
        wchar_t common_program_files_path[MAX_PATH] = {0};
        DWORD len = GetEnvironmentVariableW(
            L"CommonProgramW6432", common_program_files_path, MAX_PATH);
        if (len == 0) {
            PWSTR known_path = nullptr;
            if (SUCCEEDED(SHGetKnownFolderPath(FOLDERID_ProgramFilesCommon, 0,
                                               nullptr, &known_path))) {
                wcsncpy_s(common_program_files_path, known_path, MAX_PATH);
                CoTaskMemFree(known_path);
            } else {
                return false;
            }
        }
        raw_path.replace(pos, var.length(), common_program_files_path);
    }

    out_path = raw_path;
    return true;
}

static void show_keyboard() {
    static bool found_tab_tip_path = false;
    static bool failed_to_find_tab_tip_path = false;
    static std::wstring tab_tip_path;

    if (!found_tab_tip_path) {
        if (failed_to_find_tab_tip_path) {
            return;
        }
        if (!get_tab_tip_path(tab_tip_path)) {
            failed_to_find_tab_tip_path = true;
            return;
        }
        found_tab_tip_path = true;
    }

    ShellExecuteW(nullptr, L"open", tab_tip_path.c_str(), nullptr, nullptr,
                  SW_SHOW);
}

static void hide_keyboard() {
    HWND tab_tip_handle = FindWindowW(osk_window_class, nullptr);
    if (tab_tip_handle && IsWindow(tab_tip_handle)) {
        SendMessageW(tab_tip_handle, WM_CLOSE, 0, 0);
    }
}

}

extern "C" {
JNIEXPORT void JNICALL
Java_top_fifthlight_touchcontroller_common_platform_win32_Interface_init(
    JNIEnv* env, jclass clazz, jlong window_handle) {
    CoInitializeEx(NULL, COINIT_APARTMENTTHREADED);
    try {
        init(reinterpret_cast<HWND>(window_handle));
    } catch (const InitializeError& e) {
        env->ThrowNew(env->FindClass("java/lang/Exception"), e.what());
    }
}

JNIEXPORT jint JNICALL
Java_top_fifthlight_touchcontroller_common_platform_win32_Interface_pollEvent(
    JNIEnv* env, jclass clazz, jbyteArray buffer) {
    std::optional<std::vector<uint8_t>> event =
        touchcontroller::event::poll_event();
    if (!event.has_value()) {
        return 0;
    }

    std::vector<uint8_t> msg_buffer = event.value();

    jsize buffer_length = env->GetArrayLength(buffer);
    if (msg_buffer.size() > static_cast<size_t>(buffer_length)) {
        env->ThrowNew(env->FindClass("java/lang/Exception"), "Buffer overflow");
        return 0;
    }

    env->SetByteArrayRegion(buffer, 0, static_cast<jsize>(msg_buffer.size()),
                            reinterpret_cast<const jbyte*>(msg_buffer.data()));
    return static_cast<jint>(msg_buffer.size());
}

JNIEXPORT void JNICALL
Java_top_fifthlight_touchcontroller_common_platform_win32_Interface_pushEvent(
    JNIEnv* env, jclass clazz, jbyteArray buffer, jint length) {
    std::vector<uint8_t> data;
    data.resize(length);
    env->GetByteArrayRegion(buffer, 0, length,
                            reinterpret_cast<jbyte*>(data.data()));

    ProxyMessage message;
    if (touchcontroller::protocol::deserialize_event(message, data)) {
        switch (message.type) {
            case ProxyMessage::Vibrate: {
                break;
            }
            case ProxyMessage::KeyboardShow: {
                if (message.keyboard_show.show) {
                    show_keyboard();
                } else {
                    hide_keyboard();
                }
                break;
            }
            case ProxyMessage::Initialize: {
                touchcontroller::event::push_event(ProxyMessage{
                    ProxyMessage::Capability, {.capability = {"keyboard_show", true}}});
                break;
            }
            default:
                break;
        }
    }
}
}