"""Vector pets in the same style as the character: soft gradients, a darker outline, big glossy eyes.

Box 240 x 200, standing on y = 196. Two frames per species (idle, then a tail wag / wing flap /
nibble...) plus an egg in the species' colours. `python pets.py` writes app/src/main/assets/pet_art.json
and pets.png (a preview sheet).
"""
import json, math, os, subprocess
from geo import L, ell, smooth, path, f

D = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(D, "..", "..", "app", "src", "main", "assets", "pet_art.json")
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
INK = "#2B2140"


def rot(pts, deg, pivot):
    a = math.radians(deg)
    c, s = math.cos(a), math.sin(a)
    px, py = pivot
    return [(px + (x - px) * c - (y - py) * s, py + (x - px) * s + (y - py) * c) for x, y in pts]


def lin(a, b, x1, y1, x2, y2):
    return f"lin:{a}:{b}:{f(x1)}:{f(y1)}:{f(x2)}:{f(y2)}"


def shape(d, fill, line, width=2.2):
    return [L(d, fill), L(d, stroke=line, width=width, alpha=0.9)]


def eyes(cx, cy, dx, r=9.0, look=(0, 0), lid=None):
    """Two big glossy eyes [dx] either side of cx."""
    out = []
    for s in (1, -1):
        x = cx + s * dx
        if lid == "closed":
            out.append(L(path((x - r, cy), [("C", (x - r * 0.5, cy + r * 0.7), (x + r * 0.5, cy + r * 0.7), (x + r, cy))], close=False), stroke=INK, width=2.6))
            continue
        out.append(L(ell(x, cy, r, r * 1.12), lin("#1B1530", "#4A3A66", x, cy - r, x, cy + r)))
        out.append(L(ell(x + look[0] - r * 0.3, cy + look[1] - r * 0.38, r * 0.42, r * 0.46), "#FFFFFF"))
        out.append(L(ell(x + look[0] + r * 0.38, cy + look[1] + r * 0.42, r * 0.18, r * 0.18), "#FFFFFF", alpha=0.85))
    return out


def blush(cx, cy, dx, rx=8.0):
    return [L(ell(cx + s * dx, cy, rx, rx * 0.55), f"rad:#FF8FA3:0.7:{f(cx + s * dx)}:{f(cy)}:{f(rx)}") for s in (1, -1)]


def smile(cx, cy, w=6.0, color=INK):
    return [L(path((cx - w, cy), [("C", (cx - w * 0.6, cy + w * 0.6), (cx - w * 0.1, cy + w * 0.6), (cx, cy)),
                                  ("C", (cx + w * 0.1, cy + w * 0.6), (cx + w * 0.6, cy + w * 0.6), (cx + w, cy))], close=False), stroke=color, width=2.0)]


def ground_shadow(cx=120, w=56):
    return [L(ell(cx, 196, w, 6), "#000000", alpha=0.12)]


