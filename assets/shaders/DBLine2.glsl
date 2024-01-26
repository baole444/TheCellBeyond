#type vertex
#version 410
layout (location = 0) in vec3 aPos;
layout (location = 1) in vec3 aColor;

out vec3 fColor;

uniform mat4 uView;
uniform mat4 uProject;

void main()
{
    fColor = aColor;

    gl_Position = uProject * uView * vec4(aPos, 1.0);
}

#type fragment
#version 410
in vec3 fColor;

out vec4 color;

void main()
{
    color = vec4(fColor, 1);
}