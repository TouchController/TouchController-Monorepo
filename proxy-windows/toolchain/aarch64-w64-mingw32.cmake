set(CMAKE_SYSTEM_NAME Windows)

set(CMAKE_C_COMPILER   aarch64-w64-mingw32-gcc)
set(CMAKE_CXX_COMPILER aarch64-w64-mingw32-g++)

set(CMAKE_FIND_ROOT_PATH /opt/llvm-mingw/aarch64-w64-mingw32;/opt/llvm-mingw/aarch64-w64-mingw32/lib/jvm)

set(CMAKE_FIND_ROOT_PATH_MODE_PACKAGE ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_LIBRARY ONLY)
set(CMAKE_FIND_ROOT_PATH_MODE_INCLUDE ONLY)
