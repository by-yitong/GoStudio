#----------------------------------------------------------------
# Generated CMake target import file for configuration "Debug".
#----------------------------------------------------------------

# Commands may need to know the format version.
set(CMAKE_IMPORT_FILE_VERSION 1)

# Import target "oniguruma::onig" for configuration "Debug"
set_property(TARGET oniguruma::onig APPEND PROPERTY IMPORTED_CONFIGURATIONS DEBUG)
set_target_properties(oniguruma::onig PROPERTIES
  IMPORTED_LOCATION_DEBUG "${_IMPORT_PREFIX}/lib/libonig.so"
  IMPORTED_SONAME_DEBUG "libonig.so"
  )

list(APPEND _cmake_import_check_targets oniguruma::onig )
list(APPEND _cmake_import_check_files_for_oniguruma::onig "${_IMPORT_PREFIX}/lib/libonig.so" )

# Commands beyond this point should not need to know the version.
set(CMAKE_IMPORT_FILE_VERSION)
