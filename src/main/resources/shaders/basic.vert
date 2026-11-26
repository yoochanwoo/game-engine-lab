#version 330 core

// 메시의 VAO location과 짝입니다. 0은 위치, 1은 노멀, 2는 텍스처 좌표.
layout (location = 0) in vec3 aPosition;
layout (location = 1) in vec3 aNormal;
layout (location = 2) in vec2 aTexCoord;

// model: 로컬 좌표를 세계로. view: 세계 좌표를 카메라로. projection: 카메라를 화면 클립 공간으로.
uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProjection;

// flat: 삼각형 안에서 노멀을 섞지 않습니다. 면마다 노멀이 같아서 모서리가 각져 보입니다.
// UV는 면 안에서 섞여야 텍스처가 펴집니다.
flat out vec3 vNormal;
out vec2 vTexCoord;

void main() {
    // 크기가 축마다 다르면 노멀도 기울어집니다. 역행렬의 전치로 바로잡습니다.
    // 물체가 몇 개 없어 셰이더에서 바로 계산합니다.
    vNormal = mat3(transpose(inverse(uModel))) * aNormal;
    vTexCoord = aTexCoord;

    vec4 worldPosition = uModel * vec4(aPosition, 1.0);
    gl_Position = uProjection * uView * worldPosition;
}
