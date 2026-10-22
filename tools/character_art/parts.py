"""Character parts as layers. Body types: F (feminine) / M (masculine)."""
from geo import *

# ---------------------------------------------------------------- body proportions
# Right half outlines (x offset from the centre line); mirrored for the left.
TORSO = {
    "F": [(7.5, 134), (17, 137), (25, 141), (28, 149), (26, 164), (21.5, 186), (19.5, 204), (21.5, 222), (26, 238), (28, 250), (25, 262), (13, 267), (0, 268)],
    "M": [(10, 134), (23, 137), (32, 141), (34.5, 150), (32, 168), (28, 190), (26, 210), (26, 226), (26.5, 242), (27, 254), (23, 264), (12, 268), (0, 269)],
}
NECK = {"F": 7.2, "M": 9.6}
LEG = {  # (x, y, half width) down the right leg
    "F": [(13.8, 248, 13.8), (13.2, 285, 12.8), (11.8, 322, 10.6), (10.6, 356, 7.8), (10.8, 384, 8.6), (10.6, 420, 6.2), (10.4, 452, 4.6)],
    "M": [(14.4, 248, 14.4), (14, 285, 13.6), (12.6, 322, 11.4), (11.6, 356, 8.8), (11.8, 384, 9.6), (11.6, 420, 7), (11.4, 452, 5.4)],
}
ARM = {
    "F": [(25.2, 145, 6.5), (27.6, 170, 5.6), (28.6, 196, 4.7), (29.4, 220, 4.2), (30.6, 242, 3.5)],
    "M": [(31.5, 146, 8), (34.2, 172, 6.9), (35.4, 198, 5.9), (36.2, 222, 5.2), (37.4, 245, 4.4)],
}
Y = dict(neck=112, sh=140, ch=165, wa=204, hip=242, crotch=264, knee=356, ankle=452, foot=470)


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


def leg_region(s):
    return "legR" if s > 0 else "legL"


def arm_region(s):
    return "armR" if s > 0 else "armL"


def body(bt):
    out = []
    nk = NECK[bt]
    neck = sym((CX, 104), [("L", (CX + nk - 0.6, 104)), ("C", (CX + nk, 122), (CX + nk + 1.5, 130), (CX + nk + 9, 138)), ("L", (CX, 139))])
    out.append(L(neck, "lin:skinS:skin:100:110:100:136", region="torso"))
    # shadow the head casts on the neck
    out.append(L(sym((CX, 104), [("L", (CX + nk, 104)), ("C", (CX + nk, 122), (CX + 4, 128), (CX, 129))]), "skinS", alpha=0.7, region="torso"))
    # collarbones
    for s in (1, -1):
        out.append(L(path((CX + s * 4, 141), [("C", (CX + s * 9, 139.5), (CX + s * 14, 140), (CX + s * 18, 139))], close=False), stroke="skinD", width=0.8, alpha=0.35, region="torso"))
    for s in (1, -1):
        sp = leg_spine(bt, s)
        out.append(L(limb(sp), "lin:skinS:skinL:" + f"{CX + s * 1}:300:{CX + s * 22}:300", region=leg_region(s)))
        # inner-leg shade, knee and ankle bone
        kx = CX + s * LEG[bt][3][0]
        out.append(L(path((CX + s * 3, 268), [("C", (CX + s * 4, 300), (CX + s * 5, 330), (kx - s * 5, 356))], close=False), stroke="skinS", width=2.2, alpha=0.35, region=leg_region(s)))
        out.append(L(path((kx - 3, 353), [("C", (kx - 1.5, 349), (kx + 1.5, 349), (kx + 3, 353))], close=False), stroke="skinD", width=0.8, alpha=0.4, region=leg_region(s)))
    out.append(L(outline(TORSO[bt]), "lin:skinL:skinS:100:140:100:268", region="torso"))
    return out


def arms(bt):
    out = []
    for s in (1, -1):
        sp = arm_spine(bt, s)
        out.append(L(limb(sp), "lin:skinL:skinS:" + f"{CX + s * 22}:200:{CX + s * 38}:200", region=arm_region(s)))
        ex, ey = sp[2][0], sp[2][1]
        out.append(L(path((ex - s * 2, ey - 1), [("C", (ex - s * 0.5, ey + 1), (ex + s * 1, ey + 1), (ex + s * 2.4, ey - 0.5))], close=False), stroke="skinD", width=0.7, alpha=0.35, region=arm_region(s)))
    return out


