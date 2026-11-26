#version 330 core

flat in vec3 vNormal;

uniform vec3 uColor;
// 빛이 나아가는 방향. 예를 들어 (0, -1, 0)이면 위에서 아래로 비춥니다.
uniform vec3 uLightDirection;
uniform vec3 uLightColor;
// 빛을 등진 면도 완전히 검게 만들지 않는 최소 밝기.
uniform vec3 uAmbientColor;

out vec4 fragColor;

void main() {
    vec3 normal = normalize(vNormal);
    // 램버트 확산광은 "면에서 광원 쪽을 향하는 방향"과의 각도를 사용합니다.
    vec3 towardLight = normalize(-uLightDirection);
    float diffuse = max(dot(normal, towardLight), 0.0);

    vec3 lighting = uAmbientColor + uLightColor * diffuse;
    fragColor = vec4(uColor * lighting, 1.0);
}
