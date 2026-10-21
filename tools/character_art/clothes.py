"""Tops, bottoms and shoes built on the body spines."""
from geo import *
from parts import TORSO, LEG, ARM, Y, leg_spine, arm_spine


def torso_cut(bt, grow, hem, flare=0.0):
    """Torso outline grown outwards, cut flat at y = hem (flare widens the hem)."""
    right = TORSO[bt]
    pts = []
    for i, (x, y) in enumerate(right):
        if y > hem:
            px, py = right[i - 1]
            t = (hem - py) / (y - py)
            pts.append((CX + px + (x - px) * t + grow + flare, hem))
            break
        pts.append((CX + x + grow + (flare * max(0, y - 205) / max(1, hem - 205) if flare else 0), y))
    pts.append((CX, hem + 2))
    full = pts + [mx(p) for p in pts[::-1][1:-1]]
    return smooth(full, closed=True, t=0.85)


def top(bt, kind="tee", sleeve="short"):
    out = []
    hem = {"tee": 248, "hoodie": 256, "jacket": 258, "dress": 236, "knit": 250}[kind]
    sl = sleeve if kind == "tee" else "long"
    for s in (1, -1):
        if sl == "short":
            sp = arm_spine(bt, s, grow=2.2, until=180)
        else:
            sp = arm_spine(bt, s, grow=1.6 if kind != "hoodie" else 2.4)
        out.append(L(limb(sp), "lin:topL:top:" + f"{CX + s * 20}:150:{CX + s * 40}:150"))
        end = sp[-1]
        if sl == "long":
            out.append(L(limb([(end[0], end[1] - 6, end[2] + 0.4), (end[0], end[1], end[2] + 0.6)]), "topS"))
        out.append(L(path((sp[0][0] + s * 3, sp[0][1] + 8), [("C", (sp[1][0] + s * 2, sp[1][1]), (sp[-1][0] + s * 2, sp[-1][1] - 12), (sp[-1][0] + s * 2.5, sp[-1][1] - 3))], close=False),
                     stroke="topS", width=1.2, alpha=0.5))
    body = torso_cut(bt, 1.6, hem)
    out.append(L(body, "lin:topL:top:100:138:100:" + str(hem)))
    # side shading (darker towards the sides)
    out.append(L(body, "lin:topS:topS:60:0:140:0", alpha=0.0))
    for s in (1, -1):
        out.append(L(path((CX + s * 22, 168), [("C", (CX + s * 18, 196), (CX + s * 19, 220), (CX + s * 26, hem - 2)), ("L", (CX + s * 29, hem - 4)),
                                                ("C", (CX + s * 26, 232), (CX + s * 20, 214), (CX + s * 21, 196)), ("C", (CX + s * 22, 182), (CX + s * 27, 160), (CX + s * 27, 150))]), "topS", alpha=0.5))
    if bt == "F" and kind not in ("jacket",):
        out.append(L(path((CX - 14, 170), [("C", (CX - 7, 177), (CX + 7, 177), (CX + 14, 170))], close=False), stroke="topS", width=1.0, alpha=0.45))
    if kind == "tee":
        out.append(L(path((CX - 10, 138), [("C", (CX - 6, 150), (CX + 6, 150), (CX + 10, 138))], close=False), stroke="acc", width=2.4))
    elif kind == "knit":
        out.append(L(path((CX - 11, 137), [("C", (CX - 7, 147), (CX + 7, 147), (CX + 11, 137))], close=False), stroke="topS", width=3))
        for k in range(-3, 4):
            out.append(L(path((CX + k * 6, hem - 8), [("L", (CX + k * 6, hem))], close=False), stroke="topS", width=1.2, alpha=0.7))
    elif kind == "hoodie":
        out.append(L(sym((CX, 131), [("C", (CX + 14, 130), (CX + 23, 137), (CX + 21, 146)), ("C", (CX + 12, 152), (CX + 4, 154), (CX, 154))]), "topS"))
        for s in (1, -1):
            out.append(L(path((CX + s * 5, 152), [("L", (CX + s * 6, 180))], close=False), stroke="acc", width=1.4))
            out.append(L(ell(CX + s * 6, 181, 1.4, 1.8), "acc"))
        out.append(L(smooth([(CX - 15, 214), (CX + 15, 214), (CX + 18, 238), (CX - 18, 238)]), "topS", alpha=0.55))
    elif kind == "jacket":
        out.append(L(sym((CX, 138), [("L", (CX + 10, 138)), ("L", (CX + 7, 212)), ("L", (CX + 9, hem)), ("L", (CX, hem + 1))]), "acc"))
        for s in (1, -1):
            out.append(L(path((CX + s * 10, 137), [("L", (CX + s * 18, 150)), ("L", (CX + s * 10, 168)), ("L", (CX + s * 14, 182)), ("L", (CX + s * 7, 216))], close=False),
                         stroke="topS", width=1.6))
            out.append(L(ell(CX + s * 13, 226, 1.3, 1.3), "topL"))
    elif kind == "dress":
        out.append(L(smooth([(CX - 21, 226), (CX + 21, 226), (CX + 22, 232), (CX - 22, 232)]), "acc"))
        skirt = smooth([(CX - 20, 230), (CX + 20, 230), (CX + 32, 270), (CX + 40, 308), (CX, 314), (CX - 40, 308), (CX - 32, 270)])
        out.insert(0, L(skirt, "lin:top:topS:100:230:100:312"))
        for k in (-26, -13, 0, 13, 26):
            out.append(L(path((CX + k * 0.5, 236), [("L", (CX + k * 1.15, 310))], close=False), stroke="topS", width=1.0, alpha=0.5))
        out.append(L(path((CX - 10, 138), [("C", (CX - 6, 149), (CX + 6, 149), (CX + 10, 138))], close=False), stroke="acc", width=2))
    return out


