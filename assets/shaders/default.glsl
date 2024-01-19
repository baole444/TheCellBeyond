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

void main()
{
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

void main()
{
        if (fTexID > 0) {
                int id = int(fTexID);
                //vec4 texColor = texture(uTex[id], fTexCrd);

                //olor = vec4(texColor.rgb * fColor.a, fColor.a * texColor.a);
                // (1, 1, 1, 1) * (0.5, 0.5, 0.5, 0.5) = result color;
                color = fColor * texture(uTex[id], fTexCrd);
                //color = vec4(fTexCrd, 0, 1);
        } else {
                color = fColor;
        }


}
