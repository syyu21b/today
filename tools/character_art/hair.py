"""Hair styles: back layer (behind the body) and front layer (over the face).

Styles follow 2025-26 Korean salon trends: for women the hush cut, long layers with curtain bangs, the
build perm (C-curl ends framing the face), Elizabeth waves, the hime cut, the tassel bob, the pixie;
for men the soft two-block, comma hair, a 6:4 parted perm, curtain (센터 가르마) hair, the leaf cut, a
textured crop, the wolf cut and a natural slick-back.

Hair is built from locks (tapered clumps) so it reads as strands. Every lock casts a soft shadow on the
forehead, gets a gradient, a darker edge and a strand line; the back hair has a darker inner layer so
the head sits inside it. Coordinates are in the head art's own space (face 68 wide, eyes at y = 84,
chin ~115; shoulders at y ~156, chest ~206); warp.py scales the head onto the body.
"""
import math, re
from geo import *

HAIR = "lin:hair:hairS:100:20:100:84"
NUM = re.compile(r"-?\d+(?:\.\d+)?")


def shifted(d, dx, dy):
    """The same path moved by (dx, dy) (paths are absolute x,y pairs)."""
    vals = [float(v) for v in NUM.findall(d)]
    it = iter([v + (dx if i % 2 == 0 else dy) for i, v in enumerate(vals)])
    return NUM.sub(lambda m: f(next(it)), d)


def crown_y(x):
    """Height of the crown surface above x (roots of the bangs sit just below it)."""
    t = min(1.0, abs(x - CX) / 44)
    return 9 + 34 * (1 - math.sqrt(max(0.0, 1 - t * t)))


def lock(root, tip, w, bend=0.0, fill=None, line=True, alpha=1.0, shadow=True, curl=0.0):
    """A clump of hair from a root to a tip, in any direction. [bend] bows it sideways (x), [curl]
    hooks the tip (a C-curl). Only the long edges are outlined so roots melt into the crown;
    [shadow] lays a soft shade on the skin under it."""
    x0, y0 = root
    x1, y1 = tip
    ln = max(1.0, math.hypot(x1 - x0, y1 - y0))
    ux, uy = (x1 - x0) / ln, (y1 - y0) / ln
    nx, ny = -uy, ux

    def P(a, b, k=0.0):
        return (x0 + ux * a * ln - nx * b + bend * k, y0 + uy * a * ln - ny * b)
    tipp = (x1 - nx * curl * w * 0.6, y1 - ny * curl * w * 0.6)
    left, right = P(0, w / 2), P(0, -w / 2)
    edges = [("C", P(0.45, w * 0.62, 0.3), P(0.82, w * 0.36 + curl * w * 0.5, 1), tipp),
             ("C", P(0.78, -w * 0.22 + curl * w * 0.7, 1), P(0.45, -w * 0.6, 0.3), right)]
    d = path(left, edges + [("C", P(-4 / ln, -w * 0.2), P(-4 / ln, w * 0.2), left)])
    out = []
    if shadow:
        out.append(L(shifted(d, 0.6, 3.2), "skinD", alpha=0.18, region="head"))
    out.append(L(d, fill or HAIR, alpha=alpha, region="head"))
    if line:
        out.append(L(path(left, edges, close=False), stroke="hairD", width=0.7, alpha=0.55, region="head"))
        out.append(L(path(P(0.32, -w * 0.06), [("C", P(0.5, 0, 0.5), P(0.66, 0, 0.6), P(0.8, w * 0.04, 0.8))], close=False),
                     stroke="hairD", width=0.5, alpha=0.25, region="head"))
        out.append(L(path(P(0.18, w * 0.18), [("C", P(0.3, w * 0.2, 0.2), P(0.42, w * 0.18, 0.3), P(0.52, w * 0.12, 0.4))], close=False),
                     stroke="hairL", width=1.0, alpha=0.16, region="head"))
    return out


