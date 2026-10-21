"""Face details and accessories (vector). Each accessory -> (behind layers, front layers)."""
import math
from geo import *


def features(names):
    out = []
    if "freckles" in names:
        for s in (1, -1):
            for (dx, dy) in [(14, 93), (18, 96), (22, 92), (25, 97), (17, 100)]:
                out.append(L(ell(CX + s * dx, dy, 0.9, 0.8), "skinD", alpha=0.55))
    if "wrinkles" in names:
        for s in (1, -1):
            out.append(L(path((CX + s * 9, 101), [("C", (CX + s * 12, 106), (CX + s * 12, 110), (CX + s * 10, 113))], close=False), stroke="skinD", width=0.8, alpha=0.5))
            for k in (-1, 1):
                out.append(L(path((CX + s * 26, 83 + k * 2), [("L", (CX + s * 30, 82 + k * 4))], close=False), stroke="skinD", width=0.7, alpha=0.45))
            out.append(L(path((CX + s * 4, 60), [("C", (CX + s * 10, 58.5), (CX + s * 16, 59), (CX + s * 20, 61))], close=False), stroke="skinD", width=0.6, alpha=0.35))
    if "beard" in names:
        beard = smooth([(CX + 33, 80), (CX + 33, 95), (CX + 24, 112), (CX + 12, 124), (CX, 127), (CX - 12, 124), (CX - 24, 112), (CX - 33, 95), (CX - 33, 80),
                        (CX - 29, 92), (CX - 20, 104), (CX - 9, 106), (CX, 104.5), (CX + 9, 106), (CX + 20, 104), (CX + 29, 92)], closed=True, t=0.8)
        out.append(L(beard, "lin:hair:hairS:100:90:100:126"))
        out.append(L(beard, stroke="hairD", width=0.8, alpha=0.6))
        out.append(L(ell(CX, 110, 7, 3.6), "skin"))
    if "mustache" in names or "beard" in names:
        m = smooth([(CX, 103), (CX + 6, 101.5), (CX + 11, 104), (CX + 13, 107.5), (CX + 8, 106), (CX + 3, 106), (CX, 107),
                    (CX - 3, 106), (CX - 8, 106), (CX - 13, 107.5), (CX - 11, 104), (CX - 6, 101.5)], closed=True, t=0.8)
        out.append(L(m, "lin:hair:hairD:100:101:100:108"))
    return out


def heart(cx, cy, w):
    return (f"M{f(cx)},{f(cy + w * 0.9)} C{f(cx - w * 1.5)},{f(cy - w * 0.1)} {f(cx - w * 0.9)},{f(cy - w * 1.2)} {f(cx)},{f(cy - w * 0.45)} "
            f"C{f(cx + w * 0.9)},{f(cy - w * 1.2)} {f(cx + w * 1.5)},{f(cy - w * 0.1)} {f(cx)},{f(cy + w * 0.9)}Z")


def flower(cx, cy, r, petal, center):
    out = []
    for k in range(5):
        a = k * 2 * math.pi / 5 - math.pi / 2
        out.append(L(ell(cx + math.cos(a) * r, cy + math.sin(a) * r, r * 0.75, r * 0.75), petal))
    out.append(L(ell(cx, cy, r * 0.55, r * 0.55), center))
    return out


def star(cx, cy, r):
    pts = []
    for k in range(10):
        a = -math.pi / 2 + k * math.pi / 5
        rr = r if k % 2 == 0 else r * 0.45
        pts.append((cx + math.cos(a) * rr, cy + math.sin(a) * rr))
    return path(pts[0], [("L", p) for p in pts[1:]])


