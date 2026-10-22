"""Point warps applied to every path: the base pose (the head scaled onto the body, standing straight) and
the creator's fine-tune sliders. Each slider's warp at +1 minus the base becomes a morph target the
app blends with the slider value (export.py), so they must stay smooth and close to linear.
"""
import math, re
from geo import CX
from parts import ARM, LEG, TORSO

# The head art is drawn big (crown at y = 23.5); it is scaled down and seated on the neck, pivoting on the
# neck point (head art y 112 -> figure y 110.5) so the chin stays put on the body.
HEAD_K = 0.76
HEAD_PIVOT = (112.0, 110.53)
# The head art's cranium is too tall for the face (eyes ~2/3 of the way down): the band between the
# crown and the brows is squashed to CRANIUM_K of its height, easing in above the crown and out above
# the brows, so the eyes sit near the middle of the head; everything above moves down with it unchanged.
CRANIUM_K = 0.58
CRANIUM = (6.0, 20.0, 46.0, 63.0)  # squash ramps in over [0]..[1], holds to [2], ramps out by [3]
# The forehead and the top of the skull are narrowed a little (temples narrower than the cheekbones).
TEMPLE_NARROW = 0.06
# With the shorter cranium the whole head is made a little narrower, so it doesn't read as broad.
HEAD_W = 0.95
HEAD_PARAMS = ["faceWidth", "cheeks", "jaw", "chin"]
BODY_PARAMS = ["shoulders", "waist", "hips", "legs", "limbs"]
PARAMS = HEAD_PARAMS + BODY_PARAMS
HIP_Y = 280.0


def smoothstep(a, b, x):
    t = min(1.0, max(0.0, (x - a) / (b - a)))
    return t * t * (3 - 2 * t)


def bump(y, a, b, c, d):
    """0 below a, rising to 1 at b, 1 until c, falling to 0 at d."""
    return smoothstep(a, b, y) * (1 - smoothstep(c, d, y))


def _ramp_area(a, b, y):
    """Integral of smoothstep(a, b, t) dt from below a up to y."""
    if y <= a:
        return 0.0
    if y >= b:
        return (b - a) * 0.5 + (y - b)
    t = (y - a) / (b - a)
    return (b - a) * (t ** 3 - t ** 4 / 2)


def cranium_y(y):
    """y in the head art with the cranium squashed (see CRANIUM); unchanged below the brows."""
    t0, t1, t2, t3 = CRANIUM
    band = lambda yy: _ramp_area(t0, t1, yy) - _ramp_area(t2, t3, yy)  # integral of the squash weight
    return y + (1 - CRANIUM_K) * (band(t3 + 1) - band(y))


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
        dx *= 1 - TEMPLE_NARROW * (1 - smoothstep(50, 82, y))
        y += 6.0 * g("jaw") * smoothstep(76, 114, y)
        return CX + dx * HEAD_W * HEAD_K, HEAD_PIVOT[1] + (cranium_y(y) - HEAD_PIVOT[0]) * HEAD_K
    sh_half = TORSO[bt][4][0]
    legs = lambda yy: HIP_Y + (yy - HIP_Y) * (1 + 0.06 * g("legs")) if yy > HIP_Y else yy
    if region in ("armR", "armL"):
        s = 1 if region == "armR" else -1
        k = 1 + 0.22 * g("limbs")
        cx = CX + s * spine_x(ARM[bt], y)
        x = cx + (x - cx) * k
        x += s * sh_half * 0.12 * g("shoulders")
        return x, y
    if region in ("legR", "legL"):
        s = 1 if region == "legR" else -1
        k = 1 + 0.2 * g("limbs")
        cx = CX + s * spine_x(LEG[bt], y)
        x = cx + (x - cx) * k
        x += s * 0.12 * 14 * g("hips")
        return x, legs(y)
    # torso (and anything else): width profile down the body, longer legs below the hips
    dx = x - CX
    k = 1 + 0.12 * g("shoulders") * bump(y, 128, 140, 168, 205) + 0.12 * g("waist") * bump(y, 196, 222, 240, 262)         + 0.11 * g("hips") * bump(y, 250, 268, 292, 360)
    x = CX + dx * k
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