def locks(specs, shadow=True):
    """[(root x, root dy below the crown, tip x, tip y, width, bend[, curl])] -> layers (x relative to CX)."""
    out = []
    for sp in specs:
        rx, rdy, tx_, ty, w, b = sp[:6]
        c = sp[6] if len(sp) > 6 else 0.0
        x = CX + rx
        out += lock((x, crown_y(x) + rdy), (CX + tx_, ty), w, b, shadow=shadow, curl=c)
    return out


def crown(top=9.0, side=43.0, low=60.0, hairline=40.0):
    """Hair over the top of the skull, down behind the temples (front layer, behind the bangs)."""
    return sym((CX, top), [("C", (CX + side * 0.62, top), (CX + side, top + 20), (CX + side, low - 12)),
                           ("C", (CX + side, low - 4), (CX + side - 4, low), (CX + side - 8, low - 3)),
                           ("C", (CX + 26, hairline + 4), (CX + 12, hairline), (CX, hairline))])


def front_cap(**kw):
    c = crown(**kw)
    return [L(c, HAIR, region="head"), L(c, stroke="hairD", width=0.9, alpha=0.6, region="head")]


def part_line(x, y0=10, y1=34):
    return [L(path((x, y0), [("C", (x + 0.5, y0 + 8), (x + 0.5, y1 - 8), (x - 0.4, y1))], close=False), stroke="hairD", width=1.1, alpha=0.65, region="head"),
            L(path((x + 1.6, y0 + 2), [("C", (x + 2, y0 + 8), (x + 2, y1 - 8), (x + 1.6, y1 - 2))], close=False), stroke="skinS", width=1.2, alpha=0.35, region="head")]


def shine(y=27.0, span=37.0, alpha=0.45):
    """Glossy 'angel ring': soft lens-shaped glints along an arc across the crown."""
    out = []
    for a, b in [(-0.92, -0.42), (-0.3, 0.3), (0.42, 0.92)]:
        top, low = [], []
        for i in range(7):
            t = a + (b - a) * i / 6
            x = CX + t * span
            yy = y + (t ** 2) * 13
            th = math.sin(math.pi * i / 6)
            top.append((x, yy - th * 1.5))
            low.append((x, yy + th * 2.4))
        out.append(L(smooth(top + low[::-1], closed=True, t=0.8), "hairL", alpha=alpha, region="head"))
    return out


def back_shape(length, width=46.0, flare=0.0, tips=6, top=8.0, flip=0.0, wave=0.0):
    """Hair behind the head and shoulders: rounded top, sides falling over the shoulders, a locky hem.
    [flip] turns the hem's tips outwards (hush cut), [wave] ripples the sides."""
    side = []
    n = max(2, int((length - 60) / 22))
    for i in range(1, n + 1):
        yy = 60 + (length - 70) * i / n
        side.append((width + flare * i / n + (wave if i % 2 else -wave * 0.6), yy))
    right = [(CX + width * 0.62, top + 3), (CX + width * 0.95, top + 22), (CX + width, 60)] + [(CX + x, y) for x, y in side]
    hem = []
    for i in range(tips + 1):
        t = i / tips
        x = CX + (width + flare + flip) * (1 - 2 * t)
        yy = length + (4 if i % 2 else -3) - abs(0.5 - t) * 12
        hem.append((x, yy))
    pts = [(CX, top)] + right + hem + [mx(p) for p in right[::-1]]
    return smooth(pts, closed=True, t=0.85)


def back_layer(d, bottom, inner_w=34.0):
    """Back hair with a darker inner layer behind the neck."""
    inner = smooth([(CX, 70), (CX + inner_w, 80), (CX + inner_w + 2, min(bottom - 10, 150)), (CX, min(bottom - 4, 160)), (CX - inner_w - 2, min(bottom - 10, 150)), (CX - inner_w, 80)], t=0.9)
    return [L(d, "lin:hairS:hairD:100:30:100:" + f(bottom), region="head"), L(inner, "hairD", alpha=0.55, clip=d, region="head"),
            L(d, stroke="hairD", width=0.9, alpha=0.6, region="head")]


