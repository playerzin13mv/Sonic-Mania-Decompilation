import re, sys
p = 'dependencies/RSDKv5/android/app/src/main/java/org/rems/rsdkv5/RSDK.java'
s = open(p, encoding='utf-8').read()

def sub(pat, new):
    global s
    n, c = re.subn(pat, lambda m: new, s, count=1, flags=re.S)
    if c != 1: sys.exit('patch falhou: ' + pat[:50])
    s = n

def rep(a, b):
    global s
    if a not in s: sys.exit('patch falhou: ' + a[:50])
    s = s.replace(a, b, 1)

sub(r'        if \(basePath == null\) \{.*?Uri\.encode\("/"\);\n',
'''        if (baseDir == null) {
            String bp = getIntent().getStringExtra("basePath");
            baseDir = new java.io.File(bp != null ? bp : Launcher.defaultBase(this).getAbsolutePath());
        }
''')
rep('        cpc = getContentResolver().acquireContentProviderClient(basePath.getAuthority());\n', '')
sub(r'        try \{\n            ParcelFileDescriptor\.adoptFd.*?catch \(FileNotFoundException e\) \{\n            e\.printStackTrace\(\);\n        \}\n',
'''        try {
            if (log == null)
                log = new java.io.FileOutputStream(new java.io.File(baseDir, "log.txt"), false);
        } catch (IOException e) {
            e.printStackTrace();
        }
''')
rep('private static String pathString = null;',
    'private static String pathString = null;\n    private static java.io.File baseDir = null;')
rep('public int getFD(byte[] file, byte mode)', 'public int oldGetFD(byte[] file, byte mode)')
rep('public boolean fsExists(byte[] path)', 'public boolean oldFsExists(byte[] path)')
rep('public boolean fsIsDir(byte[] path)', 'public boolean oldFsIsDir(byte[] path)')
rep('public String[] fsDirIter(byte[] path)', 'public String[] oldFsDirIter(byte[] path)')
rep('public String fsRecurseIter(byte[] path)', 'public String oldFsRecurseIter(byte[] path)')

new_methods = '''    public int getFD(byte[] file, byte mode) {
        try {
            java.io.File f = new java.io.File(baseDir, new String(file));
            int m;
            if (mode == 'r') {
                m = ParcelFileDescriptor.MODE_READ_ONLY;
            } else {
                java.io.File par = f.getParentFile();
                if (par != null && !par.exists()) par.mkdirs();
                m = ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE;
                m |= (mode == 'a') ? ParcelFileDescriptor.MODE_APPEND : ParcelFileDescriptor.MODE_TRUNCATE;
            }
            return ParcelFileDescriptor.open(f, m).detachFd();
        } catch (Exception e) {
            return 0;
        }
    }

    public boolean fsExists(byte[] path) {
        return new java.io.File(baseDir, new String(path)).exists();
    }

    public boolean fsIsDir(byte[] path) {
        return new java.io.File(baseDir, new String(path)).isDirectory();
    }

    public String[] fsDirIter(byte[] path) {
        String p = new String(path);
        java.io.File[] list = new java.io.File(baseDir, p).listFiles();
        List<String> out = new ArrayList<String>();
        if (list != null)
            for (java.io.File f : list)
                if (f.isDirectory()) out.add(p + "/" + f.getName());
        return out.toArray(new String[0]);
    }

    private static final HashMap<String, java.util.ArrayDeque<java.io.File>> recIters = new HashMap<>();

    public String fsRecurseIter(byte[] path) {
        String p = new String(path);
        java.util.ArrayDeque<java.io.File> q = recIters.get(p);
        if (q == null) {
            q = new java.util.ArrayDeque<>();
            q.add(new java.io.File(baseDir, p));
            recIters.put(p, q);
        }
        while (!q.isEmpty()) {
            java.io.File f = q.poll();
            if (f.isDirectory()) {
                java.io.File[] c = f.listFiles();
                if (c != null) for (java.io.File x : c) q.add(x);
            } else if (f.isFile()) {
                return f.getAbsolutePath().substring(baseDir.getAbsolutePath().length() + 1);
            }
        }
        recIters.remove(p);
        return null;
    }

'''
rep('    public void showLoadingIcon() {', new_methods + '    public void showLoadingIcon() {')
open(p, 'w', encoding='utf-8').write(s)
print('RSDK.java ok')
