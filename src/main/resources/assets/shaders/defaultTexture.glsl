#type vertex
#version 330 core
layout (location=0) in vec3 aPos;
layout (location=1) in vec4 aColor;
layout (location=2) in vec2 aTexCrd;
layout (location=3) in float aTexID;
uniform mat4 uProject;
uniform mat4 uView;
out vec4 fColor;
out vec2 fTexCrd;
out float fTexID;

void main() {
    fColor = aColor;
    fTexCrd = aTexCrd;
    fTexID = aTexID;
    gl_Position =uProject * uView * vec4(aPos, 1.0);
}

#type fragment
#version 330 core

in vec4 fColor;
in vec2 fTexCrd;
in float fTexID;
uniform sampler2D uTex[8];
out vec4 color;

void main() {
    if (fTexID > 0) {
        int id = int(fTexID);
        vec4 texColor;
        switch (id) {
            case 1: texColor = texture(uTex[1], fTexCrd); break;
            case 2: texColor = texture(uTex[2], fTexCrd); break;
            case 3: texColor = texture(uTex[3], fTexCrd); break;
            case 4: texColor = texture(uTex[4], fTexCrd); break;
            case 5: texColor = texture(uTex[5], fTexCrd); break;
            case 6: texColor = texture(uTex[6], fTexCrd); break;
            case 7: texColor = texture(uTex[7], fTexCrd); break;
            default: texColor = texture(uTex[0], fTexCrd); break;
        }
        color = fColor * texColor;
    } else {
        color = fColor;
    }
}
