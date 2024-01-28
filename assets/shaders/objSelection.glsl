#type vertex
#version 330 core
layout (location=0) in vec3 aPos;
layout (location=1) in vec4 aColor;
layout (location=2) in vec2 aTexCrd;
layout (location=3) in float aTexID;
layout (location=4) in float aObjID;

uniform mat4 uProject;
uniform mat4 uView;

out vec4 fColor;
out vec2 fTexCrd;
out float fTexID;
out float fObjID;

void main()
{
    fColor = aColor;
    fTexCrd = aTexCrd;
    fTexID = aTexID;
    fObjID = aObjID;
    gl_Position =uProject * uView * vec4(aPos, 1.0);
}

#type fragment
#version 330 core


in vec4 fColor;
in vec2 fTexCrd;
in float fTexID;
in float fObjID;

uniform sampler2D uTex[8];

out vec3 color;

void main()
{
    vec4 texColor = vec4(1, 1, 1, 1);
    if (fTexID > 0) {
        int id = int(fTexID);

        texColor = fColor * texture(uTex[id], fTexCrd);
    }

    if (texColor.a < 0.5) {
        discard;
    }

    color = vec3(fObjID, fObjID, fObjID);


}
