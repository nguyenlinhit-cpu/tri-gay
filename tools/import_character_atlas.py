"""Split the generated 4x4 atlas into Android animation resources (requires Pillow)."""

from pathlib import Path
from collections import deque

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
ART = ROOT / "design/characters"
DEST = ROOT / "app/src/main/res/drawable"
SIZE = 256


def sprite_bounds(atlas):
    """Find isolated figures; generated atlases need not use pixel-exact cells."""
    width, height = atlas.size
    mask = bytearray(atlas.getchannel("A").point(lambda a: 255 if a > 32 else 0).tobytes())
    components = []
    for start in range(len(mask)):
        if not mask[start]:
            continue
        mask[start] = 0
        queue = deque([start])
        count = 0
        left = right = start % width
        top = bottom = start // width
        while queue:
            index = queue.popleft()
            x, y = index % width, index // width
            count += 1
            left, right = min(left, x), max(right, x)
            top, bottom = min(top, y), max(bottom, y)
            neighbors = []
            if x: neighbors.append(index - 1)
            if x + 1 < width: neighbors.append(index + 1)
            if y: neighbors.append(index - width)
            if y + 1 < height: neighbors.append(index + width)
            for neighbor in neighbors:
                if mask[neighbor]:
                    mask[neighbor] = 0
                    queue.append(neighbor)
        if count > 1000:
            components.append((count, (max(0, left - 2), max(0, top - 2),
                                       min(width, right + 3), min(height, bottom + 3))))
    assert len(components) == 16, f"Expected 16 isolated figures, found {len(components)}"
    bounds = sorted([b for _, b in components], key=lambda b: (b[1] + b[3]) / 2)
    return [sorted(bounds[row * 4:row * 4 + 4], key=lambda b: b[0]) for row in range(4)]


def main():
    atlas = Image.open(ART / "character_atlas.png").convert("RGBA")
    assert atlas.getchannel("A").getextrema()[0] == 0, "Atlas must be transparent"
    bounds = sprite_bounds(atlas)
    names = [
        [f"{hero}_{pose}" for pose in ("idle", "walk_a", "walk_b", "jump")]
        for hero in ("mario", "pink", "green")
    ] + [["goomba_idle", "goomba_walk_a", "goomba_walk_b", "boss_turtle"]]
    sprites = {}
    for row in range(4):
        crops = []
        for col in range(4):
            crops.append(atlas.crop(bounds[row][col]))
        # One scale per character: running frames keep the same body proportions.
        same_character = crops if row < 3 else crops[:3]
        hero_scale = min((SIZE - 12) / max(c.width for c in same_character),
                         (SIZE - 12) / max(c.height for c in same_character))
        for col, crop in enumerate(crops):
            scale = hero_scale if row < 3 or col < 3 else min(
                (SIZE - 12) / crop.width, (SIZE - 12) / crop.height)
            sprite = crop.resize((round(crop.width * scale), round(crop.height * scale)),
                                 Image.Resampling.LANCZOS)
            frame = Image.new("RGBA", (SIZE, SIZE))
            frame.alpha_composite(sprite, ((SIZE - sprite.width) // 2, SIZE - 4 - sprite.height))
            name = names[row][col]
            frame.save(DEST / f"{name}.png", optimize=True)
            sprites[name] = frame

    # The renderer already squashes this frame into a 40%-height destination.
    sprites["goomba_idle"].save(DEST / "goomba_flat.png", optimize=True)

    preview = Image.new("RGB", (4 * SIZE, 4 * (SIZE + 28)), "#d9eff4")
    draw = ImageDraw.Draw(preview)
    for row, row_names in enumerate(names):
        for col, name in enumerate(row_names):
            x, y = col * SIZE, row * (SIZE + 28)
            preview.paste(sprites[name], (x, y), sprites[name])
            draw.text((x + 12, y + SIZE + 5), name, fill="#173047")
    preview.save(ART / "preview.png", optimize=True)
    print(f"Imported {len(sprites) + 1} transparent character sprites into {DEST}")


if __name__ == "__main__":
    main()
