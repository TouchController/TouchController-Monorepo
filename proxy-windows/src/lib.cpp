#include "lib.hpp"
#include "input.hpp"
#include "protocol.hpp"
#include <deque>
#include <mutex>
#include <vector>
#include <cstring>

extern std::mutex g_event_queue_mutex;
extern std::deque<ProxyMessage> g_event_queue;

extern "C" {
	JNIEXPORT void JNICALL Java_top_fifthlight_touchcontroller_platform_win32_Interface_init(
		JNIEnv* env,
		jclass clazz,
		jlong window_handle
	) {
		try {
			init(reinterpret_cast<HWND>(window_handle));
		}
		catch (const InitializeError& e) {
			env->ThrowNew(env->FindClass("java/lang/Exception"), e.what());
		}
	}

	JNIEXPORT jint JNICALL Java_top_fifthlight_touchcontroller_platform_win32_Interface_pollEvent(
		JNIEnv* env,
		jclass clazz,
		jbyteArray buffer
	) {
		std::lock_guard<std::mutex> lock(g_event_queue_mutex);

		if (g_event_queue.empty()) {
			return 0;
		}

		ProxyMessage message = g_event_queue.front();
		g_event_queue.pop_front();

		std::vector<uint8_t> msg_buffer;
		message.serialize(msg_buffer);

		jsize buffer_length = env->GetArrayLength(buffer);
		if (msg_buffer.size() > static_cast<size_t>(buffer_length)) {
			env->ThrowNew(env->FindClass("java/lang/Exception"), "Buffer overflow");
			return 0;
		}

		env->SetByteArrayRegion(buffer, 0, static_cast<jsize>(msg_buffer.size()),
			reinterpret_cast<const jbyte*>(msg_buffer.data()));
		return static_cast<jint>(msg_buffer.size());
	}
}