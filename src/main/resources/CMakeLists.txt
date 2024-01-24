cmake_minimum_required(VERSION 3.10)
project(Norolog C)

set(SOURCE_FILES output.c)

set(SDL_DIR ${CMAKE_SOURCE_DIR}/SDL)

# Include SDL headers
include_directories(${SDL_DIR}/include)

link_directories(${SDL_DIR}/lib)

# Set the output directory for the executable
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR})

add_executable(output ${SOURCE_FILES})

target_link_libraries(output -static -lmingw32 -lSDL2main -lSDL2 -Wl,--dynamicbase -Wl,--nxcompat -lm -ldinput8 -ldxguid -ldxerr8 -luser32 -lgdi32 -lwinmm -limm32 -lole32 -loleaut32 -lshell32 -lsetupapi -lversion -luuid)
