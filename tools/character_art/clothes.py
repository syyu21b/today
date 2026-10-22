"""Tops, bottoms and shoes built on the body spines.

Garments hang a little loose from the body (they don't hug the waist), carry soft gradients and a
few fold lines where cloth gathers: armpits, waist, knees, hems.
"""
from geo import *
from parts import TORSO, LEG, ARM, Y, leg_spine, arm_spine, leg_region, arm_region

HEMS = {"tee": 250, "hoodie": 256, "jacket": 258, "dress": 214, "knit": 252}
TOP_FILL = "lin:topL:top:100:138:100:"


def tx(bt, y):
    """Body half-width at y (linear between the torso outline points)."""
    pts = TORSO[bt]
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        if y0 <= y <= y1:
            return x0 + (x1 - x0) * (y - y0) / max(1e-6, y1 - y0)
    return pts[-1][0]


def shirt_outline(bt, hem, loose=2.0, neck=8.5, neck_y=134.5):
    """A top's body: follows shoulders and chest, then falls straight-ish to a softly curved hem."""
    sh = TORSO[bt][3][0]
    ch = tx(bt, 164)
    hip = tx(bt, min(hem, 248))
    waist = max(tx(bt, 204) + loose, (ch + hip) / 2 - 1.2)
    right = [(CX + neck, neck_y), (CX + sh - 6, neck_y + 3.5), (CX + sh + 1.6, 145), (CX + sh + 2.2, 152), (CX + ch + loose * 0.7, 166),
             (CX + waist, 204), (CX + hip + loose * 0.6, hem - 6), (CX + hip + loose * 0.5, hem), (CX + hip * 0.5, hem + 1.6)]
    full = right + [(CX, hem + 2)] + [mx(p) for p in right[::-1]] + [(CX, neck_y + 6)]
    return smooth(full, closed=True, t=0.85)


def sleeve(bt, s, short, grow, cuff=True):
    sp = arm_spine(bt, s, grow=grow, until=178 if short else None)
    if short:
        # short sleeves flare a little at the opening
        sp[-1] = (sp[-1][0] + s * 0.6, sp[-1][1], sp[-1][2] + 1.2)
    reg = arm_region(s)
    out = [L(limb(sp), "lin:topL:top:" + f"{CX + s * 20}:150:{CX + s * 40}:150", region=reg)]
    # fold where the sleeve meets the body and along the outside of the arm
    out.append(L(path((sp[0][0] - s * 2, sp[0][1] + 6), [("C", (sp[0][0] - s * 1, sp[0][1] + 14), (sp[1][0] - s * 2, sp[1][1] + 2), (sp[1][0] - s * 3, sp[1][1] + 8))], close=False),
                 stroke="topS", width=1.0, alpha=0.45, region=reg))
    if not short:
        ex, ey = sp[2][0], sp[2][1]
        for k in (-3, 2):
            out.append(L(path((ex - s * 4, ey + k), [("C", (ex - s * 1, ey + k + 2.5), (ex + s * 1.5, ey + k + 2), (ex + s * 4, ey + k - 0.5))], close=False),
                         stroke="topS", width=0.9, alpha=0.4, region=reg))
    if short:
        e = sp[-1]
        out.append(L(path((e[0] - e[2] * 0.95, e[1] - 1.5), [("C", (e[0] - e[2] * 0.3, e[1] + 0.5), (e[0] + e[2] * 0.3, e[1] + 0.5), (e[0] + e[2] * 0.95, e[1] - 1.5))], close=False),
                     stroke="topS", width=0.9, alpha=0.6, region=reg))
    elif cuff:
        e = sp[-1]
        band = limb([(e[0], e[1] - (3 if short else 5), e[2] + 0.3), (e[0], e[1], e[2] + 0.5)])
        out.append(L(band, "topS" if not short else "lin:top:topS:100:170:100:180", alpha=0.85 if not short else 0.6, region=reg))
    return out


