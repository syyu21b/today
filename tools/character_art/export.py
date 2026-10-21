"""Exports every part variant to app/src/main/assets/character_art.json."""
import json, os
import parts, hair, clothes, acc
from geo import L, CX, smooth

OUT = r"C:\Users\SBS\Desktop\유신영\board-project-with-images\today\app\src\main\assets\character_art.json"
BODIES = {"SLIM": "F", "BROAD": "M"}
MOODS = ["normal", "happy", "sleepy", "sad", "excited", "calm", "love", "angry", "sick"]
HAIRS = ["short", "long", "bob", "ponytail", "twintail", "bun", "spiky", "curly", "buzz"]
EYES = ["almond", "round", "sharp", "gentle"]
BROWS = ["natural", "arched", "straight", "thick"]
FACES = ["oval", "round", "sharp", "square"]
TOPS = [("tee", "short"), ("tee", "long"), ("knit", "long"), ("hoodie", "long"), ("jacket", "long"), ("dress", "long")]
ACCS = ["cap", "glasses", "sunglasses", "headphones", "cape", "crown", "catears", "halo", "partyhat", "flowercrown", "strawhat", "witchhat", "santahat"]


def enc(layers):
    out = []
    for l in layers:
        if l.fill is None and l.stroke is None:
            continue
        if l.alpha <= 0:
            continue
        out.append([l.d, l.fill or "", l.stroke or "", round(l.width, 2), round(l.alpha, 2), l.clip or ""])
    return out


def stripes(bt, kind, sleeve):
    """Horizontal bands clipped to the shirt body."""
    body = [l for l in clothes.top(bt, kind, sleeve) if l.fill and l.fill.startswith("lin:topL:top:100:138")]
    if not body:
        return []
    clip = body[0].d
    out = []
    for y in range(160, 262, 14):
        out.append(L(smooth([(CX - 60, y), (CX + 60, y), (CX + 60, y + 6), (CX - 60, y + 6)], t=0.1), "stripe", clip=clip))
    return out


def main():
    p = {}
    for h in HAIRS:
        b, f = hair.style(h)
        p[f"hairBack.{h}"] = enc(b)
        p[f"hairFront.{h}"] = enc(f)
    for key, bt in BODIES.items():
        p[f"body.{key}"] = enc(parts.body(bt))
        p[f"arms.{key}"] = enc(parts.arms(bt))
        for k in ("pants", "shorts", "skirt"):
            p[f"bottom.{key}.{k}"] = enc(clothes.bottom(bt, k))
        for k in ("sneakers", "boots"):
            p[f"shoes.{key}.{k}"] = enc(clothes.shoes(bt, k))
        for kind, sl in TOPS:
            p[f"top.{key}.{kind}.{sl}"] = enc(clothes.top(bt, kind, sl))
            p[f"stripes.{key}.{kind}.{sl}"] = enc(stripes(bt, kind, sl))
        for e in EYES:
            for m in MOODS + ["blink"]:
                p[f"eyes.{key}.{e}.{m}"] = enc(parts.eyes(e, "sleepy" if m == "blink" else m, bt))
    for fs in FACES:
        p[f"head.{fs}"] = enc(parts.head(fs))
    p["blush.0"] = enc(parts.blush(False))
    p["blush.1"] = enc(parts.blush(True))
    p["nose"] = enc(parts.nose())
    for m in MOODS:
        p[f"mouth.{m}"] = enc(parts.mouth(m))
    for b in BROWS:
        for m in MOODS:
            p[f"brows.{b}.{m}"] = enc(parts.brows(b, m))
    for f in ("freckles", "wrinkles", "beard", "mustache"):
        p[f"feature.{f}"] = enc(acc.features([f]))
    for a in ACCS:
        b, f = acc.accessory(a)
        p[f"accBack.{a}"] = enc(b)
        p[f"accFront.{a}"] = enc(f)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    s = json.dumps({"version": 1, "parts": p}, separators=(",", ":"), ensure_ascii=False)
    open(OUT, "w", encoding="utf-8").write(s)
    print("parts", len(p), "bytes", len(s))


if __name__ == "__main__":
    main()
