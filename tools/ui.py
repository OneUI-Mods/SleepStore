"""Print visible Android UI labels for local, authorized device verification."""
import xml.etree.ElementTree as ET
from pathlib import Path
from device import adb

def dump(output=None):
    result=adb('shell','uiautomator dump /data/local/tmp/sleepstore-view.xml >/dev/null && cat /data/local/tmp/sleepstore-view.xml',capture_output=True,text=True)
    xml=result.stdout[result.stdout.index('<?xml'):]
    if output: Path(output).write_text(xml)
    for node in ET.fromstring(xml).iter('node'):
        label=node.get('text') or node.get('content-desc')
        if label: print(label, node.get('bounds'))
if __name__=='__main__':
    import sys
    dump(sys.argv[1] if len(sys.argv)>1 else None)
