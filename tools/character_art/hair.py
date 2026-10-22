"""Hair styles: back layer (behind the body) and front layer (over the face).

Bangs are built from separate locks (tapered, slightly curved clumps) so the hair reads as strands
rather than one cut-out shape; every lock gets a soft gradient, a darker edge and a strand line.
"""
import math
from geo import *

HAIR = "lin:hair:hairS:100:20:100:84"


def crown_y(x):
    """Height of the crown surface above x (roots of the bangs sit just below it)."""
    t = min(1.0, abs(x - CX) / 44)
    return 9 + 34 * (1 - math.sqrt(max(0.0, 1 - t * t)))


def lock(root, tip, w, bend=0.0, fill=None, line=True, alpha=1.0):
    """A clump of hair from a root (top centre) to a pointed tip, in any direction; [bend] bows it
    sideways (x). Only the two long edges are outlined, so roots melt into the crown."""
    x0, y0 = root
    x1, y1 = tip
    ln = max(1.0, math.hypot(x1 - x0, y1 - y0))
    ux, uy = (x1 - x0) / ln, (y1 - y0) / ln
    nx, ny = -uy, ux

    def P(a, b, k=0.0):
        return (x0 + ux * a * ln - nx * b + bend * k, y0 + uy * a * ln - ny * b)
    left, right = P(0, w / 2), P(0, -w / 2)
    edges = [("C", P(0.45, w * 0.6, 0.3), P(0.8, w * 0.34, 1), (x1, y1)),
             ("C", P(0.74, -w * 0.26, 1), P(0.45, -w * 0.6, 0.3), right)]
    d = path(left, edges + [("C", P(-4 / ln, -w * 0.2), P(-4 / ln, w * 0.2), left)])
    out = [L(d, fill or HAIR, alpha=alpha, region="head")]
    if line:
        out.append(L(path(left, edges, close=False), stroke="hairD", width=0.7, alpha=0.55, region="head"))
        out.append(L(path(P(0.35, -w * 0.05), [("C", P(0.52, 0, 0.5), P(0.66, 0, 0.6), P(0.8, w * 0.04, 0.8))], close=False),
                     stroke="hairD", width=0.5, alpha=0.25, region="head"))
    return out


def crown(top=9.0, side=43.0, low=60.0):
    """Hair over the top of the skull, down behind the temples (front layer, behind the bangs)."""
    return sym((CX, top), [("C", (CX + side * 0.62, top), (CX + side, top + 20), (CX + side, low - 12)),
                           ("C", (CX + side, low - 4), (CX + side - 4, low), (CX + side - 8, low - 3)),
                           ("C", (CX + 26, 44), (CX + 12, 40), (CX, 40))])


def shine(y=27.0, span=37.0):
    """Glossy 'angel ring': soft lens-shaped glints along an arc across the crown."""
    out = []
    for a, b in [(-0.92, -0.42), (-0.3, 0.3), (0.42, 0.92)]:
        top, low = [], []
        for i in range(7):
            t = a + (b - a) * i / 6
            x = CX + t * span
            yy = y + (t ** 2) * 13
            th = math.sin(math.pi * i / 6)
            top.append((x, yy - th * 1.6))
            low.append((x, yy + th * 2.6))
        out.append(L(smooth(top + low[::-1], closed=True, t=0.8), "hairL", alpha=0.5, region="head"))
    return out


def side_lock(s, top=52.0, bottom=110.0, w=9.0, x=37.0, curl=0.0):
    """A lock framing the face, from the temple down past the cheek."""
    return lock((CX + s * x, top), (CX + s * (x - 3 + curl), bottom), w, bend=s * (-2 + curl), fill=None)


