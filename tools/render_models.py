"""Renders the GeckoLib entity models to preview PNGs (rest pose, front and back three-quarter views).

python tools/render_models.py [names...]  ->  references/mobs/<name>.png and references/mobs/all.png
"""
import json
import math
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/horror_town"
OUT = ROOT / "references/mobs"
MOBS = ["drowned_one", "mud_lurker", "paper_effigy", "fishman", "hanged_one", "salt_husk",
        "tooth_snail", "eye_crab", "leech_eel", "face_fish", "hairless_dog"]
LABELS = {"drowned_one": "水鬼", "mud_lurker": "泥人", "paper_effigy": "纸人", "fishman": "鱼人渔夫",
          "hanged_one": "吊者", "salt_husk": "盐尸", "tooth_snail": "人牙螺", "eye_crab": "独眼蟹",
          "leech_eel": "水蛭鳗", "face_fish": "人面鱼", "hairless_dog": "无毛犬"}
VIEW = 360
BG = (34, 36, 38)


def mat_mul(a, b):
    return [[sum(a[i][k] * b[k][j] for k in range(4)) for j in range(4)] for i in range(4)]


def translate(x, y, z):
    return [[1, 0, 0, x], [0, 1, 0, y], [0, 0, 1, z], [0, 0, 0, 1]]


def rot(axis, deg):
    c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    if axis == 0:
        return [[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]]
    if axis == 1:
        return [[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]]
    return [[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]]


def apply(m, p):
    return tuple(m[i][0] * p[0] + m[i][1] * p[1] + m[i][2] * p[2] + m[i][3] for i in range(3))


def rotation_about(pivot, r):
    """GeckoLib: x mirrored, x/y rotations negated, applied Z then Y then X about the pivot."""
    px, py, pz = -pivot[0], pivot[1], pivot[2]
    m = translate(px, py, pz)
    m = mat_mul(m, rot(2, r[2]))
    m = mat_mul(m, rot(1, -r[1]))
    m = mat_mul(m, rot(0, -r[0]))
    return mat_mul(m, translate(-px, -py, -pz))


# Corners of each face as seen from outside: top-left, top-right, bottom-right, bottom-left (unit cube, x mirrored).
FACES = {
    "north": [(1, 1, 0), (0, 1, 0), (0, 0, 0), (1, 0, 0)],
    "south": [(0, 1, 1), (1, 1, 1), (1, 0, 1), (0, 0, 1)],
    "east": [(0, 1, 0), (0, 1, 1), (0, 0, 1), (0, 0, 0)],
    "west": [(1, 1, 1), (1, 1, 0), (1, 0, 0), (1, 0, 1)],
    "up": [(1, 1, 1), (0, 1, 1), (0, 1, 0), (1, 1, 0)],
    "down": [(1, 0, 0), (0, 0, 0), (0, 0, 1), (1, 0, 1)],
}


def quads(name):
    geo = json.loads((ASSETS / f"geo/entity/{name}.geo.json").read_text())["minecraft:geometry"][0]
    tex = Image.open(ASSETS / f"textures/entity/{name}.png").convert("RGBA")
    sx = tex.width / geo["description"]["texture_width"]
    sy = tex.height / geo["description"]["texture_height"]
    bones = {b["name"]: b for b in geo["bones"]}
    cache = {}

    def world(bname):
        if bname not in cache:
            b = bones[bname]
            m = world(b["parent"]) if "parent" in b else translate(0, 0, 0)
            if "rotation" in b:
                m = mat_mul(m, rotation_about(b["pivot"], b["rotation"]))
            cache[bname] = m
        return cache[bname]

    out = []
    for b in geo["bones"]:
        m = world(b["name"])
        for c in b.get("cubes", []):
            inf = c.get("inflate", 0)
            ox, oy, oz = c["origin"]
            w, h, d = c["size"]
            x0, x1 = -(ox + w) - inf, -ox + inf
            lo = (x0, oy - inf, oz - inf)
            span = (x1 - x0, h + 2 * inf, d + 2 * inf)
            cm = m
            if "rotation" in c:
                cm = mat_mul(m, rotation_about(c.get("pivot", [0, 0, 0]), c["rotation"]))
            for face, corners in FACES.items():
                f = c["uv"].get(face)
                if not f:
                    continue
                pts = [apply(cm, tuple(lo[i] + k[i] * span[i] for i in range(3))) for k in corners]
                u, v = f["uv"]
                uw, vh = f["uv_size"]
                nu, nv = max(1, round(abs(uw))), max(1, round(abs(vh)))
                for i in range(nu):
                    for j in range(nv):
                        px = int((u + (i + 0.5) * uw / nu) * sx)
                        py = int((v + (j + 0.5) * vh / nv) * sy)
                        if not (0 <= px < tex.width and 0 <= py < tex.height):
                            continue
                        col = tex.getpixel((px, py))
                        if col[3] < 10:
                            continue
                        def lerp(a, b_):
                            tl, tr, br, bl = pts
                            top = [tl[k] + (tr[k] - tl[k]) * a for k in range(3)]
                            bot = [bl[k] + (br[k] - bl[k]) * a for k in range(3)]
                            return tuple(top[k] + (bot[k] - top[k]) * b_ for k in range(3))
                        cell = [lerp(i / nu, j / nv), lerp((i + 1) / nu, j / nv),
                                lerp((i + 1) / nu, (j + 1) / nv), lerp(i / nu, (j + 1) / nv)]
                        out.append((cell, col[:3], pts))
    return out


