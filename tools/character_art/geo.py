"""Tiny vector toolkit: symmetric paths, ellipses, layers. Coordinates in a 200 x 500 box, centre x = 100."""
CX = 100.0


def f(v):
    s = f"{v:.1f}"
    return s[:-2] if s.endswith(".0") else s


def P(p):
    return f"{f(p[0])},{f(p[1])}"


def mx(p):
    return (2 * CX - p[0], p[1])


def sym(start, segs):
    """Closed path symmetric about x=100. start and the last point lie on the centre line;
    segs are ('C', c1, c2, p) / ('L', p) / ('Q', c, p) for the right half, top to bottom."""
    d = [f"M{P(start)}"]
    pts = [start]
    for s in segs:
        if s[0] == "C":
            d.append(f"C{P(s[1])} {P(s[2])} {P(s[3])}")
        elif s[0] == "Q":
            d.append(f"Q{P(s[1])} {P(s[2])}")
        else:
            d.append(f"L{P(s[1])}")
        pts.append(s[-1])
    # left half: walk back, mirrored
    for i in range(len(segs) - 1, -1, -1):
        s = segs[i]
        prev = pts[i]
        if s[0] == "C":
            d.append(f"C{P(mx(s[2]))} {P(mx(s[1]))} {P(mx(prev))}")
        elif s[0] == "Q":
            d.append(f"Q{P(mx(s[1]))} {P(mx(prev))}")
        else:
            d.append(f"L{P(mx(prev))}")
    return " ".join(d) + "Z"


def path(start, segs, close=True):
    d = [f"M{P(start)}"]
    for s in segs:
        if s[0] == "C":
            d.append(f"C{P(s[1])} {P(s[2])} {P(s[3])}")
        elif s[0] == "Q":
            d.append(f"Q{P(s[1])} {P(s[2])}")
        else:
            d.append(f"L{P(s[1])}")
    return " ".join(d) + ("Z" if close else "")


def mirror_path(start, segs, close=True):
    return path(mx(start), [(s[0],) + tuple(mx(q) for q in s[1:]) for s in segs], close)


def ell(cx, cy, rx, ry):
    k = 0.5523
    return (f"M{f(cx)},{f(cy - ry)} C{f(cx + rx * k)},{f(cy - ry)} {f(cx + rx)},{f(cy - ry * k)} {f(cx + rx)},{f(cy)} "
            f"C{f(cx + rx)},{f(cy + ry * k)} {f(cx + rx * k)},{f(cy + ry)} {f(cx)},{f(cy + ry)} "
            f"C{f(cx - rx * k)},{f(cy + ry)} {f(cx - rx)},{f(cy + ry * k)} {f(cx - rx)},{f(cy)} "
            f"C{f(cx - rx)},{f(cy - ry * k)} {f(cx - rx * k)},{f(cy - ry)} {f(cx)},{f(cy - ry)}Z")


class L:
    """One layer: fill (role / #hex / gradient spec), optional stroke, opacity."""
    def __init__(self, d, fill=None, stroke=None, width=0.0, alpha=1.0, clip=None, region=None):
        self.d, self.fill, self.stroke, self.width, self.alpha, self.clip = d, fill, stroke, width, alpha, clip
        # Which body region the layer belongs to (warp.py): head / torso / armR / armL / legR / legL; None = by part.
        self.region = region


def smooth(pts, closed=True, t=1.0):
    """Catmull-Rom spline through pts as a cubic Bezier path."""
    n = len(pts)
    d = [f"M{P(pts[0])}"]
    rng = range(n) if closed else range(n - 1)
    for i in rng:
        p0 = pts[(i - 1) % n] if (closed or i > 0) else pts[0]
        p1 = pts[i]
        p2 = pts[(i + 1) % n]
        p3 = pts[(i + 2) % n] if (closed or i + 2 < n) else pts[-1]
        c1 = (p1[0] + (p2[0] - p0[0]) / 6 * t, p1[1] + (p2[1] - p0[1]) / 6 * t)
        c2 = (p2[0] - (p3[0] - p1[0]) / 6 * t, p2[1] - (p3[1] - p1[1]) / 6 * t)
        d.append(f"C{P(c1)} {P(c2)} {P(p2)}")
    return " ".join(d) + ("Z" if closed else "")


def limb(spine):
    """Closed outline around a centreline [(x, y, half_width)], rounded ends."""
    import math
    right, left = [], []
    for i, (x, y, w) in enumerate(spine):
        a = spine[max(i - 1, 0)]
        b = spine[min(i + 1, len(spine) - 1)]
        dx, dy = b[0] - a[0], b[1] - a[1]
        ln = math.hypot(dx, dy) or 1
        nx, ny = -dy / ln, dx / ln
        right.append((x + nx * w, y + ny * w))
        left.append((x - nx * w, y - ny * w))
    return smooth(right + left[::-1], closed=True, t=0.9)


def mirror_pts(pts):
    return [mx(p) for p in pts]
