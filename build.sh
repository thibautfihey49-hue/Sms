#!/usr/bin/env bash
set -euo pipefail
if [ -f gradle/wrapper/gradle-wrapper.jar ]; then WRAPPER=./gradlew; else WRAPPER=gradle; fi
case "${1:-build}" in
  build) $WRAPPER clean assembleRelease ;;
  debug) $WRAPPER clean assembleDebug ;;
  clean) $WRAPPER clean 2>/dev/null ;;
  *) echo "Usage: ./build.sh [build|debug|clean]"; exit 1 ;;
esac
