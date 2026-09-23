#!/usr/bin/env python3
"""Draw stand-in PNGs so the overlay is visible before real art is added."""

import struct
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "assets"
W, H = 960, 640
OUTLINE = (92, 62, 54, 255)
FUR = (244, 216, 184, 255)
FUR_DARK = (226, 186, 146, 255)
EAR = (232, 156, 166, 255)
BLUSH = (232, 150, 156, 150)
NOSE = (214, 112, 124, 255)
WHITE = (255, 252, 246, 255)
PUPIL = (42, 36, 34, 255)
MOUTH = (126, 58, 68, 255)
MOUTH_DARK = (72, 28, 36, 255)
HAIR = (198, 124, 76, 255)
HAIR_DARK = (154, 86, 50, 255)
BOARD = (64, 68, 76, 255)
KEY = (214, 218, 224, 255)
MOUSE = (78, 82, 90, 255)
PAD = (214, 112, 124, 255)


class Canvas:
    def __init__(self, width, height):
        self.w = width
        self.h = height
        self.px = bytearray(width * height * 4)

    def put(self, x, y, color):
        if not (0 <= x < self.w and 0 <= y < self.h):
            return
        i = (y * self.w + x) * 4
        sr, sg, sb, sa = color
        if sa >= 255:
            self.px[i:i + 4] = bytes((sr, sg, sb, 255))
            return
        if sa <= 0:
            return
        dr, dg, db, da = self.px[i:i + 4]
        a = sa / 255.0
        inv = 1.0 - a
        self.px[i] = int(sr * a + dr * inv)
        self.px[i + 1] = int(sg * a + dg * inv)
        self.px[i + 2] = int(sb * a + db * inv)
        self.px[i + 3] = int(min(255, sa + da * inv))

    def ellipse(self, cx, cy, rx, ry, color):
        if rx <= 0 or ry <= 0:
            return
        x0 = max(0, int(cx - rx - 1))
        x1 = min(self.w - 1, int(cx + rx + 1))
        y0 = max(0, int(cy - ry - 1))
        y1 = min(self.h - 1, int(cy + ry + 1))
        for y in range(y0, y1 + 1):
            dy = (y - cy) / ry
            span = 1 - dy * dy
            if span < 0:
                continue
            half = (span ** 0.5) * rx
            left = max(x0, int(cx - half))
            right = min(x1, int(cx + half))
            for x in range(left, right + 1):
                self.put(x, y, color)

    def blob(self, cx, cy, rx, ry, fill, outline=OUTLINE, width=5):
        if outline is not None:
            self.ellipse(cx, cy, rx, ry, outline)
            self.ellipse(cx, cy, max(1, rx - width), max(1, ry - width), fill)
        else:
            self.ellipse(cx, cy, rx, ry, fill)

    def rect(self, x, y, w, h, color):
        x0 = max(0, int(x))
        y0 = max(0, int(y))
        x1 = min(self.w, int(x + w))
        y1 = min(self.h, int(y + h))
        for yy in range(y0, y1):
            for xx in range(x0, x1):
                self.put(xx, yy, color)

    def save(self, path):
        raw = bytearray()
        row = self.w * 4
        for y in range(self.h):
            raw.append(0)
            raw.extend(self.px[y * row:(y + 1) * row])

        def chunk(tag, data):
            return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

        ihdr = struct.pack(">IIBBBBB", self.w, self.h, 8, 6, 0, 0, 0)
        png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b"")
        path = Path(path)
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_bytes(png)


def body():
    canvas = Canvas(W, H)
    canvas.rect(180, 420, 580, 180, BOARD)
    for row in range(5):
        for col in range(14):
            canvas.rect(192 + col * 40, 432 + row * 32, 32, 24, KEY)
    canvas.blob(250, 150, 54, 70, FUR)
    canvas.blob(430, 150, 54, 70, FUR)
    canvas.ellipse(250, 168, 24, 34, EAR)
    canvas.ellipse(430, 168, 24, 34, EAR)
    canvas.blob(360, 390, 210, 120, FUR)
    canvas.blob(340, 230, 168, 132, FUR)
    canvas.ellipse(270, 250, 28, 16, BLUSH)
    canvas.ellipse(410, 250, 28, 16, BLUSH)
    canvas.blob(340, 246, 18, 12, NOSE, width=3)
    canvas.save(ROOT / "body.png")


def bangs(shift_x, shift_y, path):
    canvas = Canvas(W, H)
    spots = ((292, 132), (340, 118), (392, 134))
    for cx, cy in spots:
        canvas.blob(cx + shift_x, cy + shift_y, 38, 52, HAIR, HAIR_DARK, 4)
        canvas.ellipse(cx + shift_x, cy + shift_y + 16, 16, 22, HAIR)
    canvas.save(path)


