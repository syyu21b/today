"""Tops, bottoms and shoes built on the body landmarks (parts.Y / TORSO / spines).

Garments hang from the shoulders and fall straight from the chest instead of hugging the waist,
gather in a few folds (armpits, elbows, waist, knees, hems) and carry soft gradients. Sleeve layers are
tagged with the arm regions; export.py puts them in a separate part drawn over the arms.
"""
from geo import *
from parts import TORSO, LEG, ARM, Y, tx, leg_spine, arm_spine, leg_region, arm_region

TOP_FILL = "lin:topL:top:100:138:100:"
KINDS = ("tee", "shirt", "knit", "hoodie", "jacket", "dress")


def hem_of(bt, kind):
    y = Y[bt]
    return {"tee": y["navel"] + 16, "shirt": y["navel"] + 26, "knit": y["navel"] + 18, "hoodie": y["navel"] + 28,
            "jacket": y["navel"] + 32, "dress": y["waist"] + 4}[kind]


EASE = {"tee": 2.4, "shirt": 2.8, "knit": 3.2, "hoodie": 4.2, "jacket": 3.6, "dress": 1.2}


def garment(bt, hem, ease, neck=None, neck_drop=6.0):
    """A top's body: follows neck, shoulders and chest, then falls straight to a softly curved hem."""
    y = Y[bt]
    nk = neck or (10.5 if bt == "F" else 13.5)
    sh = TORSO[bt][4][0]
    chest = max(tx(bt, y["arm"]), tx(bt, y["bust"])) + ease
    hip = tx(bt, min(hem, y["hip"])) + ease * 0.7
    right = [(CX + nk, y["neck"] - 1.5), (CX + sh - 9, y["neck"] + 3), (CX + sh + 0.8, y["sh"] - 1), (CX + sh + 1.4, y["sh"] + 7),
             (CX + chest, y["arm"] + 4)]
    for t in (0.3, 0.6, 0.85):
        yy = y["arm"] + 4 + (hem - y["arm"] - 4) * t
        right.append((CX + max(tx(bt, yy) + ease, chest + (hip - chest) * t), yy))
    right += [(CX + hip, hem), (CX + hip * 0.5, hem + 1.6)]
    full = right + [(CX, hem + 2)] + [mx(p) for p in right[::-1]] + [(CX, y["neck"] + neck_drop)]
    return smooth(full, closed=True, t=0.85)


def sleeve(bt, s, short, grow, kind):
    y = Y[bt]
    sp = arm_spine(bt, s, grow=grow, until=y["ub"] if short else None)
    if short:
        sp[-1] = (sp[-1][0] + s * 0.4, sp[-1][1], sp[-1][2] + 0.9)  # short sleeves flare a little
    else:
        sp = [(x, yy, w + (0.8 if yy > 240 else 0)) for (x, yy, w) in sp]  # a little room at the forearm
    reg = arm_region(s)
    out = [L(limb(sp), "lin:topL:top:" + f"{f(sp[0][0] - s * 12)}:200:{f(sp[0][0] + s * 12)}:200", region=reg)]
    # fold where the sleeve meets the body, and the elbow folds of long sleeves
    out.append(L(path((sp[0][0] - s * 4, sp[0][1] + 12), [("C", (sp[0][0] - s * 3, sp[0][1] + 20), (sp[1][0] - s * 4, sp[1][1] + 6), (sp[1][0] - s * 5, sp[1][1] + 14))], close=False),
                 stroke="topS", width=1.0, alpha=0.45, region=reg))
    if not short:
        ex, ey = sp[4][0], sp[4][1]
        # elbow: one soft fold on the inside, a light pull on the outside
        out.append(L(path((ex - s * 5, ey - 2), [("C", (ex - s * 2, ey + 1.5), (ex + s * 1, ey + 2), (ex + s * 3, ey + 0.5))], close=False),
                     stroke="topS", width=0.9, alpha=0.35, region=reg))
        out.append(L(path((ex + s * 2, ey + 6), [("C", (ex + s * 3.5, ey + 9), (ex + s * 4.5, ey + 12), (ex + s * 5, ey + 15))], close=False),
                     stroke="topS", width=0.8, alpha=0.25, region=reg))
        e = sp[-1]
        cuff_h = 7 if kind in ("knit", "hoodie") else 5
        out.append(L(limb([(e[0], e[1] - cuff_h, e[2] + 0.4), (e[0], e[1], e[2] + 0.6)]), "topS", alpha=0.8, region=reg))
        if kind in ("knit", "hoodie"):
            for k in (-0.5, 0, 0.5):
                out.append(L(path((e[0] + k * e[2], e[1] - cuff_h + 1), [("L", (e[0] + k * e[2], e[1] - 0.5))], close=False), stroke="top", width=0.7, alpha=0.6, region=reg))
    else:
        e = sp[-1]
        out.append(L(path((e[0] - e[2] * 0.95, e[1] - 1.5), [("C", (e[0] - e[2] * 0.3, e[1] + 0.8), (e[0] + e[2] * 0.3, e[1] + 0.8), (e[0] + e[2] * 0.95, e[1] - 1.5))], close=False),
                     stroke="topS", width=0.9, alpha=0.6, region=reg))
    return out