# ------------------------------------------------------------------ species
def dog(frame):
    O, OS, C, LINE = "#F0A24E", "#D07A2E", "#FFF4E0", "#7A4422"
    out = ground_shadow()
    tail = rot([(160, 160), (182, 150), (192, 126), (184, 108), (176, 116), (178, 132), (168, 146), (156, 150)], -14 if frame else 6, (160, 160))
    out += shape(smooth(tail, t=0.9), lin(O, OS, 160, 150, 190, 110), LINE)
    out.append(L(smooth(rot([(184, 108), (190, 112), (188, 122), (180, 118)], -14 if frame else 6, (160, 160)), t=0.9), C))
    body = smooth([(120, 110), (152, 120), (166, 160), (160, 192), (120, 196), (80, 192), (74, 160), (88, 120)], t=0.9)
    out += shape(body, lin(O, OS, 120, 110, 120, 196), LINE)
    out.append(L(smooth([(120, 128), (138, 140), (140, 172), (120, 190), (100, 172), (102, 140)], t=0.9), C))
    for x in (100, 140):  # front paws
        out += shape(ell(x, 188, 13, 9), lin(C, "#F2DCC0", x, 180, x, 197), LINE, 2.0)
        out += [L(path((x - 3, 184), [("L", (x - 3, 190))], close=False), stroke=LINE, width=1.2, alpha=0.6),
                L(path((x + 3, 184), [("L", (x + 3, 190))], close=False), stroke=LINE, width=1.2, alpha=0.6)]
    for s in (1, -1):  # pointy ears
        ear = [(120 + s * 22, 52), (120 + s * 46, 22), (120 + s * 52, 70)]
        out += shape(smooth(ear, t=0.5), lin(O, OS, 120 + s * 40, 30, 120 + s * 40, 70), LINE)
        out.append(L(smooth([(120 + s * 30, 54), (120 + s * 44, 34), (120 + s * 46, 64)], t=0.5), "#FFC9B0"))
    head = smooth([(120, 34), (156, 42), (172, 76), (160, 112), (120, 124), (80, 112), (68, 76), (84, 42)], t=0.95)
    out += shape(head, lin("#F6B56A", O, 120, 34, 120, 124), LINE)
    out.append(L(smooth([(120, 84), (146, 90), (150, 110), (120, 122), (90, 110), (94, 90)], t=0.9), C))
    for s in (1, -1):
        out.append(L(smooth([(120 + s * 22, 66), (120 + s * 36, 64), (120 + s * 34, 72), (120 + s * 24, 72)], t=0.8), C, alpha=0.9))
    out += eyes(120, 80, 22, 8.5)
    out.append(L(ell(120, 96, 7, 5), lin("#3A2A3A", "#1B1530", 120, 91, 120, 101)))
    out.append(L(ell(118, 94, 2.2, 1.4), "#FFFFFF", alpha=0.8))
    out += smile(120, 103, 7)
    out += [L(path((116, 106), [("C", (117, 114), (123, 114), (124, 106))]), "#FF7C8F")]
    out += blush(120, 98, 34)
    return out


def cat(frame):
    G, GS, C, LINE = "#B4B9CC", "#7E8499", "#F6F6FB", "#4E5368"
    out = ground_shadow()
    tail = rot([(156, 186), (186, 176), (198, 146), (192, 112), (184, 108), (186, 140), (176, 168), (154, 176)], 10 if frame else -2, (156, 186))
    out += shape(smooth(tail, t=0.9), lin(G, GS, 160, 180, 196, 110), LINE)
    for k in range(3):
        a = rot([(186 + k * 2, 150 - k * 14), (196, 146 - k * 14)], 10 if frame else -2, (156, 186))
        out.append(L(path(a[0], [("L", a[1])], close=False), stroke=GS, width=3, alpha=0.8))
    body = smooth([(120, 112), (150, 122), (162, 160), (156, 192), (120, 196), (84, 192), (78, 160), (90, 122)], t=0.9)
    out += shape(body, lin(G, GS, 120, 112, 120, 196), LINE)
    out.append(L(smooth([(120, 128), (136, 142), (136, 176), (120, 190), (104, 176), (104, 142)], t=0.9), C))
    for x in (102, 138):
        out += shape(ell(x, 189, 12, 8.5), lin(C, "#E4E4EE", x, 181, x, 197), LINE, 2.0)
    for s in (1, -1):
        ear = [(120 + s * 20, 50), (120 + s * 44, 18), (120 + s * 50, 66)]
        out += shape(smooth(ear, t=0.4), lin(G, GS, 120 + s * 40, 26, 120 + s * 40, 66), LINE)
        out.append(L(smooth([(120 + s * 28, 52), (120 + s * 42, 30), (120 + s * 44, 60)], t=0.4), "#FFB8C6"))
    head = smooth([(120, 36), (156, 44), (172, 78), (160, 110), (120, 122), (80, 110), (68, 78), (84, 44)], t=0.95)
    out += shape(head, lin("#C6CADA", G, 120, 36, 120, 122), LINE)
    for dx, h in ((-8, 16), (0, 20), (8, 16)):  # tabby stripes
        out.append(L(path((120 + dx, 38), [("L", (120 + dx * 0.8, 38 + h))], close=False), stroke=GS, width=3.2, alpha=0.8))
    for s in (1, -1):
        out.append(L(path((120 + s * 50, 68), [("L", (120 + s * 40, 70))], close=False), stroke=GS, width=3, alpha=0.7))
    out.append(L(smooth([(120, 88), (140, 92), (142, 108), (120, 118), (98, 108), (100, 92)], t=0.9), C))
    out += eyes(120, 80, 22, 9)
    out.append(L(smooth([(115, 94), (125, 94), (120, 99)], t=0.3), "#FF8FA3"))
    out += smile(120, 100, 6)
    for s in (1, -1):
        for k in (-1, 1):
            out.append(L(path((120 + s * 26, 100 + k * 2), [("L", (120 + s * 48, 98 + k * 6))], close=False), stroke="#FFFFFF", width=1.2, alpha=0.9))
    out += blush(120, 96, 34)
    return out


