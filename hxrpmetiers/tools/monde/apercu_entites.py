import os, sys
from PIL import Image, ImageDraw
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import entites
ms = entites.especes()
want = sys.argv[1:]
if want: ms = [m for m in ms if m.id in want]
cell = 220
img = Image.new('RGBA', (cell * 4, len(ms) * (cell // 1) ), (150, 180, 210, 255))
d = ImageDraw.Draw(img)
for k, m in enumerate(ms):
    tex = m.texture()
    a = entites.rendu(m, tex, cell, yaw=35, pitch=20)
    b = entites.rendu(m, tex, cell, yaw=-120, pitch=15)
    img.alpha_composite(a, (0, k * cell)); img.alpha_composite(b, (cell, k * cell))
    t = tex.resize((tex.width * 3, tex.height * 3), Image.NEAREST)
    bg = Image.new('RGBA', t.size, (60, 60, 60, 255)); bg.alpha_composite(t)
    img.paste(bg.crop((0, 0, min(bg.width, cell * 2), min(bg.height, cell))), (cell * 2, k * cell))
    d.text((4, k * cell + 4), m.id, fill=(0, 0, 0, 255))
img.save(os.path.join(os.path.dirname(__file__), '../preview/out/animaux.png'))