def hands(bt):
    """Relaxed hands (drawn over the clothes so sleeves and hips never hide them)."""
    out = []
    for s in (1, -1):
        hx, hy, hw = arm_spine(bt, s)[-1]
        k = 1.08 if bt == "F" else 1.2
        def h(dx, dy):
            return (hx + s * dx * k, hy + dy * k)
        palm = smooth([h(-3.6, -2), h(3.4, -2), h(4.6, 4), h(4.4, 9.5), h(2.6, 13.6), h(0, 14.6), h(-2.6, 13.4), h(-3.8, 9), h(-4.2, 3)], closed=True, t=0.9)
        out.append(L(palm, "lin:skinL:skinS:" + f"{f(hx)}:{f(hy)}:{f(hx)}:{f(hy + 15 * k)}", region=arm_region(s)))
        # thumb in front, finger lines
        thumb = smooth([h(-3.6, 1), h(-1.2, 3.5), h(-0.6, 8), h(-2, 10), h(-4.2, 7.5), h(-4.6, 3.5)], closed=True, t=0.9)
        out.append(L(thumb, "skin", region=arm_region(s)))
        out.append(L(thumb, stroke="skinD", width=0.6, alpha=0.45, region=arm_region(s)))
        for dx in (0.9, 2.6):
            out.append(L(path(h(dx, 9), [("L", h(dx + 0.2, 13.2))], close=False), stroke="skinD", width=0.5, alpha=0.4, region=arm_region(s)))
        out.append(L(palm, stroke="skinD", width=0.6, alpha=0.45, region=arm_region(s)))
    return out


# ---------------------------------------------------------------- head
# Right half of the face outline from the temple down to the chin: (x offset, y). Same point count for
# every shape so they also work as morph targets.
FACE_SHAPES = {
    "oval":   [(33.5, 58), (34, 72), (32.6, 86), (28, 99), (19.5, 109.5), (8, 114.2)],
    "round":  [(34.5, 58), (35.2, 72), (34.6, 87), (31.2, 100), (22.5, 110), (9, 113.8)],
    "sharp":  [(33, 58), (33.6, 72), (32, 86), (26.6, 99), (16.5, 109.6), (5.5, 115)],
    "square": [(34.8, 58), (35.4, 72), (35, 88), (33, 100.5), (25, 109.6), (10, 113.2)],
}
CHIN_Y = {"oval": 115.4, "round": 114.8, "sharp": 116.2, "square": 114.2}


def face_outline(shape="oval"):
    right = [(CX, 23.5), (CX + 19, 26), (CX + 29, 36), (CX + 32.6, 46)] + [(CX + x, y) for x, y in FACE_SHAPES[shape]]
    chin = (CX, CHIN_Y[shape])
    full = right + [chin] + [mx(p) for p in right[::-1][:-1]]
    return smooth(full, closed=True, t=0.95)


def head(shape="oval"):
    out = []
    pts = FACE_SHAPES[shape]
    w = pts[1][0]
    # ears
    for s in (1, -1):
        ear = path((CX + s * (w - 2.5), 73), [("C", (CX + s * (w + 5), 66.5), (CX + s * (w + 8.5), 80), (CX + s * (w + 4.5), 89)),
                                               ("C", (CX + s * (w + 2.5), 93), (CX + s * (w - 1), 94.5), (CX + s * (w - 2.5), 92))])
        out.append(L(ear, "lin:skinS:skin:" + f"{CX + s * w}:80:{CX + s * (w + 7)}:80", region="head"))
        out.append(L(path((CX + s * (w - 0.5), 76.5), [("C", (CX + s * (w + 4), 73.5), (CX + s * (w + 5.5), 82), (CX + s * (w + 2.5), 88)),
                                                        ("C", (CX + s * (w + 1.5), 84), (CX + s * (w + 1), 80), (CX + s * w, 78.5))]), "skinD", alpha=0.45))
    face = face_outline(shape)
    out.append(L(face, "lin:skinL:skin:100:30:100:112"))
    # warm glow on the cheeks and a cool shade towards the jaw and the sides
    out.append(L(face, "rad:skinL:0.75:100:72:30"))
    for s in (1, -1):
        x2, y2 = pts[3]
        x3, y3 = pts[4]
        out.append(L(smooth([(CX + s * (pts[0][0] - 0.3), 60), (CX + s * (pts[1][0] - 0.2), 72), (CX + s * (pts[2][0] - 0.2), 86), (CX + s * (x2 - 0.2), y2),
                             (CX + s * (x3 - 0.2), y3), (CX + s * (x3 - 4), y3 - 2.5), (CX + s * (x2 - 5), y2 - 4), (CX + s * (pts[2][0] - 4.5), 85),
                             (CX + s * (pts[1][0] - 4), 72)], closed=True, t=0.9), "skinS", alpha=0.22))
    out.append(L(path((CX - 9, CHIN_Y[shape] - 3.2), [("C", (CX - 4, CHIN_Y[shape] - 1.2), (CX + 4, CHIN_Y[shape] - 1.2), (CX + 9, CHIN_Y[shape] - 3.2))], close=False),
                 stroke="skinS", width=1.6, alpha=0.18))
    out.append(L(face, stroke="line", width=0.9, alpha=0.5))
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


