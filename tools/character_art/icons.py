"""Writes the launcher icons (adaptive foreground + themed monochrome) from the illustrated art.

One icon per preset outfit (see Outfits.presets in Models.kt and AppIconManager.kt).
`python icons.py` writes app/src/main/res/drawable/ic_launcher_foreground*.xml and
ic_launcher_monochrome*.xml, plus icons.png (a preview sheet) next to this script.
"""
import os, subprocess
import render, clothes
from render import roles, layers, CHROME, D

RES = os.path.join(D, "..", "..", "app", "src", "main", "res", "drawable")
# Head and shoulders of the 200 x 545 art, mapped onto the 108 x 108 adaptive canvas.
CROP_X, CROP_Y, CROP = 32.0, 10.0, 136.0
INSET = 16.0  # keeps the head inside the 66dp safe zone
SCALE = (108 - 2 * INSET) / CROP

PRESETS = {
    "default": dict(hair="#3B2A20", top="#6C4BF4", acc="#FFD34E", bottom="#2D3A5C", shoes="#1E1E28"),
    "street": dict(hair="#111111", top="#F2F2F2", acc="#E94B5A", bottom="#2B2B2B", shoes="#F2F2F2", stripe="#1B1B1B"),
    "ocean": dict(hair="#D9A441", top="#2E9BDA", acc="#B6F0FF", bottom="#E8DCC2", shoes="#35506B", sleeve="short"),
    "forest": dict(hair="#5A3B22", top="#3F8F4E", acc="#D8C27A", bottom="#6B4F35", shoes="#3A2A1C"),
    "sunset": dict(hair="#B4462E", top="#FF8C42", acc="#FFE066", bottom="#5B2A4E", shoes="#2A1A26", hairStyle="long", sleeve="short"),
    "neon": dict(hair="#FF4FD8", top="#1B1B2F", acc="#3DFFB4", bottom="#3D2EFF", shoes="#3DFFB4", stripe="#3DFFB4", hairStyle="long"),
}
BASE = dict(body="F", skin="#F5C9A3", iris="#6B4FD0", lip="#E8707E", hairStyle="short", topKind="tee", sleeve="long",
            bottomKind="pants", eyes="almond", mood="normal")

# Themed (monochrome) icons only use alpha: hair solid, face and clothes lighter, features cut darker.
MONO_ALPHA = {"hair": 1.0, "skin": 0.55, "top": 0.8, "line": 1.0, "eyeLine": 1.0, "white": 0.2}


def options(key):
    o = dict(BASE, **PRESETS[key])
    return o


def all_layers(o):
    out = layers(o)
    if "stripe" in o:
        # Same stripes the app draws, slotted in right after the shirt body.
        import warp
        st = clothes.stripes(o["body"], o["topKind"], o["sleeve"])
        for l in st:
            l.d = warp.warp_path(l.d, "torso", o["body"], {})
            l.clip = warp.warp_path(l.clip, "torso", o["body"], {})
        idx = max(i for i, l in enumerate(out) if l.fill and l.fill.startswith("lin:topL:top:100:")) + 1
        out[idx:idx] = st
    return out


def argb(hex_rgb, alpha=1.0):
    return "#%02X%s" % (round(alpha * 255), hex_rgb.lstrip("#").upper())


def gradient_xml(spec, r, alpha):
    kind = spec.split(":")[0]
    if kind == "lin":
        _, a, b, x1, y1, x2, y2 = spec.split(":")
        return (f'<gradient android:type="linear" android:startX="{x1}" android:startY="{y1}" android:endX="{x2}" android:endY="{y2}" '
                f'android:startColor="{argb(r.get(a, a), alpha)}" android:endColor="{argb(r.get(b, b), alpha)}"/>')
    _, a, al, cx, cy, rr = spec.split(":")
    return (f'<gradient android:type="radial" android:centerX="{cx}" android:centerY="{cy}" android:gradientRadius="{rr}" '
            f'android:startColor="{argb(r.get(a, a), alpha * float(al))}" android:endColor="{argb(r.get(a, a), 0)}"/>')


def role_of(name):
    for k in ("hair", "skin", "top", "white", "eyeLine", "line"):
        if name.startswith(k):
            return k
    return None


