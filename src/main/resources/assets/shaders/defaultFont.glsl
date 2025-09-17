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

void main()
{
    vec3 sample = texture(uFontTex, fTexCrd).rgb;
    ivec2 sz = textureSize(uFontTex, 0);
    float dx = dFdx(fTexCrd.x) * sz.x;
    float dy = dFdy(fTexCrd.y) * sz.y;
    float toPixels = 8.0f * inversesqrt(dx * dx + dy * dy);
    float sigDist = median(sample.r, sample.g, sample.b) - 0.5;
    float alpha = clamp(sigDist * toPixels + 0.5, 0.0, 1.0);
    color = vec4(fColor.rgb, fColor.a * alpha);
}