def back_strands(xs, y0, y1, wave=0.0):
    out = []
    for x in xs:
        out.append(L(path((CX + x, y0), [("C", (CX + x * 1.05 + wave, y0 + (y1 - y0) * 0.35), (CX + x * 1.08 - wave, y0 + (y1 - y0) * 0.7), (CX + x * 1.02, y1))], close=False),
                     stroke="hairD", width=0.8, alpha=0.4, region="head"))
        out.append(L(path((CX + x + 3, y0 + 10), [("C", (CX + x * 1.05 + 3 + wave, y0 + (y1 - y0) * 0.3), (CX + x * 1.06 + 3 - wave, y0 + (y1 - y0) * 0.55), (CX + x + 3, y0 + (y1 - y0) * 0.7))], close=False),
                     stroke="hairL", width=1.2, alpha=0.22, region="head"))
    return out


def framing(s, top, bottom, w, x=37.0, bend=-2.0, curl=0.0, flip=0.0):
    """A face-framing lock from the temple down past the cheek ([flip] kicks the tip outwards)."""
    return lock((CX + s * x, top), (CX + s * (x - 3 + flip), bottom), w, bend=s * bend, curl=s * curl, shadow=True)


def wavy_lock(s, x, top, bottom, w, amp=4.0, n=3):
    """An S-waved lock hanging at x (one side of the face)."""
    pts_r, pts_l = [], []
    for i in range(n * 2 + 1):
        t = i / (n * 2)
        yy = top + (bottom - top) * t
        off = math.sin(t * n * math.pi) * amp
        ww = w * (1 - 0.65 * t) / 2
        pts_r.append((CX + s * (x + off + ww), yy))
        pts_l.append((CX + s * (x + off - ww), yy))
    d = smooth(pts_r + [(CX + s * (x + math.sin(n * math.pi) * amp), bottom + 3)] + pts_l[::-1], closed=True, t=0.9)
    return [L(d, HAIR, region="head"), L(d, stroke="hairD", width=0.8, alpha=0.55, region="head"),
            L(path(pts_r[1], [("C", pts_r[2], pts_r[3], pts_r[4])], close=False), stroke="hairL", width=1.0, alpha=0.25, region="head")]


# ------------------------------------------------------------------ bangs
def curtain_bangs(length=70, part=0.0, spread=1.0):
    """Centre-parted bangs curving out to frame the face (커튼뱅)."""
    out = []
    for s in (1, -1):
        out += locks([(part + s * 3, 6, part + s * 22 * spread, length - 10, 11, s * 7, -s * 0.3), (part + s * 9, 6, part + s * 31 * spread, length - 2, 13, s * 6, -s * 0.2),
                      (s * 20, 6, s * 38, length + 6, 12, s * 3)])
    return out


def see_through_bangs(length=64, n=7, w=7.5):
    """Thin, airy bangs with gaps (시스루뱅): forehead shows between the locks."""
    specs = []
    for i in range(n):
        t = (i / (n - 1)) * 2 - 1
        specs.append((t * 24, 8, t * 27 + (1.5 if i % 2 else -1.5), length - abs(t) * 4 + (2 if i % 2 else 0), w, t * 2))
    return locks(specs)


def blunt_bangs(length=64):
    """Full straight-cut bangs (히메컷)."""
    specs = [(-28, 8, -31, length - 2, 14, -1), (28, 8, 31, length - 2, 14, 1), (-17, 6, -18.5, length, 14, 0), (17, 6, 18.5, length, 14, 0),
             (-6, 5, -6.5, length + 0.5, 13, 0), (6, 5, 6.5, length + 0.5, 13, 0)]
    return locks(specs)


def side_bangs(length=66, toward=-1):
    """Long bangs swept to one side (toward = -1: viewer's left)."""
    t = toward
    return locks([(-t * 26, 8, -t * 33, 62, 12, -t * 2), (-t * 12, 6, t * 4, 58, 18, t * 6), (0, 6, t * 20, length - 4, 18, t * 7), (t * 12, 6, t * 31, length + 2, 16, t * 5),
                  (t * 26, 8, t * 38, length + 8, 12, t * 2)])