def path_xml(l, r, mono):
    attrs, inner = [f'android:pathData="{l.d}"'], ""
    if mono:
        def col(name):
            a = MONO_ALPHA.get(role_of(name.split(":")[1] if ":" in name else name), 0.9)
            return argb("#FFFFFF", a * l.alpha)
        if l.fill:
            attrs.append(f'android:fillColor="{col(l.fill)}"')
        if l.stroke:
            attrs.append(f'android:strokeColor="{col(l.stroke)}"')
    else:
        if l.fill and ":" in l.fill:
            inner = f'<aapt:attr name="android:fillColor">{gradient_xml(l.fill, r, l.alpha)}</aapt:attr>'
        elif l.fill:
            attrs.append(f'android:fillColor="{argb(r.get(l.fill, l.fill), l.alpha)}"')
        if l.stroke:
            attrs.append(f'android:strokeColor="{argb(r.get(l.stroke, l.stroke), l.alpha)}"')
    if l.stroke:
        attrs.append(f'android:strokeWidth="{round(l.width, 2)}" android:strokeLineCap="round" android:strokeLineJoin="round"')
    a = " ".join(attrs)
    p = f"<path {a}>{inner}</path>" if inner else f"<path {a}/>"
    if l.clip:
        return f'<group><clip-path android:pathData="{l.clip}"/>{p}</group>'
    return p


def vector(key, mono):
    o = options(key)
    r = dict(roles(o), stripe=o.get("stripe", o["acc"]))
    body = "\n        ".join(path_xml(l, r, mono) for l in all_layers(o)
                             if (l.fill or l.stroke) and l.alpha > 0 and not (mono and l.fill and l.fill.startswith("rad:")))
    what = "Themed icon silhouette" if mono else "Launcher face"
    tx, ty = INSET - CROP_X * SCALE, INSET - CROP_Y * SCALE
    return f"""<?xml version="1.0" encoding="utf-8"?>
<!-- {what}: {key} outfit (generated by tools/character_art/icons.py). -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp" android:height="108dp"
    android:viewportWidth="108" android:viewportHeight="108">
    <clip-path android:pathData="M0,0h108v108h-108z"/>
    <group android:translateX="{tx:.3f}" android:translateY="{ty:.3f}" android:scaleX="{SCALE:.5f}" android:scaleY="{SCALE:.5f}">
        {body}
    </group>
</vector>
"""


def preview():
    """Sheet of every icon on the gold background, inside the circular launcher mask."""
    cells = []
    S = 216
    for i, key in enumerate(PRESETS):
        o = options(key)
        defs, items = render.svg_body(o, i)
        x = i * (S + 16)
        cells.append(
            f'<g transform="translate({x},0)"><clipPath id="m{i}"><circle cx="{S / 2}" cy="{S / 2}" r="{S / 2 * 72 / 108}"/></clipPath>'
            f'<g clip-path="url(#m{i})"><rect width="{S}" height="{S}" fill="#FFD34E"/>'
            f'<svg width="{S}" height="{S}" viewBox="{CROP_X - INSET / SCALE} {CROP_Y - INSET / SCALE} {108 / SCALE} {108 / SCALE}"><defs>{defs}</defs>{items}</svg></g></g>')
    W = len(PRESETS) * (S + 16)
    svg = f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{S}"><rect width="100%" height="100%" fill="#1B2236"/>{"".join(cells)}</svg>'
    p = os.path.join(D, "icons.svg")
    open(p, "w", encoding="utf-8").write(svg)
    subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", f"--screenshot={os.path.join(D, 'icons.png')}",
                    f"--window-size={W},{S}", "file:///" + p.replace("\\", "/")], capture_output=True)


def main():
    for key in PRESETS:
        suffix = "" if key == "default" else "_" + key
        for mono, name in ((False, "ic_launcher_foreground"), (True, "ic_launcher_monochrome")):
            open(os.path.join(RES, f"{name}{suffix}.xml"), "w", encoding="utf-8", newline="\n").write(vector(key, mono))
    preview()
    print("icons", len(PRESETS))


if __name__ == "__main__":
    main()
