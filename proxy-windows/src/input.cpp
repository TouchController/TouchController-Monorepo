#include "input.hpp"
#include <map>
#include <memory>
#include <vector>
#include <windows.h>

std::mutex g_event_queue_mutex;
std::deque<ProxyMessage> g_event_queue;

#ifdef __MINGW32__
typedef enum {
	FEEDBACK_TOUCH_CONTACTVISUALIZATION = 1,
	FEEDBACK_PEN_BARRELVISUALIZATION = 2,
	FEEDBACK_PEN_TAP = 3,
	FEEDBACK_PEN_DOUBLETAP = 4,
	FEEDBACK_PEN_PRESSANDHOLD = 5,
	FEEDBACK_PEN_RIGHTTAP = 6,
	FEEDBACK_TOUCH_TAP = 7,
	FEEDBACK_TOUCH_DOUBLETAP = 8,
	FEEDBACK_TOUCH_PRESSANDHOLD = 9,
	FEEDBACK_TOUCH_RIGHTTAP = 10,
	FEEDBACK_GESTURE_PRESSANDTAP = 11,
	FEEDBACK_MAX = 0xFFFFFFFF
} FEEDBACK_TYPE;
#endif

namespace {
	HMODULE GetUser32Module() {
		static HMODULE module = NULL;
		if (module == NULL) {
			module = GetModuleHandle(TEXT("USER32.dll"));
		}
		return module;
	}

	typedef BOOL(WINAPI* SetWindowFeedbackSetting)(HWND hwnd, FEEDBACK_TYPE feedback, DWORD dwFlags, UINT32 size, CONST VOID* configuration);
	BOOL SetWindowFeedbackSettingCompat(HWND hwnd, FEEDBACK_TYPE feedback, DWORD dwFlags, UINT32 size, CONST VOID* configuration) {
		static SetWindowFeedbackSetting SetWindowFeedbackSettingNative = NULL;
		if (SetWindowFeedbackSettingNative == NULL) {
			HMODULE module = GetUser32Module();
			if (module != NULL) {
				SetWindowFeedbackSettingNative =
					(SetWindowFeedbackSetting)GetProcAddress(module, "SetWindowFeedbackSetting");
			}
		}
		if (SetWindowFeedbackSettingNative != NULL) {
			return SetWindowFeedbackSettingNative(hwnd, feedback, dwFlags, size, configuration);
		}
		else {
			return TRUE;
		}
	}

	void disable_feedback(HWND handle, FEEDBACK_TYPE feedback) {
		BOOL enabled = FALSE;
		if (!SetWindowFeedbackSettingCompat(
			handle,
			feedback,
			0,
			sizeof(BOOL),
			&enabled
		)) {
			throw InitializeError("SetWindowFeedbackSetting failed");
		}
	}

	struct PointerData {
		uint32_t pointer_id;
		uint64_t prev_tick;
	};

	struct PointerState {
		uint64_t tick = 0;
		uint32_t next_id = 1;
		std::unordered_map<uint32_t, PointerData> id_map;
	};

	std::mutex g_pointer_state_mutex;
	PointerState g_pointer_state;

	void handle_touch_event(CWPSTRUCT* msg) {
		const UINT count = LOWORD(msg->wParam);
		HTOUCHINPUT handle = reinterpret_cast<HTOUCHINPUT>(msg->lParam);

		std::vector<TOUCHINPUT> pointers(count);
		if (!GetTouchInputInfo(handle, count, pointers.data(), sizeof(TOUCHINPUT))) {
			throw EventError("GetTouchInputInfo failed");
		}

		// Query window size by getting client rect
		RECT client_rect;
		if (!GetClientRect(msg->hwnd, &client_rect)) {
			throw EventError("GetClientRect failed");
		}

		// Query origin of client area
		POINT origin{ 0, 0 };
		if (!ClientToScreen(msg->hwnd, &origin)) {
			throw EventError("ClientToScreen failed");
		}

		const float scaled_origin_left = static_cast<float>(origin.x);
		const float scaled_origin_top = static_cast<float>(origin.y);
		const float scaled_client_width = static_cast<float>(client_rect.right - client_rect.left) * 100;
		const float scaled_client_height = static_cast<float>(client_rect.bottom - client_rect.top) * 100;

		std::lock_guard<std::mutex> queue_lock(g_event_queue_mutex);
		std::lock_guard<std::mutex> pointer_state_lock(g_pointer_state_mutex);

		PointerState& state = g_pointer_state;
		state.tick++;

		for (auto& pointer : pointers) {
			const float x = (pointer.x - scaled_origin_left) / scaled_client_width;
			const float y = (pointer.y - scaled_origin_top) / scaled_client_height;

			uint32_t pointer_id;
			auto it = state.id_map.find(pointer.dwID);
			if (it != state.id_map.end()) {
				pointer_id = it->second.pointer_id;
				it->second.prev_tick = state.tick;
			}
			else {
				pointer_id = state.next_id++;
				state.id_map.emplace(pointer.dwID, PointerData{ pointer_id, state.tick });
			}

			if (pointer.dwFlags & TOUCHEVENTF_DOWN || pointer.dwFlags & TOUCHEVENTF_MOVE) {
				g_event_queue.push_back(ProxyMessage{ ProxyMessage::Type::Add, {.add = {pointer_id, x, y}} });
			}
			else if (pointer.dwFlags & TOUCHEVENTF_UP) {
				g_event_queue.push_back(ProxyMessage{ ProxyMessage::Type::Remove, {.remove = {pointer_id}} });
				state.id_map.erase(pointer.dwID);
			}
		}

		auto it = state.id_map.begin();
		while (it != state.id_map.end()) {
			if (it->second.prev_tick != state.tick) {
				g_event_queue.push_back(ProxyMessage{ ProxyMessage::Type::Remove, {.remove = {it->second.pointer_id}} });
				it = state.id_map.erase(it);
			}
			else {
				++it;
			}
		}
	}

	LRESULT CALLBACK event_hook(int ncode, WPARAM wparam, LPARAM lparam) {
		if (ncode >= 0) {
			auto* msg = reinterpret_cast<CWPSTRUCT*>(lparam);
			if (msg->message == WM_TOUCH) {
				try {
					handle_touch_event(msg);
				}
				catch (const EventError& e) {
					OutputDebugStringA(e.what());
				}
			}
		}
		return CallNextHookEx(nullptr, ncode, wparam, lparam);
	}
}

void init(HWND handle) {
	if (!RegisterTouchWindow(handle, TWF_WANTPALM)) {
		throw InitializeError("RegisterTouchWindow failed");
	}

	const FEEDBACK_TYPE feedbacks[] = {
		FEEDBACK_TOUCH_CONTACTVISUALIZATION,
		FEEDBACK_TOUCH_TAP,
		FEEDBACK_TOUCH_DOUBLETAP,
		FEEDBACK_TOUCH_PRESSANDHOLD,
		FEEDBACK_TOUCH_RIGHTTAP,
		FEEDBACK_GESTURE_PRESSANDTAP
	};

	for (auto feedback : feedbacks) {
		disable_feedback(handle, feedback);
	}

	DWORD thread_id = GetCurrentThreadId();
	if (!SetWindowsHookEx(WH_CALLWNDPROC, event_hook, nullptr, thread_id)) {
		throw InitializeError("SetWindowsHookEx failed");
	}
}
