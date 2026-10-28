"""Smoke-test native controls and preference persistence on an attached test device."""
import subprocess, xml.etree.ElementTree as ET, time

def adb(*args):
    return subprocess.check_output(['adb', *args], text=True, timeout=30)
def nodes():
    adb('shell','uiautomator','dump','/sdcard/jelly-ui.xml')
    return list(ET.fromstring(adb('shell','cat','/sdcard/jelly-ui.xml')).iter('node'))
def find(key,value):
    return next(n for n in nodes() if n.get(key)==value)
def bounds(n):
    import re
    return list(map(int,re.findall(r'\d+',n.get('bounds'))))
def tap(n):
    a,b,c,d=bounds(n);adb('shell','input','tap',str((a+c)//2),str((b+d)//2))

tap(find('content-desc','Lilac'))
slider=find('content-desc','Squishiness, firm to extra jelly')
a,b,c,d=bounds(slider)
adb('shell','input','tap',str(int(a+(c-a)*.82)),str((b+d)//2))
g=find('content-desc','Gravity')
if g.get('checked')!='true': tap(g)
tap(find('text','Shake'))
tap(find('text','Reset'))
play=next(n for n in nodes() if n.get('content-desc','').startswith('3D dolphin room'))
a,b,c,d=bounds(play)
x=(a+c)//2;y=int(b+(d-b)*.45)
adb('shell','input','tap',str(x),str(y))
adb('shell','input','swipe',str(x),str(y),str(int(a+(c-a)*.7)),str(int(b+(d-b)*.25)),'350')
adb('shell','am','force-stop','com.jellydolphin.app')
adb('shell','am','start','-W','-n','com.jellydolphin.app/.MainActivity')
root=ET.fromstring(adb('shell','run-as','com.jellydolphin.app','cat','shared_prefs/jelly.xml'))
prefs={el.get('name'):el.get('value') for el in root}
assert prefs['colour']=='2',prefs
assert float(prefs['softness'])>.7,prefs
assert prefs['gravity']=='true',prefs
assert find('content-desc','Lilac, selected').get('selected')=='true'
assert find('content-desc','Gravity').get('checked')=='true'
assert adb('shell','pidof','com.jellydolphin.app').strip()
print('PASS: colour, softness, gravity, shake/reset, touch/drag injection, and relaunch persistence')
