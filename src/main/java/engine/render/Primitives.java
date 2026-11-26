package engine.render;

/**
 * 파일 없이 코드로 만드는 단순한 메시입니다.
 *
 * <p>면마다 노멀을 따로 넣습니다. 한 정점을 인접한 면이 공유하면 노멀이 하나가 되어
 * 모서리가 둥글게 보이므로, 큐브는 면마다 정점 4개를 따로 둡니다. 그래서 조명 아래
 * 면이 각져 보이는 플랫 셰이딩이 됩니다.
 *
 * <p>정점 순서는 바깥에서 볼 때 반시계입니다. {@link Renderer}가 뒷면을 컬링합니다.
 */
public final class Primitives {

    private Primitives() {
    }

    /** 중심이 원점이고 한 변의 길이가 1인 큐브. */
    public static Mesh cube() {
        FaceWriter writer = new FaceWriter(6);
        writer.face(0f, 0f, 1f,
                -0.5f, -0.5f, 0.5f,
                0.5f, -0.5f, 0.5f,
                0.5f, 0.5f, 0.5f,
                -0.5f, 0.5f, 0.5f);
        writer.face(0f, 0f, -1f,
                0.5f, -0.5f, -0.5f,
                -0.5f, -0.5f, -0.5f,
                -0.5f, 0.5f, -0.5f,
                0.5f, 0.5f, -0.5f);
        writer.face(0f, 1f, 0f,
                -0.5f, 0.5f, 0.5f,
                0.5f, 0.5f, 0.5f,
                0.5f, 0.5f, -0.5f,
                -0.5f, 0.5f, -0.5f);
        writer.face(0f, -1f, 0f,
                -0.5f, -0.5f, -0.5f,
                0.5f, -0.5f, -0.5f,
                0.5f, -0.5f, 0.5f,
                -0.5f, -0.5f, 0.5f);
        writer.face(1f, 0f, 0f,
                0.5f, -0.5f, 0.5f,
                0.5f, -0.5f, -0.5f,
                0.5f, 0.5f, -0.5f,
                0.5f, 0.5f, 0.5f);
        writer.face(-1f, 0f, 0f,
                -0.5f, -0.5f, -0.5f,
                -0.5f, -0.5f, 0.5f,
                -0.5f, 0.5f, 0.5f,
                -0.5f, 0.5f, -0.5f);
        return writer.build();
    }

    /**
     * XZ 평면 위의 사각형. 중심은 원점, 크기는 2x2, 노멀은 위(+Y)입니다.
     * 넓은 바닥이 필요하면 오브젝트의 scale로 키웁니다.
     */
    public static Mesh ground() {
        FaceWriter writer = new FaceWriter(1);
        writer.face(0f, 1f, 0f,
                -1f, 0f, 1f,
                1f, 0f, 1f,
                1f, 0f, -1f,
                -1f, 0f, -1f);
        return writer.build();
    }

    /**
     * 면 하나를 "정점 4개 + 그 면을 가리키는 노멀"로 버퍼에 쌓습니다.
     * 삼각형 두 개(0-1-2, 0-2-3)로 사각형을 만듭니다.
     */
    private static final class FaceWriter {
        private final float[] vertices;
        private final int[] indices;
        private int vertexCount;
        private int indexCount;

        FaceWriter(int faceCount) {
            vertices = new float[faceCount * 4 * Mesh.FLOATS_PER_VERTEX];
            indices = new int[faceCount * 6];
        }

        void face(float normalX, float normalY, float normalZ, float... xyz) {
            if (xyz.length != 12) {
                throw new IllegalArgumentException("한 면은 정점 4개, 좌표 12개여야 합니다.");
            }
            int baseVertex = vertexCount;
            for (int i = 0; i < 4; i++) {
                int offset = vertexCount * Mesh.FLOATS_PER_VERTEX;
                vertices[offset] = xyz[i * 3];
                vertices[offset + 1] = xyz[i * 3 + 1];
                vertices[offset + 2] = xyz[i * 3 + 2];
                vertices[offset + 3] = normalX;
                vertices[offset + 4] = normalY;
                vertices[offset + 5] = normalZ;
                // 코드로 만든 도형은 텍스처가 없습니다. UV는 0으로 둡니다.
                vertices[offset + 6] = 0f;
                vertices[offset + 7] = 0f;
                vertexCount++;
            }
            indices[indexCount++] = baseVertex;
            indices[indexCount++] = baseVertex + 1;
            indices[indexCount++] = baseVertex + 2;
            indices[indexCount++] = baseVertex;
            indices[indexCount++] = baseVertex + 2;
            indices[indexCount++] = baseVertex + 3;
        }

        Mesh build() {
            return Mesh.upload(vertices, indices);
        }
    }
}
