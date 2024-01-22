cmake_minimum_required(VERSION 3.10)
project(Norolog C)

set(SOURCE_FILES
    output.c
)

# Set the output directory for the executable
set(CMAKE_RUNTIME_OUTPUT_DIRECTORY ${CMAKE_SOURCE_DIR})

add_executable(output ${SOURCE_FILES})
