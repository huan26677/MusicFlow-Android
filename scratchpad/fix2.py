p='E:/Du_An/MusicFlow/app/src/androidTest/java/com/example/musicflow/core/playback/RealPickerSafTest.kt'
s=open(p,encoding='utf-8').read()
old = '''                clickNode(findNode(root) {
                    it.trim().equals("Recent", true) ||
                        it.trim().equals("Gáº§n Ä‘Ã¢y", true)
                })'''
new = '''                clickNode(findNode(root) {
                    it.trim().equals("Recent", true) ||
                        it.trim().equals("Gáº§n Ä‘Ã¢y", true)
                }))'''
assert old in s, 'old block not found'
s = s.replace(old, new, 1)
open(p,'w',encoding='utf-8').write(s)
print('done')
