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
for t in 1 2 3 4 5; do curl -fsSL -o /tmp/dl.tgz https://downloads.xiph.org/releases/ogg/libogg-1.3.5.tar.gz && tar xzf /tmp/dl.tgz -C $D/libogg --strip-components=1 && break; sleep 5; done
for t in 1 2 3 4 5; do curl -fsSL -o /tmp/dl.tgz https://downloads.xiph.org/releases/theora/libtheora-1.1.1.tar.gz && tar xzf /tmp/dl.tgz -C $D/libtheora --strip-components=1 && break; sleep 5; done
cat > $D/libogg/include/ogg/config_types.h << 'EOT'
#ifndef __CONFIG_TYPES_H__
#define __CONFIG_TYPES_H__
#include <inttypes.h>
#include <stdint.h>
#include <sys/types.h>
typedef int16_t ogg_int16_t;
typedef uint16_t ogg_uint16_t;
typedef int32_t ogg_int32_t;
typedef uint32_t ogg_uint32_t;
typedef int64_t ogg_int64_t;
typedef uint64_t ogg_uint64_t;
#endif
EOT
S=dependencies/RSDKv5/android/app/src/main
cp patches/Launcher.java $S/java/org/rems/rsdkv5/Launcher.java
python3 scripts/patch_rsdk.py
mkdir -p $S/assets
cp -r patches/mods $S/assets/mods
cp -r "$S/assets/mods/Shaders/Data/Shaders/GL3" "$S/assets/mods/Shaders/Data/Shaders/OGL"
sed -i 's/IntegerGeorge802/Playerzin13MV/g' dependencies/RSDKv5/RSDKv5/RSDK/User/Dummy/DummyStorage.cpp
test -f $D/libogg/src/bitwise.c && test -f $D/libtheora/lib/state.c
cp patches/DTOverlay.java $S/java/org/rems/rsdkv5/DTOverlay.java
python3 scripts/patch_overlay.py
rm -rf "$S/assets/mods/New Mania Touch Controls"
python3 -m pip install -q pillow
python3 scripts/make_icon.py
cp patches/DTMenu.java $S/java/org/rems/rsdkv5/DTMenu.java
python3 scripts/patch_overlay2.py
sed -i 's/pill(c, modsR, "MODS", false)/pill(c, modsR, "MENU", false)/' $S/java/org/rems/rsdkv5/DTOverlay.java
python3 scripts/make_skin.py $S/assets/mods
python3 scripts/patch_overlay3.py
python3 scripts/patch_menu3.py