def back_shape(length, width=46.0, flare=0.0, tips=5, top=8.0):
    """Hair behind the head and shoulders: rounded top, straight-ish sides, a soft locky hem."""
    pts = [(CX, top), (CX + width * 0.62, top + 3), (CX + width * 0.95, top + 22), (CX + width, 60)]
    if length > 100:
        pts += [(CX + width + 1, 100), (CX + width + 2 + flare * 0.5, (100 + length) / 2), (CX + width + flare, length - 8)]
    hem = []
    for i in range(tips + 1):
        t = i / tips
        x = CX + (width + flare) * (1 - 2 * t)
        y = length + (3.5 if i % 2 else -3) - abs(0.5 - t) * 10
        hem.append((x, y))
    pts += hem
    if length > 100:
        pts += [(CX - width - flare, length - 8), (CX - width - 2 - flare * 0.5, (100 + length) / 2), (CX - width - 1, 100)]
    pts += [(CX - width, 60), (CX - width * 0.95, top + 22), (CX - width * 0.62, top + 3)]
    return smooth(pts, closed=True, t=0.85)


def back_layer(d, bottom):
    return [L(d, "lin:hairS:hairD:100:30:100:" + f(bottom), region="head"), L(d, stroke="hairD", width=0.9, alpha=0.6, region="head")]


def back_strands(xs, y0, y1):
    out = []
    for x in xs:
        out.append(L(path((CX + x, y0), [("C", (CX + x * 1.05, y0 + (y1 - y0) * 0.35), (CX + x * 1.08, y0 + (y1 - y0) * 0.7), (CX + x * 1.02, y1))], close=False),
                     stroke="hairD", width=0.8, alpha=0.4, region="head"))
    return out


def front_cap(bottom=74.0):
    return [L(crown(), HAIR, region="head"), L(crown(), stroke="hairD", width=0.9, alpha=0.6, region="head")]


# Bang sets: (root x, root y, tip x, tip y, width, bend)
BANGS = {
    "soft": [(-30, 16, -37, 66, 18, -2), (30, 16, 37, 66, 18, 2), (-19, 12, -25, 70, 18, -1), (19, 12, 24, 69, 18, 1),
             (-8, 10, -11, 67, 17, 0), (9, 10, 10, 72, 17, 1), (1, 9, -1, 61, 12, 0)],
    "straight": [(-30, 16, -34, 64, 18, -1), (30, 16, 34, 64, 18, 1), (-19, 12, -21, 66, 18, 0), (19, 12, 21, 66, 18, 0),
                 (-7, 10, -8, 67, 18, 0), (7, 10, 8, 67, 18, 0)],
    "short": [(-30, 16, -35, 58, 18, -2), (30, 16, 35, 58, 18, 2), (-18, 12, -24, 61, 18, -2), (18, 12, 22, 61, 18, 2),
              (-6, 10, -10, 60, 17, -1), (7, 10, 6, 63, 17, 0)],
    "parted": [(-28, 14, -38, 70, 20, -3), (-16, 11, -29, 66, 18, -3), (-5, 10, -18, 58, 16, -3),
               (28, 14, 38, 70, 20, 3), (16, 11, 29, 66, 18, 3), (5, 10, 18, 58, 16, 3)],
    "side": [(-30, 16, -38, 70, 18, -2), (-18, 12, -30, 66, 18, -3), (-4, 10, -18, 62, 20, -5), (10, 10, -2, 58, 20, -6),
             (24, 12, 14, 62, 18, -4), (32, 16, 33, 70, 14, 1)],
}


def bangs(kind):
    out = []
    for (rx, ry, tx, ty, w, b) in BANGS[kind]:
        x = CX + rx
        out += lock((x, crown_y(x) + 7), (CX + tx, ty), w, b)
    return out


