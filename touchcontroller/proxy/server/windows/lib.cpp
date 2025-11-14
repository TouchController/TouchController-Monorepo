#include "lib.hpp"
#include "event.hpp"
#include "input.hpp"
#include "protocol.hpp"

#include <cstring>
#include <deque>
#include <mutex>
#include <vector>

#include <winstring.h>
#include <roapi.h>
#include <inputpaneinterop.h>
// Gross hack to work around MINGW-packages#22160
#define ____FIReference_1_boolean_INTERFACE_DEFINED__
#include <windows.ui.viewmanagement.h>
using ABI::Windows::UI::ViewManagement::IInputPane2;

extern std::mutex g_event_queue_mutex;
extern std::deque<ProxyMessage> g_event_queue;

namespace {

static HWND game_window_handle;

static bool toggle_keyboard(boolean show) {
    HSTRING class_name = nullptr;
    HRESULT result = WindowsCreateString(
        RuntimeClass_Windows_UI_ViewManagement_InputPane,
        wcslen(RuntimeClass_Windows_UI_ViewManagement_InputPane), &class_name);

    if (FAILED(result)) {
        std::cerr << "WindowsCreateString failed: 0x" << std::hex << result
                  << std::endl;
        return false;
    }

    IInputPaneInterop* input_pane_interop = nullptr;
    result =
        RoGetActivationFactory(class_name, __uuidof(IInputPaneInterop),
                               reinterpret_cast<void**>(&input_pane_interop));
    WindowsDeleteString(class_name);
    if (FAILED(result)) {
        std::cerr << "RoGetActivationFactory failed: 0x" << std::hex << result
                  << std::endl;
        return false;
    }

    IInputPane2* input_pane = nullptr;
    result = input_pane_interop->GetForWindow(
        game_window_handle, __uuidof(IInputPane2),
        reinterpret_cast<void**>(&input_pane));
    input_pane_interop->Release();
    if (FAILED(result)) {
        std::cerr << "GetForWindow failed: 0x" << std::hex << result
                  << std::endl;
        return false;
    }

    boolean toggle_result;
    if (show) {
        result = input_pane->TryShow(&toggle_result);
    } else {
        result = input_pane->TryHide(&toggle_result);
    }
    input_pane->Release();
    if (FAILED(result)) {
        std::cerr << "TryShow/TryHide failed: 0x" << std::hex << result
                  << std::endl;
        return false;
    }
    return true;
}

}

extern "C" {

JNIEXPORT void JNICALL
Java_top_fifthlight_touchcontroller_common_platform_win32_Interface_init(
    JNIEnv* env, jclass clazz, jlong window_handle) {
    CoInitializeEx(NULL, COINIT_APARTMENTTHREADED);
    game_window_handle = reinterpret_cast<HWND>(window_handle);
    try {
        init(game_window_handle);
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
                if (!toggle_keyboard(message.keyboard_show.show)) {
                    std::cerr << "Failed to " << (message.keyboard_show.show ? "show" : "hide") << " keyboard" << std::endl;
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