def stripes(bt, kind, sleeve_kind):
    """Horizontal bands clipped to the garment body."""
    hem = hem_of(bt, kind)
    clip = garment(bt, hem, EASE[kind])
    out = []
    for yy in range(int(Y[bt]["neck"]) + 22, int(hem) + 2, 15):
        out.append(L(smooth([(CX - 70, yy), (CX + 70, yy), (CX + 70, yy + 6.5), (CX - 70, yy + 6.5)], t=0.1), "stripe", clip=clip, region="torso"))
    return out


def top(bt, kind="tee", sleeve_kind="short"):
    out = []
    y = Y[bt]
    hem = hem_of(bt, kind)
    ease = EASE[kind]
    short = sleeve_kind == "short" and kind == "tee"
    for s in (1, -1):
        out += sleeve(bt, s, short, {"hoodie": 2.8, "knit": 2.4, "jacket": 2.4, "shirt": 2.0}.get(kind, 1.8 if not short else 1.8), kind)
    if kind == "dress":
        w0 = tx(bt, y["waist"]) + 1.5
        bottom = y["knee"] - 22
        skirt = smooth([(CX - w0, y["waist"] + 2), (CX + w0, y["waist"] + 2), (CX + w0 + 10, y["hip"]), (CX + w0 + 22, (y["hip"] + bottom) / 2), (CX + w0 + 30, bottom - 4),
                        (CX + 20, bottom + 3), (CX, bottom + 1), (CX - 20, bottom + 3), (CX - w0 - 30, bottom - 4), (CX - w0 - 22, (y["hip"] + bottom) / 2),
                        (CX - w0 - 10, y["hip"])], t=0.9)
        out.append(L(skirt, "lin:top:topS:100:" + f"{f(y['waist'])}:100:{f(bottom)}", region="torso"))
        for k in (-34, -20, -7, 7, 20, 34):
            x0 = CX + k * 0.4
            out.append(L(path((x0, y["waist"] + 10), [("C", (x0 + k * 0.1, y["hip"] + 6), (CX + k * 0.9, bottom - 30), (CX + k * 1.15, bottom))], close=False),
                         stroke="topS", width=1.1, alpha=0.45, region="torso"))
            out.append(L(path((x0 + 3, y["waist"] + 14), [("C", (x0 + 3 + k * 0.1, y["hip"] + 8), (CX + k * 0.9 + 4, bottom - 28), (CX + k * 1.15 + 5, bottom - 1))], close=False),
                         stroke="topL", width=1.4, alpha=0.3, region="torso"))
        out.append(L(skirt, stroke="topS", width=0.9, alpha=0.6, region="torso"))
    body = garment(bt, hem, ease)
    out.append(L(body, TOP_FILL + f(hem), region="torso"))
    # cloth shading: darker towards the sides, folds under the arms and at the waist
    for s in (1, -1):
        sh = TORSO[bt][4][0]
        chest = max(tx(bt, y["arm"]), tx(bt, y["bust"])) + ease
        out.append(L(smooth([(CX + s * (sh - 3), y["sh"] + 8), (CX + s * chest, y["arm"] + 6), (CX + s * (chest - 0.5), y["waist"]), (CX + s * (tx(bt, hem) + ease * 0.6), hem - 2),
                             (CX + s * (tx(bt, hem) - 4), hem - 3), (CX + s * (chest - 6), y["waist"]), (CX + s * (chest - 6), y["arm"] + 8)], closed=True, t=0.9),
                     "topS", alpha=0.22, region="torso"))
        out.append(L(path((CX + s * (chest - 2), y["arm"] + 2), [("C", (CX + s * (chest - 6), y["arm"] + 14), (CX + s * (chest - 10), y["bust"] + 8), (CX + s * (chest - 16), y["ub"] + 4))], close=False),
                     stroke="topS", width=1.0, alpha=0.4, region="torso"))
        out.append(L(path((CX + s * (chest - 1), y["waist"] - 6), [("C", (CX + s * (chest - 5), y["waist"] - 1), (CX + s * (chest - 10), y["waist"] + 1), (CX + s * (chest - 15), y["waist"] + 2))], close=False),
                     stroke="topS", width=0.9, alpha=0.3, region="torso"))
    if bt == "F" and kind != "jacket":
        # soft fold under the bust
        out.append(L(path((CX - 22, y["ub"] - 2), [("C", (CX - 10, y["ub"] + 5), (CX + 10, y["ub"] + 5), (CX + 22, y["ub"] - 2))], close=False), stroke="topS", width=1.0, alpha=0.35, region="torso"))
        for s in (1, -1):
            out.append(L(path((CX + s * 6, y["bust"] - 12), [("C", (CX + s * 12, y["bust"] - 4), (CX + s * 18, y["bust"] - 2), (CX + s * 26, y["bust"] - 8))], close=False),
                         stroke="topL", width=2.2, alpha=0.35, region="torso"))
    elif bt == "M" and kind != "jacket":
        # flat chest planes
        for s in (1, -1):
            out.append(L(path((CX + s * 4, y["bust"] + 4), [("C", (CX + s * 14, y["bust"] + 7), (CX + s * 24, y["bust"] + 6), (CX + s * 32, y["bust"] - 2))], close=False),
                         stroke="topS", width=1.0, alpha=0.3, region="torso"))
    nk = 10.5 if bt == "F" else 13.5
    n0 = y["neck"]
    if kind == "tee":
        out.append(L(path((CX - nk + 0.5, n0 - 1), [("C", (CX - nk * 0.6, n0 + 10), (CX + nk * 0.6, n0 + 10), (CX + nk - 0.5, n0 - 1))], close=False), stroke="acc", width=2.8, region="torso"))
        out.append(L(path((CX - 26, hem - 1), [("C", (CX - 9, hem + 1.6), (CX + 9, hem + 1.6), (CX + 26, hem - 1))], close=False), stroke="topS", width=0.8, alpha=0.5, region="torso"))
    elif kind == "knit":
        out.append(L(path((CX - nk, n0 - 1), [("C", (CX - nk * 0.6, n0 + 11), (CX + nk * 0.6, n0 + 11), (CX + nk, n0 - 1))], close=False), stroke="topS", width=4.2, region="torso"))
        wb = tx(bt, hem) + ease * 0.7
        band = smooth([(CX - wb - 0.5, hem - 9), (CX + wb + 0.5, hem - 9), (CX + wb, hem + 0.5), (CX, hem + 2), (CX - wb, hem + 0.5)], t=0.4)
        out.append(L(band, "topS", alpha=0.55, region="torso"))
        for k in range(-7, 8):
            out.append(L(path((CX + k * 4.6, hem - 8), [("L", (CX + k * 4.8, hem))], close=False), stroke="topS", width=0.9, alpha=0.6, region="torso"))
        for s in (1, -1):  # cable stitches
            x = CX + s * 11
            out.append(L(path((x, n0 + 16), [("C", (x + 3.5, n0 + 34), (x - 3.5, n0 + 52), (x, n0 + 70)), ("C", (x + 3.5, n0 + 88), (x - 3.5, n0 + 106), (x, hem - 10))], close=False),
                         stroke="topS", width=1.3, alpha=0.4, region="torso"))
    elif kind == "hoodie":
        w = nk + 15
        hood = smooth([(CX - w, n0 + 9), (CX - w + 5, n0 - 5), (CX - 9, n0 - 10), (CX + 9, n0 - 10), (CX + w - 5, n0 - 5), (CX + w, n0 + 9), (CX + 14, n0 + 15), (CX, n0 + 22), (CX - 14, n0 + 15)], t=0.85)
        out.append(L(hood, "lin:topS:top:100:" + f"{f(n0 - 10)}:100:{f(n0 + 22)}", region="torso"))
        out.append(L(smooth([(CX - nk - 2, n0 + 5), (CX - 7, n0 - 1), (CX + 7, n0 - 1), (CX + nk + 2, n0 + 5), (CX + 7, n0 + 13), (CX, n0 + 18), (CX - 7, n0 + 13)], t=0.85),
                     "topS", alpha=0.8, region="torso"))
        out.append(L(hood, stroke="topS", width=0.9, alpha=0.6, region="torso"))
        for s in (1, -1):
            out.append(L(path((CX + s * 5.5, n0 + 16), [("C", (CX + s * 6, n0 + 28), (CX + s * 7, n0 + 40), (CX + s * 6.5, n0 + 54))], close=False), stroke="acc", width=1.6, region="torso"))
            out.append(L(ell(CX + s * 6.5, n0 + 56, 1.6, 2.4), "acc", region="torso"))
        pw = 19 if bt == "F" else 23
        pocket = smooth([(CX - pw, hem - 40), (CX + pw, hem - 40), (CX + pw + 5, hem - 10), (CX - pw - 5, hem - 10)], t=0.25)
        out.append(L(pocket, "topS", alpha=0.3, region="torso"))
        out.append(L(pocket, stroke="topS", width=1.0, alpha=0.6, region="torso"))
        wb = tx(bt, hem) + ease * 0.7
        band = smooth([(CX - wb - 0.5, hem - 8), (CX + wb + 0.5, hem - 8), (CX + wb, hem + 0.5), (CX, hem + 2), (CX - wb, hem + 0.5)], t=0.4)
        out.append(L(band, "topS", alpha=0.6, region="torso"))
    elif kind == "jacket":
        # open jacket: the accent-coloured shirt shows between the lapels
        inner = sym((CX, n0 - 1), [("L", (CX + 11, n0 - 1)), ("C", (CX + 13, n0 + 50), (CX + 11, n0 + 100), (CX + 12, hem - 1)), ("L", (CX, hem))])
        out.append(L(inner, "lin:acc:accS:100:" + f"{f(n0)}:100:{f(hem)}", region="torso"))
        out.append(L(path((CX - 9, n0), [("C", (CX - 5.5, n0 + 8), (CX + 5.5, n0 + 8), (CX + 9, n0))], close=False), stroke="accS", width=1.6, region="torso"))
        for s in (1, -1):
            lapel = path((CX + s * 11, n0 - 3), [("L", (CX + s * 24, n0 + 4)), ("L", (CX + s * 21, n0 + 20)), ("L", (CX + s * 26, n0 + 25)), ("L", (CX + s * 12, n0 + 74)),
                                                  ("C", (CX + s * 12.5, n0 + 58), (CX + s * 12.5, n0 + 30), (CX + s * 11, n0 - 3))])
            out.append(L(lapel, "lin:topS:top:" + f"{f(CX + s * 11)}:{f(n0 + 20)}:{f(CX + s * 26)}:{f(n0 + 20)}", region="torso"))
            out.append(L(lapel, stroke="topS", width=1.0, alpha=0.8, region="torso"))
            out.append(L(path((CX + s * 12, n0 + 74), [("C", (CX + s * 12.5, n0 + 100), (CX + s * 12.5, hem - 20), (CX + s * 13, hem))], close=False), stroke="topS", width=1.2, alpha=0.8, region="torso"))
            out.append(L(path((CX + s * 19, hem - 34), [("L", (CX + s * 31, hem - 35))], close=False), stroke="topS", width=1.2, alpha=0.6, region="torso"))
        for yy in (hem - 52, hem - 30):
            out.append(L(ell(CX - 15, yy, 1.5, 1.5), "topS", region="torso"))
    elif kind == "shirt":
        # button-down: pointed collar, placket with buttons, a chest pocket
        for yy in range(int(n0 + 18), int(hem - 4), 19):
            out.append(L(ell(CX + 0.4, yy, 1.4, 1.4), "topS", region="torso"))
        out.append(L(path((CX + 2.6, n0 + 6), [("C", (CX + 3, n0 + 60), (CX + 2.8, hem - 40), (CX + 3, hem))], close=False), stroke="topS", width=0.9, alpha=0.6, region="torso"))
        px, py = CX + 12, y["bust"] - 12
        pocket = smooth([(px, py), (px + 12, py), (px + 12, py + 15), (px + 6, py + 17), (px, py + 15)], t=0.2)
        out.append(L(pocket, stroke="topS", width=0.9, alpha=0.6, region="torso"))
        out.append(L(path((px, py + 3), [("L", (px + 12, py + 3))], close=False), stroke="topS", width=0.8, alpha=0.5, region="torso"))
        for s_ in (1, -1):
            collar = path((CX + s_ * 1, n0 + 6), [("L", (CX + s_ * (nk - 0.5), n0 - 3)), ("C", (CX + s_ * (nk + 3), n0 - 1), (CX + s_ * (nk + 6), n0 + 2), (CX + s_ * (nk + 7), n0 + 5)),
                                                   ("L", (CX + s_ * 6, n0 + 18))])
            out.append(L(collar, "lin:topL:top:100:" + f"{f(n0 - 3)}:100:{f(n0 + 18)}", region="torso"))
            out.append(L(collar, stroke="topS", width=1.0, alpha=0.8, region="torso"))
    elif kind == "dress":
        out.append(L(path((CX - nk - 1, n0 - 0.5), [("C", (CX - nk * 0.6, n0 + 12), (CX + nk * 0.6, n0 + 12), (CX + nk + 1, n0 - 0.5))], close=False), stroke="acc", width=2.4, region="torso"))
        w0 = tx(bt, y["waist"])
        sash = smooth([(CX - w0 - 1.5, y["waist"] - 4), (CX + w0 + 1.5, y["waist"] - 4), (CX + w0 + 2.5, y["waist"] + 5), (CX - w0 - 2.5, y["waist"] + 5)], t=0.3)
        out.append(L(sash, "lin:acc:accS:100:" + f"{f(y['waist'] - 4)}:100:{f(y['waist'] + 5)}", region="torso"))
        by = y["waist"]
        bow = smooth([(CX, by), (CX + 8, by - 6), (CX + 10.5, by), (CX + 8, by + 6), (CX, by), (CX - 8, by + 6), (CX - 10.5, by), (CX - 8, by - 6)], t=0.6)
        out.append(L(bow, "acc", region="torso"))
        out.append(L(ell(CX, by, 2.2, 2.5), "accS", region="torso"))
    out.append(L(body, stroke="topS", width=0.8, alpha=0.5, region="torso"))
    return out


