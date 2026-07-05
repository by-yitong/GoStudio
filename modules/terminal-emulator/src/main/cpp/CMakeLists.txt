cmake_minimum_required(VERSION 4.3.0)
project(terminal-emulator)

set(CMAKE_C_STANDARD 11)

add_library(termux SHARED termux.c)

find_library(log-lib log)
target_link_libraries(termux ${log-lib})