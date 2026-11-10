import sys
p = 'dependencies/RSDKv5/android/app/src/main/java/org/rems/rsdkv5/DTOverlay.java'
s = open(p, encoding='utf-8').read()
pairs = [
    ('    private int al(int a) {\n        return (int) (a * OPAC[opIdx]);\n    }',
     '    private int al(int a) {\n        return opIdx == 2 ? 255 : (int) (a * OPAC[opIdx]);\n    }'),
    ('opIdx = prefs.getInt("op", 1);', 'opIdx = prefs.getInt("op2", 2);'),
    ('.putInt("op", opIdx)', '.putInt("op2", opIdx)'),
]
for a, b in pairs:
    if a not in s:
        sys.exit('patch_vis5: nao achei: ' + a[:50])
    s = s.replace(a, b, 1)
open(p, 'w', encoding='utf-8').write(s)
print('vis5 ok')
