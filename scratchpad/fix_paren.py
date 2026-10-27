p='E:/Du_An/MusicFlow/app/src/androidTest/java/com/example/musicflow/core/playback/RealPickerSafTest.kt'
s=open(p,encoding='utf-8').read()
import re
m=re.search(r'it\.trim\(\)\.equals\("G.*?y", true\)', s)
print(repr(m.group(0)) if m else 'none')
