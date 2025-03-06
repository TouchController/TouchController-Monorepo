cmake_minimum_required(VERSION 3.10)
project(TouchController-Proxy-Windows)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_SHARED_LINKER_FLAGS " -static")

add_subdirectory(${CMAKE_SOURCE_DIR}/../proxy-common proxy-common.out)

include(CheckCXXCompilerFlag)
check_cxx_compiler_flag(-Wno-ignored-attributes HAVE_NO_IGNORE_ATTRIBUTE)

find_package(JNI REQUIRED)
find_library(WS2_32_LIBRARY ws2_32)

add_library(proxy_windows SHARED "src/winver.hpp" "src/lib.cpp" "src/lib.hpp" "src/input.cpp" "src/input.hpp" )
target_include_directories(proxy_windows PRIVATE ${JNI_INCLUDE_DIRS})
target_link_libraries(proxy_windows PRIVATE ws2_32 proxy_protocol)
target_link_options(proxy_windows PRIVATE -Wl,-Bstatic -lc++ -lunwind -Wl,-Bdynamic)

if (HAVE_NO_IGNORE_ATTRIBUTE)
    target_compile_options(proxy_windows PRIVATE -Wno-ignored-attributes)
endif (HAVE_NO_IGNORE_ATTRIBUTE)
