import subprocess, shlex, sys, os
SERIAL=os.environ.get('ADB_SERIAL', '')
def adb(*args, **kw):
    return subprocess.run(['adb',*(['-s',SERIAL] if SERIAL else []),*args],check=True,**kw)
def root(script):
    return adb('shell','su -c '+shlex.quote(script),capture_output=True,text=True).stdout
if __name__=='__main__':
    print(root(sys.argv[1]))
