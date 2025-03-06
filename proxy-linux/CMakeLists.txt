cmake_minimum_required(VERSION 3.10)
project(TouchController-Proxy-Linux)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)

add_subdirectory(${CMAKE_SOURCE_DIR}/../proxy-common proxy-common.out)

find_package(JNI QUIET)
if(NOT JNI_FOUND)
    if(NOT DEFINED JAVA_HOME)
        if(NOT DEFINED ENV{JAVA_HOME})
            message(FATAL_ERROR "JAVA_HOME not exist")
        endif()
        set(JAVA_HOME $ENV{JAVA_HOME})
    endif()

    get_filename_component(JAVA_HOME "${JAVA_HOME}" ABSOLUTE)
    
    if(NOT EXISTS "${JAVA_HOME}")
        message(FATAL_ERROR "JAVA_HOME path '${JAVA_HOME}' does not exist")
    endif()

    if(CMAKE_SYSTEM_NAME STREQUAL "Linux")
        set(JNI_PLATFORM_SUBDIR "linux")
    elseif(CMAKE_SYSTEM_NAME STREQUAL "Darwin")
        set(JNI_PLATFORM_SUBDIR "darwin")
    elseif(CMAKE_SYSTEM_NAME STREQUAL "Windows")
        set(JNI_PLATFORM_SUBDIR "win32")
    else()
        message(FATAL_ERROR "Unsupported platform: ${CMAKE_SYSTEM_NAME}")
    endif()

    set(JNI_HEADER_PATHS
        "${JAVA_HOME}/include"
        "${JAVA_HOME}/include/${JNI_PLATFORM_SUBDIR}"
    )

    find_path(JNI_MAIN_INCLUDE_DIR
        NAMES jni.h
        PATHS ${JNI_HEADER_PATHS}
        NO_CMAKE_FIND_ROOT_PATH
        NO_DEFAULT_PATH
        REQUIRED
    )

    set(JNI_INCLUDE_DIRS
        ${JNI_MAIN_INCLUDE_DIR}
        "${JNI_MAIN_INCLUDE_DIR}/${JNI_PLATFORM_SUBDIR}"
    )
endif()

find_package(PkgConfig REQUIRED)
pkg_check_modules(WAYLAND_CLIENT REQUIRED wayland-client)

add_library(proxy_linux_wayland SHARED "src/wayland.hpp" "src/wayland.cpp")
target_include_directories(proxy_linux_wayland PRIVATE ${JNI_INCLUDE_DIRS} ${WAYLAND_CLIENT_INCLUDE_DIRS})
target_link_libraries(proxy_linux_wayland PRIVATE ${WAYLAND_CLIENT_LIBRARIES} proxy_protocol)