def fish(frame):
    O, OS, W, LINE = "#FF8A3D", "#E0602A", "#FFFFFF", "#9A3A1A"
    out = []
    bob = -4 if frame else 0
    tail = rot([(70, 120), (36, 92), (30, 112), (38, 120), (30, 128), (36, 148)], 8 if frame else -6, (70, 120))
    out += shape(smooth([(p[0], p[1] + bob) for p in tail], t=0.6), lin("#FFB27A", O, 30, 120, 70, 120), LINE)
    fin_top = smooth([(96, 84 + bob), (120, 54 + bob), (150, 62 + bob), (156, 86 + bob)], t=0.8)
    out += shape(fin_top, lin("#FFB27A", O, 120, 56, 120, 86), LINE)
    body = smooth([(62, 120 + bob), (90, 82 + bob), (150, 78 + bob), (190, 104 + bob), (196, 124 + bob), (180, 148 + bob), (130, 164 + bob), (84, 156 + bob)], t=0.9)
    out += shape(body, lin("#FFA65E", OS, 120, 80, 120, 164), LINE)
    for x, w in ((152, 9), (112, 10)):  # white bands
        band = smooth([(x - w, 80 + bob), (x + w, 80 + bob), (x + w + 4, 120 + bob), (x + w, 160 + bob), (x - w, 160 + bob), (x - w + 4, 120 + bob)], t=0.6)
        out.append(L(band, W, clip=body))
        out.append(L(band, stroke=INK, width=1.6, alpha=0.6, clip=body))
    out.append(L(body, stroke=LINE, width=2.2, alpha=0.9))
    fin = rot([(130, 130 + bob), (118, 150 + bob), (138, 148 + bob)], -16 if frame else 6, (130, 130 + bob))
    out += shape(smooth(fin, t=0.6), "#FFB27A", LINE, 1.8)
    out += [L(ell(172, 110 + bob, 10, 11), lin("#1B1530", "#4A3A66", 172, 99, 172, 121)),
            L(ell(169, 106 + bob, 4.2, 4.6), "#FFFFFF"), L(ell(176, 114 + bob, 1.8, 1.8), "#FFFFFF", alpha=0.85)]
    out += smile(188, 128 + bob, 4)
    out.append(L(ell(176, 132 + bob, 6, 3.5), "rad:#FF8FA3:0.7:176:" + f(132 + bob) + ":6"))
    for (x, y, r) in ((206, 80, 5), (216, 60, 3.5), (200, 46, 2.5)):
        yy = y + (-6 if frame else 0)
        out += [L(ell(x, yy, r, r), "#BDEBFF", alpha=0.5), L(ell(x, yy, r, r), stroke="#7FC8E8", width=1.2, alpha=0.8),
                L(ell(x - r * 0.35, yy - r * 0.35, r * 0.3, r * 0.3), "#FFFFFF")]
    return out