def eyes(style="almond", mood="normal", bt="F", sides=(1, -1)):
    out = []
    wing = 4 if bt == "F" else 1.2
    I, c1, c2, O, l1, l2 = EYE_STYLES[style]
    for s in sides:
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
            iris = ell(cx_, cy_, 6.6 * k, 7.9 * k)
            out.append(L(iris, "lin:irisD:iris:100:77:100:90", clip=white))
            out.append(L(ell(cx_, cy_ + 3.4, 4.6 * k, 3.4 * k), "rad:irisL:0.8:" + f"{f(cx_)}:{f(cy_ + 4)}:5", clip=white))
            out.append(L(iris, stroke="irisD", width=0.8, clip=white))
            out.append(L(ell(cx_, cy_ + 0.2, 2.9 * k, 3.6 * k), "pupil", clip=white))
            # shadow of the upper lid
            out.append(L(ell(ex + s * 0.5, EYE_Y - 6.8, 11.5, 5), "eyeLine", alpha=0.3, clip=white))
            out.append(L(ell(cx_ - 2.4, cy_ - 2.9, 2.3, 2.7), "white", clip=white))
            out.append(L(ell(cx_ + 2.6, cy_ + 3.4, 1.1, 1.1), "white", alpha=0.9, clip=white))
            out.append(L(ell(cx_ - 3.2, cy_ + 2.6, 0.7, 0.7), "white", alpha=0.6, clip=white))
            if mood == "excited":
                out.append(L(ell(cx_ + 1.8, cy_ - 1, 1.3, 1.3), "white", clip=white))
        # waterline: a soft light line along the lower lid
        out.append(L(path(a((l2[0] + 1.5, l2[1] - 0.8)), [("C", a((1.5, l2[1] - 0.1)), a((l1[0] - 1, l1[1] - 0.8)), a((O[0] - 1.2, O[1] + 0.8)))], close=False),
                     stroke="white", width=0.7, alpha=0.5))
        # upper lash band: thin at the inner corner, thick + winged at the outer one
        lash = path(a(I, 0.4), [("C", a(c1), a(c2), a(O)),
                                ("L", a((O[0] + wing, O[1] - wing * 0.8))),
                                ("C", a((O[0] + 1.5, O[1] - 2.6)), a((c2[0] + 0.5, c2[1] - 2.4)), a((c2[0] - 1, c2[1] - 2.2))),
                                ("C", a((c1[0], c1[1] - 1.8)), a((I[0] + 1, I[1] - 1)), a((I[0] - 0.6, I[1] - 0.2)))])
        out.append(L(lash, "eyeLine"))
        out.append(L(path(a((l2[0] + 1, l2[1] - 0.3)), [("C", a((2, l2[1] + 0.4)), a((l1[0] - 1, l1[1] - 0.4)), a((O[0] - 0.3, O[1] + 1.4)))], close=False),
                     stroke="eyeLine", width=0.9, alpha=0.55))
    if mood == "sad" and 1 in sides:
        out.append(L(path((CX + EYE_X + 5, EYE_Y + 6), [("C", (CX + EYE_X + 3, EYE_Y + 10), (CX + EYE_X + 4, EYE_Y + 13), (CX + EYE_X + 5.5, EYE_Y + 13)),
                                                        ("C", (CX + EYE_X + 7, EYE_Y + 13), (CX + EYE_X + 7.5, EYE_Y + 10), (CX + EYE_X + 5, EYE_Y + 6))]), "#9EDCFF", alpha=0.9))
    return out


def brows(style="natural", mood="normal", sides=(1, -1)):
    out = []
    lift = {"sad": (-2, 2.5), "angry": (3, -2.5), "excited": (-2, -1)}.get(mood, (0, 0))
    for s in sides:
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
    return [L(path((CX + 0.6, 92.5), [("C", (CX + 2, 96), (CX + 2.6, 98), (CX + 0.8, 99.6))], close=False), stroke="skinD", width=0.9, alpha=0.6),
            L(ell(CX - 1.4, 99.8, 1.7, 0.8), "skinD", alpha=0.3), L(ell(CX + 0.6, 95, 0.7, 1.6), "skinL", alpha=0.6)]


def mouth(mood="normal"):
    y = 106
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
