"""Hair styles: back layer (behind the body) and front layer (over the face)."""
from geo import *

HAIR_LINE = dict(stroke="hairD", width=1.0, alpha=0.6)


def strands(points, width=0.9, alpha=0.45, role="hairD"):
    return [L(path(p[0], [("C", p[1], p[2], p[3])], close=False), stroke=role, width=width, alpha=alpha) for p in points]


def shine(y=36, w=24):
    """Glossy band across the crown."""
    d = path((CX - w, y + 6), [("C", (CX - w * 0.5, y - 2), (CX + w * 0.5, y - 2), (CX + w, y + 6)),
                               ("C", (CX + w * 0.5, y + 1.5), (CX - w * 0.5, y + 1.5), (CX - w, y + 6))])
    return [L(d, "hairL", alpha=0.55)]


def cap_top():
    """Shared crown shape that covers the skull."""
    return sym((CX, 13), [("C", (CX + 27, 13), (CX + 42, 30), (CX + 41, 58)), ("L", (CX, 58))])


def bangs(kind="soft"):
    if kind == "straight":
        segs = [("C", (CX + 27, 13), (CX + 42, 30), (CX + 41, 58)), ("C", (CX + 41, 64), (CX + 39, 68), (CX + 36, 69)),
                ("L", (CX + 30, 66)), ("L", (CX + 24, 69)), ("L", (CX + 16, 66)), ("L", (CX + 8, 69)), ("L", (CX, 66))]
    elif kind == "side":
        # swept to one side: asymmetric
        return path((CX + 41, 58), [("C", (CX + 42, 30), (CX + 27, 13), (CX, 13)), ("C", (CX - 27, 13), (CX - 42, 30), (CX - 41, 58)),
                                    ("C", (CX - 40, 66), (CX - 37, 72), (CX - 35, 76)), ("C", (CX - 30, 62), (CX - 22, 52), (CX - 8, 46)),
                                    ("C", (CX + 5, 52), (CX + 18, 64), (CX + 33, 74)), ("C", (CX + 37, 70), (CX + 40, 65), (CX + 41, 58))])
    elif kind == "parted":
        segs = [("C", (CX + 27, 13), (CX + 42, 30), (CX + 41, 58)), ("C", (CX + 41, 66), (CX + 38, 72), (CX + 35, 75)),
                ("C", (CX + 30, 60), (CX + 18, 44), (CX + 2, 36)), ("L", (CX, 35))]
    elif kind == "short":
        segs = [("C", (CX + 27, 13), (CX + 41, 28), (CX + 40, 52)), ("C", (CX + 40, 58), (CX + 38, 62), (CX + 36, 64)),
                ("L", (CX + 30, 56)), ("L", (CX + 25, 61)), ("L", (CX + 19, 54)), ("L", (CX + 12, 60)), ("L", (CX + 6, 53)), ("L", (CX, 58))]
    else:  # soft, pointed anime bangs
        segs = [("C", (CX + 27, 13), (CX + 42, 30), (CX + 41, 58)), ("C", (CX + 41, 66), (CX + 39, 71), (CX + 36, 74)),
                ("C", (CX + 35, 64), (CX + 32, 58), (CX + 29, 55)), ("C", (CX + 28, 62), (CX + 25, 68), (CX + 22, 71)),
                ("C", (CX + 21, 61), (CX + 19, 55), (CX + 15, 51)), ("C", (CX + 13, 59), (CX + 10, 66), (CX + 6, 69)),
                ("C", (CX + 5, 60), (CX + 3, 54), (CX, 50))]
    return sym((CX, 13), segs)


def side_locks(length=104, width=6):
    out = []
    for s in (1, -1):
        x = CX + s * 37
        d = path((x - s * 2, 58), [("C", (x + s * 4, 70), (x + s * 3, length - 20), (x + s * (width - 3), length)),
                                   ("C", (x - s * 1, length - 6), (x - s * 4, length - 22), (x - s * 4.5, 66))])
        out.append(L(d, "lin:hair:hairS:100:60:100:" + str(length)))
    return out


def ring(y=27):
    """Glossy 'angel ring' arc across the crown with a jagged lower edge."""
    top = []
    for x in range(-38, 39, 4):
        top.append((CX + x, y + (x / 38) ** 2 * 14 - 1))
    low = []
    for i, x in enumerate(range(38, -39, -4)):
        low.append((CX + x, y + (x / 38) ** 2 * 14 + (5.5 if i % 2 else 2.5)))
    return smooth(top + low, closed=True, t=0.5)


MAIN = "lin:hair:hairS:100:14"


