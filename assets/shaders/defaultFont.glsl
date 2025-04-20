#type vertex
#version 330 core
layout(location=0) in vec2 aPos;
layout(location=1) in vec4 aColor;
layout(location=2) in vec2 aTexCrd;

out vec4 fColor;
out vec2 fTexCrd;

uniform mat4 uProject;

void main()
{
    fColor = aColor;
    fTexCrd = aTexCrd;
    gl_Position = uProject * vec4(aPos, 0.0, 1);
}

#type fragment
#version 330 core

in vec4 fColor;
in vec2 fTexCrd;

uniform sampler2D uFontTex;

out vec4 color;

void main()
{
    float alpha = texture(uFontTex, fTexCrd).r;
    color = vec4(1, 1, 1, fColor.a * alpha);
}