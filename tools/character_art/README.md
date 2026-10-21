# Character art tools

Vector art for the illustrated character (200 x 545 box, top at y = -45).

- `parts.py`, `hair.py`, `clothes.py`, `acc.py`: the art (paths, colour roles, gradients).
- `render.py`: preview sheets through headless Chrome, e.g. `python render.py base|face|acc`.
- `export.py`: writes `app/src/main/assets/character_art.json`, which the app draws (CharacterArt.kt / PixelAvatar.kt).
