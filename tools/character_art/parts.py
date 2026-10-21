"""Character parts as layers. Body types: F (feminine) / M (masculine)."""
from geo import *

# ---------------------------------------------------------------- body proportions
# Right half outlines (x offset from the centre line); mirrored for the left.
TORSO = {
    "F": [(8, 136), (19, 139), (26, 143), (28, 151), (25, 168), (19.5, 196), (19, 214), (24, 234), (27.5, 250), (24, 262), (12, 266), (0, 267)],
    "M": [(10, 136), (24, 139), (32, 143), (34, 152), (30, 172), (26, 200), (25, 222), (25.5, 240), (26, 254), (22, 264), (11, 267), (0, 268)],
}
NECK = {"F": 7.5, "M": 10}
LEG = {  # (x, y, half width) down the right leg
    "F": [(13.5, 246, 13.5), (12.5, 290, 12.5), (11, 330, 10), (10.2, 360, 7.4), (10.6, 388, 8), (10.6, 425, 5.6), (10.6, 455, 4.2)],
    "M": [(14, 246, 14), (13.5, 290, 13.2), (12, 330, 11), (11.2, 360, 8.4), (11.6, 388, 9), (11.6, 425, 6.3), (11.6, 455, 4.9)],
}
ARM = {
    "F": [(25, 146, 6.6), (28.5, 172, 5.6), (30.5, 198, 4.6), (32.5, 224, 4.1), (34, 248, 3.3)],
    "M": [(31, 147, 8), (35, 174, 6.8), (37, 200, 5.6), (39, 226, 5), (40.5, 250, 4)],
}
Y = dict(neck=112, sh=140, ch=165, wa=205, hip=240, crotch=262, knee=360, ankle=455, foot=470)
BODY = {bt: dict(sh=TORSO[bt][3][0], wa=TORSO[bt][6][0], hip=TORSO[bt][8][0], an=LEG[bt][-1][0]) for bt in TORSO}


def outline(right, grow=0.0, until=None):
    pts = [(CX + x + (grow if x > 0 else 0), y) for x, y in right if until is None or y <= until]
    if until is not None:
        pts.append((CX, until))
    full = pts + [mx(p) for p in pts[::-1][1:-1]]
    return smooth(full, closed=True, t=1.0)


def leg_spine(bt, s, grow=0.0, until=None):
    return [(CX + s * x, y, w + grow) for (x, y, w) in LEG[bt] if until is None or y <= until]


def arm_spine(bt, s, grow=0.0, until=None):
    return [(CX + s * x, y, w + grow) for (x, y, w) in ARM[bt] if until is None or y <= until]


def body(bt):
    out = []
    nk = NECK[bt]
    out.append(L(sym((CX, Y["neck"] - 4), [("L", (CX + nk, Y["neck"] - 4)), ("C", (CX + nk, 125), (CX + nk + 1, 132), (CX + nk + 7, 140)), ("L", (CX, 141))]), "skin"))
    out.append(L(sym((CX, Y["neck"] + 2), [("L", (CX + nk, Y["neck"] + 2)), ("C", (CX + nk, 119), (CX + 3, 124), (CX, 125))]), "skinS", alpha=0.85))
    for s in (1, -1):
        out.append(L(limb(leg_spine(bt, s)), "lin:skinS:skinL:" + f"{CX + s * 2}:300:{CX + s * 24}:300"))
        # knee and shin highlights
        kx = CX + s * LEG[bt][3][0]
        out.append(L(path((kx - 2.5, 356), [("C", (kx - 1, 352), (kx + 1, 352), (kx + 2.5, 356))], close=False), stroke="skinD", width=0.8, alpha=0.4))
    out.append(L(outline(TORSO[bt]), "skin"))
    return out


def arms(bt):
    out = []
    for s in (1, -1):
        sp = arm_spine(bt, s)
        out.append(L(limb(sp), "lin:skinL:skinS:" + f"{CX + s * 22}:200:{CX + s * 40}:200"))
        hx, hy = sp[-1][0], sp[-1][1]
        w = 4.6 if bt == "F" else 5.6
        hand = smooth([(hx - s * 3.2, hy - 2), (hx + s * 3.8, hy - 1.5), (hx + s * w, hy + 6), (hx + s * 3, hy + 12),
                       (hx - s * 1, hy + 13.5), (hx - s * 3.6, hy + 9), (hx - s * 4.5, hy + 3)], closed=True)
        out.append(L(hand, "skin"))
        out.append(L(path((hx - s * 4.2, hy + 4), [("C", (hx - s * 5.8, hy + 7), (hx - s * 5, hy + 10), (hx - s * 3, hy + 10))], close=False), stroke="skinD", width=0.8, alpha=0.6))
    return out


