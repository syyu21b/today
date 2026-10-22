"""Composes a character from options and renders preview sheets through headless Chrome."""
import os, subprocess, sys
import parts, hair, clothes, acc, warp
from geo import L

D = os.path.dirname(os.path.abspath(__file__))
CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"


def hexrgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t):
    return "#%02X%02X%02X" % tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def roles(o):
    sk, hr, ir = hexrgb(o["skin"]), hexrgb(o["hair"]), hexrgb(o["iris"])
    tp, bo, sh, ac, lp = hexrgb(o["top"]), hexrgb(o["bottom"]), hexrgb(o["shoes"]), hexrgb(o["acc"]), hexrgb(o.get("lip", "#E8707E"))
    K, W = (40, 22, 34), (255, 255, 255)
    return {
        "skin": o["skin"], "skinL": mix(sk, W, 0.18), "skinS": mix(sk, (196, 110, 110), 0.28), "skinD": mix(sk, (120, 60, 60), 0.5),
        "line": mix(sk, K, 0.65),
        "hair": o["hair"], "hairS": mix(hr, K, 0.35), "hairD": mix(hr, K, 0.6), "hairL": mix(hr, W, 0.35),
        "iris": o["iris"], "irisD": mix(ir, K, 0.6), "irisL": mix(ir, W, 0.55), "pupil": mix(ir, K, 0.85),
        "white": "#FFFFFF", "eyeLine": "#2A1A26", "lip": o.get("lip", "#E8707E"), "lipLine": mix(lp, K, 0.45), "blush": "#FF7A8C",
        "top": o["top"], "topS": mix(tp, K, 0.3), "topL": mix(tp, W, 0.15),
        "bottom": o["bottom"], "bottomS": mix(bo, K, 0.3),
        "shoes": o["shoes"], "shoesL": mix(sh, W, 0.25), "shoesS": mix(sh, K, 0.35), "sole": "#EDEDF2",
        "acc": o["acc"], "accS": mix(ac, K, 0.3),
    }


def tagged(layers_, region):
    for l in layers_:
        if l.region is None:
            l.region = region
    return layers_


def layers(o):
    """The character's layers in drawing order, warped by the base pose and o["tune"] (fine-tune sliders)."""
    bt = o.get("body", "F")
    back, front = hair.style(o.get("hairStyle", "long"))
    accs = [tuple(tagged(x, "torso" if a == "cape" else "head") for x in acc.accessory(a)) for a in o.get("acc_items", [])]
    mood = o.get("mood", "normal")
    out = []
    out += tagged(back, "head")
    for b, _ in accs:
        out += tagged(b, "head")
    out += tagged(parts.body(bt), "torso")
    out += tagged(clothes.bottom(bt, o.get("bottomKind", "pants")), "torso") if o.get("topKind") != "dress" else []
    out += tagged(clothes.shoes(bt, o.get("shoesKind", "sneakers")), "torso")
    out += tagged(parts.arms(bt), "torso")
    out += tagged(clothes.top(bt, o.get("topKind", "tee"), o.get("sleeve", "short")), "torso")
    out += tagged(parts.hands(bt), "torso")
    face = parts.head(o.get("face", "oval")) + parts.blush(o.get("blushStrong", False)) + parts.nose() + acc.features(o.get("features", []))         + parts.mouth(mood) + parts.eyes(o.get("eyes", "almond"), mood, bt) + front + parts.brows(o.get("brows", "natural"), mood)
    out += tagged(face, "head")
    for _, fr in accs:
        out += tagged(fr, "head")
    tune = o.get("tune", {})
    for l in out:
        l.d = warp.warp_path(l.d, l.region, bt, tune)
        if l.clip:
            l.clip = warp.warp_path(l.clip, l.region, bt, tune)
        l.fill = warp.warp_paint(l.fill, l.region, bt, tune)
    return out