def accessory(name):
    b, fr = [], []
    if name == "cap":
        fr.append(L(smooth([(CX - 40, 44), (CX - 38, 20), (CX - 20, 4), (CX, 0), (CX + 20, 4), (CX + 38, 20), (CX + 40, 44), (CX, 46)], t=0.9), "lin:#FF6B7A:#C93848:100:0:100:46"))
        fr.append(L(smooth([(CX - 46, 44), (CX, 38), (CX + 46, 44), (CX + 44, 52), (CX, 50), (CX - 44, 52)], t=0.8), "#B8323F"))
        fr.append(L(star(CX, 20, 7), "#FFE066"))
        fr.append(L(ell(CX, 2, 3, 2), "#C93848"))
    elif name == "glasses":
        for s in (1, -1):
            fr.append(L(heart(CX + s * 16, 85, 8.5), "#FF8FB5", alpha=0.45))
            fr.append(L(heart(CX + s * 16, 85, 8.5), stroke="#E23D78", width=2.0))
            fr.append(L(ell(CX + s * 13, 80, 1.6, 1.2), "white", alpha=0.8))
        fr.append(L(path((CX - 5, 80), [("C", (CX - 2, 77.5), (CX + 2, 77.5), (CX + 5, 80))], close=False), stroke="#E23D78", width=1.8))
        for s in (1, -1):
            fr.append(L(path((CX + s * 27, 80), [("L", (CX + s * 35, 79))], close=False), stroke="#E23D78", width=1.8))
    elif name == "sunglasses":
        for s in (1, -1):
            lens = smooth([(CX + s * 5, 79), (CX + s * 27, 78), (CX + s * 26, 88), (CX + s * 17, 93), (CX + s * 8, 89)], t=0.8)
            fr.append(L(lens, "lin:#3A4A6A:#101018:100:78:100:93"))
            fr.append(L(path((CX + s * 10, 81), [("L", (CX + s * 15, 81))], close=False), stroke="white", width=1.4, alpha=0.6))
            fr.append(L(path((CX + s * 26, 79), [("L", (CX + s * 35, 78))], close=False), stroke="#101018", width=2.0))
        fr.append(L(path((CX - 5, 80), [("L", (CX + 5, 80))], close=False), stroke="#101018", width=2.2))
    elif name == "headphones":
        fr.append(L(path((CX - 40, 70), [("C", (CX - 44, 10), (CX + 44, 10), (CX + 40, 70))], close=False), stroke="#4A4A5E", width=5))
        fr.append(L(path((CX - 40, 70), [("C", (CX - 44, 10), (CX + 44, 10), (CX + 40, 70))], close=False), stroke="#7C6CF0", width=2))
        for s in (1, -1):
            fr.append(L(smooth([(CX + s * 34, 66), (CX + s * 45, 64), (CX + s * 48, 82), (CX + s * 45, 98), (CX + s * 34, 96), (CX + s * 33, 82)], t=0.9), "lin:#B3A8FF:#5246B8:100:64:100:98"))
            fr.append(L(ell(CX + s * 42, 81, 3, 7), "#3A3A4A", alpha=0.5))
    elif name == "cape":
        b.append(L(smooth([(CX - 30, 140), (CX + 30, 140), (CX + 42, 220), (CX + 52, 330), (CX + 30, 338), (CX, 334), (CX - 30, 338), (CX - 52, 330), (CX - 42, 220)], t=0.8),
                   "lin:#D0344A:#7A1A28:100:140:100:338"))
        for k in (-30, -12, 12, 30):
            b.append(L(path((CX + k * 0.6, 160), [("C", (CX + k * 0.8, 230), (CX + k * 1.1, 290), (CX + k * 1.4, 332))], close=False), stroke="#5E1220", width=1.2, alpha=0.5))
        fr.append(L(smooth([(CX - 26, 140), (CX - 10, 146), (CX, 148), (CX + 10, 146), (CX + 26, 140), (CX + 20, 137), (CX, 141), (CX - 20, 137)], t=0.8), "#B0283A"))
        fr.append(L(ell(CX, 147, 5, 5), "lin:#FFF2A8:#E0A100:100:142:100:152"))
    elif name == "crown":
        pts = [(CX - 22, 22), (CX - 24, 2), (CX - 13, 12), (CX, -6), (CX + 13, 12), (CX + 24, 2), (CX + 22, 22)]
        fr.append(L(path(pts[0], [("L", p) for p in pts[1:]]), "lin:#FFF2A8:#E0A100:100:-6:100:22"))
        fr.append(L(path(pts[0], [("L", p) for p in pts[1:]]), stroke="#B07800", width=1.0))
        fr.append(L(smooth([(CX - 23, 18), (CX + 23, 18), (CX + 23, 24), (CX - 23, 24)], t=0.3), "#E0A100"))
        for (x, c) in [(-12, "#FF4D6D"), (0, "#3DDCFF"), (12, "#FF4D6D")]:
            fr.append(L(ell(CX + x, 21, 2.4, 2.4), c))
        for x in (-24, 0, 24):
            fr.append(L(ell(CX + x, (2 if x else -6) - 1, 2, 2), "#FFFFFF"))
    elif name == "catears":
        for s in (1, -1):
            outer = path((CX + s * 14, 18), [("L", (CX + s * 30, -8)), ("L", (CX + s * 40, 30))])
            fr.append(L(outer, MAIN_HAIR))
            fr.append(L(outer, stroke="hairD", width=1.0, alpha=0.7))
            fr.append(L(path((CX + s * 20, 18), [("L", (CX + s * 30, 2)), ("L", (CX + s * 35, 26))]), "lin:#FFD0D8:#F59AA8:100:2:100:26"))
    elif name == "halo":
        fr.append(L(ell(CX, -6, 26, 6), stroke="#FFE27A", width=4))
        fr.append(L(ell(CX, -6, 26, 6), stroke="#FFFBE0", width=1.4))
        b.append(L(ell(CX, -6, 34, 12), "rad:#FFF3A0:0.5:100:-6:34"))
    elif name == "partyhat":
        cone = path((CX, -30), [("L", (CX + 18, 22)), ("C", (CX + 6, 26), (CX - 6, 26), (CX - 18, 22))])
        fr.append(L(cone, "lin:#B28CFF:#6C4BF4:100:-30:100:24"))
        for k in (0, 1, 2):
            fr.append(L(path((CX - 6 - k * 4, 6 + k * -12 + 10), [("L", (CX + 6 + k * 4, -6 + k * -12 + 22))], close=False), stroke="#FFE066", width=3, clip=cone))
        fr.append(L(ell(CX, -31, 5, 5), "#FFE066"))
    elif name == "flowercrown":
        fr.append(L(path((CX - 40, 40), [("C", (CX - 30, 18), (CX + 30, 18), (CX + 40, 40))], close=False), stroke="#3F9A4E", width=2.5))
        for i, x in enumerate(range(-36, 37, 12)):
            y = 40 - (1 - (x / 40) ** 2) * 16
            fr.append(L(ell(CX + x + 5, y + 1, 4, 2), "#7FD36E"))
            fr += flower(CX + x, y, 3.6, ["#FF7FAE", "#FFFFFF", "#B28CFF", "#FFD34E"][i % 4], "#FFE066" if i % 4 != 3 else "#FF8C42")
    elif name == "strawhat":
        fr.append(L(ell(CX, 30, 62, 13), "lin:#F6E2A0:#C9A04A:100:17:100:43"))
        fr.append(L(smooth([(CX - 30, 30), (CX - 26, 6), (CX, 0), (CX + 26, 6), (CX + 30, 30), (CX, 34)], t=0.9), "lin:#F6E2A0:#E8C66A:100:0:100:34"))
        fr.append(L(smooth([(CX - 30, 22), (CX + 30, 22), (CX + 30, 30), (CX, 34), (CX - 30, 30)], t=0.6), "#E94B5A"))
        for x in range(-50, 51, 10):
            fr.append(L(path((CX + x, 26), [("L", (CX + x * 1.08, 38))], close=False), stroke="#C9A04A", width=0.8, alpha=0.6))
    elif name == "witchhat":
        fr.append(L(ell(CX, 34, 56, 11), "lin:#5F4790:#2A1D44:100:23:100:45"))
        cone = smooth([(CX - 28, 32), (CX - 18, 4), (CX - 2, -24), (CX + 16, -42), (CX + 26, -40), (CX + 12, -22), (CX + 20, 4), (CX + 28, 32), (CX, 36)], t=0.8)
        fr.append(L(cone, "lin:#5F4790:#3B2A5C:80:-40:120:34"))
        fr.append(L(smooth([(CX - 26, 22), (CX + 26, 22), (CX + 28, 31), (CX, 35), (CX - 28, 31)], t=0.6), "#FF8C1A"))
        fr.append(L(smooth([(CX - 5, 23), (CX + 5, 23), (CX + 5, 32), (CX - 5, 32)], t=0.2), "#FFD34E"))
    elif name == "santahat":
        cone = smooth([(CX - 36, 34), (CX - 26, 4), (CX, -14), (CX + 24, -12), (CX + 40, 6), (CX + 44, 26), (CX + 34, 12), (CX + 30, 34)], t=0.8)
        fr.append(L(cone, "lin:#FF6B6B:#B8243A:100:-14:100:34"))
        fr.append(L(smooth([(CX - 40, 30), (CX - 20, 26), (CX, 25), (CX + 20, 26), (CX + 40, 30), (CX + 40, 40), (CX, 38), (CX - 40, 40)], t=0.7), "#FFFFFF"))
        fr.append(L(ell(CX + 44, 28, 7, 7), "#FFFFFF"))
        fr.append(L(ell(CX + 42, 26, 3, 3), "#DDE3EE", alpha=0.8))
    return b, fr


MAIN_HAIR = "lin:hair:hairS:100:-8:100:30"
