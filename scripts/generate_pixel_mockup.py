#!/usr/bin/env python3
"""
Generator mockupów Google Pixel (Pixel 8 / Pixel 9 Pro).
Automatycznie dodaje brakujący Status Bar (zegar, wycięcie na aparat punch-hole, bateria, wifi)
oraz Gesture Bar (pasek gestów nawigacji).

Użycie:
    python3 scripts/generate_pixel_mockup.py [ścieżka_do_obrazka_lub_katalogu]
"""

import sys
import os
import glob
from PIL import Image, ImageDraw, ImageFont, ImageFilter

def create_pixel_mockup(input_path, output_path, bg_mode="transparent"):
    screen = Image.open(input_path).convert("RGBA")
    sw, sh = screen.size

    top_color = screen.getpixel((sw // 2, 10))
    bottom_color = screen.getpixel((sw // 2, sh - 10))

    top_h = 105
    bot_h = 75

    full_w = sw
    full_h = top_h + sh + bot_h

    display = Image.new("RGBA", (full_w, full_h), top_color)
    display.paste(screen, (0, top_h))

    bot_bg = Image.new("RGBA", (full_w, bot_h), bottom_color)
    display.paste(bot_bg, (0, top_h + sh))

    draw_disp = ImageDraw.Draw(display)

    is_dark = (top_color[0]*0.299 + top_color[1]*0.587 + top_color[2]*0.114) < 140
    icon_color = (245, 245, 245, 255) if is_dark else (20, 20, 22, 255)

    # Aparat (Punch-hole)
    cam_cx = full_w // 2
    cam_cy = top_h // 2 + 2
    cam_r = 19
    draw_disp.ellipse([cam_cx - cam_r, cam_cy - cam_r, cam_cx + cam_r, cam_cy + cam_r], 
                      fill=(5, 5, 8, 255), outline=(35, 35, 38, 255), width=2)
    draw_disp.ellipse([cam_cx - 6, cam_cy - 6, cam_cx - 2, cam_cy - 2], fill=(25, 25, 40, 180))

    # Zegar
    try:
        font_time = ImageFont.truetype("/usr/share/fonts/inter/Inter-Medium.otf", 38)
    except:
        font_time = ImageFont.load_default()
    draw_disp.text((64, 32), "9:41", font=font_time, fill=icon_color)

    # Bateria
    bx, by = full_w - 112, 38
    draw_disp.rounded_rectangle([bx, by, bx + 50, by + 26], radius=7, outline=icon_color, width=3)
    draw_disp.rectangle([bx + 50, by + 8, bx + 54, by + 18], fill=icon_color)
    draw_disp.rounded_rectangle([bx + 5, by + 5, bx + 38, by + 21], radius=4, fill=icon_color)

    # Zasięg
    sx, sy = full_w - 152, 44
    for i in range(4):
        h = (i + 1) * 5
        draw_disp.rectangle([sx + i * 7, sy + 18 - h, sx + i * 7 + 4, sy + 18], fill=icon_color)

    # Wi-Fi
    wx, wy = full_w - 204, 38
    draw_disp.arc([wx, wy, wx + 30, wy + 30], 200, 340, fill=icon_color, width=3)
    draw_disp.arc([wx + 5, wy + 7, wx + 25, wy + 26], 200, 340, fill=icon_color, width=3)
    draw_disp.ellipse([wx + 12, wy + 19, wx + 18, wy + 25], fill=icon_color)

    # Pasek nawigacji gestami (Gesture Pill)
    pill_w = 340
    pill_h = 10
    pill_x = (full_w - pill_w) // 2
    pill_y = top_h + sh + (bot_h - pill_h) // 2
    pill_color = (230, 230, 230, 220) if is_dark else (40, 40, 40, 220)
    draw_disp.rounded_rectangle([pill_x, pill_y, pill_x + pill_w, pill_y + pill_h], radius=5, fill=pill_color)

    # Zaokrąglenie ekranu
    screen_radius = 82
    mask = Image.new("L", (full_w, full_h), 0)
    mask_draw = ImageDraw.Draw(mask)
    mask_draw.rounded_rectangle([0, 0, full_w, full_h], radius=screen_radius, fill=255)

    # Obudowa
    bezel = 30
    body_w = full_w + bezel * 2
    body_h = full_h + bezel * 2
    body_radius = screen_radius + bezel

    pad = 120
    canvas_w = body_w + pad * 2
    canvas_h = body_h + pad * 2

    canvas = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))

    if bg_mode == "gradient":
        grad = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
        g_draw = ImageDraw.Draw(grad)
        for y in range(canvas_h):
            ratio = y / canvas_h
            r = int(22 + (35 - 22) * ratio)
            g = int(24 + (32 - 24) * ratio)
            b = int(32 + (50 - 32) * ratio)
            g_draw.line([(0, y), (canvas_w, y)], fill=(r, g, b, 255))
        canvas.paste(grad, (0, 0))

    # Cień
    shadow = Image.new("RGBA", (canvas_w, canvas_h), (0, 0, 0, 0))
    sh_draw = ImageDraw.Draw(shadow)
    sh_draw.rounded_rectangle([pad, pad + 20, pad + body_w, pad + body_h + 20], radius=body_radius, fill=(0, 0, 0, 125))
    shadow = shadow.filter(ImageFilter.GaussianBlur(38))
    canvas.paste(shadow, (0, 0), shadow)

    # Ramka telefonu
    frame = Image.new("RGBA", (body_w, body_h), (0, 0, 0, 0))
    f_draw = ImageDraw.Draw(frame)
    f_draw.rounded_rectangle([0, 0, body_w, body_h], radius=body_radius, fill=(36, 38, 42, 255), outline=(78, 80, 88, 255), width=4)
    f_draw.rounded_rectangle([4, 4, body_w - 4, body_h - 4], radius=body_radius - 4, fill=(14, 14, 16, 255))

    canvas.paste(frame, (pad, pad), frame)
    canvas.paste(display, (pad + bezel, pad + bezel), mask)

    scale = 1600 / canvas_h
    out_w = int(canvas_w * scale)
    out_h = 1600
    final_img = canvas.resize((out_w, out_h), Image.Resampling.LANCZOS)
    
    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
    final_img.save(output_path, "PNG", optimize=True)

if __name__ == "__main__":
    target = sys.argv[1] if len(sys.argv) > 1 else "/home/laghert/Pobrane"
    if os.path.isfile(target):
        files = [target]
        out_dir = os.path.dirname(target)
    else:
        files = sorted(glob.glob(os.path.join(target, "Screenshot_*.jpg")) + glob.glob(os.path.join(target, "Screenshot_*.png")))
        out_dir = os.path.join(target, "Pixel_Mockups")

    for f in files:
        base = os.path.splitext(os.path.basename(f))[0]
        out_t = os.path.join(out_dir, f"Pixel9_{base}_transparent.png")
        out_s = os.path.join(out_dir, f"Pixel9_{base}_showcase.png")
        create_pixel_mockup(f, out_t, "transparent")
        create_pixel_mockup(f, out_s, "gradient")
        print(f"Wygenerowano: {out_t}")