# ---------------------------------------------------------------- head
def head(shape="oval"):
    jaw = {"oval": (34, 11, 7), "round": (36, 15, 11), "sharp": (33, 8, 5), "square": (36, 17, 9)}[shape]
    w, jw, cw = jaw
    out = []
    # ears
    for s in (1, -1):
        out.append(L(path((CX + s * (w - 2), 74), [("C", (CX + s * (w + 7), 68), (CX + s * (w + 9), 88), (CX + s * (w - 1), 94))]), "skin"))
        out.append(L(path((CX + s * (w - 1), 77), [("C", (CX + s * (w + 4), 74), (CX + s * (w + 5), 86), (CX + s * w, 89))], close=False), stroke="skinD", width=1.1, alpha=0.8))
    face = sym((CX, 26), [("C", (CX + w * 0.75, 26), (CX + w, 44), (CX + w, 66)),
                          ("C", (CX + w, 86), (CX + jw + 10, 104), (CX + jw, 113)),
                          ("C", (CX + cw, 119), (CX + 4, 121), (CX, 121))])
    out.append(L(face, "lin:skinL:skin:100:40:100:120"))
    # soft shade along the cheek and jaw
    for s in (1, -1):
        out.append(L(path((CX + s * (w - 0.5), 70), [("C", (CX + s * w, 88), (CX + s * (jw + 10), 104), (CX + s * jw, 113)),
                                                     ("C", (CX + s * cw, 119), (CX + s * 4, 121), (CX, 121)),
                                                     ("C", (CX + s * 6, 118), (CX + s * (jw + 4), 108), (CX + s * (w - 5), 88))]), "skinS", alpha=0.35))
    out.append(L(face, stroke="line", width=1.0, alpha=0.55))
    return out


# ---------------------------------------------------------------- eyes
EYE_X, EYE_Y = 16, 84

EYE_STYLES = {
    #          inner        c1            c2           outer        l1          l2
    "almond": [(-8, 1.5), (-4, -7.5), (5, -8.5), (9.5, -1.5), (6.5, 6), (-3, 6.5)],
    "round":  [(-8, 1), (-5, -9), (5, -9.5), (9, 0.5), (6.5, 8), (-4, 8)],
    "sharp":  [(-8, 2.5), (-3, -5.5), (6, -8.5), (10.5, -4), (7, 4), (-3, 6)],
    "gentle": [(-8, 0), (-4, -7.5), (5, -7), (9.5, 2.5), (6, 7), (-3, 6.5)],
}


