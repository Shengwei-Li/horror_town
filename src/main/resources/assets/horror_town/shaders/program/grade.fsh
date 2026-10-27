#version 150

// Fog Harbor colour grade: drains most of the colour out of the world, pushes what is left towards a damp
// grey-green, darkens the corners of the screen and adds a little film grain.

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Time;

in vec2 texCoord;

out vec4 fragColor;

const float SATURATION = 0.4;
const vec3 TINT = vec3(0.93, 1.0, 0.94);
const float CONTRAST = 0.15;
const float VIGNETTE = 0.45;
const float GRAIN = 0.035;

void main() {
    vec3 colour = texture(DiffuseSampler, texCoord).rgb;

    float luma = dot(colour, vec3(0.3, 0.59, 0.11));
    colour = mix(vec3(luma), colour, SATURATION) * TINT;
    colour = mix(colour, colour * colour * (3.0 - 2.0 * colour), CONTRAST);

    vec2 fromCentre = (texCoord - 0.5) * vec2(1.2, 1.0);
    float edge = smoothstep(0.25, 0.85, length(fromCentre));
    colour *= 1.0 - edge * VIGNETTE;

    float noise = fract(sin(dot(texCoord * OutSize + Time * 913.0, vec2(12.9898, 78.233))) * 43758.5453);
    colour += (noise - 0.5) * GRAIN;

    fragColor = vec4(colour, 1.0);
}
