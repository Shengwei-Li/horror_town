"""Turns a PixelLab reference (64x64 on a flat background) into a 16x16 item icon: knocks out the background
from the corners, crops to the subject, shrinks it keeping hard pixels, and darkens the outer edge to an outline."""
from collections import deque

from PIL import Image


def knock_out(img, tolerance=40):
    img = img.convert("RGBA")
    px = img.load()
    w, h = img.size
    seen = set()
    for corner in ((0, 0), (w - 1, 0), (0, h - 1), (w - 1, h - 1)):
        bg = px[corner]
        queue = deque([corner])
        while queue:
            x, y = queue.popleft()
            if (x, y) in seen or not (0 <= x < w and 0 <= y < h):
                continue
            c = px[x, y]
            if sum(abs(c[k] - bg[k]) for k in range(3)) > tolerance:
                continue
            seen.add((x, y))
            px[x, y] = (0, 0, 0, 0)
            queue.extend(((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1)))
    return img


def shrink(img, size=16, fill=0.95):
    box = img.getbbox()
    img = img.crop(box)
    w, h = img.size
    scale = size * fill / max(w, h)
    nw, nh = max(1, round(w * scale)), max(1, round(h * scale))
    small = img.resize((nw, nh), Image.BOX)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.alpha_composite(small, ((size - nw) // 2, (size - nh) // 2))
    px = out.load()
    for y in range(size):
        for x in range(size):
            r, g, b, a = px[x, y]
            px[x, y] = (r, g, b, 255) if a >= 110 else (0, 0, 0, 0)
    # outline: opaque pixels touching transparency get darker
    edge = []
    for y in range(size):
        for x in range(size):
            if px[x, y][3] and any(not (0 <= x + dx < size and 0 <= y + dy < size) or px[x + dx, y + dy][3] == 0
                                   for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                edge.append((x, y))
    for x, y in edge:
        r, g, b, a = px[x, y]
        px[x, y] = (int(r * 0.55), int(g * 0.55), int(b * 0.55), 255)
    return out


def icon_from(ref_path, out_path, tolerance=40):
    shrink(knock_out(Image.open(ref_path), tolerance)).save(out_path)


def tile_from(ref_path, out_path, inset=0.12):
    """A block texture from a reference of a bevelled block: the middle of it, shrunk to 16x16."""
    img = Image.open(ref_path).convert("RGBA")
    w, h = img.size
    m = int(w * inset)
    img.crop((m, m, w - m, h - m)).resize((16, 16), Image.BOX).convert("RGB").convert("RGBA").save(out_path)