def hamster(frame):
    O, OS, C, LINE = "#ECB868", "#C98C3E", "#FFF8EC", "#7A5228"
    out = ground_shadow(120, 60)
    for s in (1, -1):
        out += shape(ell(120 + s * 36, 58, 15, 14), lin(O, OS, 120, 46, 120, 72), LINE)
        out.append(L(ell(120 + s * 36, 60, 8, 7.5), "#FFC0B4"))
    body = smooth([(120, 50), (164, 64), (180, 120), (170, 178), (120, 196), (70, 178), (60, 120), (76, 64)], t=0.95)
    out += shape(body, lin("#F4CA84", OS, 120, 50, 120, 196), LINE)
    out.append(L(smooth([(120, 96), (156, 112), (158, 160), (120, 190), (82, 160), (84, 112)], t=0.95), C))
    for s in (1, -1):  # chubby cheeks
        out.append(L(ell(120 + s * 32, 112, 18, 15), C))
    out += eyes(120, 92, 24, 8)
    out.append(L(smooth([(116, 104), (124, 104), (120, 108)], t=0.3), "#FF8FA3"))
    out += smile(120, 110, 5)
    out += blush(120, 112, 34, 9)
    seed_y = 140 if frame else 148
    seed = smooth([(120, seed_y - 12), (128, seed_y - 2), (124, seed_y + 10), (116, seed_y + 10), (112, seed_y - 2)], t=0.8)
    out += shape(seed, lin("#5E4434", "#3A2A20", 120, seed_y - 12, 120, seed_y + 10), "#2A1A14", 1.6)
    out.append(L(path((120, seed_y - 8), [("L", (120, seed_y + 6))], close=False), stroke="#E8E0D0", width=2, alpha=0.8))
    for s in (1, -1):
        out += shape(ell(120 + s * 10, seed_y + 6, 7, 6), C, LINE, 1.6)
    for x in (96, 144):
        out += shape(ell(x, 190, 11, 6.5), "#FFD9CC", LINE, 1.6)
    return out


def parrot(frame):
    G, GS, B, Y, R, LINE = "#4CC36A", "#2E9A4E", "#3D7BFF", "#FFE066", "#E94B5A", "#1F6A36"
    out = ground_shadow(120, 40)
    out += shape(smooth([(110, 160), (100, 196), (116, 190), (120, 198), (124, 190), (140, 196), (130, 160)], t=0.5), lin(B, "#2A55C0", 120, 160, 120, 198), "#1E3E8A")
    body = smooth([(120, 76), (150, 92), (158, 136), (146, 172), (120, 180), (94, 172), (82, 136), (90, 92)], t=0.95)
    out += shape(body, lin(G, GS, 120, 76, 120, 180), LINE)
    out.append(L(smooth([(120, 110), (140, 124), (140, 160), (120, 174), (100, 160), (100, 124)], t=0.9), Y, alpha=0.9))
    for s in (1, -1):
        up = frame and s == 1
        wing = [(120 + s * 26, 100), (120 + s * 50, 116), (120 + s * 52, 150), (120 + s * 40, 170), (120 + s * 30, 140)]
        if up:
            wing = rot(wing, -40, (146, 104))
        out += shape(smooth(wing, t=0.8), lin(G, B, 120 + s * 30, 100, 120 + s * 46, 170), LINE)
        out.append(L(smooth([p for p in wing[2:]] + [wing[-1]], t=0.8), R, alpha=0.8))
    head = smooth([(120, 30), (150, 40), (160, 72), (146, 100), (120, 106), (94, 100), (80, 72), (90, 40)], t=0.95)
    out += shape(head, lin("#6AD884", G, 120, 30, 120, 106), LINE)
    for k in range(3):  # crest
        out += shape(smooth([(112 + k * 8, 36), (104 + k * 10, 14 - k * 2), (118 + k * 8, 30)], t=0.5), lin(R, G, 110, 14, 120, 36), LINE, 1.6)
    out += eyes(120, 66, 20, 8)
    beak = smooth([(108, 82), (132, 82), (130, 98), (120, 110), (112, 98)], t=0.7)
    out += shape(beak, lin("#FFB238", "#E08A10", 120, 82, 120, 110), "#8A5208", 1.8)
    out.append(L(path((110, 90), [("C", (116, 94), (124, 94), (130, 90))], close=False), stroke="#8A5208", width=1.4, alpha=0.7))
    out += blush(120, 82, 30, 7)
    for x in (110, 130):
        out += [L(path((x - 6, 180), [("L", (x + 6, 180))], close=False), stroke="#E08A10", width=3)]
    return out