def eyes(style="almond", mood="normal", bt="F"):
    out = []
    wing = 4 if bt == "F" else 1.2
    I, c1, c2, O, l1, l2 = EYE_STYLES[style]
    for s in (1, -1):
        ex = CX + s * EYE_X
        def a(p, dy=0.0):
            return (ex + s * p[0], EYE_Y + p[1] + dy)
        # double-lid crease
        out.append(L(path(a((I[0] + 2, c1[1] - 1)), [("C", a((c1[0], c1[1] - 4.2)), a((c2[0], c2[1] - 4.2)), a((O[0] + 0.5, O[1] - 5)))], close=False),
                     stroke="skinD", width=0.8, alpha=0.45))
        if mood in ("happy", "calm", "sleepy"):
            if mood == "happy":
                d = path(a((I[0], 3)), [("C", a((c1[0], -5)), a((c2[0], -5.5)), a((O[0], 2)))], close=False)
            elif mood == "calm":
                d = path(a(I), [("C", a((c1[0], 5)), a((c2[0], 5)), a(O))], close=False)
            else:
                d = path(a(I), [("C", a((c1[0], 2.5)), a((c2[0], 2.5)), a(O))], close=False)
            out.append(L(d, stroke="eyeLine", width=2.3))
            out.append(L(path(a(O), [("L", a((O[0] + 3, O[1] - 2)))], close=False), stroke="eyeLine", width=1.5))
            continue
        white = path(a(I), [("C", a(c1), a(c2), a(O)), ("C", a(l1), a(l2), a(I))])
        out.append(L(white, "white"))
        cx_, cy_ = ex + s * 0.4, EYE_Y + 1.2
        if mood == "love":
            hx, hy = cx_, cy_
            heart = (f"M{f(hx)},{f(hy + 5.5)} C{f(hx - 8)},{f(hy)} {f(hx - 5.5)},{f(hy - 7)} {f(hx)},{f(hy - 2.8)} "
                     f"C{f(hx + 5.5)},{f(hy - 7)} {f(hx + 8)},{f(hy)} {f(hx)},{f(hy + 5.5)}Z")
            out.append(L(heart, "#FF4F8B", clip=white))
            out.append(L(ell(hx - 2.5, hy - 2.5, 1.4, 1.4), "white", alpha=0.9))
        elif mood == "sick":
            out.append(L(path((cx_ - 3.8, cy_ - 3.8), [("L", (cx_ + 3.8, cy_ + 3.8))], close=False), stroke="eyeLine", width=1.9))
            out.append(L(path((cx_ + 3.8, cy_ - 3.8), [("L", (cx_ - 3.8, cy_ + 3.8))], close=False), stroke="eyeLine", width=1.9))
        else:
            k = 0.82 if mood == "angry" else 1.0
            out.append(L(ell(cx_, cy_, 6.3 * k, 7.6 * k), "lin:irisD:iris:100:77:100:91", clip=white))
            out.append(L(ell(cx_, cy_ + 3.6, 4.4 * k, 3.0 * k), "irisL", alpha=0.55, clip=white))
            out.append(L(ell(cx_, cy_ + 0.4, 2.7 * k, 3.3 * k), "pupil", clip=white))
            # shadow of the upper lid
            out.append(L(ell(ex + s * 0.5, EYE_Y - 6.5, 11, 4.5), "eyeLine", alpha=0.28, clip=white))
            out.append(L(ell(cx_ - 2.4, cy_ - 2.8, 2.0, 2.3), "white", clip=white))
            out.append(L(ell(cx_ + 2.5, cy_ + 3.2, 1.0, 1.0), "white", alpha=0.85, clip=white))
            if mood == "excited":
                out.append(L(ell(cx_ + 1.8, cy_ - 1, 1.2, 1.2), "white", clip=white))
        # upper lash band: thin at the inner corner, thick + winged at the outer one
        lash = path(a(I, 0.4), [("C", a(c1), a(c2), a(O)),
                                ("L", a((O[0] + wing, O[1] - wing * 0.8))),
                                ("C", a((O[0] + 1.5, O[1] - 2.6)), a((c2[0] + 0.5, c2[1] - 2.4)), a((c2[0] - 1, c2[1] - 2.2))),
                                ("C", a((c1[0], c1[1] - 1.8)), a((I[0] + 1, I[1] - 1)), a((I[0] - 0.6, I[1] - 0.2)))])
        out.append(L(lash, "eyeLine"))
        out.append(L(path(a((l2[0] + 1, l2[1] - 0.3)), [("C", a((2, l2[1] + 0.4)), a((l1[0] - 1, l1[1] - 0.4)), a((O[0] - 0.3, O[1] + 1.4)))], close=False),
                     stroke="eyeLine", width=0.9, alpha=0.55))
    if mood == "sad":
        out.append(L(path((CX + EYE_X + 5, EYE_Y + 6), [("C", (CX + EYE_X + 3, EYE_Y + 10), (CX + EYE_X + 4, EYE_Y + 13), (CX + EYE_X + 5.5, EYE_Y + 13)),
                                                        ("C", (CX + EYE_X + 7, EYE_Y + 13), (CX + EYE_X + 7.5, EYE_Y + 10), (CX + EYE_X + 5, EYE_Y + 6))]), "#9EDCFF", alpha=0.9))
    return out


