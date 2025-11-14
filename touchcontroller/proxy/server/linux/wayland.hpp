#pragma once
#include <jni.h>

extern "C" {
JNIEXPORT void JNICALL
Java_top_fifthlight_touchcontroller_common_platform_wayland_Interface_init(
    JNIEnv *env, jclass clazz, jlong display_handle, jlong surface_handle);

JNIEXPORT void JNICALL
Java_top_fifthlight_touchcontroller_common_platform_wayland_Interface_resize(
    JNIEnv *env, jclass clazz, jint width, jint height);

JNIEXPORT jint JNICALL
Java_top_fifthlight_touchcontroller_common_platform_wayland_Interface_pollEvents(
    JNIEnv *env, jclass clazz, jbyteArray buffer);
}