def side_panel(s, outer, inner, top, bottom):
    """A straight curtain of hair beside the face, cut blunt at [bottom] (bob, hime cut)."""
    d = path((CX + s * inner, top), [("C", (CX + s * (outer - 2), top + 12), (CX + s * outer, top + 40), (CX + s * outer, bottom)), ("L", (CX + s * (inner - 4), bottom)),
                                     ("C", (CX + s * (inner - 2), bottom - 18), (CX + s * (inner - 1), top + 26), (CX + s * (inner - 4), top + 8))])
    out = [L(shifted(d, -s * 0.5, 2), "skinD", alpha=0.15, region="head"), L(d, HAIR, region="head"), L(d, stroke="hairD", width=0.8, alpha=0.6, region="head")]
    for k in (0.35, 0.7):
        x = inner + (outer - inner) * k
        out.append(L(path((CX + s * x, top + 16), [("C", (CX + s * (x + 1.5), top + 40), (CX + s * (x + 2), bottom - 20), (CX + s * (x + 1.5), bottom - 2))], close=False),
                     stroke="hairD", width=0.6, alpha=0.35, region="head"))
    return out


def head_shape(bottom, side=45.0, nape=34.0):
    """Close-cropped back hair (men's cuts, pixie)."""
    return smooth([(CX, 6), (CX + 30, 8), (CX + side, 28), (CX + side - 1, 60), (CX + side - 5, 80), (CX + nape, bottom - 2), (CX, bottom), (CX - nape, bottom - 2),
                   (CX - side + 5, 80), (CX - side + 1, 60), (CX - side, 28), (CX - 30, 8)], closed=True, t=0.8)


