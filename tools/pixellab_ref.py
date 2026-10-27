"""Generates one PixelLab reference image. The API key is read from the file given in PIXELLAB_KEY_FILE.

Usage: python tools/pixellab_ref.py <out.png> <width> <height> "<description>"
"""
import base64
import json
import os
import sys
import urllib.error
import urllib.request

out, width, height, description = sys.argv[1], int(sys.argv[2]), int(sys.argv[3]), sys.argv[4]
key = open(os.environ["PIXELLAB_KEY_FILE"], encoding="utf-8").read().strip()
body = json.dumps({
    "description": description,
    "image_size": {"width": width, "height": height},
    "view": "side",
    "shading": "detailed shading",
    "detail": "highly detailed",
    "outline": "selective outline",
}).encode()
request = urllib.request.Request("https://api.pixellab.ai/v1/generate-image-pixflux", data=body, method="POST",
                                 headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"})
try:
    with urllib.request.urlopen(request, timeout=180) as response:
        data = json.load(response)
except urllib.error.HTTPError as e:
    sys.exit(f"HTTP {e.code}: {e.read().decode(errors='replace')[:500]}")
with open(out, "wb") as f:
    f.write(base64.b64decode(data["image"]["base64"].split(",")[-1]))
print("ok", out, data.get("usage"))
