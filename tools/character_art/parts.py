"""Character parts as layers. Genders: F / M.

Body proportions follow figure-drawing landmarks for a ~6.2-heads stylised adult (webtoon style):
head = 1 unit (H = 72.9 art units; the drawn head is now smaller, crown ~53 to chin ~113, see warp.py),
shoulders 1.45 H, chest 2 H, waist 2.6 H (F, higher) / 2.7 H (M), hips 3.2 H, crotch 3.42 H,
knees 4.72 H, ankles 5.92 H, soles 6.2 H. Elbows sit at the waist, wrists at the crotch, fingertips at
mid-thigh. Women: narrower shoulders (~1.6 head widths), a defined waist, hips wider than the chest.
Men: shoulders ~2 head widths, a V-taper to a straighter waist, narrower hips.
"""
from geo import *

H = 72.9
Y = {
    "F": dict(chin=113, neck=136, sh=146, arm=170, bust=186, ub=200, waist=229.5, navel=248, hip=273, crotch=290, knee=384, calf=408, ankle=472, sole=492),
    "M": dict(chin=114, neck=135, sh=146, arm=170, bust=186, ub=202, waist=237, navel=250, hip=270, crotch=291, knee=384, calf=408, ankle=472, sole=492),
}
# Right half of the torso (x offset from the centre line, y), neck base to crotch.
TORSO = {
    "F": [(10.5, 133), (19, 136.5), (30, 139.5), (38, 143.5), (40.5, 150), (38, 159), (34.5, 169), (34.6, 181), (33.6, 193), (29.6, 209), (25.6, 229),
          (26.8, 241), (31.4, 255), (36.6, 268), (38, 278), (35.4, 288), (25, 294.5), (12, 296.5), (0, 297)],
    "M": [(14, 132.5), (26, 135.5), (39.5, 139), (47.5, 143.5), (50, 151), (47, 161), (42.4, 171), (41.4, 184), (39.6, 199), (36, 216), (32.6, 236),
          (33, 250), (34, 262), (34.8, 272), (34, 283), (31, 291.5), (20, 296.5), (10, 298.5), (0, 299)],
}
NECK = {"F": 10.2, "M": 13.6}
LEG = {  # (x, y, half width) down the right leg
    "F": [(17.6, 280, 17.6), (16.2, 300, 16.6), (15, 330, 14.6), (13.6, 360, 12), (12.6, 384, 10.2), (12.8, 408, 11), (12.2, 435, 8.6), (11.6, 458, 6.8), (11.4, 472, 6.2)],
    "M": [(18.6, 278, 18.6), (17.6, 300, 17.6), (16.3, 330, 15.6), (14.9, 360, 13.2), (13.9, 384, 11.6), (14.1, 408, 12.6), (13.5, 435, 10), (12.9, 458, 8), (12.7, 472, 7.4)],
}
ARM = {  # shoulder joint -> elbow (at the waist) -> wrist (at the crotch)
    "F": [(30.4, 147, 10.4), (32, 166, 8.4), (32.8, 190, 7.4), (33.4, 212, 6.5), (33.8, 231, 6.0), (34.6, 250, 6.2), (35.6, 268, 5.2), (36.4, 284, 4.1)],
    "M": [(37, 147, 13.4), (39, 167, 11), (39.8, 191, 9.8), (40.2, 214, 8.5), (40.4, 237, 7.7), (41, 256, 7.9), (41.8, 273, 6.7), (42.4, 288, 5.1)],
}


