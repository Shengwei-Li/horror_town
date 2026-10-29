#!/bin/sh
# PixelLab pixel-art references for the transition belt and the island's interior.
# Usage: PIXELLAB_KEY_FILE=<key file> sh tools/inland_refs.sh
OUT=references/inland
mkdir -p $OUT
STYLE="pixel art, side view landscape, overcast grey fog, desaturated grey-green palette, chinese folk horror, eerie"
scene() { python tools/pixellab_ref.py "$OUT/ref_$1.png" 160 96 "$2, $STYLE"; }
scene brine_waste "cracked dry salt-crusted wasteland behind a foggy coast, dead reeds, an overturned rotting wooden fishing boat, a weathered stone stele with red carved characters"
scene barnacle_wood "a damp forest of oak and birch trees whose trunks are crusted with barnacles and dead coral, puddles, moss, mist between the trunks"
scene mildew_plains "abandoned wet farmland plains, mouldy rotting haystacks, stagnant pools, a ragged paper scarecrow, grey moss patches on grass"
scene sodden_taiga "dark dripping spruce forest, hanging grey lichen, podzol and moss ground, a rope hanging from a branch"
scene weeping_crags "tall grey stone crags with sea water seeping and trickling down the cliffs, barnacles and dead coral growing on the rock far from the sea"
python tools/pixellab_ref.py "$OUT/ref_barnacles.png" 64 64 "a cluster of grey-white barnacles on a flat surface, top view, pixel art, transparent background"