def stripes(bt, kind, sleeve_kind):
    """Horizontal bands clipped to the shirt body."""
    hem = HEMS[kind]
    clip = shirt_outline(bt, hem, loose=2.6 if kind == "knit" else 2.0)
    out = []
    for y in range(158, hem + 2, 14):
        out.append(L(smooth([(CX - 60, y), (CX + 60, y), (CX + 60, y + 6), (CX - 60, y + 6)], t=0.1), "stripe", clip=clip, region="torso"))
    return out


def top(bt, kind="tee", sleeve_kind="short"):
    out = []
    hem = HEMS[kind]
    short = sleeve_kind == "short" and kind == "tee"
    loose = {"tee": 2.0, "knit": 2.6, "hoodie": 3.4, "jacket": 3.0, "dress": 1.0}[kind]
    for s in (1, -1):
        out += sleeve(bt, s, short, grow={"hoodie": 2.6, "knit": 2.2, "jacket": 2.2}.get(kind, 1.9 if not short else 2.4), cuff=kind != "dress")
    if kind == "dress":
        skirt = smooth([(CX - 20, 208), (CX + 20, 208), (CX + 28, 236), (CX + 38, 276), (CX + 44, 308), (CX + 22, 314), (CX, 312), (CX - 22, 314), (CX - 44, 308), (CX - 38, 276), (CX - 28, 236)], t=0.9)
        out.append(L(skirt, "lin:top:topS:100:210:100:312", region="torso"))
        for k in (-30, -16, -3, 10, 24):
            x0 = CX + k * 0.45
            out.append(L(path((x0, 216), [("C", (x0 + k * 0.1, 250), (CX + k * 0.95, 285), (CX + k * 1.25, 310))], close=False), stroke="topS", width=1.1, alpha=0.45, region="torso"))
            out.append(L(path((x0 + 3, 222), [("C", (x0 + 3 + k * 0.1, 254), (CX + k * 0.95 + 4, 288), (CX + k * 1.25 + 5, 309))], close=False), stroke="topL", width=1.4, alpha=0.35, region="torso"))
        out.append(L(path((CX - 44, 308), [("C", (CX - 20, 316), (CX + 20, 316), (CX + 44, 308))], close=False), stroke="topS", width=1.0, alpha=0.6, region="torso"))
    body = shirt_outline(bt, hem, loose)
    out.append(L(body, TOP_FILL + str(hem), region="torso"))
    # cloth shading: darker at the sides, folds under the arms and at the waist
    for s in (1, -1):
        sh = TORSO[bt][3][0]
        out.append(L(smooth([(CX + s * (sh - 2), 152), (CX + s * (sh + 1.6), 156), (CX + s * (tx(bt, 204) + loose - 0.2), 204), (CX + s * (tx(bt, 240) + loose * 0.6), hem - 2),
                             (CX + s * (tx(bt, 240) - 3), hem - 3), (CX + s * (tx(bt, 204) - 1.5), 204), (CX + s * (sh - 5), 160)], closed=True, t=0.9), "topS", alpha=0.22, region="torso"))
        out.append(L(path((CX + s * (sh - 3), 154), [("C", (CX + s * (sh - 6), 166), (CX + s * (sh - 9), 176), (CX + s * (sh - 13), 184))], close=False),
                     stroke="topS", width=1.0, alpha=0.45, region="torso"))
    if bt == "F" and kind not in ("jacket",):
        out.append(L(path((CX - 15, 172), [("C", (CX - 8, 178), (CX + 8, 178), (CX + 15, 172))], close=False), stroke="topS", width=1.0, alpha=0.4, region="torso"))
    if kind == "tee":
        out.append(L(path((CX - 9, 135), [("C", (CX - 6, 145), (CX + 6, 145), (CX + 9, 135))], close=False), stroke="acc", width=2.6, region="torso"))
        out.append(L(path((CX - 22, hem - 1), [("C", (CX - 8, hem + 1.6), (CX + 8, hem + 1.6), (CX + 22, hem - 1))], close=False), stroke="topS", width=0.8, alpha=0.5, region="torso"))
    elif kind == "knit":
        out.append(L(path((CX - 10, 135), [("C", (CX - 7, 146), (CX + 7, 146), (CX + 10, 135))], close=False), stroke="topS", width=3.6, region="torso"))
        band = smooth([(CX - tx(bt, 248) - 2, hem - 8), (CX + tx(bt, 248) + 2, hem - 8), (CX + tx(bt, 248) + 1.6, hem + 0.5), (CX, hem + 2), (CX - tx(bt, 248) - 1.6, hem + 0.5)], t=0.4)
        out.append(L(band, "topS", alpha=0.55, region="torso"))
        for k in range(-5, 6):
            out.append(L(path((CX + k * 4.6, hem - 7), [("L", (CX + k * 4.8, hem))], close=False), stroke="topS", width=0.9, alpha=0.6, region="torso"))
        for s in (1, -1):  # cable stitches
            x = CX + s * 9
            out.append(L(path((x, 150), [("C", (x + 3, 165), (x - 3, 180), (x, 195)), ("C", (x + 3, 210), (x - 3, 225), (x, hem - 9))], close=False), stroke="topS", width=1.2, alpha=0.4, region="torso"))
    elif kind == "hoodie":
        hood = smooth([(CX - 24, 144), (CX - 19, 132), (CX - 8, 127), (CX + 8, 127), (CX + 19, 132), (CX + 24, 144), (CX + 12, 150), (CX, 156), (CX - 12, 150)], t=0.85)
        out.append(L(hood, "lin:topS:top:100:127:100:156", region="torso"))
        out.append(L(smooth([(CX - 13, 141), (CX - 6, 136), (CX + 6, 136), (CX + 13, 141), (CX + 6, 149), (CX, 153), (CX - 6, 149)], t=0.85), "topS", alpha=0.8, region="torso"))
        out.append(L(hood, stroke="topS", width=0.9, alpha=0.6, region="torso"))
        for s in (1, -1):
            out.append(L(path((CX + s * 5, 150), [("C", (CX + s * 5.5, 160), (CX + s * 6.5, 170), (CX + s * 6, 182))], close=False), stroke="acc", width=1.5, region="torso"))
            out.append(L(ell(CX + s * 6, 183.5, 1.5, 2.2), "acc", region="torso"))
        pocket = smooth([(CX - 16, 214), (CX + 16, 214), (CX + 20, 240), (CX - 20, 240)], t=0.25)
        out.append(L(pocket, "topS", alpha=0.35, region="torso"))
        out.append(L(pocket, stroke="topS", width=1.0, alpha=0.6, region="torso"))
        band = smooth([(CX - tx(bt, 248) - 3, hem - 7), (CX + tx(bt, 248) + 3, hem - 7), (CX + tx(bt, 248) + 2.6, hem + 0.5), (CX, hem + 2), (CX - tx(bt, 248) - 2.6, hem + 0.5)], t=0.4)
        out.append(L(band, "topS", alpha=0.6, region="torso"))
    elif kind == "jacket":
        # inner shirt in the accent colour, open jacket panels with lapels over it
        inner = sym((CX, 136), [("L", (CX + 9, 136)), ("C", (CX + 11, 170), (CX + 9, 210), (CX + 10, hem - 1)), ("L", (CX, hem))])
        out.append(L(inner, "lin:acc:accS:100:150:100:260", region="torso"))
        out.append(L(path((CX - 8, 137), [("C", (CX - 5, 144), (CX + 5, 144), (CX + 8, 137))], close=False), stroke="accS", width=1.6, region="torso"))
        for s in (1, -1):
            lapel = path((CX + s * 9, 134), [("L", (CX + s * 19, 140)), ("L", (CX + s * 16, 152)), ("L", (CX + s * 20, 156)), ("L", (CX + s * 9.5, 196)),
                                             ("C", (CX + s * 10, 184), (CX + s * 10, 160), (CX + s * 9, 134))])
            out.append(L(lapel, "lin:topS:top:" + f"{CX + s * 9}:150:{CX + s * 20}:150", region="torso"))
            out.append(L(lapel, stroke="topS", width=1.0, alpha=0.8, region="torso"))
            out.append(L(path((CX + s * 9.5, 196), [("C", (CX + s * 10, 215), (CX + s * 10, 235), (CX + s * 10.5, hem))], close=False), stroke="topS", width=1.2, alpha=0.8, region="torso"))
            out.append(L(path((CX + s * 15, 222), [("L", (CX + s * 24, 221))], close=False), stroke="topS", width=1.1, alpha=0.6, region="torso"))
        for y in (206, 226):
            out.append(L(ell(CX - 12.5, y, 1.3, 1.3), "topS", region="torso"))
    elif kind == "dress":
        out.append(L(path((CX - 10, 135.5), [("C", (CX - 6, 147), (CX + 6, 147), (CX + 10, 135.5))], close=False), stroke="acc", width=2.2, region="torso"))
        sash = smooth([(CX - tx(bt, 204) - 1.5, 204), (CX + tx(bt, 204) + 1.5, 204), (CX + tx(bt, 210) + 2, 212), (CX - tx(bt, 210) - 2, 212)], t=0.3)
        out.append(L(sash, "lin:acc:accS:100:204:100:212", region="torso"))
        bow = smooth([(CX, 208), (CX + 7, 203), (CX + 9, 208), (CX + 7, 213), (CX, 208), (CX - 7, 213), (CX - 9, 208), (CX - 7, 203)], t=0.6)
        out.append(L(bow, "acc", region="torso"))
        out.append(L(ell(CX, 208, 2, 2.2), "accS", region="torso"))
    out.append(L(body, stroke="topS", width=0.8, alpha=0.5, region="torso"))
    return out