def tx(bt, y):
    """Torso half-width at y (linear between the outline points)."""
    pts = TORSO[bt]
    if y <= pts[0][1]:
        return pts[0][0]
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        if y0 <= y <= y1:
            return x0 + (x1 - x0) * (y - y0) / max(1e-6, y1 - y0)
    return pts[-1][0]


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
    y = Y[bt]
    nk = NECK[bt]
    # neck: a cylinder flaring into the trapezius
    neck = sym((CX, 100), [("L", (CX + nk, 100)), ("C", (CX + nk + 0.3, 120), (CX + nk + 1.5, 129), (CX + nk + 8, y["neck"] + 1)), ("L", (CX, y["neck"] + 4))])
    out.append(L(neck, "lin:skinS:skin:100:110:100:136", region="torso"))
    # shadow of the jaw on the neck, sternocleidomastoid lines
    out.append(L(sym((CX, 100), [("L", (CX + nk, 100)), ("C", (CX + nk, 116), (CX + 5, 122), (CX, 123.5))]), "skinS", alpha=0.75, region="torso"))
    for s in (1, -1):
        out.append(L(path((CX + s * (nk - 1.5), 112), [("C", (CX + s * (nk - 3), 122), (CX + s * 5, 130), (CX + s * 2.5, 135))], close=False), stroke="skinS", width=1.2, alpha=0.4, region="torso"))
    if bt == "M":
        out.append(L(path((CX - 2, 121), [("C", (CX - 0.8, 124), (CX + 0.8, 124), (CX + 2, 121))], close=False), stroke="skinD", width=0.9, alpha=0.35, region="torso"))
    for s in (1, -1):
        sp = leg_spine(bt, s)
        reg = leg_region(s)
        out.append(L(limb(sp), "lin:skinS:skinL:" + f"{CX + s * 2}:340:{CX + s * 26}:340", region=reg))
        kx = CX + s * LEG[bt][4][0]
        # inner thigh shade, kneecap, shin line, ankle bone
        out.append(L(path((CX + s * 4, y["crotch"]), [("C", (CX + s * 5, 320), (CX + s * 6, 350), (kx - s * 6, y["knee"] - 4))], close=False), stroke="skinS", width=3, alpha=0.3, region=reg))
        # soft kneecap: a light plane with a faint shadow under it
        out.append(L(ell(kx, y["knee"] - 3, 5.5, 4.2), "skinL", alpha=0.35, region=reg))
        out.append(L(path((kx - 4, y["knee"] + 2.5), [("C", (kx - 1.5, y["knee"] + 4.5), (kx + 1.5, y["knee"] + 4.5), (kx + 4, y["knee"] + 2.5))], close=False), stroke="skinS", width=1.2, alpha=0.3, region=reg))
        ax = CX + s * LEG[bt][-1][0]
        out.append(L(ell(ax - s * 4, y["ankle"] - 4, 1.6, 2.2), "skinS", alpha=0.6, region=reg))
    out.append(L(outline(TORSO[bt]), "lin:skinL:skinS:100:140:100:296", region="torso"))
    # collarbones and the pit of the neck
    for s in (1, -1):
        out.append(L(path((CX + s * 3.5, y["neck"] + 6), [("C", (CX + s * 10, y["neck"] + 4), (CX + s * 18, y["neck"] + 5), (CX + s * (27 if bt == "F" else 34), y["neck"] + 4))], close=False),
                     stroke="skinD", width=0.9, alpha=0.35, region="torso"))
    out.append(L(ell(CX, y["neck"] + 6.5, 2.4, 1.6), "skinS", alpha=0.5, region="torso"))
    return out


def arms(bt):
    out = []
    for s in (1, -1):
        sp = arm_spine(bt, s)
        reg = arm_region(s)
        out.append(L(limb(sp), "lin:skinL:skinS:" + f"{CX + s * (sp[0][0] - 8)}:220:{CX + s * (sp[0][0] + 10)}:220", region=reg))
        # deltoid edge and the elbow crease
        out.append(L(path((sp[0][0] + s * 5, sp[0][1] + 6), [("C", (sp[1][0] + s * 3, sp[1][1] + 4), (sp[2][0] + s * 1, sp[2][1] - 6), (sp[2][0] - s * 1.5, sp[2][1]))], close=False),
                     stroke="skinS", width=1.1, alpha=0.35, region=reg))
        ex, ey = sp[4][0], sp[4][1]
        out.append(L(path((ex - s * 3, ey - 1), [("C", (ex - s * 1, ey + 1.5), (ex + s * 1, ey + 1.5), (ex + s * 3, ey - 0.5))], close=False), stroke="skinD", width=0.8, alpha=0.4, region=reg))
    return out


