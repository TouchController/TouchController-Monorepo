#pragma once
#include <jni.h>

extern "C" {
	JNIEXPORT void JNICALL Java_top_fifthlight_touchcontroller_platform_win32_Interface_init(
		JNIEnv* env,
		jclass clazz,
		jlong window_handle
	);

	JNIEXPORT jint JNICALL Java_top_fifthlight_touchcontroller_platform_win32_Interface_pollEvent(
		JNIEnv* env,
		jclass clazz,
		jbyteArray buffer
	);
}
