# Install script for directory: /run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/src/main/cpp/oniguruma

# Set the install prefix
if(NOT DEFINED CMAKE_INSTALL_PREFIX)
  set(CMAKE_INSTALL_PREFIX "/usr/local")
endif()
string(REGEX REPLACE "/$" "" CMAKE_INSTALL_PREFIX "${CMAKE_INSTALL_PREFIX}")

# Set the install configuration name.
if(NOT DEFINED CMAKE_INSTALL_CONFIG_NAME)
  if(BUILD_TYPE)
    string(REGEX REPLACE "^[^A-Za-z0-9_]+" ""
           CMAKE_INSTALL_CONFIG_NAME "${BUILD_TYPE}")
  else()
    set(CMAKE_INSTALL_CONFIG_NAME "RelWithDebInfo")
  endif()
  message(STATUS "Install configuration: \"${CMAKE_INSTALL_CONFIG_NAME}\"")
endif()

# Set the component getting installed.
if(NOT CMAKE_INSTALL_COMPONENT)
  if(COMPONENT)
    message(STATUS "Install component: \"${COMPONENT}\"")
    set(CMAKE_INSTALL_COMPONENT "${COMPONENT}")
  else()
    set(CMAKE_INSTALL_COMPONENT)
  endif()
endif()

# Install shared libraries without execute permission?
if(NOT DEFINED CMAKE_INSTALL_SO_NO_EXE)
  set(CMAKE_INSTALL_SO_NO_EXE "0")
endif()

# Is this installation the result of a crosscompile?
if(NOT DEFINED CMAKE_CROSSCOMPILING)
  set(CMAKE_CROSSCOMPILING "TRUE")
endif()

# Set path to fallback-tool for dependency-resolution.
if(NOT DEFINED CMAKE_OBJDUMP)
  set(CMAKE_OBJDUMP "/home/admins/Android/Sdk/ndk/27.0.12077973/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-objdump")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libonig.so" AND
     NOT IS_SYMLINK "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libonig.so")
    file(RPATH_CHECK
         FILE "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libonig.so"
         RPATH "")
  endif()
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib" TYPE SHARED_LIBRARY FILES "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/build/intermediates/cxx/RelWithDebInfo/2m4s4y1m/obj/arm64-v8a/libonig.so")
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libonig.so" AND
     NOT IS_SYMLINK "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libonig.so")
    if(CMAKE_INSTALL_DO_STRIP)
      execute_process(COMMAND "/home/admins/Android/Sdk/ndk/27.0.12077973/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-strip" "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libonig.so")
    endif()
  endif()
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/include" TYPE FILE FILES
    "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/src/main/cpp/oniguruma/src/oniguruma.h"
    "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/src/main/cpp/oniguruma/src/oniggnu.h"
    )
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/cmake/oniguruma" TYPE FILE FILES
    "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/.cxx/RelWithDebInfo/2m4s4y1m/arm64-v8a/oniguruma/generated/onigurumaConfig.cmake"
    "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/.cxx/RelWithDebInfo/2m4s4y1m/arm64-v8a/oniguruma/generated/onigurumaConfigVersion.cmake"
    )
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/oniguruma/onigurumaTargets.cmake")
    file(DIFFERENT _cmake_export_file_changed FILES
         "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/oniguruma/onigurumaTargets.cmake"
         "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/.cxx/RelWithDebInfo/2m4s4y1m/arm64-v8a/oniguruma/CMakeFiles/Export/8c73ef290f98ec84adf6bb2530e3b311/onigurumaTargets.cmake")
    if(_cmake_export_file_changed)
      file(GLOB _cmake_old_config_files "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/oniguruma/onigurumaTargets-*.cmake")
      if(_cmake_old_config_files)
        string(REPLACE ";" ", " _cmake_old_config_files_text "${_cmake_old_config_files}")
        message(STATUS "Old export file \"$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/cmake/oniguruma/onigurumaTargets.cmake\" will be replaced.  Removing files [${_cmake_old_config_files_text}].")
        unset(_cmake_old_config_files_text)
        file(REMOVE ${_cmake_old_config_files})
      endif()
      unset(_cmake_old_config_files)
    endif()
    unset(_cmake_export_file_changed)
  endif()
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/cmake/oniguruma" TYPE FILE FILES "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/.cxx/RelWithDebInfo/2m4s4y1m/arm64-v8a/oniguruma/CMakeFiles/Export/8c73ef290f98ec84adf6bb2530e3b311/onigurumaTargets.cmake")
  if(CMAKE_INSTALL_CONFIG_NAME MATCHES "^([Rr][Ee][Ll][Ww][Ii][Tt][Hh][Dd][Ee][Bb][Ii][Nn][Ff][Oo])$")
    file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/cmake/oniguruma" TYPE FILE FILES "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/.cxx/RelWithDebInfo/2m4s4y1m/arm64-v8a/oniguruma/CMakeFiles/Export/8c73ef290f98ec84adf6bb2530e3b311/onigurumaTargets-relwithdebinfo.cmake")
  endif()
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/doc/onig" TYPE FILE FILES
    "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/src/main/cpp/oniguruma/doc/API"
    "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/src/main/cpp/oniguruma/doc/API.ja"
    "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/src/main/cpp/oniguruma/doc/RE"
    "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/src/main/cpp/oniguruma/doc/RE.ja"
    "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/src/main/cpp/oniguruma/doc/FAQ"
    "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/src/main/cpp/oniguruma/doc/FAQ.ja"
    "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/src/main/cpp/oniguruma/doc/CALLOUTS.BUILTIN"
    "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/src/main/cpp/oniguruma/doc/CALLOUTS.BUILTIN.ja"
    "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/src/main/cpp/oniguruma/doc/CALLOUTS.API"
    "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/src/main/cpp/oniguruma/doc/CALLOUTS.API.ja"
    "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/src/main/cpp/oniguruma/doc/UNICODE_PROPERTIES"
    )
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/doc/onig" TYPE FILE FILES
    "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/src/main/cpp/oniguruma/AUTHORS"
    "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/src/main/cpp/oniguruma/COPYING"
    "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/src/main/cpp/oniguruma/HISTORY"
    "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/src/main/cpp/oniguruma/README.md"
    )
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib/pkgconfig" TYPE FILE FILES "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/.cxx/RelWithDebInfo/2m4s4y1m/arm64-v8a/oniguruma/oniguruma.pc")
endif()

if(CMAKE_INSTALL_COMPONENT STREQUAL "Unspecified" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/bin" TYPE PROGRAM FILES "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/.cxx/RelWithDebInfo/2m4s4y1m/arm64-v8a/oniguruma/onig-config")
endif()

string(REPLACE ";" "\n" CMAKE_INSTALL_MANIFEST_CONTENT
       "${CMAKE_INSTALL_MANIFEST_FILES}")
if(CMAKE_INSTALL_LOCAL_ONLY)
  file(WRITE "/run/media/admins/DATA/myitem/project/GoStudio2/modules/sora-oniguruma-native/.cxx/RelWithDebInfo/2m4s4y1m/arm64-v8a/oniguruma/install_local_manifest.txt"
     "${CMAKE_INSTALL_MANIFEST_CONTENT}")
endif()
