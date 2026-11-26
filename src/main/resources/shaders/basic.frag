#version 330 core

flat in vec3 vNormal;
in vec2 vTexCoord;

uniform vec3 uColor;
// 재질에 이미지가 없으면 렌더러가 흰 텍스처를 묶습니다. 그러면 색은 uColor 그대로입니다.
uniform sampler2D uBaseColorTexture;
// 빛이 나아가는 방향. 예를 들어 (0, -1, 0)이면 위에서 아래로 비춥니다.
uniform vec3 uLightDirection;
uniform vec3 uLightColor;
// 빛을 등진 면도 완전히 검게 만들지 않는 최소 밝기.
uniform vec3 uAmbientColor;

out vec4 fragColor;

void main() {
    vec4 texel = texture(uBaseColorTexture, vTexCoord);
    // 나뭇잎처럼 투명한 픽셀은 그리지 않습니다. 흰 텍스처의 알파는 1이라 영향이 없습니다.
    if (texel.a < 0.5) {
        discard;
    }

    vec3 normal = normalize(vNormal);
    // 램버트 확산광은 "면에서 광원 쪽을 향하는 방향"과의 각도를 사용합니다.
    vec3 towardLight = normalize(-uLightDirection);
    float diffuse = max(dot(normal, towardLight), 0.0);

    vec3 lighting = uAmbientColor + uLightColor * diffuse;
    vec3 albedo = uColor * texel.rgb;
    fragColor = vec4(albedo * lighting, 1.0);
}