def bottom(bt, kind="pants"):
    out = []
    if kind == "skirt":
        sk = smooth([(CX - 22, 226), (CX + 22, 226), (CX + 30, 256), (CX + 37, 294), (CX + 18, 299), (CX, 298), (CX - 18, 299), (CX - 37, 294), (CX - 30, 256)], t=0.9)
        out.append(L(sk, "lin:bottom:bottomS:100:226:100:298", region="torso"))
        for k in range(-3, 4):  # pleats: a lit and a shaded side each
            x0, x1 = CX + k * 6.2, CX + k * 10.6
            out.append(L(smooth([(x0, 232), (x0 + 3, 232), (x1 + 5, 296), (x1, 297)], t=0.2), "bottomS", alpha=0.45, clip=sk, region="torso"))
            out.append(L(path((x0 - 0.5, 232), [("L", (x1 - 1, 297))], close=False), stroke="bottomS", width=0.8, alpha=0.7, region="torso"))
        out.append(L(smooth([(CX - 22, 225), (CX + 22, 225), (CX + 22.6, 232), (CX - 22.6, 232)], t=0.2), "bottomS", region="torso"))
        out.append(L(sk, stroke="bottomS", width=0.8, alpha=0.6, region="torso"))
        return out
    until = None if kind == "pants" else 300
    for s in (1, -1):
        sp = leg_spine(bt, s, grow=2.4 if kind == "pants" else 3.4, until=until)
        if kind == "pants":
            sp = [(x, y, max(w, 7.0 if bt == "F" else 7.8)) for (x, y, w) in sp]  # straight leg, not skin tight
        reg = leg_region(s)
        out.append(L(limb(sp), "lin:bottomS:bottom:" + f"{CX + s * 2}:300:{CX + s * 20}:300", region=reg))
        e = sp[-1][1] - 2
        out.append(L(path((CX + s * 9, 272), [("C", (CX + s * 10, 272 + (e - 272) * 0.35), (CX + s * 10.5, 272 + (e - 272) * 0.7), (CX + s * 10.6, e))], close=False),
                     stroke="bottomS", width=0.9, alpha=0.55, region=reg))
        if kind == "pants":
            kx = CX + s * 11
            for k in (0, 6):  # creases behind the knee
                out.append(L(path((kx - s * 6, 350 + k), [("C", (kx - s * 2, 353 + k), (kx + s * 2, 352 + k), (kx + s * 6, 348 + k))], close=False), stroke="bottomS", width=0.8, alpha=0.45, region=reg))
            x, y, w = sp[-1]
            out.append(L(path((x - w, y - 3), [("C", (x - w * 0.4, y - 1.5), (x + w * 0.4, y - 1.5), (x + w, y - 3))], close=False), stroke="bottomS", width=1.0, alpha=0.6, region=reg))
        else:
            x, y, w = sp[-1]
            out.append(L(limb([(x, y - 5, w + 0.6), (x, y, w + 0.8)]), "bottomS", alpha=0.85, region=reg))
    hips = smooth([(CX - 23, 225), (CX + 23, 225), (CX + 28.5, 246), (CX + 27, 262), (CX + 9, 272), (CX, 268), (CX - 9, 272), (CX - 27, 262), (CX - 28.5, 246)], t=0.9)
    out.append(L(hips, "lin:bottom:bottomS:100:225:100:272", region="torso"))
    out.append(L(smooth([(CX - 23, 224), (CX + 23, 224), (CX + 23.4, 231), (CX - 23.4, 231)], t=0.2), "bottomS", region="torso"))
    for x in (-15, 15):
        out.append(L(path((CX + x, 224), [("L", (CX + x, 232))], close=False), stroke="bottom", width=1.4, alpha=0.7, region="torso"))
    out.append(L(path((CX + 1, 232), [("C", (CX + 1.5, 245), (CX + 1, 255), (CX - 1, 263))], close=False), stroke="bottomS", width=0.9, alpha=0.6, region="torso"))
    for s in (1, -1):
        out.append(L(path((CX + s * 20, 233), [("C", (CX + s * 18, 240), (CX + s * 18, 244), (CX + s * 24, 246))], close=False), stroke="bottomS", width=0.9, alpha=0.5, region="torso"))
    return out


