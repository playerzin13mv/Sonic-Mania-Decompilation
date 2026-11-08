#!/bin/bash
set -e
A=dependencies/RSDKv5/android/app
sed -i 's/applicationId "[^"]*"/applicationId "com.sonicmaniadt.app"/' $A/build.gradle
sed -i '/signingConfig signingConfigs.release/d' $A/build.gradle
sed -i 's|\(<string name="app_name">\)[^<]*|\1Sonic Mania: D.T|' $A/src/main/res/values/strings.xml
J=dependencies/RSDKv5/android
ln -sfn ../../../../.. $J/app/jni/Game
sed -i 's/-Xmx1024M/-Xmx4096M/g' $J/gradle.properties
D=dependencies/RSDKv5/dependencies/android
rm -rf $D/libogg $D/libtheora
mkdir -p $D/libogg $D/libtheora
curl -fsSL https://downloads.xiph.org/releases/ogg/libogg-1.3.5.tar.gz | tar xz -C $D/libogg --strip-components=1
curl -fsSL https://downloads.xiph.org/releases/theora/libtheora-1.1.1.tar.gz | tar xz -C $D/libtheora --strip-components=1
