import re
p='E:/Du_An/MusicFlow/app/src/androidTest/java/com/example/musicflow/core/playback/RealPickerSafTest.kt'
s=open(p,encoding='utf-8').read()
print('len',len(s))
print('select count', s.count('private fun selectRealDocument'))
print('deadline count', s.count('val deadline = SystemClock.uptimeMillis() + 25_000L'))