def style(name):
    """-> (back layers, front layers)"""
    back, front = [], []
    if name == "long":
        d = back_shape(214, 47, flare=4, tips=6)
        back += back_layer(d, 214) + back_strands([28, 38, -28, -38, 10, -10], 90, 205)
        front += front_cap() + bangs("soft")
        for s in (1, -1):
            front += side_lock(s, 50, 150, 10, 38, curl=1)
            front += side_lock(s, 54, 124, 7, 33, curl=2)
    elif name == "bob":
        d = back_shape(122, 47, flare=2, tips=6)
        back += back_layer(d, 122)
        front += front_cap() + bangs("straight")
        for s in (1, -1):
            front += lock((CX + s * 39, 46), (CX + s * 39, 123), 13, s * 3)
            front += lock((CX + s * 34, 52), (CX + s * 31, 116), 9, s * 2)
    elif name == "short":
        d = back_shape(92, 45, tips=5)
        back += back_layer(d, 92)
        front += front_cap(64) + bangs("short")
        for s in (1, -1):
            front += lock((CX + s * 38, 46), (CX + s * 37, 86), 10, s * -1)
    elif name == "ponytail":
        tail = smooth([(CX + 30, 26), (CX + 50, 22), (CX + 62, 46), (CX + 60, 92), (CX + 56, 140), (CX + 62, 178), (CX + 52, 206),
                       (CX + 46, 176), (CX + 40, 140), (CX + 42, 96), (CX + 44, 60), (CX + 36, 40)], closed=True, t=0.85)
        back += [L(tail, "lin:hair:hairS:140:30:140:206", region="head"), L(tail, stroke="hairD", width=0.9, alpha=0.6, region="head")]
        back += back_strands([47, 52], 60, 190)
        back += back_layer(back_shape(94, 45, tips=5), 94)
        front += front_cap() + bangs("parted")
        front.append(L(ell(CX + 36, 30, 4.2, 5.2), "lin:accS:acc:136:25:136:36", region="head"))
        for s in (1, -1):
            front += side_lock(s, 54, 100, 6, 36, curl=1)
    elif name == "twintail":
        for s in (1, -1):
            tail = smooth([(CX + s * 34, 30), (CX + s * 58, 28), (CX + s * 70, 64), (CX + s * 66, 120), (CX + s * 62, 168), (CX + s * 70, 198), (CX + s * 58, 220),
                           (CX + s * 50, 190), (CX + s * 46, 150), (CX + s * 48, 104), (CX + s * 50, 64), (CX + s * 40, 42)], closed=True, t=0.85)
            back += [L(tail, "lin:hair:hairS:100:30:100:220", region="head"), L(tail, stroke="hairD", width=0.9, alpha=0.6, region="head")]
            back += back_strands([s * 56, s * 60], 70, 200)
        back += back_layer(back_shape(94, 45, tips=5), 94)
        front += front_cap() + bangs("soft")
        for s in (1, -1):
            front.append(L(ell(CX + s * 39, 34, 4.6, 5.4), "lin:accS:acc:100:28:100:40", region="head"))
            front += side_lock(s, 54, 108, 7, 36, curl=1)
    elif name == "bun":
        bun = ell(CX, 9, 15, 12.5)
        back += [L(bun, "lin:hair:hairS:100:-3:100:21", region="head"), L(bun, stroke="hairD", width=0.9, alpha=0.6, region="head")]
        back += back_layer(back_shape(92, 45, tips=5), 92)
        front += [L(bun, "lin:hairL:hairS:100:-6:100:21", region="head"), L(bun, stroke="hairD", width=0.9, alpha=0.6, region="head")]
        front += [L(path((CX - 10, 2), [("C", (CX - 5, 8), (CX + 2, 9), (CX + 9, 4))], close=False), stroke="hairD", width=0.7, alpha=0.4, region="head")]
        front.append(L(path((CX - 14, 15), [("C", (CX - 5, 19), (CX + 5, 19), (CX + 14, 15))], close=False), stroke="acc", width=2.6, region="head"))
        front += front_cap() + bangs("side")
        for s in (1, -1):
            front += side_lock(s, 56, 100, 5, 36, curl=1.5)
    elif name == "spiky":
        back += back_layer(back_shape(90, 45, tips=5), 90)
        for (rx, ry, tx, ty, w) in [(-34, 38, -51, 30, 16), (34, 38, 51, 32, 16), (-24, 28, -36, 8, 17), (24, 28, 37, 9, 17),
                                    (-10, 24, -14, 2, 17), (11, 24, 16, 3, 17)]:
            front += lock((CX + rx, ry), (CX + tx, ty), w)
        front += front_cap(64) + bangs("short")
        for s in (1, -1):
            front += lock((CX + s * 38, 44), (CX + s * 41, 86), 10, s * 2)
    elif name == "curly":
        def wavy(rx, ry, cy, n, amp, start, end):
            pts = []
            for i in range(n + 1):
                t = start + (end - start) * i / n
                r = 1 + (amp if i % 2 else -amp * 0.4)
                pts.append((CX + math.cos(t) * rx * r, cy - math.sin(t) * ry * r))
            return pts
        outer = wavy(52, 60, 64, 26, 0.07, math.radians(-75), math.radians(255))
        d = smooth(outer + [(CX, 116)], closed=True, t=0.8)
        back += back_layer(d, 120)
        cr = wavy(46, 52, 60, 20, 0.08, math.radians(-12), math.radians(192))
        cr += [(CX - 34, 64), (CX - 24, 54), (CX - 12, 62), (CX, 52), (CX + 12, 62), (CX + 24, 54), (CX + 34, 64)]
        cd = smooth(cr, closed=True, t=0.8)
        front += [L(cd, HAIR, region="head"), L(cd, stroke="hairD", width=0.9, alpha=0.6, region="head")]
        for (x, y) in [(CX - 30, 28), (CX - 10, 18), (CX + 12, 20), (CX + 32, 32), (CX - 40, 50), (CX + 42, 54), (CX - 18, 40), (CX + 20, 42), (CX, 32)]:
            front.append(L(path((x - 4.5, y + 2), [("C", (x - 4.5, y - 4.5), (x + 4.5, y - 4.5), (x + 4.5, y + 1)), ("C", (x + 4.5, y + 4.5), (x, y + 4.5), (x - 1, y + 1))], close=False),
                           stroke="hairD", width=0.9, alpha=0.5, region="head"))
            front.append(L(ell(x - 1, y - 2, 2.2, 1.2), "hairL", alpha=0.4, region="head"))
        return back, front
    elif name == "comma":
        # 쉼표머리: bangs swept to one side, the front lock curling back in like a comma
        back += back_layer(back_shape(90, 45, tips=5), 90)
        front += front_cap(64)
        for (rx, tx, ty, w, b) in [(34, 38, 58, 14, 2), (24, 14, 58, 20, -5), (12, 0, 62, 22, -7), (-2, -14, 66, 22, -8), (-16, -28, 64, 20, -6), (-30, -38, 58, 16, -2)]:
            x = CX + rx
            front += lock((x, crown_y(x) + 7), (CX + tx, ty), w, b)
        curl = smooth([(CX - 24, 47), (CX - 9, 51), (CX - 3.5, 63), (CX - 9, 73), (CX - 19, 72), (CX - 13.5, 67), (CX - 11.5, 61), (CX - 16, 55)], t=0.9)
        front += [L(curl, HAIR, region="head"), L(curl, stroke="hairD", width=0.7, alpha=0.6, region="head")]
        for s in (1, -1):
            front += lock((CX + s * 38, 46), (CX + s * 37, 82), 9, s * -1)
    elif name == "parted":
        # 가르마: a side part, longer hair swept across the forehead on one side, the other side tucked back
        back += back_layer(back_shape(92, 45, tips=5), 92)
        front += front_cap(64)
        px = CX + 13
        for (rx, ry, tx, ty, w, b) in [(8, 14, -36, 64, 20, -6), (2, 16, -28, 60, 22, -8), (6, 20, -14, 54, 20, -7), (-12, 14, -40, 70, 14, -2)]:
            front += lock((CX + rx, ry), (CX + tx, ty), w, b)
        for (rx, ry, tx, ty, w, b) in [(18, 14, 38, 50, 14, 4), (26, 18, 40, 62, 12, 2)]:
            front += lock((CX + rx, ry), (CX + tx, ty), w, b)
        front.append(L(path((px, 12), [("C", (px + 0.5, 20), (px + 0.5, 28), (px - 0.5, 36))], close=False), stroke="hairD", width=1.1, alpha=0.6, region="head"))
        for s in (1, -1):
            front += lock((CX + s * 38, 48), (CX + s * 37, 84), 9, s * -1)
    elif name == "slicked":
        # 올백: combed back with a little lift at the front, forehead fully open
        back += back_layer(back_shape(90, 45, tips=5), 90)
        top = sym((CX, 6), [("C", (CX + 26, 5), (CX + 43, 20), (CX + 43, 46)), ("C", (CX + 43, 54), (CX + 40, 58), (CX + 36, 58)),
                            ("C", (CX + 35, 48), (CX + 30, 40), (CX + 18, 37)), ("C", (CX + 8, 35), (CX + 3, 37), (CX, 39.5))])
        front += [L(top, HAIR, region="head"), L(top, stroke="hairD", width=0.9, alpha=0.6, region="head")]
        for k in (-28, -18, -8, 2, 12, 22, 30):
            x0 = CX + k * 0.9
            front.append(L(path((x0, 37 + abs(k) * 0.12), [("C", (x0 + k * 0.05, 26), (CX + k * 0.75, 16), (CX + k * 0.55, 9))], close=False),
                           stroke="hairD", width=0.7, alpha=0.4, region="head"))
        for s in (1, -1):
            front += lock((CX + s * 39, 44), (CX + s * 38, 76), 8, s * -1)
    elif name == "wavy":
        # long waves: a wavy silhouette, soft side-swept bangs and rippling side locks
        pts = [(CX, 8), (CX + 30, 11), (CX + 45, 30), (CX + 47, 62)]
        for i in range(7):
            y = 80 + i * 21
            pts.append((CX + 49 + (5 if i % 2 else -2) + i * 0.8, y))
        hem = [(CX + 52, 220), (CX + 30, 228), (CX + 10, 222), (CX - 10, 226), (CX - 30, 222), (CX - 52, 224)]
        pts += hem
        for i in range(6, -1, -1):
            y = 80 + i * 21
            pts.append((CX - 49 - (5 if i % 2 == 0 else -2) - i * 0.8, y))
        pts += [(CX - 47, 62), (CX - 45, 30), (CX - 30, 11)]
        d = smooth(pts, closed=True, t=0.9)
        back += back_layer(d, 226) + back_strands([30, 40, -30, -40, 14, -14], 90, 214)
        front += front_cap() + bangs("side")
        for s in (1, -1):
            wave = smooth([(CX + s * 34, 50), (CX + s * 44, 52), (CX + s * 47, 80), (CX + s * 42, 100), (CX + s * 48, 124), (CX + s * 44, 150), (CX + s * 38, 158),
                           (CX + s * 38, 140), (CX + s * 34, 120), (CX + s * 38, 98), (CX + s * 34, 74)], closed=True, t=0.9)
            front += [L(wave, HAIR, region="head"), L(wave, stroke="hairD", width=0.8, alpha=0.6, region="head")]
            front.append(L(path((CX + s * 40, 70), [("C", (CX + s * 44, 86), (CX + s * 38, 104), (CX + s * 43, 128))], close=False), stroke="hairD", width=0.6, alpha=0.35, region="head"))
    elif name == "buzz":
        d = sym((CX, 17), [("C", (CX + 25, 17), (CX + 36.5, 31), (CX + 36, 54)), ("C", (CX + 35, 52), (CX + 31, 45), (CX + 25, 41)),
                           ("C", (CX + 16, 37.5), (CX + 8, 38), (CX, 39))])
        front += [L(d, HAIR, alpha=0.95, region="head"), L(d, stroke="hairD", width=0.8, alpha=0.5, region="head")]
        for k in range(-5, 6):
            front.append(L(ell(CX + k * 5.5, 30 + abs(k) * 1.2, 0.6, 0.6), "hairD", alpha=0.35, region="head"))
        return back, front
    front += shine()
    return back, front