def eyes(kind, path):
    canvas = Canvas(W, H)
    centers = ((286, 214), (396, 214))
    for cx, cy in centers:
        if kind == "open":
            canvas.blob(cx, cy, 30, 34, WHITE, width=4)
            canvas.ellipse(cx + 2, cy + 2, 13, 16, PUPIL)
            canvas.ellipse(cx + 8, cy - 8, 6, 6, WHITE)
        elif kind == "half":
            canvas.blob(cx, cy, 30, 14, WHITE, width=4)
            canvas.ellipse(cx + 2, cy + 1, 12, 6, PUPIL)
        else:
            canvas.blob(cx, cy + 4, 30, 6, OUTLINE, outline=None)
    canvas.save(path)


def mouth(kind, path):
    canvas = Canvas(W, H)
    if kind == "closed":
        canvas.blob(340, 286, 26, 6, MOUTH, outline=None)
    elif kind == "half":
        canvas.blob(340, 292, 20, 14, MOUTH_DARK, MOUTH, 4)
    else:
        canvas.blob(340, 300, 28, 26, MOUTH_DARK, MOUTH, 4)
        canvas.ellipse(340, 292, 12, 6, (230, 160, 166, 255))
    canvas.save(path)


def mouse(kind, path):
    canvas = Canvas(W, H)
    canvas.blob(850, 500, 48, 62, MOUSE, width=4)
    canvas.rect(846, 456, 4, 70, OUTLINE)
    if kind == "idle":
        canvas.blob(850, 430, 36, 28, FUR, width=4)
        canvas.ellipse(838, 444, 8, 6, PAD)
        canvas.ellipse(862, 444, 8, 6, PAD)
    elif kind == "left":
        canvas.blob(830, 468, 30, 24, FUR, width=4)
        canvas.ellipse(824, 476, 9, 7, PAD)
    else:
        canvas.blob(870, 468, 30, 24, FUR, width=4)
        canvas.ellipse(876, 476, 9, 7, PAD)
    canvas.save(path)


def paw(kind, path):
    canvas = Canvas(170, 160)
    if kind == "idle":
        canvas.blob(85, 78, 48, 58, FUR, width=4)
        canvas.ellipse(68, 96, 12, 9, PAD)
        canvas.ellipse(102, 96, 12, 9, PAD)
        canvas.ellipse(85, 112, 14, 10, PAD)
    elif kind == "raised":
        canvas.blob(85, 70, 46, 42, FUR, width=4)
        canvas.ellipse(62, 58, 14, 16, FUR)
        canvas.ellipse(108, 58, 14, 16, FUR)
        canvas.ellipse(85, 46, 14, 16, FUR)
        canvas.ellipse(70, 86, 10, 8, PAD)
        canvas.ellipse(100, 86, 10, 8, PAD)
        canvas.blob(85, 132, 16, 22, FUR, width=3)
    else:
        canvas.blob(85, 118, 62, 28, FUR, width=4)
        canvas.ellipse(48, 104, 16, 14, FUR)
        canvas.ellipse(122, 104, 16, 14, FUR)
        canvas.ellipse(85, 96, 16, 14, FUR)
        canvas.ellipse(64, 124, 10, 8, PAD)
        canvas.ellipse(106, 124, 10, 8, PAD)
        canvas.ellipse(85, 136, 12, 8, PAD)
    canvas.save(path)


def main():
    body()
    bangs(0, 0, ROOT / "bangs" / "1.png")
    bangs(-16, -10, ROOT / "bangs" / "2.png")
    bangs(18, 8, ROOT / "bangs" / "3.png")
    eyes("open", ROOT / "eyes" / "open.png")
    eyes("half", ROOT / "eyes" / "half.png")
    eyes("closed", ROOT / "eyes" / "closed.png")
    mouth("closed", ROOT / "mouth" / "closed.png")
    mouth("half", ROOT / "mouth" / "half.png")
    mouth("open", ROOT / "mouth" / "open.png")
    mouse("idle", ROOT / "mouse" / "idle.png")
    mouse("left", ROOT / "mouse" / "left.png")
    mouse("right", ROOT / "mouse" / "right.png")
    paw("idle", ROOT / "hand" / "idle.png")
    paw("raised", ROOT / "hand" / "raised.png")
    paw("press", ROOT / "hand" / "press.png")
    print("placeholders written to", ROOT)


if __name__ == "__main__":
    main()