def decorate(front, back):
    """Gloss, strands and outlines on the main pieces."""
    out_front, out_back = [], []
    for i, l in enumerate(back):
        out_back.append(l)
        if l.fill and not l.fill.startswith("acc"):
            out_back.append(L(l.d, stroke="hairD", width=0.9, alpha=0.6))
    for l in front:
        out_front.append(l)
        if l.fill and l.fill.startswith(MAIN):
            out_front.append(L(ring(), "hairL", alpha=0.55, clip=l.d))
            for k in (-26, -12, 4, 18, 30):
                out_front.append(L(path((CX + k * 0.4 + 4, 22), [("C", (CX + k * 0.8, 34), (CX + k * 1.0, 50), (CX + k * 1.05, 68))], close=False),
                                   stroke="hairD", width=0.7, alpha=0.28, clip=l.d))
            out_front.append(L(l.d, stroke="hairD", width=0.9, alpha=0.75))
        elif l.fill and l.fill.startswith("lin:hair"):
            out_front.append(L(l.d, stroke="hairD", width=0.8, alpha=0.6))
    return out_back, out_front


def style(name):
    back, front = _style(name)
    return decorate(front, back)


def _style(name):
    """-> (back layers, front layers)"""
    back, front = [], []
    if name == "long":
        back.append(L(sym((CX, 12), [("C", (CX + 30, 12), (CX + 46, 36), (CX + 45, 70)), ("C", (CX + 44, 110), (CX + 52, 160), (CX + 46, 212)),
                                    ("C", (CX + 38, 222), (CX + 24, 218), (CX + 16, 210)), ("C", (CX + 10, 205), (CX + 5, 208), (CX, 206))]), "lin:hairS:hair:100:20:100:210"))
        back += strands([((CX + 30, 120), (CX + 36, 150), (CX + 38, 180), (CX + 34, 205)), ((CX - 30, 120), (CX - 36, 150), (CX - 38, 180), (CX - 34, 205))])
        front.append(L(bangs("soft"), "lin:hair:hairS:100:14:100:74"))
        front += side_locks(150, 7)
    elif name == "bob":
        back.append(L(sym((CX, 12), [("C", (CX + 30, 12), (CX + 46, 34), (CX + 45, 66)), ("C", (CX + 45, 90), (CX + 46, 110), (CX + 42, 124)),
                                    ("C", (CX + 30, 128), (CX + 15, 125), (CX, 124))]), "lin:hairS:hair:100:20:100:124"))
        front.append(L(bangs("straight"), "lin:hair:hairS:100:14:100:74"))
        for s in (1, -1):
            front.append(L(path((CX + s * 36, 60), [("C", (CX + s * 44, 80), (CX + s * 45, 105), (CX + s * 40, 124)), ("C", (CX + s * 35, 120), (CX + s * 31, 105), (CX + s * 32, 70))]), "lin:hair:hairS:100:60:100:124"))
    elif name == "short":
        back.append(L(sym((CX, 12), [("C", (CX + 30, 12), (CX + 44, 32), (CX + 42, 60)), ("C", (CX + 42, 76), (CX + 38, 86), (CX + 34, 90)), ("L", (CX, 90))]), "hairS"))
        front.append(L(bangs("short"), "lin:hair:hairS:100:14:100:66"))
        for s in (1, -1):
            front.append(L(path((CX + s * 36, 56), [("C", (CX + s * 39, 66), (CX + s * 38, 76), (CX + s * 35, 84)), ("C", (CX + s * 33, 76), (CX + s * 33, 66), (CX + s * 33, 58))]), "hair"))
    elif name == "ponytail":
        back.append(L(path((CX + 26, 30), [("C", (CX + 52, 26), (CX + 62, 60), (CX + 56, 110)), ("C", (CX + 52, 150), (CX + 60, 175), (CX + 50, 200)),
                                           ("C", (CX + 44, 170), (CX + 36, 140), (CX + 38, 100)), ("C", (CX + 40, 70), (CX + 36, 46), (CX + 26, 40))]), "lin:hair:hairS:140:40:140:200"))
        back.append(L(sym((CX, 12), [("C", (CX + 30, 12), (CX + 44, 32), (CX + 42, 62)), ("C", (CX + 42, 80), (CX + 38, 90), (CX + 34, 94)), ("L", (CX, 94))]), "hairS"))
        back += strands([((CX + 45, 70), (CX + 50, 110), (CX + 48, 150), (CX + 50, 190))])
        front.append(L(bangs("parted"), "lin:hair:hairS:100:14:100:78"))
        front.append(L(ell(CX + 34, 33, 4, 5), "acc"))
        front += side_locks(100, 4)
    elif name == "twintail":
        for s in (1, -1):
            back.append(L(path((CX + s * 34, 30), [("C", (CX + s * 62, 34), (CX + s * 68, 80), (CX + s * 62, 130)), ("C", (CX + s * 58, 170), (CX + s * 66, 195), (CX + s * 56, 215)),
                                                   ("C", (CX + s * 48, 180), (CX + s * 44, 150), (CX + s * 46, 110)), ("C", (CX + s * 48, 75), (CX + s * 44, 50), (CX + s * 34, 42))]), "lin:hair:hairS:100:40:100:215"))
            front.append(L(ell(CX + s * 38, 36, 4.5, 5), "acc"))
        back.append(L(sym((CX, 12), [("C", (CX + 30, 12), (CX + 44, 32), (CX + 42, 62)), ("C", (CX + 42, 80), (CX + 38, 90), (CX + 34, 94)), ("L", (CX, 94))]), "hairS"))
        front.append(L(bangs("soft"), "lin:hair:hairS:100:14:100:74"))
        front += side_locks(104, 5)
    elif name == "bun":
        back.append(L(ell(CX, 10, 15, 13), "lin:hairL:hairS:100:-2:100:22"))
        back.append(L(sym((CX, 12), [("C", (CX + 30, 12), (CX + 44, 32), (CX + 42, 62)), ("C", (CX + 42, 80), (CX + 38, 90), (CX + 34, 94)), ("L", (CX, 94))]), "hairS"))
        front.append(L(ell(CX, 10, 15, 13), "lin:hairL:hair:100:-2:100:22"))
        front.append(L(path((CX - 15, 14), [("C", (CX - 5, 18), (CX + 5, 18), (CX + 15, 14))], close=False), stroke="acc", width=2.5))
        front.append(L(bangs("side"), "lin:hair:hairS:100:14:100:78"))
        front += side_locks(98, 3.5)
    elif name == "spiky":
        back.append(L(sym((CX, 12), [("C", (CX + 30, 12), (CX + 44, 32), (CX + 42, 60)), ("C", (CX + 42, 76), (CX + 38, 86), (CX + 34, 90)), ("L", (CX, 90))]), "hairS"))
        spikes = [("L", (CX + 10, 0)), ("L", (CX + 16, 10)), ("L", (CX + 30, 3)), ("L", (CX + 31, 16)), ("L", (CX + 46, 18)), ("L", (CX + 40, 30)),
                  ("L", (CX + 50, 42)), ("L", (CX + 41, 48)), ("C", (CX + 42, 58), (CX + 40, 64), (CX + 37, 68)), ("L", (CX + 31, 58)), ("L", (CX + 26, 66)),
                  ("L", (CX + 19, 55)), ("L", (CX + 12, 64)), ("L", (CX + 6, 53)), ("L", (CX, 60))]
        front.append(L(sym((CX, 4), spikes), MAIN + ":100:66"))
    elif name == "curly":
        import math
        def wavy(rx, ry, cy, n, amp, start, end):
            pts = []
            for i in range(n + 1):
                t = start + (end - start) * i / n
                r = 1 + (amp if i % 2 else -amp * 0.4)
                pts.append((CX + math.cos(t) * rx * r, cy - math.sin(t) * ry * r))
            return pts
        outer = wavy(50, 60, 66, 26, 0.07, math.radians(-75), math.radians(255))
        back.append(L(smooth(outer + [(CX, 118)], closed=True, t=0.8), "hairS"))
        crown = wavy(44, 50, 60, 20, 0.08, math.radians(-15), math.radians(195))
        crown += [(CX - 30, 62), (CX - 20, 52), (CX - 8, 58), (CX + 4, 50), (CX + 16, 58), (CX + 28, 52), (CX + 36, 62)]
        front.append(L(smooth(crown, closed=True, t=0.8), MAIN + ":100:66"))
        for (x, y) in [(CX - 30, 30), (CX - 10, 20), (CX + 12, 22), (CX + 32, 34), (CX - 40, 52), (CX + 42, 56), (CX - 18, 42), (CX + 20, 44)]:
            front.append(L(path((x - 4, y + 2), [("C", (x - 4, y - 4), (x + 4, y - 4), (x + 4, y + 1)), ("C", (x + 4, y + 4), (x, y + 4), (x - 1, y + 1))], close=False),
                           stroke="hairD", width=0.9, alpha=0.5))
    elif name == "buzz":
        front.append(L(sym((CX, 18), [("C", (CX + 26, 18), (CX + 37, 32), (CX + 36, 54)), ("C", (CX + 35, 52), (CX + 30, 44), (CX + 24, 41)),
                                     ("C", (CX + 16, 38), (CX + 8, 39), (CX, 40))]), MAIN + ":100:52", alpha=0.95))
    return back, front