def hands(bt):
    """Relaxed hands seen from the front: palm towards the thigh, so the hand shows its thumb side --
    a narrow back of the hand, the thumb resting in front and fingers gently curled towards the body.
    ~0.55 head long; drawn over the clothes so sleeves and hips never hide them."""
    out = []
    for s in (1, -1):
        hx, hy, hw = arm_spine(bt, s)[-1]
        k = 1.0 if bt == "F" else 1.15
        reg = arm_region(s)

        def h(dx, dy):
            return (hx + s * dx * k, hy - 3 + dy * k)
        # back of the hand and the curled fingers (index finger in front)
        hand = smooth([h(-3.8, 0), h(3.8, 0), h(5, 7), h(5.4, 15), h(4.6, 23), h(2.8, 31), h(0.6, 37), h(-1.8, 39.5), h(-3.6, 37.5), h(-3.4, 33),
                       h(-4.6, 26), h(-5.2, 16), h(-4.8, 7)], closed=True, t=0.9)
        out.append(L(hand, "lin:skinL:skinS:" + f"{f(hx + s * 5 * k)}:{f(hy)}:{f(hx - s * 5 * k)}:{f(hy)}", region=reg))
        # finger joints: three soft creases where the fingers bend
        for (x0, y0, x1, y1) in [(4.4, 22, 1.4, 25), (3, 29, 0.2, 31.5), (1.4, 34, -1.4, 35.5)]:
            out.append(L(path(h(x0, y0), [("C", h(x0 - 1, y0 + 1.6), h(x1 + 1, y1 - 0.4), h(x1, y1))], close=False), stroke="skinD", width=0.6, alpha=0.45, region=reg))
        out.append(L(path(h(-2.6, 18), [("C", h(-1.8, 25), h(-0.8, 31), h(-1.2, 37))], close=False), stroke="skinS", width=1.1, alpha=0.45, region=reg))
        # thumb lying along the front of the hand
        thumb = smooth([h(-4.4, 5), h(-2.2, 8), h(-1.6, 15), h(-2.4, 22), h(-4, 25), h(-5.6, 22.5), h(-6, 14), h(-5.6, 8)], closed=True, t=0.9)
        out.append(L(thumb, "lin:skin:skinS:" + f"{f(hx)}:{f(hy)}:{f(hx)}:{f(hy + 25 * k)}", region=reg))
        out.append(L(thumb, stroke="skinD", width=0.6, alpha=0.5, region=reg))
        out.append(L(ell(*h(-3.8, 22.2), 1.1, 1.0), "skinL", alpha=0.7, region=reg))
        out.append(L(hand, stroke="skinD", width=0.6, alpha=0.5, region=reg))
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
# Male faces: a little longer, a wider jaw with a clear angle and a flatter, broader chin.
FACE_SHAPES_M = {
    "oval":   [(34, 58), (34.6, 72), (34, 87), (31.4, 100.5), (23.5, 111), (10.5, 116.2)],
    "round":  [(35, 58), (35.6, 72), (35.4, 88), (33, 101), (25.5, 111), (11.5, 115.6)],
    "sharp":  [(33.6, 58), (34.2, 72), (33.4, 87), (29.6, 100.5), (20, 111.2), (7.5, 117)],
    "square": [(35.4, 58), (36, 72), (36, 89), (35, 102.5), (28.5, 112), (12.5, 115.8)],
}
CHIN_Y_M = {"oval": 117.2, "round": 116.6, "sharp": 118, "square": 116.6}


def shapes(g):
    return (FACE_SHAPES_M, CHIN_Y_M) if g == "M" else (FACE_SHAPES, CHIN_Y)


def face_outline(shape="oval", g="F"):
    fs, cy = shapes(g)
    right = [(CX, 23.5), (CX + 19, 26), (CX + 29, 36), (CX + 32.6, 46)] + [(CX + x, y) for x, y in fs[shape]]
    chin = (CX, cy[shape])
    full = right + [chin] + [mx(p) for p in right[::-1][:-1]]
    return smooth(full, closed=True, t=0.95)