def bottom(bt, kind="pants"):
    out = []
    if kind == "skirt":
        sk = smooth([(CX - 21, 228), (CX + 21, 228), (CX + 30, 258), (CX + 36, 296), (CX + 10, 301), (CX, 300), (CX - 10, 301), (CX - 36, 296), (CX - 30, 258)])
        out.append(L(sk, "lin:bottom:bottomS:100:228:100:300"))
        for k in (-24, -12, 0, 12, 24):
            out.append(L(path((CX + k * 0.6, 232), [("L", (CX + k * 1.25, 298))], close=False), stroke="bottomS", width=1.0, alpha=0.6))
        out.append(L(smooth([(CX - 21, 227), (CX + 21, 227), (CX + 21.5, 232), (CX - 21.5, 232)]), "bottomS"))
        return out
    until = None if kind == "pants" else 300
    for s in (1, -1):
        sp = leg_spine(bt, s, grow=2.3 if kind == "pants" else 3.2, until=until)
        out.append(L(limb(sp), "lin:bottom:bottomS:" + f"{CX + s * 4}:300:{CX + s * 26}:300"))
        e = sp[-1][1] - 2
        out.append(L(path((CX + s * 5, 272), [("C", (CX + s * 6, 272 + (e - 272) * 0.35), (CX + s * 7, 272 + (e - 272) * 0.7), (CX + s * 8.5, e))], close=False), stroke="bottomS", width=1.2, alpha=0.6))
    hips = smooth([(CX - 22, 226), (CX + 22, 226), (CX + 28, 246), (CX + 26, 262), (CX + 8, 270), (CX, 266), (CX - 8, 270), (CX - 26, 262), (CX - 28, 246)])
    out.append(L(hips, "lin:bottom:bottomS:100:226:100:270"))
    out.append(L(smooth([(CX - 22, 226), (CX + 22, 226), (CX + 22.5, 232), (CX - 22.5, 232)]), "bottomS"))
    out.append(L(path((CX, 232), [("L", (CX, 262))], close=False), stroke="bottomS", width=1.0, alpha=0.6))
    return out


def shoes(bt, kind="sneakers"):
    out = []
    for s in (1, -1):
        x, y, w = leg_spine(bt, s)[-1]
        if kind == "boots":
            boot = smooth([(x - s * (w + 1.5), 412), (x + s * (w + 1.5), 412), (x + s * (w + 1.2), 452), (x + s * (w + 6), 466), (x + s * (w + 6), 474),
                           (x - s * (w + 1.5), 474), (x - s * (w + 2), 450)])
            out.append(L(boot, "lin:shoesL:shoes:" + f"{x}:412:{x}:474"))
        else:
            shoe = smooth([(x - s * (w + 1.5), 451), (x + s * (w + 1), 450), (x + s * (w + 5), 462), (x + s * (w + 8), 470),
                           (x + s * (w + 6), 476), (x - s * (w + 2), 476), (x - s * (w + 2.5), 464)])
            out.append(L(shoe, "lin:shoesL:shoes:" + f"{x}:450:{x}:476"))
            out.append(L(path((x - s * 1, 455), [("L", (x + s * 3.5, 459))], close=False), stroke="shoesS", width=1.0, alpha=0.7))
        out.append(L(smooth([(x - s * (w + 2.6), 473), (x + s * (w + 7), 472), (x + s * (w + 6.5), 477.5), (x - s * (w + 2.4), 477.5)]), "sole"))
    return out
