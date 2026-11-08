#!/bin/bash
set -e
A=dependencies/RSDKv5/android/app
sed -i 's/applicationId "[^"]*"/applicationId "com.sonicmaniadt.app"/' $A/build.gradle
sed -i '/signingConfig signingConfigs.release/d' $A/build.gradle
sed -i 's|\(<string name="app_name">\)[^<]*|\1Sonic Mania: D.T|' $A/src/main/res/values/strings.xml
J=dependencies/RSDKv5/android
ln -sfn ../../../../.. $J/app/jni/Game
sed -i 's/-Xmx1024M/-Xmx4096M/g' $J/gradle.properties
sed -i 's|"-DANDROID_STL=c++_shared",|"-DANDROID_STL=c++_shared", "-DMANIA_PREPLUS=ON",|' $J/app/build.gradle
