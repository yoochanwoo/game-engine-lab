#version 330 core

in vec2 vTexCoord;

uniform sampler2D uTexture;
uniform vec4 uColor;
uniform int uTextured;

out vec4 fragColor;

void main() {
    float alpha = uTextured == 1 ? texture(uTexture, vTexCoord).r : 1.0;
    fragColor = vec4(uColor.rgb, uColor.a * alpha);
}
