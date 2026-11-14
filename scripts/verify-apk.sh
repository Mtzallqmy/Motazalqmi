#!/usr/bin/env bash
set -euo pipefail
apk="${1:?APK path required}"
sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:?Android SDK required}}"
build_tools="$sdk_root/build-tools/35.0.0"
"$build_tools/apksigner" verify --verbose --print-certs "$apk"
"$build_tools/aapt2" dump badging "$apk" | head -3
python3 - "$apk" <<'PY'
import sys,zipfile
with zipfile.ZipFile(sys.argv[1]) as z:
    libs=[x for x in z.namelist() if x.startswith('lib/') and x.endswith('.so')]
    assert all(x.startswith('lib/arm64-v8a/') for x in libs),libs
    print('ABI: arm64 only' if libs else 'ABI: pure Kotlin/Java, works on ARM64 without dummy JNI')
PY