def hedgehog(frame):
    S, SD, C, LINE = "#8A6448", "#5E4130", "#F7E2BE", "#4A3020"
    out = ground_shadow(120, 66)
    squash = 3 if frame else 0
    n = 22
    spikes = []
    for i in range(n + 1):
        a = math.radians(205 - i * (230 / n))
        r = 78 if i % 2 == 0 else 60
        spikes.append((120 + math.cos(a) * r * 1.08, 146 + squash - math.sin(a) * r * (0.96 - squash * 0.01)))
    back = path(spikes[0], [("L", p) for p in spikes[1:]] + [("C", (200, 190), (40, 190), spikes[0])])
    out += shape(back, lin(S, SD, 120, 70, 120, 186), LINE)
    out.append(L(ell(120, 140 + squash, 62, 54), "#A07A5A", alpha=0.35))
    for i in range(1, n, 2):
        a = math.radians(205 - i * (230 / n))
        out.append(L(path((120 + math.cos(a) * 40, 146 - math.sin(a) * 36), [("L", (120 + math.cos(a) * 64, 146 + squash - math.sin(a) * 58))], close=False),
                     stroke="#B08A6A", width=2, alpha=0.7))
    face = smooth([(120, 92 + squash), (154, 104 + squash), (166, 142), (150, 184), (120, 192), (90, 184), (74, 142), (86, 104 + squash)], t=0.95)
    out += shape(face, lin("#FFF0D4", "#E8C9A0", 120, 92, 120, 192), LINE)
    for s in (1, -1):
        out += shape(ell(120 + s * 34, 108 + squash, 9, 8), "#E8C9A0", LINE, 1.6)
    out += eyes(120, 136, 20, 8)
    out.append(L(ell(120, 154, 7, 5.5), lin("#3A2A3A", "#1B1530", 120, 149, 120, 160)))
    out.append(L(ell(118, 152, 2, 1.3), "#FFFFFF", alpha=0.8))
    out += smile(120, 162, 6)
    out += blush(120, 156, 30, 8)
    for x in (100, 140):
        out += shape(ell(x, 191, 10, 6), "#E8C9A0", LINE, 1.6)
    return out


def lizard(frame):
    Y, YS, SP, C, LINE = "#F2C14E", "#D99A2E", "#8A5A2B", "#FFE9A8", "#7A4A18"
    out = ground_shadow(120, 70)
    tail = [(150, 176), (190, 180), (214, 166), (220, 144), (210, 134), (204, 148), (190, 164), (160, 166)]
    tail = rot(tail, -10 if frame else 0, (150, 172))
    out += shape(smooth(tail, t=0.9), lin(Y, YS, 150, 170, 220, 140), LINE)
    for i, (x, y) in enumerate([(176, 172), (198, 168), (212, 152)]):
        p = rot([(x, y)], -10 if frame else 0, (150, 172))[0]
        out.append(L(ell(p[0], p[1] - 2, 4, 3), SP, alpha=0.8))
    body = smooth([(80, 140), (120, 128), (160, 140), (166, 170), (140, 188), (96, 188), (74, 170)], t=0.9)
    out += shape(body, lin(Y, YS, 120, 128, 120, 188), LINE)
    for (x, y) in [(104, 150), (126, 144), (146, 156), (114, 172), (136, 174)]:
        out.append(L(ell(x, y, 5, 4), SP, alpha=0.8))
    for x in (88, 152):
        out += shape(smooth([(x - 10, 180), (x + 10, 180), (x + 14, 194), (x - 14, 194)], t=0.5), lin(Y, YS, x, 180, x, 194), LINE, 1.8)
        for k in (-8, 0, 8):
            out.append(L(ell(x + k, 195, 3.2, 2.4), C))
    head = smooth([(120, 70), (160, 80), (176, 108), (160, 134), (120, 142), (80, 134), (64, 108), (80, 80)], t=0.95)
    out += shape(head, lin("#F8D06A", Y, 120, 70, 120, 142), LINE)
    for (x, y) in [(100, 84), (118, 78), (138, 84)]:
        out.append(L(ell(x, y, 4.5, 3.5), SP, alpha=0.7))
    out += eyes(120, 106, 30, 10.5)
    out.append(L(path((92, 124), [("C", (108, 134), (132, 134), (148, 124))], close=False), stroke=LINE, width=2.2))
    out += blush(120, 122, 40, 8)
    for s in (1, -1):
        out.append(L(ell(120 + s * 6, 118, 1.4, 1), LINE))
    return out