def shoes(bt, kind="sneakers"):
    out = []
    for s in (1, -1):
        x, y, w = leg_spine(bt, s)[-1]
        reg = leg_region(s)
        x += s * 0.6
        if kind == "boots":
            shaft = smooth([(x - w - 2.2, 404), (x + w + 2.2, 404), (x + w + 1.8, 448), (x - w - 1.8, 448)], t=0.2)
            out.append(L(shaft, "lin:shoesL:shoes:" + f"{f(x - w)}:420:{f(x + w)}:420", region=reg))
            out.append(L(shaft, stroke="shoesS", width=0.8, alpha=0.6, region=reg))
            out.append(L(smooth([(x - w - 2.6, 402), (x + w + 2.6, 402), (x + w + 2.4, 410), (x - w - 2.4, 410)], t=0.2), "shoesL", region=reg))
        foot = smooth([(x - w - 1.6, 448), (x + w + 1.6, 448), (x + s * 1.2 + w + 4.8, 462), (x + s * 1.5 + w + 5.2, 472), (x + s * 1.2, 476.5),
                       (x + s * 1.5 - w - 5.2, 472), (x + s * 1.2 - w - 4.8, 462)], t=0.9)
        out.append(L(foot, ("lin:shoesL:shoes:" if kind == "sneakers" else "lin:shoes:shoesS:") + f"{f(x)}:448:{f(x)}:476", region=reg))
        if kind == "sneakers":
            tongue = smooth([(x - 3.4, 450), (x + 3.4, 450), (x + s * 0.8 + 3.8, 464), (x + s * 0.8 - 3.8, 464)], t=0.4)
            out.append(L(tongue, "shoesS", alpha=0.45, region=reg))
            for k in range(3):
                yy = 453 + k * 3.8
                out.append(L(path((x - 3.4 + s * k * 0.3, yy), [("L", (x + 3.4 + s * k * 0.3, yy + 0.6))], close=False), stroke="sole", width=1.0, alpha=0.9, region=reg))
            out.append(L(path((x + s * (w + 2.2), 458), [("C", (x + s * (w + 3.6), 463), (x + s * (w + 3.6), 466), (x + s * (w + 2.4), 469))], close=False), stroke="acc", width=1.4, alpha=0.9, region=reg))
        sole = smooth([(x + s * 1.5 - w - 5.8, 471.5), (x + s * 1.5 + w + 5.8, 471.5), (x + s * 1.5 + w + 5.4, 478.5), (x + s * 1.2, 479.5), (x + s * 1.5 - w - 5.4, 478.5)], t=0.6)
        out.append(L(sole, "sole" if kind == "sneakers" else "shoesS", region=reg))
        out.append(L(foot, stroke="shoesS", width=0.8, alpha=0.6, region=reg))
    return out
