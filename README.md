Car Dashboard for Android
=========================

A car dashboard for android with FM music streaming, see [hverr/car-dashboard](https://github.com/hverr/car-dashboard) for more information.

Installation
------------

1. Download the latest release
2. Run `adb install -r` on the downloaded APK file.

Build
-----
Install the Android SDK and run

```sh
ANDROID_HOME=/path/to/android-sdk ./gradlew build
adb install -r ./app/build/outputs/apk/apk-debug.apk
```