def svg_body(o, gid):
    r = roles(o)
    defs, items = [], []
    for i, l in enumerate(layers(o)):
        fill = "none"
        if l.fill:
            if l.fill.startswith("lin:"):
                _, a, b, x1, y1, x2, y2 = l.fill.split(":")
                n = f"g{gid}_{i}"
                defs.append(f'<linearGradient id="{n}" gradientUnits="userSpaceOnUse" x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}"><stop offset="0" stop-color="{r.get(a, a)}"/><stop offset="1" stop-color="{r.get(b, b)}"/></linearGradient>')
                fill = f"url(#{n})"
            elif l.fill.startswith("rad:"):
                _, a, al, cx, cy, rr = l.fill.split(":")
                n = f"g{gid}_{i}"
                defs.append(f'<radialGradient id="{n}" gradientUnits="userSpaceOnUse" cx="{cx}" cy="{cy}" r="{rr}"><stop offset="0" stop-color="{r.get(a, a)}" stop-opacity="{al}"/><stop offset="1" stop-color="{r.get(a, a)}" stop-opacity="0"/></radialGradient>')
                fill = f"url(#{n})"
            else:
                fill = r.get(l.fill, l.fill)
        stroke = f' stroke="{r.get(l.stroke, l.stroke)}" stroke-width="{l.width}" stroke-linecap="round" stroke-linejoin="round"' if l.stroke else ""
        clip = ""
        if l.clip:
            cn = f"c{gid}_{i}"
            defs.append(f'<clipPath id="{cn}"><path d="{l.clip}"/></clipPath>')
            clip = f' clip-path="url(#{cn})"'
        items.append(f'<path d="{l.d}" fill="{fill}"{stroke} opacity="{l.alpha}"{clip}/>')
    return "".join(defs), "".join(items)


def sheet(configs, out, scale=2.0, cols=None, crop=None):
    cols = cols or len(configs)
    vw, vh = crop[2] if crop else 200, crop[3] if crop else 545
    ox, oy = (crop[0], crop[1]) if crop else (0, -45)
    W, H = int(vw * scale), int(vh * scale)
    rows = (len(configs) + cols - 1) // cols
    parts_svg = []
    for i, o in enumerate(configs):
        defs, items = svg_body(o, i)
        x, y = (i % cols) * W, (i // cols) * H
        parts_svg.append(f'<svg x="{x}" y="{y}" width="{W}" height="{H}" viewBox="{ox} {oy} {vw} {vh}"><defs>{defs}</defs>{items}</svg>')
    total = f'<svg xmlns="http://www.w3.org/2000/svg" width="{cols * W}" height="{rows * H}"><rect width="100%" height="100%" fill="#1B2236"/>{"".join(parts_svg)}</svg>'
    p = os.path.join(D, out.replace(".png", ".svg"))
    open(p, "w", encoding="utf-8").write(total)
    subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", f"--screenshot={os.path.join(D, out)}",
                    f"--window-size={cols * W},{rows * H}", "file:///" + p.replace("\\", "/")], capture_output=True)


BASE_F = dict(body="F", skin="#F6D5C3", hair="#3B2A3E", iris="#7A4FD0", top="#F4F1FA", bottom="#3A3F66", shoes="#2A2A36", acc="#8B6CFF",
              hairStyle="long", topKind="tee", bottomKind="skirt", eyes="almond")
BASE_M = dict(body="M", skin="#E9C2A6", hair="#2A2430", iris="#3E6FB8", top="#3E5C8A", bottom="#2B2B33", shoes="#E8E8EE", acc="#F2C14E",
              hairStyle="short", topKind="jacket", bottomKind="pants", eyes="sharp", brows="thick", lip="#C97A7A")

if __name__ == "__main__":
    what = sys.argv[1] if len(sys.argv) > 1 else "base"
    if what == "base":
        sheet([BASE_F, BASE_M], "base.png", scale=2)
    elif what == "acc":
        items = ["cap", "glasses", "sunglasses", "headphones", "cape", "crown", "catears", "halo", "partyhat", "flowercrown", "strawhat", "witchhat", "santahat"]
        sheet([dict(BASE_F, acc_items=[i]) for i in items], "accs.png", scale=2.2, crop=(30, -50, 140, 190), cols=7)
        sheet([dict(BASE_F, acc_items=["cape"]), dict(BASE_M, features=["beard", "wrinkles"], hair="#9E9EA8", acc_items=["strawhat"]),
               dict(BASE_M, features=["mustache"], hairStyle="curly"), dict(BASE_F, features=["freckles"], hairStyle="twintail", hair="#E8B45A")], "feat.png", scale=1.8, cols=4)
    elif what == "hair":
        hs = ["short", "long", "bob", "ponytail", "twintail", "bun", "spiky", "curly", "buzz"]
        sheet([dict(BASE_F if i % 2 == 0 else BASE_M, hairStyle=h, hair=["#7A5038", "#D9A84E", "#2E2A3A"][i % 3]) for i, h in enumerate(hs)], "hair.png", scale=2.4, crop=(20, -15, 160, 160), cols=5)
    elif what == "face":
        sheet([BASE_F, BASE_M], "face.png", scale=6, crop=(55, 0, 90, 130))
