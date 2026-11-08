import sys
b = 'dependencies/RSDKv5/'
p = b + 'android/app/src/main/java/org/rems/rsdkv5/RSDK.java'
s = open(p, encoding='utf-8').read()
a = 'public static native void nativeOnTouch(int fingerID, int action, float x, float y);'
m = '    public void showLoadingIcon() {'
if a not in s or m not in s:
    sys.exit('patch_overlay: ancora nao encontrada no RSDK.java')
s = s.replace(a, a + '\n    public static native void nativeSetKey(int vk, boolean down);', 1)
s = s.replace(m, '''    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        try {
            DTOverlay.attach(this, baseDir);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

''' + m, 1)
open(p, 'w', encoding='utf-8').write(s)

q = b + 'dependencies/android/androidHelpers.cpp'
c = open(q, encoding='utf-8').read()
if 'nativeSetKey' not in c:
    c += '''

extern "C" JNIEXPORT void JNICALL jnifunc(nativeSetKey, RSDK, jint vk, jboolean down)
{
    if (down)
        SKU::UpdateKeyState((int32)vk);
    else
        SKU::ClearKeyState((int32)vk);
}
'''
open(q, 'w', encoding='utf-8').write(c)
print('overlay ok')
