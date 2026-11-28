#version 330 core

layout (location = 0) in vec2 aPosition;
layout (location = 1) in vec2 aTexCoord;

uniform vec2 uScreen;

out vec2 vTexCoord;

void main() {
    vec2 clip = vec2(aPosition.x / uScreen.x * 2.0 - 1.0, 1.0 - aPosition.y / uScreen.y * 2.0);
    gl_Position = vec4(clip, 0.0, 1.0);
    vTexCoord = aTexCoord;
}
