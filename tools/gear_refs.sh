#!/bin/sh
# PixelLab pixel-art references for the gear made from the Tide Mother and the monsters' drops.
# Usage: PIXELLAB_KEY_FILE=<key file> sh tools/gear_refs.sh
OUT=references/gear
mkdir -p $OUT
STYLE="minecraft item icon, pixel art, dark muted colours, chinese folk horror, plain transparent background"
ref() { python tools/pixellab_ref.py "$OUT/ref_$1.png" 64 64 "$2, $STYLE"; }
ref tide_staff "a tall staff of pale grey bone wrapped in wet black hair, topped by a cage of curled tentacles holding a glowing red eye, dripping sea water"
ref water_sword "a translucent blade made of swirling sea water, a flying sword of water, teal and white foam"
ref tide_veil "a ragged dark green-grey veil of wet seaweed and barnacle-studded cloth, a hood that hides the face"
ref coin_sword "a chinese taoist exorcism sword made of old green bronze copper coins tied together with red string, red tassel"
ref talisman "a yellow paper taoist talisman with red ink seal script, slightly burnt at one corner"
ref salt_pouch "a small tied cloth pouch of coarse white sea salt, spilling grains"
ref scale_chestplate "a crude armour vest of overlapping grey-green fish scales stitched on pale leather"
ref soul_lantern "a pale paper lantern of stretched pale skin, a small cold blue flame inside, human teeth around the rim"
ref tide_glass "an old brass spyglass with a cloudy murky fish eye set as the lens"
ref leech_jar "a fat black leech, glistening, curled"
ref salt_crust "a block texture of cracked white sea salt crust on grey wet mud, top view, seamless square tile"
ref tide_bone "a block texture of huge ancient pale bone, porous, with barnacles and green algae, seamless square tile"