def bottom(bt, kind="pants"):
    out = []
    y = Y[bt]
    wy = y["navel"] - 6  # waistband sits a little below the navel line
    ww = tx(bt, wy) + 1.5
    if kind == "skirt":
        hem = y["crotch"] + 40
        # A-line: hugs the waist, clears the hips and flares to the hem
        sk = smooth([(CX - ww, wy), (CX + ww, wy), (CX + ww + 9, y["hip"] - 2), (CX + ww + 17, (y["hip"] + hem) / 2), (CX + ww + 25, hem - 3), (CX + 22, hem + 1.5),
                     (CX, hem + 0.5), (CX - 22, hem + 1.5), (CX - ww - 25, hem - 3), (CX - ww - 17, (y["hip"] + hem) / 2), (CX - ww - 9, y["hip"] - 2)], t=0.8)
        out.append(L(sk, "lin:bottom:bottomS:100:" + f"{f(wy)}:100:{f(hem)}", region="torso"))
        for k in range(-4, 5):  # pleats: a shaded side each
            x0, x1 = CX + k * 5.4, CX + k * 12.2
            out.append(L(smooth([(x0, wy + 6), (x0 + 2.6, wy + 6), (x1 + 4.6, hem + 1), (x1, hem + 1.5)], t=0.2), "bottomS", alpha=0.45, clip=sk, region="torso"))
            out.append(L(path((x0 - 0.5, wy + 6), [("L", (x1 - 1, hem + 1.5))], close=False), stroke="bottomS", width=0.8, alpha=0.7, region="torso"))
        out.append(L(smooth([(CX - ww, wy - 1), (CX + ww, wy - 1), (CX + ww + 0.6, wy + 6), (CX - ww - 0.6, wy + 6)], t=0.2), "bottomS", region="torso"))
        out.append(L(sk, stroke="bottomS", width=0.8, alpha=0.6, region="torso"))
        return out
    until = None if kind == "pants" else y["crotch"] + 44
    for s in (1, -1):
        sp = leg_spine(bt, s, grow=2.8 if kind == "pants" else 3.6, until=until)
        if kind == "pants":
            # relaxed straight leg: room at the knee and a slight break over the shoe
            mn = 10.0 if bt == "F" else 11.2
            sp = [(x, yy, max(w, mn)) for (x, yy, w) in sp]
            sp[-1] = (sp[-1][0], sp[-1][1] + 6, sp[-1][2] + 0.6)
        reg = leg_region(s)
        out.append(L(limb(sp), "lin:bottomS:bottom:" + f"{f(CX + s * 2)}:340:{f(CX + s * 24)}:340", region=reg))
        e = sp[-1][1] - 2
        out.append(L(path((CX + s * 10, y["crotch"] - 6), [("C", (CX + s * 11, y["crotch"] + (e - y["crotch"]) * 0.35), (CX + s * 11.5, y["crotch"] + (e - y["crotch"]) * 0.7), (CX + s * 11.6, e))], close=False),
                     stroke="bottomS", width=0.9, alpha=0.5, region=reg))
        if kind == "pants":
            kx = CX + s * LEG[bt][4][0]
            for k in (0, 7):
                out.append(L(path((kx - s * 7, y["knee"] - 6 + k), [("C", (kx - s * 2, y["knee"] - 3 + k), (kx + s * 2, y["knee"] - 4 + k), (kx + s * 7, y["knee"] - 8 + k))], close=False),
                             stroke="bottomS", width=0.8, alpha=0.45, region=reg))
            x, yy, w = sp[-1]
            for k in (0, 1):
                out.append(L(path((x - w, yy - 8 - k * 6), [("C", (x - w * 0.4, yy - 5 - k * 6), (x + w * 0.4, yy - 5 - k * 6), (x + w, yy - 8 - k * 6))], close=False),
                             stroke="bottomS", width=0.9, alpha=0.5, region=reg))
        else:
            x, yy, w = sp[-1]
            out.append(L(limb([(x, yy - 6, w + 0.6), (x, yy, w + 0.9)]), "bottomS", alpha=0.85, region=reg))
    hipw = tx(bt, y["hip"]) + 2.2
    hips = smooth([(CX - ww, wy), (CX + ww, wy), (CX + hipw, y["hip"]), (CX + hipw - 2, y["crotch"] - 4), (CX + 10, y["crotch"] + 6), (CX, y["crotch"] + 2),
                   (CX - 10, y["crotch"] + 6), (CX - hipw + 2, y["crotch"] - 4), (CX - hipw, y["hip"])], t=0.9)
    out.append(L(hips, "lin:bottom:bottomS:100:" + f"{f(wy)}:100:{f(y['crotch'] + 6)}", region="torso"))
    out.append(L(smooth([(CX - ww, wy - 1), (CX + ww, wy - 1), (CX + ww + 0.4, wy + 7), (CX - ww - 0.4, wy + 7)], t=0.2), "bottomS", region="torso"))
    for x in (-ww * 0.65, ww * 0.65):
        out.append(L(path((CX + x, wy - 1), [("L", (CX + x, wy + 8))], close=False), stroke="bottom", width=1.6, alpha=0.7, region="torso"))
    out.append(L(path((CX + 1, wy + 7), [("C", (CX + 1.5, wy + 22), (CX + 1, y["crotch"] - 14), (CX - 1, y["crotch"] - 2))], close=False), stroke="bottomS", width=0.9, alpha=0.6, region="torso"))
    for s in (1, -1):  # front pockets
        out.append(L(path((CX + s * (ww - 4), wy + 8), [("C", (CX + s * (ww - 6), wy + 16), (CX + s * (ww - 5), wy + 21), (CX + s * (hipw - 1), wy + 24))], close=False),
                     stroke="bottomS", width=0.9, alpha=0.5, region="torso"))
    return out


