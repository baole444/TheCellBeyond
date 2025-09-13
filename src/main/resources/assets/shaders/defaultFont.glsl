#type vertex
#version 330 core
layout(location=0) in vec2 aPos;
layout(location=1) in vec4 aColor;
layout(location=2) in vec2 aTexCrd;

out vec4 fColor;
out vec2 fTexCrd;

uniform mat4 uProject;
uniform mat4 uView;

void main()
{
    fColor = aColor;
    fTexCrd = aTexCrd;
    gl_Position = uProject * uView *vec4(aPos, 1, 1);
}

#type fragment
#version 330 core

in vec4 fColor;
in vec2 fTexCrd;

uniform sampler2D uFontTex;
uniform float uPxRange = 4.0;

out vec4 color;

float median(float r, float g, float b) {
    return max(min(r, g), min(max(r, g), b));
}

float screenPxRange() {
    vec2 unitRange = vec2(uPxRange) / vec2(textureSize(uFontTex, 0));
    vec2 screenTexSize = vec2(1.0) / fwidth(fTexCrd);
    return max(0.5 * dot(unitRange, screenTexSize), 1.0);
}

void main()
{
    vec3 msd = texture(uFontTex, fTexCrd).rgb;
    float sd = median(msd.r, msd.g, msd.b);
    float screenPxDistance = screenPxRange() * (sd - 0.5);
    float alpha = clamp(screenPxDistance + 0.5, 0.0, 1.0);
    color = vec4(fColor.rgb, fColor.a * alpha);
}