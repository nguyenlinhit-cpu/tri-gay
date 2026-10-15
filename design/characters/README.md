# Character art

Original beige, pink, and green explorer characters redrawn as soft-lit cartoon
sprites, alongside a mushroom enemy and turtle boss. Generated with the built-in
`image_gen` tool; the full generation prompt is in `prompt.txt`.

`character_atlas.png` is the original transparent 4 × 4 source. Columns in the
first three rows are idle, run A, run B, and jump. The last row contains mushroom
idle, run A, run B, and the turtle boss. `preview.png` shows the imported frames.

To reimport (Python with Pillow):

```sh
python3 tools/import_character_atlas.py
```

The importer preserves alpha, normalizes character scale and foot placement, and
writes 256 × 256 PNGs to `app/src/main/res/drawable`. The stomped enemy uses the
idle artwork compressed vertically by the existing game renderer. Resource names,
collision bounds, and gameplay timing stay compatible with the existing game.

Build and verify the installable APK with `nix run path:.` from the project root.