def shoes(bt, kind="sneakers"):
    out = []
    y = Y[bt]
    for s in (1, -1):
        x, ay, w = leg_spine(bt, s)[-1]
        reg = leg_region(s)
        sole = y["sole"]
        k = 1.0 if bt == "F" else 1.12
        x += s * 0.6
        if kind == "boots":
            top = y["knee"] + 30
            shaft = smooth([(x - w - 3.6, top), (x + w + 3.6, top), (x + w + 2.6, ay - 2), (x - w - 2.6, ay - 2)], t=0.2)
            out.append(L(shaft, "lin:shoesL:shoes:" + f"{f(x - w)}:{f(top)}:{f(x + w)}:{f(top)}", region=reg))
            out.append(L(shaft, stroke="shoesS", width=0.8, alpha=0.6, region=reg))
            out.append(L(smooth([(x - w - 4, top - 2), (x + w + 4, top - 2), (x + w + 3.8, top + 7), (x - w - 3.8, top + 7)], t=0.2), "shoesL", region=reg))
        foot = smooth([(x - (w + 2) * k, ay - 5), (x + (w + 2) * k, ay - 5), (x + s * 1.5 + (w + 6) * k, sole - 12), (x + s * 2 + (w + 7) * k, sole - 4), (x + s * 1.5, sole),
                       (x + s * 2 - (w + 7) * k, sole - 4), (x + s * 1.5 - (w + 6) * k, sole - 12)], t=0.9)
        out.append(L(foot, ("lin:shoesL:shoes:" if kind == "sneakers" else "lin:shoes:shoesS:") + f"{f(x)}:{f(ay - 5)}:{f(x)}:{f(sole)}", region=reg))
        if kind == "sneakers":
            tongue = smooth([(x - 4.2 * k, ay - 3), (x + 4.2 * k, ay - 3), (x + s * 1 + 4.6 * k, sole - 12), (x + s * 1 - 4.6 * k, sole - 12)], t=0.4)
            out.append(L(tongue, "shoesS", alpha=0.4, region=reg))
            for i in range(3):
                yy = ay + i * 4
                out.append(L(path((x - 4.2 * k + s * i * 0.3, yy), [("L", (x + 4.2 * k + s * i * 0.3, yy + 0.6))], close=False), stroke="sole", width=1.1, alpha=0.9, region=reg))
            out.append(L(path((x + s * (w + 3) * k, sole - 14), [("C", (x + s * (w + 4.6) * k, sole - 9), (x + s * (w + 4.6) * k, sole - 6), (x + s * (w + 3.2) * k, sole - 3.5))], close=False),
                         stroke="acc", width=1.5, alpha=0.9, region=reg))
        band = smooth([(x + s * 2 - (w + 7.6) * k, sole - 5.5), (x + s * 2 + (w + 7.6) * k, sole - 5.5), (x + s * 2 + (w + 7) * k, sole + 1.6), (x + s * 1.5, sole + 2.6),
                       (x + s * 2 - (w + 7) * k, sole + 1.6)], t=0.6)
        out.append(L(band, "sole" if kind == "sneakers" else "shoesS", region=reg))
        out.append(L(foot, stroke="shoesS", width=0.8, alpha=0.6, region=reg))
    return out
