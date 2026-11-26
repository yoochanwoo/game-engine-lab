package engine.render;

import static org.lwjgl.opengl.GL33C.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL33C.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL33C.GL_FLOAT;
import static org.lwjgl.opengl.GL33C.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL33C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL33C.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL33C.glBindBuffer;
import static org.lwjgl.opengl.GL33C.glBindVertexArray;
import static org.lwjgl.opengl.GL33C.glBufferData;
import static org.lwjgl.opengl.GL33C.glDeleteBuffers;
import static org.lwjgl.opengl.GL33C.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL33C.glDrawElements;
import static org.lwjgl.opengl.GL33C.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL33C.glGenBuffers;
import static org.lwjgl.opengl.GL33C.glGenVertexArrays;
import static org.lwjgl.opengl.GL33C.glVertexAttribPointer;

/**
 * GPU에 올려 둔 삼각형 메시입니다.
 *
 * <p>정점 데이터는 자바 배열에만 있으면 GPU가 그리지 못합니다. 그래서 세 종류의
 * 버퍼 객체를 만듭니다.
 *
 * <ul>
 *   <li><b>VBO</b> (vertex buffer): 정점의 위치, 노멀, 텍스처 좌표.
 *   <li><b>EBO</b> (element buffer): 삼각형을 만들 정점 번호. 같은 정점을 여러 삼각형이 재사용합니다.
 *   <li><b>VAO</b> (vertex array): "0번은 위치, 1번은 노멀, 2번은 UV"라는 해석 방법.
 *       셰이더의 {@code layout(location = 0/1/2)}와 짝입니다.
 * </ul>
 *
 * <p>한 메시를 여러 {@link engine.scene.GameObject}가 공유할 수 있습니다.
 * 큐브를 다섯 개 그려도 GPU 버퍼는 하나면 됩니다. 나중에 모델 파일을 읽어도
 * 그 결과는 이 클래스의 인스턴스가 되고, 오브젝트는 가리키는 메시만 바꾸면 됩니다.
 * 지우는 책임은 메시를 만든 쪽(지금은 데모 게임)에 있습니다.
 */
public final class Mesh {

    /** 정점 하나: 위치 xyz + 노멀 xyz + 텍스처 좌표 uv. */
    public static final int FLOATS_PER_VERTEX = 8;

    private int vao;
    private int vbo;
    private int ebo;
    private final int indexCount;

    private Mesh(int vao, int vbo, int ebo, int indexCount) {
        this.vao = vao;
        this.vbo = vbo;
        this.ebo = ebo;
        this.indexCount = indexCount;
    }

    /**
     * 정점과 인덱스를 GPU에 올립니다. OpenGL 컨텍스트가 만들어진 뒤, 그 스레드에서 호출해야 합니다.
     *
     * @param vertices 위치 3개, 노멀 3개, UV 2개가 정점마다 반복되는 배열
     * @param indices 삼각형 3개씩의 정점 번호
     */
    public static Mesh upload(float[] vertices, int[] indices) {
        if (vertices.length == 0 || vertices.length % FLOATS_PER_VERTEX != 0) {
            throw new IllegalArgumentException("정점 배열 길이는 " + FLOATS_PER_VERTEX + "의 배수여야 합니다.");
        }
        if (indices.length == 0 || indices.length % 3 != 0) {
            throw new IllegalArgumentException("인덱스는 삼각형 단위(3의 배수)여야 합니다.");
        }

        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        int ebo = glGenBuffers();

        glBindVertexArray(vao);

        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

        // EBO를 VAO가 묶여 있는 동안 바인딩해야, VAO가 인덱스 버퍼를 기억합니다.
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

        int stride = FLOATS_PER_VERTEX * Float.BYTES;
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);

        return new Mesh(vao, vbo, ebo, indices.length);
    }

    public void draw() {
        glBindVertexArray(vao);
        glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0L);
        glBindVertexArray(0);
    }

    public void destroy() {
        if (vao == 0) {
            return;
        }
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        glDeleteBuffers(ebo);
        vao = 0;
        vbo = 0;
        ebo = 0;
    }
}
