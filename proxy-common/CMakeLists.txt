cmake_minimum_required(VERSION 3.10)
project(TouchController-Proxy-Protocol-Cpp)

set(CMAKE_CXX_STANDARD 20)

add_library(proxy_protocol STATIC "src/event.hpp" "src/event.cpp")
target_include_directories(proxy_protocol INTERFACE src)