# ------------------------------------------------------------------ styles
def style(name):
    """-> (back layers, front layers)"""
    back, front = [], []
    if name == "long":  # long layers + curtain bangs
        d = back_shape(232, 48, flare=4, tips=8)
        back += back_layer(d, 232) + back_strands([30, 40, -30, -40, 12, -12], 96, 222)
        front += front_cap(hairline=42) + part_line(CX)
        front += curtain_bangs(72)
        for s in (1, -1):
            front += framing(s, 52, 150, 11, 38, bend=-3, flip=2)
            front += framing(s, 60, 128, 8, 34, bend=-2, flip=3)
    elif name == "hush":  # 허쉬컷: crown volume, wispy flipped-out layers, air bangs
        d = back_shape(158, 50, flare=6, tips=9, flip=6)
        back += back_layer(d, 158) + back_strands([28, 40, -28, -40], 90, 150)
        front += front_cap(side=45, top=6) + see_through_bangs(64, 6, 8)
        for s in (1, -1):
            front += framing(s, 48, 118, 12, 39, bend=-4, curl=-0.5, flip=7)
            front += framing(s, 70, 146, 10, 43, bend=-2, curl=-0.6, flip=9)
            front += lock((CX + s * 44, 96), (CX + s * 56, 160), 9, bend=s * 2, curl=-s * 0.7, shadow=False)
    elif name == "build":  # 빌드펌 / 허그펌: long, C-curl ends wrapping the face
        d = back_shape(224, 52, flare=8, tips=8, wave=3)
        back += back_layer(d, 224) + back_strands([30, 42, -30, -42], 100, 214, wave=4)
        front += front_cap(hairline=42) + side_bangs(70, -1)
        for s in (1, -1):
            front += lock((CX + s * 38, 54), (CX + s * 32, 132), 14, bend=s * 4, curl=s * 0.9)
            front += lock((CX + s * 44, 96), (CX + s * 40, 196), 15, bend=s * 6, curl=s * 0.9, shadow=False)
    elif name == "wavy":  # 엘리자벳펌: full S-waves
        d = back_shape(230, 52, flare=6, tips=8, wave=5)
        back += back_layer(d, 230) + back_strands([30, 42, -30, -42, 14, -14], 96, 220, wave=5)
        front += front_cap(hairline=42) + side_bangs(68, 1)
        for s in (1, -1):
            front += wavy_lock(s, 39, 52, 170, 9.5, amp=3.2, n=3)
            front += wavy_lock(s, 46, 90, 212, 9, amp=3.6, n=3)
    elif name == "hime":  # 히메컷: blunt bangs, cheek-length side panels, long straight back
        d = back_shape(236, 47, flare=1, tips=10)
        back += back_layer(d, 236) + back_strands([30, 40, -30, -40, 14, -14], 96, 228)
        front += front_cap(hairline=42) + blunt_bangs(64)
        for s in (1, -1):
            front += side_panel(s, 41, 32, 48, 106)
    elif name == "bob":  # 태슬컷 단발: blunt jaw-length bob, see-through bangs
        d = smooth([(CX, 8), (CX + 30, 10), (CX + 47, 30), (CX + 49, 70), (CX + 49, 112), (CX + 47, 126), (CX + 30, 128), (CX, 127), (CX - 30, 128), (CX - 47, 126),
                    (CX - 49, 112), (CX - 49, 70), (CX - 47, 30), (CX - 30, 10)], closed=True, t=0.8)
        back += back_layer(d, 128, 30)
        front += front_cap(side=45, hairline=42) + see_through_bangs(64, 7, 7.5)
        for s in (1, -1):
            front += side_panel(s, 47, 34, 44, 126)
    elif name == "pixie":  # 픽시컷: cropped close, short side-swept fringe, ears free
        back += back_layer(head_shape(90, 45, 34), 90, 28)
        front += front_cap(side=44, top=6, low=58)
        front += locks([(26, 6, 34, 56, 12, 2), (12, 4, 22, 52, 16, 4), (-2, 4, 6, 56, 18, 6), (-16, 4, -10, 60, 18, 6), (-28, 6, -32, 62, 14, 2)])
        for s in (1, -1):
            front += lock((CX + s * 40, 48), (CX + s * 39, 74), 8, bend=s * -1)
    elif name == "ponytail":  # high ponytail with curtain bangs
        tail = smooth([(CX + 28, 18), (CX + 50, 12), (CX + 64, 40), (CX + 64, 96), (CX + 60, 150), (CX + 66, 192), (CX + 54, 224), (CX + 48, 192), (CX + 44, 150),
                       (CX + 46, 98), (CX + 48, 56), (CX + 38, 30)], closed=True, t=0.85)
        back += [L(tail, "lin:hair:hairS:150:20:150:224", region="head"), L(tail, stroke="hairD", width=0.9, alpha=0.6, region="head")]
        back += back_strands([52, 56], 60, 210)
        back += back_layer(back_shape(92, 45, tips=5), 92, 28)
        front += front_cap(hairline=42) + curtain_bangs(68, spread=0.95)
        front.append(L(ell(CX + 34, 22, 4.4, 5.4), "lin:accS:acc:134:17:134:27", region="head"))
        for s in (1, -1):
            front += framing(s, 54, 112, 6, 36, bend=-2, flip=1)
    elif name == "twintail":
        for s in (1, -1):
            tail = smooth([(CX + s * 34, 30), (CX + s * 58, 28), (CX + s * 70, 64), (CX + s * 66, 120), (CX + s * 62, 168), (CX + s * 70, 198), (CX + s * 58, 220),
                           (CX + s * 50, 190), (CX + s * 46, 150), (CX + s * 48, 104), (CX + s * 50, 64), (CX + s * 40, 42)], closed=True, t=0.85)
            back += [L(tail, "lin:hair:hairS:100:30:100:220", region="head"), L(tail, stroke="hairD", width=0.9, alpha=0.6, region="head")]
            back += back_strands([s * 56, s * 60], 70, 200)
        back += back_layer(back_shape(94, 45, tips=5), 94, 28)
        front += front_cap(hairline=42) + see_through_bangs(64, 7, 8)
        for s in (1, -1):
            front.append(L(ell(CX + s * 39, 34, 4.6, 5.4), "lin:accS:acc:100:28:100:40", region="head"))
            front += framing(s, 54, 108, 7, 36, bend=-2, flip=1)
    elif name == "bun":  # messy top bun, loose face-framing strands
        bun = smooth([(CX, -6), (CX + 14, -2), (CX + 17, 10), (CX + 11, 21), (CX, 23), (CX - 11, 21), (CX - 17, 10), (CX - 14, -2)], closed=True, t=0.9)
        back += back_layer(back_shape(92, 45, tips=5), 92, 28)
        front += [L(bun, "lin:hairL:hairS:100:-6:100:23", region="head"), L(bun, stroke="hairD", width=0.9, alpha=0.6, region="head")]
        for (a, b) in [((CX - 12, 2), (CX + 8, 16)), ((CX - 6, -3), (CX + 12, 8)), ((CX - 14, 12), (CX + 4, 21))]:
            front.append(L(path(a, [("C", (a[0] + 6, a[1] - 3), (b[0] - 6, b[1] + 3), b)], close=False), stroke="hairD", width=0.7, alpha=0.45, region="head"))
        front.append(L(path((CX - 15, 18), [("C", (CX - 5, 23), (CX + 5, 23), (CX + 15, 18))], close=False), stroke="acc", width=2.6, region="head"))
        front += front_cap(hairline=42) + see_through_bangs(62, 6, 7)
        for s in (1, -1):
            front += lock((CX + s * 37, 54), (CX + s * 34, 120), 4.5, bend=s * 3, curl=s * 0.6)
    # ---------------- men
    elif name == "short":  # soft two-block: short sides, fuller top, natural fringe to the brows
        back += back_layer(head_shape(92), 92, 28)
        front += front_cap(side=45, top=4, low=56)
        front += locks([(-30, 6, -36, 60, 14, -2), (30, 6, 36, 60, 14, 2), (-18, 4, -22, 66, 15, -2), (18, 4, 20, 66, 15, 2), (-6, 4, -8, 68, 15, -1), (7, 4, 6, 68, 15, 1)])
        for s in (1, -1):
            front += lock((CX + s * 40, 44), (CX + s * 39, 76), 8, bend=s * -1)
    elif name == "comma":  # 쉼표머리: fringe swept to one side, the front lock curling back like a comma
        back += back_layer(head_shape(94), 94, 28)
        front += front_cap(side=45, top=4, low=56)
        front += locks([(34, 6, 38, 58, 14, 2), (24, 4, 14, 58, 20, -5), (12, 4, 0, 62, 22, -7), (-2, 4, -14, 66, 22, -8), (-16, 4, -28, 64, 20, -6), (-30, 6, -38, 58, 16, -2)])
        curl = smooth([(CX - 24, 47), (CX - 9, 51), (CX - 3.5, 63), (CX - 9, 73), (CX - 19, 72), (CX - 13.5, 67), (CX - 11.5, 61), (CX - 16, 55)], t=0.9)
        front += [L(shifted(curl, 0.6, 3), "skinD", alpha=0.16, region="head"), L(curl, HAIR, region="head"), L(curl, stroke="hairD", width=0.7, alpha=0.6, region="head")]
        for s in (1, -1):
            front += lock((CX + s * 39, 46), (CX + s * 38, 80), 9, bend=s * -1)
    elif name == "parted":  # 가르마펌 (6:4): soft waves flowing away from a side part
        back += back_layer(back_shape(94, 46, tips=5), 94, 28)
        front += front_cap(side=46, top=4, low=56, hairline=38)
        px = 12
        front += locks([(px - 3, 2, -30, 56, 22, -9, 0.3), (px - 8, 4, -36, 64, 18, -6, 0.3), (px - 1, 8, -16, 50, 18, -7, 0.4), (-24, 6, -40, 70, 13, -2)])
        front += locks([(px + 4, 4, 34, 52, 16, 6, -0.3), (px + 12, 8, 40, 62, 13, 3)])
        front += part_line(CX + px, 8, 30)
        for s in (1, -1):
            front += lock((CX + s * 40, 48), (CX + s * 38, 82), 9, bend=s * -1)
    elif name == "curtain":  # 센터 가르마 / 애즈펌: middle part, waves framing the face to the cheekbones
        back += back_layer(back_shape(98, 46, tips=6), 98, 28)
        front += front_cap(side=46, top=4, low=58, hairline=40) + part_line(CX)
        for s in (1, -1):
            front += locks([(s * 2, 4, s * 24, 66, 13, s * 8, -s * 0.5), (s * 9, 4, s * 33, 74, 14, s * 6, -s * 0.4), (s * 22, 6, s * 40, 84, 12, s * 3, -s * 0.3)])
            front += lock((CX + s * 40, 50), (CX + s * 38, 90), 9, bend=s * -1)
    elif name == "leaf":  # 리프컷: longer layers, bangs splitting into leaf-shaped tips, longer nape
        back += back_layer(back_shape(118, 48, tips=7, flip=3), 118, 30)
        front += front_cap(side=47, top=4, low=58, hairline=40)
        front += locks([(-28, 6, -38, 72, 14, -4, -0.5), (-14, 4, -26, 70, 16, -6, -0.4), (-2, 4, -10, 66, 16, -3, -0.2), (6, 4, 12, 68, 16, 3, 0.2),
                        (16, 4, 28, 72, 16, 6, 0.4), (28, 6, 40, 76, 14, 4, 0.5)])
        for s in (1, -1):
            front += lock((CX + s * 40, 48), (CX + s * 42, 98), 11, bend=s * 2, curl=-s * 0.4)
    elif name == "crop":  # textured crop: short choppy fringe high on the forehead, tight sides
        back += back_layer(head_shape(88, 43, 32), 88, 26)
        front += front_cap(side=43, top=3, low=54, hairline=38)
        specs = []
        for i in range(9):
            t = (i / 8) * 2 - 1
            specs.append((t * 28, 4, t * 30 + (2 if i % 2 else -2), 52 + (3 if i % 3 == 0 else 0) - abs(t) * 3, 9, (1 if i % 2 else -1) * 2))
        front += locks(specs)
        for s in (1, -1):  # faded sides
            front.append(L(path((CX + s * 42, 50), [("C", (CX + s * 43, 60), (CX + s * 41, 70), (CX + s * 38, 78))], close=False), stroke="hairS", width=3, alpha=0.45, region="head"))
    elif name == "wolf":  # 울프컷: shaggy layered top, choppy fringe, longer tails at the nape
        d = back_shape(140, 50, tips=7, flip=8)
        back += back_layer(d, 140, 30) + back_strands([36, -36], 90, 134)
        front += front_cap(side=47, top=3, low=58, hairline=40)
        front += locks([(-30, 6, -40, 66, 12, -4, -0.4), (-18, 4, -26, 68, 13, -4), (-6, 4, -10, 64, 12, -2), (6, 4, 8, 66, 12, 2), (18, 4, 26, 68, 13, 4), (30, 6, 40, 66, 12, 4, 0.4)])
        for s in (1, -1):
            front += lock((CX + s * 40, 48), (CX + s * 46, 104), 11, bend=s * 3, curl=-s * 0.5)
            front += lock((CX + s * 44, 80), (CX + s * 54, 130), 10, bend=s * 2, curl=-s * 0.6, shadow=False)
    elif name == "slicked":  # natural slick-back: combed back with lift and texture, forehead open
        back += back_layer(back_shape(92, 45, tips=5), 92, 28)
        top = sym((CX, 2), [("C", (CX + 26, 1), (CX + 44, 18), (CX + 44, 46)), ("C", (CX + 44, 54), (CX + 41, 58), (CX + 37, 58)),
                            ("C", (CX + 35, 48), (CX + 30, 40), (CX + 18, 37)), ("C", (CX + 8, 35), (CX + 3, 37), (CX, 39.5))])
        front += [L(shifted(top, 0.4, 2.4), "skinD", alpha=0.15, region="head"), L(top, HAIR, region="head"), L(top, stroke="hairD", width=0.9, alpha=0.6, region="head")]
        for k in (-28, -18, -8, 2, 12, 22, 30):
            x0 = CX + k * 0.9
            front.append(L(path((x0, 37 + abs(k) * 0.12), [("C", (x0 + k * 0.05, 24), (CX + k * 0.75, 12), (CX + k * 0.55, 4))], close=False),
                           stroke="hairD", width=0.7, alpha=0.4, region="head"))
            front.append(L(path((x0 + 2.5, 34), [("C", (x0 + 2.5 + k * 0.05, 24), (CX + k * 0.75 + 2, 14), (CX + k * 0.55 + 2, 8))], close=False),
                           stroke="hairL", width=1.0, alpha=0.25, region="head"))
        front += lock((CX - 8, 38), (CX - 14, 54), 5, -3)
        for s in (1, -1):
            front += lock((CX + s * 39, 44), (CX + s * 38, 76), 8, bend=s * -1)
    elif name == "spiky":  # kept for old saves
        back += back_layer(back_shape(90, 45, tips=5), 90, 28)
        for (rx, ry, tx_, ty, w) in [(-34, 38, -51, 30, 16), (34, 38, 51, 32, 16), (-24, 28, -36, 8, 17), (24, 28, 37, 9, 17), (-10, 24, -14, 2, 17), (11, 24, 16, 3, 17)]:
            front += lock((CX + rx, ry), (CX + tx_, ty), w, shadow=False)
        front += front_cap(side=45, top=4, low=56)
        front += locks([(-30, 6, -35, 58, 18, -2), (30, 6, 35, 58, 18, 2), (-18, 4, -24, 61, 18, -2), (18, 4, 22, 61, 18, 2), (-6, 4, -10, 60, 17, -1), (7, 4, 6, 63, 17, 0)])
    elif name == "curly":  # soft curls (shadow perm / jelly perm)
        def wavy(rx, ry, cy, n, amp, start, end):
            pts = []
            for i in range(n + 1):
                t = start + (end - start) * i / n
                r = 1 + (amp if i % 2 else -amp * 0.4)
                pts.append((CX + math.cos(t) * rx * r, cy - math.sin(t) * ry * r))
            return pts
        outer = wavy(52, 60, 64, 26, 0.07, math.radians(-75), math.radians(255))
        d = smooth(outer + [(CX, 116)], closed=True, t=0.8)
        back += back_layer(d, 120, 30)
        cr = wavy(46, 52, 60, 20, 0.08, math.radians(-12), math.radians(192))
        cr += [(CX - 34, 64), (CX - 24, 56), (CX - 12, 64), (CX, 56), (CX + 12, 64), (CX + 24, 56), (CX + 34, 64)]
        cd = smooth(cr, closed=True, t=0.8)
        front += [L(shifted(cd, 0.5, 3), "skinD", alpha=0.15, region="head"), L(cd, HAIR, region="head"), L(cd, stroke="hairD", width=0.9, alpha=0.6, region="head")]
        for (x, y) in [(CX - 30, 28), (CX - 10, 18), (CX + 12, 20), (CX + 32, 32), (CX - 40, 50), (CX + 42, 54), (CX - 18, 40), (CX + 20, 42), (CX, 32)]:
            front.append(L(path((x - 4.5, y + 2), [("C", (x - 4.5, y - 4.5), (x + 4.5, y - 4.5), (x + 4.5, y + 1)), ("C", (x + 4.5, y + 4.5), (x, y + 4.5), (x - 1, y + 1))], close=False),
                           stroke="hairD", width=0.9, alpha=0.5, region="head"))
            front.append(L(ell(x - 1, y - 2, 2.2, 1.2), "hairL", alpha=0.4, region="head"))
        return back, front
    elif name == "buzz":
        d = sym((CX, 17), [("C", (CX + 25, 17), (CX + 36.5, 31), (CX + 36, 54)), ("C", (CX + 35, 52), (CX + 31, 45), (CX + 25, 41)),
                           ("C", (CX + 16, 37.5), (CX + 8, 38), (CX, 39))])
        front += [L(d, HAIR, alpha=0.95, region="head"), L(d, stroke="hairD", width=0.8, alpha=0.5, region="head")]
        for k in range(-5, 6):
            front.append(L(ell(CX + k * 5.5, 30 + abs(k) * 1.2, 0.6, 0.6), "hairD", alpha=0.35, region="head"))
        return back, front
    front += shine()
    return back, front


ALL = ["long", "hush", "build", "wavy", "hime", "bob", "pixie", "ponytail", "twintail", "bun",
       "short", "comma", "parted", "curtain", "leaf", "crop", "wolf", "slicked", "spiky", "curly", "buzz"]