def brows(style="natural", mood="normal"):
    out = []
    lift = {"sad": (-2, 2.5), "angry": (3, -2.5), "excited": (-2, -1)}.get(mood, (0, 0))
    for s in (1, -1):
        bx = CX + s * 15.5
        by = 70
        def a(p):
            return (bx + s * p[0], by + p[1])
        inner_dy, outer_dy = lift[0], lift[1]
        if style == "straight":
            d = path(a((-8, 1 + inner_dy)), [("C", a((-3, -0.5 + inner_dy)), a((3, -0.5)), a((8, 0.5 + outer_dy))), ("C", a((3, 1.5)), a((-3, 2 + inner_dy)), a((-8, 2.6 + inner_dy)))])
        elif style == "arched":
            d = path(a((-8, 1.5 + inner_dy)), [("C", a((-3, -3.5 + inner_dy)), a((3, -4)), a((8.5, 0.5 + outer_dy))), ("C", a((3, -2)), a((-3, -1.4 + inner_dy)), a((-8, 2.8 + inner_dy)))])
        elif style == "thick":
            d = path(a((-8.5, 0.5 + inner_dy)), [("C", a((-3, -2.5 + inner_dy)), a((3, -3)), a((8.5, 0 + outer_dy))), ("C", a((3, 1)), a((-3, 2.3 + inner_dy)), a((-8.5, 3.8 + inner_dy)))])
        else:
            d = path(a((-8, 1.5 + inner_dy)), [("C", a((-3, -2.2 + inner_dy)), a((3, -2.6)), a((8, 0.6 + outer_dy))), ("C", a((3, -0.8)), a((-3, -0.3 + inner_dy)), a((-8, 2.8 + inner_dy)))])
        out.append(L(d, "hairD"))
    return out


def nose():
    return [L(path((CX + 0.5, 93), [("C", (CX + 2, 97), (CX + 2.5, 99), (CX + 0.5, 100.5))], close=False), stroke="skinD", width=1.0, alpha=0.7),
            L(ell(CX - 1.5, 100.8, 1.6, 0.8), "skinD", alpha=0.35)]


def mouth(mood="normal"):
    y = 107.5
    if mood in ("happy", "excited", "love"):
        open_ = path((CX - 6, y - 1), [("C", (CX - 3, y + 0), (CX + 3, y + 0), (CX + 6, y - 1)), ("C", (CX + 4, y + 7), (CX - 4, y + 7), (CX - 6, y - 1))])
        return [L(open_, "#8E2F45"), L(path((CX - 3.5, y + 3.6), [("C", (CX - 1, y + 2), (CX + 1, y + 2), (CX + 3.5, y + 3.6)), ("C", (CX + 2, y + 5.4), (CX - 2, y + 5.4), (CX - 3.5, y + 3.6))]), "#FF8FA3"),
                L(path((CX - 6, y - 1), [("C", (CX - 3, y + 0.3), (CX + 3, y + 0.3), (CX + 6, y - 1))], close=False), stroke="lipLine", width=0.9)]
    if mood in ("sad", "angry"):
        return [L(path((CX - 4.5, y + 2), [("C", (CX - 2, y - 0.5), (CX + 2, y - 0.5), (CX + 4.5, y + 2))], close=False), stroke="lipLine", width=1.3)]
    if mood == "sleepy":
        return [L(ell(CX, y + 1, 2.2, 1.6), "#9E4A5C")]
    if mood == "sick":
        return [L(path((CX - 5, y + 1), [("C", (CX - 3, y - 1), (CX - 1, y + 3), (CX, y + 1)), ("C", (CX + 1, y - 1), (CX + 3, y + 3), (CX + 5, y + 1))], close=False), stroke="lipLine", width=1.1)]
    # normal / calm: soft smile with lip tint
    return [L(path((CX - 4.5, y + 1.6), [("C", (CX - 2, y + 0.2), (CX + 2, y + 0.2), (CX + 4.5, y + 1.6)), ("C", (CX + 2, y + 4), (CX - 2, y + 4), (CX - 4.5, y + 1.6))]), "lip", alpha=0.75),
            L(path((CX - 5, y), [("C", (CX - 2, y + 2.6), (CX + 2, y + 2.6), (CX + 5, y))], close=False), stroke="lipLine", width=1.0)]


def blush(strong=False):
    a = 0.55 if strong else 0.35
    return [L(ell(CX + s * 20, 96, 7, 3.6), f"rad:blush:{a}:{CX + s * 20}:96:7.5") for s in (1, -1)]
