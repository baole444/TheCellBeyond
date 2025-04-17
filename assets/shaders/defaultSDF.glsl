#type vertex
#version 330 core

layout(location=0) in vec2 aPos;
layout(location=1) in vec3 aColor;
layout(location=2) in vec2 aTexCrd;

out vec2 fTexCrd;
out vec3 fColor;

uniform mat4 uProject;

void main() {
    fTexCrd = aTexCrd;
    fColor = aColor;
    gl_Position = uProjection * vec4(aPos, -5, 1);
}

#type fragment
#version 330 core

in vec2 fTexCrd;
in vec3 fColor;

uniform sampler2D uFontTex;

out vec4 color;

void main() {
    float upperPointCutOff = 0.5;
    float midPointCutOff = 0.49;
    float c = texture(uFontTex, fTexCrd).r;
    if (c > upperPointCutOff)
        color = vec4(1, 1, 1, 1) * vec4(fColor, 1);
    else if (c > midPointCutOff) {
        float smoothC = smoothstep(midPointCutOff, upperPointCutOff, c);
        color = vec4(1, 1, 1, smoothC) * vec4(fColor, 1);
    }
    else
        color = vec4(0, 0, 0, 0);
}
