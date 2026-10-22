"""Point warps applied to every path: the base pose (head sits a little lower on a shorter neck) and
the creator's fine-tune sliders. Each slider's warp at +1 minus the base becomes a morph target the
app blends with the slider value (export.py), so they must stay smooth and close to linear.
"""
import math, re
from geo import CX
from parts import ARM, LEG, TORSO

# The head art is drawn big (crown at y = 23.5); it is scaled to 1/6.2 of the figure and seated on the neck.
HEAD_K = 0.797
HEAD_CROWN = (23.5, 40.0)  # crown y in the head art -> crown y on the figure
HEAD_PARAMS = ["faceWidth", "cheeks", "jaw", "chin"]
BODY_PARAMS = ["shoulders", "waist", "hips", "legs", "limbs"]
PARAMS = HEAD_PARAMS + BODY_PARAMS
HIP_Y = 280.0
# Relaxed standing pose (contrapposto): weight on the figure's right leg (viewer's left, s = -1),
# pelvis shifted over it and tilted up on that side, shoulders tilted the other way, the free leg's
# knee turned slightly in.
TILT_HIP = 0.06
TILT_SH = 0.03
SHIFT = 3.0


def smoothstep(a, b, x):
    t = min(1.0, max(0.0, (x - a) / (b - a)))
    return t * t * (3 - 2 * t)


def bump(y, a, b, c, d):
    """0 below a, rising to 1 at b, 1 until c, falling to 0 at d."""
    return smoothstep(a, b, y) * (1 - smoothstep(c, d, y))


def spine_x(spine, y):
    pts = [(x, yy) for (x, yy, _) in spine]
    if y <= pts[0][1]:
        return pts[0][0]
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        if y0 <= y <= y1:
            return x0 + (x1 - x0) * (y - y0) / (y1 - y0)
    return pts[-1][0]


def warp_point(x, y, region, bt, P, pose=True):
    g = lambda k: P.get(k, 0.0)
    if region == "head":
        dx = x - CX
        dx *= 1 + 0.09 * g("faceWidth")
        dx *= 1 + 0.1 * g("cheeks") * math.exp(-((y - 88) / 15) ** 2)
        wc = smoothstep(90, 114, y) * (1 - smoothstep(118, 130, y)) * (1 - smoothstep(24, 40, abs(dx)))
        dx *= 1 + 0.32 * g("chin") * wc
        y += 6.0 * g("jaw") * smoothstep(76, 114, y)
        return CX + dx * HEAD_K, HEAD_CROWN[1] + (y - HEAD_CROWN[0]) * HEAD_K
    sh_half = TORSO[bt][4][0]
    legs = lambda yy: HIP_Y + (yy - HIP_Y) * (1 + 0.06 * g("legs")) if yy > HIP_Y else yy
    if region in ("armR", "armL"):
        s = 1 if region == "armR" else -1
        k = 1 + 0.22 * g("limbs")
        cx = CX + s * spine_x(ARM[bt], y)
        x = cx + (x - cx) * k
        x += s * sh_half * 0.12 * g("shoulders")
        if pose:
            y -= TILT_SH * s * 36  # the arm hangs from its (tilted) shoulder
        return x, y
    if region in ("legR", "legL"):
        s = 1 if region == "legR" else -1
        k = 1 + 0.2 * g("limbs")
        cx = CX + s * spine_x(LEG[bt], y)
        x = cx + (x - cx) * k
        x += s * 0.12 * 14 * g("hips")
        if pose:
            down = smoothstep(290, 472, y)
            if s < 0:  # standing leg slants in under the body
                x += -SHIFT + 3.0 * down
            else:  # free leg: knee turned in, foot relaxed slightly out
                x += -SHIFT - 3.0 * bump(y, 300, 372, 396, 466) + 3.5 * smoothstep(400, 472, y)
            y += TILT_HIP * (x - CX) * (1 - down)
        return x, legs(y)
    # torso (and anything else): width profile down the body, longer legs below the hips
    dx = x - CX
    k = 1 + 0.12 * g("shoulders") * bump(y, 128, 140, 168, 205) + 0.12 * g("waist") * bump(y, 196, 222, 240, 262)         + 0.11 * g("hips") * bump(y, 250, 268, 292, 360)
    x = CX + dx * k
    if pose:
        y += -TILT_SH * dx * bump(y, 118, 134, 170, 215) + TILT_HIP * dx * smoothstep(232, 272, y) * (1 - smoothstep(300, 472, y))
        x += -SHIFT * smoothstep(190, 270, y)
    return x, legs(y)


NUM = re.compile(r"-?\d+(?:\.\d+)?(?:e-?\d+)?")


def numbers(d):
    return [float(v) for v in NUM.findall(d)]


def warp_path(d, region, bt, P):
    """Every path here uses absolute M/L/C/Q/Z, so its numbers are plain x,y pairs."""
    vals = numbers(d)
    out = []
    for i in range(0, len(vals), 2):
        out += list(warp_point(vals[i], vals[i + 1], region, bt, P))
    it = iter(out)
    return NUM.sub(lambda m: fmt(next(it)), d)


def fmt(v):
    s = f"{v:.2f}".rstrip("0").rstrip(".")
    return "0" if s in ("-0", "") else s


def warp_paint(spec, region, bt, P):
    """Moves gradient end points along with the shape."""
    if not spec or ":" not in spec:
        return spec
    p = spec.split(":")
    if p[0] == "lin":
        x1, y1 = warp_point(float(p[3]), float(p[4]), region, bt, P)
        x2, y2 = warp_point(float(p[5]), float(p[6]), region, bt, P)
        return ":".join(p[:3] + [fmt(x1), fmt(y1), fmt(x2), fmt(y2)])
    if p[0] == "rad":
        cx, cy = warp_point(float(p[3]), float(p[4]), region, bt, P)
        return ":".join(p[:3] + [fmt(cx), fmt(cy), p[5]])
    return spec