def head(shape="oval", g="F"):
    out = []
    fs, chin_y = shapes(g)
    pts = fs[shape]
    w = pts[1][0]
    # ears
    for s in (1, -1):
        ear = path((CX + s * (w - 2.5), 73), [("C", (CX + s * (w + 5), 66.5), (CX + s * (w + 8.5), 80), (CX + s * (w + 4.5), 89)),
                                               ("C", (CX + s * (w + 2.5), 93), (CX + s * (w - 1), 94.5), (CX + s * (w - 2.5), 92))])
        out.append(L(ear, "lin:skinS:skin:" + f"{CX + s * w}:80:{CX + s * (w + 7)}:80", region="head"))
        out.append(L(path((CX + s * (w - 0.5), 76.5), [("C", (CX + s * (w + 4), 73.5), (CX + s * (w + 5.5), 82), (CX + s * (w + 2.5), 88)),
                                                        ("C", (CX + s * (w + 1.5), 84), (CX + s * (w + 1), 80), (CX + s * w, 78.5))]), "skinD", alpha=0.45))
    face = face_outline(shape, g)
    out.append(L(face, "lin:skinL:skin:100:30:100:112"))
    # warm glow on the cheeks and a cool shade towards the jaw and the sides
    out.append(L(face, "rad:skinL:0.75:100:72:30"))
    for s in (1, -1):
        x2, y2 = pts[3]
        x3, y3 = pts[4]
        out.append(L(smooth([(CX + s * (pts[0][0] - 0.3), 60), (CX + s * (pts[1][0] - 0.2), 72), (CX + s * (pts[2][0] - 0.2), 86), (CX + s * (x2 - 0.2), y2),
                             (CX + s * (x3 - 0.2), y3), (CX + s * (x3 - 4), y3 - 2.5), (CX + s * (x2 - 5), y2 - 4), (CX + s * (pts[2][0] - 4.5), 85),
                             (CX + s * (pts[1][0] - 4), 72)], closed=True, t=0.9), "skinS", alpha=0.22))
    cy = chin_y[shape]
    out.append(L(path((CX - 9, cy - 3.2), [("C", (CX - 4, cy - 1.2), (CX + 4, cy - 1.2), (CX + 9, cy - 3.2))], close=False),
                 stroke="skinS", width=1.6, alpha=0.18))
    if g == "M":
        # jaw angle shading
        for s in (1, -1):
            x3, y3 = pts[3]
            out.append(L(path((CX + s * (x3 - 1.5), y3 - 6), [("C", (CX + s * (x3 - 2.5), y3), (CX + s * (pts[4][0] - 1), pts[4][1] - 2), (CX + s * (pts[4][0] - 5), pts[4][1] - 1))], close=False),
                         stroke="skinS", width=1.4, alpha=0.25))
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
    wing = 4 if bt == "F" else 0.6
    ky = 1.0 if bt == "F" else 0.8  # male eyes: narrower opening, thinner lash band
    kr = 1.0 if bt == "F" else 0.9
    I, c1, c2, O, l1, l2 = EYE_STYLES[style]
    for s in sides:
        ex = CX + s * EYE_X
        def a(p, dy=0.0):
            return (ex + s * p[0], EYE_Y + (p[1] + dy) * ky)
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
            k *= kr
            iris = ell(cx_, cy_, 6.6 * k, 7.9 * k)
            out.append(L(iris, "lin:irisD:iris:100:77:100:90", clip=white))
            out.append(L(ell(cx_, cy_ + 3.4, 4.6 * k, 3.4 * k), "rad:irisL:0.8:" + f"{f(cx_)}:{f(cy_ + 4)}:5", clip=white))
            out.append(L(iris, stroke="irisD", width=0.8, clip=white))
            out.append(L(ell(cx_, cy_ + 0.2, 2.9 * k, 3.6 * k), "pupil", clip=white))
            # shadow of the upper lid
            out.append(L(ell(ex + s * 0.5, EYE_Y - 6.8, 11.5, 5), "eyeLine", alpha=0.3, clip=white))
            out.append(L(ell(cx_ - 2.4 * kr, cy_ - 2.9 * kr, 2.3 * kr, 2.7 * kr), "white", clip=white))
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


def brows(style="natural", mood="normal", sides=(1, -1), g="F"):
    out = []
    lift = {"sad": (-2, 2.5), "angry": (3, -2.5), "excited": (-2, -1)}.get(mood, (0, 0))
    for s in sides:
        bx = CX + s * 15.5
        by = 70 if g == "F" else 71.2
        kx, ky = (1.0, 1.0) if g == "F" else (1.1, 1.55)
        def a(p):
            return (bx + s * p[0] * kx, by + p[1] * ky)
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


def nose(g="F"):
    if g == "M":
        return [L(path((CX + 1.6, 84), [("C", (CX + 2.4, 88), (CX + 2.8, 92), (CX + 3, 95))], close=False), stroke="skinD", width=0.8, alpha=0.3),
                L(path((CX + 0.8, 92.5), [("C", (CX + 2.6, 96), (CX + 3.2, 98.4), (CX + 1, 100.2))], close=False), stroke="skinD", width=1.0, alpha=0.65),
                L(path((CX - 3.6, 99.4), [("C", (CX - 2.6, 100.8), (CX - 0.6, 101), (CX + 0.6, 100.4))], close=False), stroke="skinD", width=0.8, alpha=0.4),
                L(ell(CX + 0.8, 94.5, 0.8, 2.2), "skinL", alpha=0.55)]
    return [L(path((CX + 0.6, 92.5), [("C", (CX + 2, 96), (CX + 2.6, 98), (CX + 0.8, 99.6))], close=False), stroke="skinD", width=0.9, alpha=0.6),
            L(ell(CX - 1.4, 99.8, 1.7, 0.8), "skinD", alpha=0.3), L(ell(CX + 0.6, 95, 0.7, 1.6), "skinL", alpha=0.6)]


def mouth(mood="normal", g="F"):
    y = 106 if g == "F" else 107.4
    if g == "M" and mood in ("normal", "calm"):
        # no lip tint: a firm line and a soft shadow under the lower lip
        return [L(path((CX - 5.2, y + 0.8), [("C", (CX - 2, y + 1.9), (CX + 2, y + 1.9), (CX + 5.2, y + 0.4))], close=False), stroke="lipLine", width=1.1),
                L(path((CX - 3, y + 4.4), [("C", (CX - 1, y + 5.2), (CX + 1, y + 5.2), (CX + 3, y + 4.4))], close=False), stroke="skinS", width=1.3, alpha=0.45)]
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
