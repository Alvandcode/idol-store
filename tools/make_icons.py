from PIL import Image, ImageDraw
import os

SRC = 'logo.png'
RES = 'app/src/main/res'
im = Image.open(SRC).convert('RGBA')
print('src size:', im.size)

# نمونه‌رنگ پس‌زمینه تیره از داخل گوشه بالایی چپ artwork
region = im.crop((140, 140, 240, 240))
px = [p for p in region.getdata() if p[3] > 200]
avg = tuple(sum(c[i] for c in px) // len(px) for i in range(3))
bg_color = avg + (255,)
print('bg color: #%02X%02X%02X' % avg)


def save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path, 'PNG')
    print('wrote', path, img.size)


legacy = {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}
fg = {'mdpi': 108, 'hdpi': 162, 'xhdpi': 216, 'xxhdpi': 324, 'xxxhdpi': 432}

# --- آیکون‌های legacy (مربع گرد تیره با گوشه شفاف) ---
for d, s in legacy.items():
    icon = im.resize((s, s), Image.LANCZOS)
    save(icon, f'{RES}/mipmap-{d}/ic_launcher.png')
    # round: برش دایره‌ای
    r = im.resize((s * 2, s * 2), Image.LANCZOS)
    mask = Image.new('L', (s * 2, s * 2), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, s * 2 - 1, s * 2 - 1), fill=255)
    round_icon = Image.new('RGBA', (s * 2, s * 2), (0, 0, 0, 0))
    round_icon.paste(r, (0, 0), mask)
    save(round_icon.resize((s, s), Image.LANCZOS), f'{RES}/mipmap-{d}/ic_launcher_round.png')

# --- لایه‌های adaptive ---
# foreground: کل لوگو در 78% بوم (لبه مربعِ baked بیرون از محدوده ماسک می‌ماند)
for d, s in fg.items():
    fg_img = Image.new('RGBA', (s, s), (0, 0, 0, 0))
    scaled = im.resize((int(s * 0.78),) * 2, Image.LANCZOS)
    off = (s - scaled.size[0]) // 2
    fg_img.paste(scaled, (off, off), scaled)
    save(fg_img, f'{RES}/mipmap-{d}/ic_launcher_foreground.png')
    # background: پرشده با رنگ تیره نمونه‌گیری‌شده
    bg = Image.new('RGBA', (s, s), bg_color)
    save(bg, f'{RES}/mipmap-{d}/ic_launcher_background.png')

# --- آیکون داخل اپ (صفحه ورود) ---
save(im.resize((512, 512), Image.LANCZOS), f'{RES}/drawable-nodpi/ic_logo.png')
print('DONE')
