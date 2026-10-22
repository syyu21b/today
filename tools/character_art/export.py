"""Exports every part variant to app/src/main/assets/character_art.json.

Layer format: [path, fill, stroke, width, alpha, clip, morphs]. Paths come out in the base pose
(warp.py); morphs = [[param index, 0 = path / 1 = clip, [number index, delta, ...]], ...] say how the
path's numbers move per unit of each fine-tune slider (warp.PARAMS). Features that the app moves and
scales as a whole (eyes, brows, nose, mouth) also get their pivots in "anchors.<face shape>".
"""
import json, os
import parts, hair, clothes, acc, warp
from geo import L, CX

OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "app", "src", "main", "assets", "character_art.json")
GENDERS = ["F", "M"]
MOODS = ["normal", "happy", "sleepy", "sad", "excited", "calm", "love", "angry", "sick"]
HAIRS = hair.ALL
EYES = ["almond", "round", "sharp", "gentle"]
BROWS = ["natural", "arched", "straight", "thick"]
FACES = ["oval", "round", "sharp", "square"]
TOPS = [("tee", "short"), ("tee", "long"), ("knit", "long"), ("hoodie", "long"), ("jacket", "long"), ("dress", "long"), ("shirt", "long")]
ACCS = ["cap", "glasses", "sunglasses", "headphones", "cape", "crown", "catears", "halo", "partyhat", "flowercrown", "strawhat", "witchhat", "santahat"]
SIDES = {"R": (1,), "L": (-1,)}

# Pivots (art coordinates before the base warp) the app scales / moves the features around.
ANCHORS = [("eyeR", CX + parts.EYE_X, parts.EYE_Y), ("eyeL", CX - parts.EYE_X, parts.EYE_Y), ("browR", CX + 15.5, 70), ("browL", CX - 15.5, 70),
           ("nose", CX, 96), ("mouth", CX, 106.5), ("neck", CX, 112)]


def deltas(base, moved):
    a, b = warp.numbers(base), warp.numbers(moved)
    assert len(a) == len(b)
    out = []
    for i, (x, y) in enumerate(zip(a, b)):
        dv = round(y - x, 2)
        if abs(dv) >= 0.01:
            out += [i, dv]
    return out


def enc(layers, region, bt="F"):
    out = []
    for l in layers:
        if (l.fill is None and l.stroke is None) or l.alpha <= 0:
            continue
        reg = l.region or region
        params = warp.HEAD_PARAMS if reg == "head" else warp.BODY_PARAMS
        d = warp.warp_path(l.d, reg, bt, {})
        clip = warp.warp_path(l.clip, reg, bt, {}) if l.clip else ""
        morphs = []
        for name in params:
            unit = {name: 1.0}
            for target, src, base in ((0, l.d, d), (1, l.clip, clip)):
                if not src:
                    continue
                dl = deltas(base, warp.warp_path(src, reg, bt, unit))
                if dl:
                    morphs.append([warp.PARAMS.index(name), target, dl])
        row = [d, warp.warp_paint(l.fill, reg, bt, {}) or "", l.stroke or "", round(l.width, 2), round(l.alpha, 2), clip]
        if morphs:
            row.append(morphs)
        out.append(row)
    return out


def anchors(shape, g):
    """One tiny path per pivot ("M x,y L x,y"), morphed like everything else so pivots follow the face."""
    out = []
    for _, x, y in ANCHORS:
        out.append(L(f"M{x},{y} L{x},{y}", stroke="line", width=0, alpha=0.01))
    # chin bottom (portrait framing)
    chin = parts.shapes(g)[1][shape]
    out.append(L(f"M{CX},{chin} L{CX},{chin}", stroke="line", width=0, alpha=0.01))
    return enc(out, "head")


def main():
    p = {}
    for h in HAIRS:
        b, fr = hair.style(h)
        p[f"hairBack.{h}"] = enc(b, "head")
        p[f"hairFront.{h}"] = enc(fr, "head")
    for bt in GENDERS:
        p[f"body.{bt}"] = enc(parts.body(bt), "torso", bt)
        p[f"arms.{bt}"] = enc(parts.arms(bt), "torso", bt)
        p[f"hands.{bt}"] = enc(parts.hands(bt), "torso", bt)
        for k in ("pants", "shorts", "skirt"):
            p[f"bottom.{bt}.{k}"] = enc(clothes.bottom(bt, k), "torso", bt)
        for k in ("sneakers", "boots"):
            p[f"shoes.{bt}.{k}"] = enc(clothes.shoes(bt, k), "torso", bt)
        for kind, sl in TOPS:
            layers = clothes.top(bt, kind, sl)
            # the garment body goes under the arms, the sleeves over them
            p[f"top.{bt}.{kind}.{sl}"] = enc([l for l in layers if not (l.region or "").startswith("arm")], "torso", bt)
            p[f"sleeves.{bt}.{kind}.{sl}"] = enc([l for l in layers if (l.region or "").startswith("arm")], "torso", bt)
            p[f"stripes.{bt}.{kind}.{sl}"] = enc(clothes.stripes(bt, kind, sl), "torso", bt)
        for e in EYES:
            for m in MOODS + ["blink"]:
                for side, sides in SIDES.items():
                    p[f"eyes.{bt}.{e}.{m}.{side}"] = enc(parts.eyes(e, "sleepy" if m == "blink" else m, bt, sides), "head")
    for g in GENDERS:
        for fs in FACES:
            p[f"head.{g}.{fs}"] = enc(parts.head(fs, g), "head")
            p[f"anchors.{g}.{fs}"] = anchors(fs, g)
        p[f"nose.{g}"] = enc(parts.nose(g), "head")
        for m in MOODS:
            p[f"mouth.{g}.{m}"] = enc(parts.mouth(m, g), "head")
        for b in BROWS:
            for m in MOODS:
                for side, sides in SIDES.items():
                    p[f"brows.{g}.{b}.{m}.{side}"] = enc(parts.brows(b, m, sides, g), "head")
    p["blush.0"] = enc(parts.blush(False), "head")
    p["blush.1"] = enc(parts.blush(True), "head")
    for f in ("freckles", "wrinkles", "beard", "mustache"):
        p[f"feature.{f}"] = enc(acc.features([f]), "head")
    for a in ACCS:
        b, fr = acc.accessory(a)
        region = "torso" if a == "cape" else "head"
        p[f"accBack.{a}"] = enc(b, region)
        p[f"accFront.{a}"] = enc(fr, region)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    s = json.dumps({"version": 2, "params": warp.PARAMS, "anchors": [a[0] for a in ANCHORS] + ["chin"], "parts": p},
                   separators=(",", ":"), ensure_ascii=False)
    open(OUT, "w", encoding="utf-8").write(s)
    print("parts", len(p), "bytes", len(s))


if __name__ == "__main__":
    main()