def egg(main):
    out = ground_shadow(120, 42)
    shell = smooth([(120, 50), (152, 70), (170, 120), (160, 170), (120, 194), (80, 170), (70, 120), (88, 70)], t=0.95)
    out += [L(shell, lin("#FFFDF6", "#EADFCB", 110, 60, 130, 194))]
    for (x, y, rx, ry) in [(104, 92, 10, 8), (140, 116, 12, 10), (110, 150, 9, 7), (146, 162, 7, 6), (130, 74, 6, 5)]:
        out.append(L(ell(x, y, rx, ry), main, alpha=0.85, clip=shell))
    out.append(L(smooth([(98, 72), (108, 66), (100, 96), (92, 110), (90, 92)], t=0.8), "#FFFFFF", alpha=0.75))
    out.append(L(shell, stroke="#8A7A66", width=2.2, alpha=0.8))
    return out


SPECIES = {"DOG": (dog, "#F0A24E"), "CAT": (cat, "#9EA4BA"), "FISH": (fish, "#FF8A3D"), "HAMSTER": (hamster, "#ECB868"),
           "PARROT": (parrot, "#4CC36A"), "HEDGEHOG": (hedgehog, "#8A6448"), "LIZARD": (lizard, "#F2C14E")}


def enc(layers):
    return [[l.d, l.fill or "", l.stroke or "", round(l.width, 2), round(l.alpha, 2), l.clip or ""] for l in layers if l.fill or l.stroke]


def svg(layers, gid):
    defs, items = [], []
    for i, l in enumerate(layers):
        fill = "none"
        if l.fill and l.fill.startswith("lin:"):
            _, a, b, x1, y1, x2, y2 = l.fill.split(":")
            defs.append(f'<linearGradient id="g{gid}_{i}" gradientUnits="userSpaceOnUse" x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}"><stop offset="0" stop-color="{a}"/><stop offset="1" stop-color="{b}"/></linearGradient>')
            fill = f"url(#g{gid}_{i})"
        elif l.fill and l.fill.startswith("rad:"):
            _, a, al, cx, cy, r = l.fill.split(":")
            defs.append(f'<radialGradient id="g{gid}_{i}" gradientUnits="userSpaceOnUse" cx="{cx}" cy="{cy}" r="{r}"><stop offset="0" stop-color="{a}" stop-opacity="{al}"/><stop offset="1" stop-color="{a}" stop-opacity="0"/></radialGradient>')
            fill = f"url(#g{gid}_{i})"
        elif l.fill:
            fill = l.fill
        stroke = f' stroke="{l.stroke}" stroke-width="{l.width}" stroke-linecap="round" stroke-linejoin="round"' if l.stroke else ""
        clip = ""
        if l.clip:
            defs.append(f'<clipPath id="c{gid}_{i}"><path d="{l.clip}"/></clipPath>')
            clip = f' clip-path="url(#c{gid}_{i})"'
        items.append(f'<path d="{l.d}" fill="{fill}"{stroke} opacity="{l.alpha}"{clip}/>')
    return "".join(defs), "".join(items)


def preview(data):
    cells, S = [], 1.2
    W, H = int(240 * S), int(200 * S)
    rows = list(data.items())
    for r, (name, (fr, eg)) in enumerate(rows):
        for c, layers in enumerate(fr + [eg]):
            defs, items = svg(layers, f"{r}_{c}")
            cells.append(f'<svg x="{c * W}" y="{r * H}" width="{W}" height="{H}" viewBox="0 0 240 200"><defs>{defs}</defs>{items}</svg>')
    total = f'<svg xmlns="http://www.w3.org/2000/svg" width="{3 * W}" height="{len(rows) * H}"><rect width="100%" height="100%" fill="#7EC8F0"/>{"".join(cells)}</svg>'
    p = os.path.join(D, "pets.svg")
    open(p, "w", encoding="utf-8").write(total)
    subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", f"--screenshot={os.path.join(D, 'pets.png')}",
                    f"--window-size={3 * W},{len(rows) * H}", "file:///" + p.replace("\\", "/")], capture_output=True)
    os.remove(p)


def main():
    data = {name: ([fn(0), fn(1)], egg(main)) for name, (fn, main) in SPECIES.items()}
    out = {name: {"frames": [enc(a) for a in fr], "egg": enc(eg)} for name, (fr, eg) in data.items()}
    s = json.dumps({"version": 1, "w": 240, "h": 200, "pets": out}, separators=(",", ":"))
    open(OUT, "w", encoding="utf-8").write(s)
    preview(data)
    print("pets", len(out), "bytes", len(s))


if __name__ == "__main__":
    main()