def render(qs, yaw, pitch=-20):
    view = mat_mul(rot(0, pitch), rot(1, yaw))
    light = (0.4, 0.8, -0.45)
    projected = []
    for cell, col, face in qs:
        p = [apply(view, q) for q in cell]
        f = [apply(view, q) for q in face]
        e1 = [f[1][k] - f[0][k] for k in range(3)]
        e2 = [f[3][k] - f[0][k] for k in range(3)]
        n = (e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0])
        ln = math.sqrt(sum(a * a for a in n)) or 1
        if n[2] / ln < -0.02:  # (n points into the cube) facing away
            continue
        shade = 0.55 + 0.45 * max(0.0, -(n[0] * light[0] + n[1] * light[1] + n[2] * light[2]) / ln)
        depth = sum(q[2] for q in p) / 4
        projected.append((depth, [(-q[0], q[1]) for q in p], tuple(int(c * shade) for c in col)))  # camera looks +z
    if not projected:
        return Image.new("RGB", (VIEW, VIEW), BG)
    xs = [x for _, pts, _ in projected for x, _ in pts]
    ys = [y for _, pts, _ in projected for _, y in pts]
    size = max(max(xs) - min(xs), max(ys) - min(ys))
    scale = (VIEW * 0.82) / size
    cx, cy = (max(xs) + min(xs)) / 2, (max(ys) + min(ys)) / 2
    ss = 2
    size_px = VIEW * ss
    img = Image.new("RGB", (size_px, size_px), BG)
    px = img.load()
    zbuf = [[1e9] * size_px for _ in range(size_px)]
    for depth_, pts, col in projected:
        sp = [((x - cx) * scale * ss + size_px / 2, -(y - cy) * scale * ss + size_px / 2) for x, y in pts]
        z = depth_
        xs_, ys_ = [p[0] for p in sp], [p[1] for p in sp]
        x0, x1 = max(0, int(min(xs_))), min(size_px - 1, int(math.ceil(max(xs_))))
        y0, y1 = max(0, int(min(ys_))), min(size_px - 1, int(math.ceil(max(ys_))))
        for tri in ((sp[0], sp[1], sp[2]), (sp[0], sp[2], sp[3])):
            (ax, ay), (bx, by), (qx, qy) = tri
            den = (by - qy) * (ax - qx) + (qx - bx) * (ay - qy)
            if abs(den) < 1e-9:
                continue
            for yy in range(y0, y1 + 1):
                for xx in range(x0, x1 + 1):
                    fx, fy = xx + 0.5, yy + 0.5
                    l1 = ((by - qy) * (fx - qx) + (qx - bx) * (fy - qy)) / den
                    l2 = ((qy - ay) * (fx - qx) + (ax - qx) * (fy - qy)) / den
                    if l1 < -0.01 or l2 < -0.01 or l1 + l2 > 1.01:
                        continue
                    if z < zbuf[yy][xx]:
                        zbuf[yy][xx] = z
                        px[xx, yy] = col
    return img.resize((VIEW, VIEW), Image.LANCZOS)


def font(size):
    for f in ("C:/Windows/Fonts/msyh.ttc", "C:/Windows/Fonts/simhei.ttf"):
        try:
            return ImageFont.truetype(f, size)
        except OSError:
            pass
    return ImageFont.load_default()


def sheet(name):
    qs = quads(name)
    img = Image.new("RGB", (VIEW * 2, VIEW + 44), BG)
    img.paste(render(qs, 215), (0, 44))
    img.paste(render(qs, 35), (VIEW, 44))
    ImageDraw.Draw(img).text((12, 6), f"{LABELS.get(name, name)}  {name}", fill=(220, 220, 210), font=font(26))
    return img


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    names = sys.argv[1:] or MOBS
    sheets = []
    for n in names:
        s = sheet(n)
        s.save(OUT / f"{n}.png")
        sheets.append(s)
        print("rendered", n)
    if len(sheets) > 1:
        cols = 2
        rows = (len(sheets) + cols - 1) // cols
        w, h = sheets[0].size
        allimg = Image.new("RGB", (w * cols, h * rows), BG)
        for i, s in enumerate(sheets):
            allimg.paste(s, ((i % cols) * w, (i // cols) * h))
        allimg.save(OUT / "all.png")


if __name__ == "__main__":
    main()
