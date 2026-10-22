"""Point warps applied to every path: the base pose (head sits a little lower on a shorter neck) and
the creator's fine-tune sliders. Each slider's warp at +1 minus the base becomes a morph target the
app blends with the slider value (export.py), so they must stay smooth and close to linear.
"""
import math, re
from geo import CX
from parts import ARM, LEG, TORSO

HEAD_DROP = 7.0
HEAD_PARAMS = ["faceWidth", "cheeks", "jaw", "chin"]
BODY_PARAMS = ["shoulders", "waist", "hips", "legs", "limbs"]
PARAMS = HEAD_PARAMS + BODY_PARAMS
HIP_Y = 242.0


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


def warp_point(x, y, region, bt, P):
    g = lambda k: P.get(k, 0.0)
    if region == "head":
        dx = x - CX
        dx *= 1 + 0.09 * g("faceWidth")
        dx *= 1 + 0.1 * g("cheeks") * math.exp(-((y - 88) / 15) ** 2)
        wc = smoothstep(90, 114, y) * (1 - smoothstep(118, 130, y)) * (1 - smoothstep(24, 40, abs(dx)))
        dx *= 1 + 0.32 * g("chin") * wc
        y += 6.0 * g("jaw") * smoothstep(76, 114, y)
        return CX + dx, y + HEAD_DROP
    sh_half = TORSO[bt][3][0]
    if region in ("armR", "armL"):
        s = 1 if region == "armR" else -1
        k = 1 + 0.22 * g("limbs")
        cx = CX + s * spine_x(ARM[bt], y)
        x = cx + (x - cx) * k
        x += s * sh_half * 0.14 * g("shoulders")
        return x, y
    if region in ("legR", "legL"):
        s = 1 if region == "legR" else -1
        k = 1 + 0.2 * g("limbs")
        cx = CX + s * spine_x(LEG[bt], y)
        x = cx + (x - cx) * k
        x += s * 14 * 0.12 * g("hips") * 0.8
        if y > HIP_Y:
            y = HIP_Y + (y - HIP_Y) * (1 + 0.08 * g("legs"))
        return x, y
    # torso (and anything else): width profile down the body, longer legs below the hips
    dx = x - CX
    k = 1 + 0.14 * g("shoulders") * bump(y, 126, 138, 158, 190) + 0.13 * g("waist") * bump(y, 168, 196, 212, 238) \
        + 0.12 * g("hips") * bump(y, 214, 238, 262, 320)
    x = CX + dx * k
    if y > HIP_Y:
        y = HIP_Y + (y - HIP_Y) * (1 + 0.08 * g("legs"))
    return x, y


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
