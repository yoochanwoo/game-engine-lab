package engine.render;

import static org.lwjgl.opengl.GL33C.GL_BACK;
import static org.lwjgl.opengl.GL33C.GL_CCW;
import static org.lwjgl.opengl.GL33C.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL33C.GL_CULL_FACE;
import static org.lwjgl.opengl.GL33C.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL33C.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL33C.GL_MULTISAMPLE;
import static org.lwjgl.opengl.GL33C.glClear;
import static org.lwjgl.opengl.GL33C.glClearColor;
import static org.lwjgl.opengl.GL33C.glCullFace;
import static org.lwjgl.opengl.GL33C.glEnable;
import static org.lwjgl.opengl.GL33C.glFrontFace;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 화면을 지우고, 셰이더에 카메라와 빛을 넣은 뒤 메시를 그립니다.
 *
 * <p>메시의 형태와 오브젝트의 위치는 여기서 만들지 않습니다. 이미 만들어진 행렬과
 * 색을 유니폼으로 넘길 뿐입니다. 그래서 큐브든 나중에 읽어 들인 모델이든 그리는
 * 코드는 같습니다.
 */
public final class Renderer {

    private final Vector3f lightDirection = new Vector3f(-0.4f, -1f, -0.25f).normalize();
    private final Vector3f lightColor = new Vector3f(1f, 0.97f, 0.92f);
    private final Vector3f ambientColor = new Vector3f(0.28f, 0.30f, 0.34f);
    private float clearRed = 0.62f;
    private float clearGreen = 0.78f;
    private float clearBlue = 0.92f;

    private Shader shader;

    public void init() {
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW);
        glEnable(GL_MULTISAMPLE);
        shader = Shader.load("/shaders/basic.vert", "/shaders/basic.frag");
    }

    public void setClearColor(float red, float green, float blue) {
        this.clearRed = red;
        this.clearGreen = green;
        this.clearBlue = blue;
    }

    /**
     * 디렉셔널 라이트. {@code direction}은 빛이 나아가는 방향입니다.
     * 예를 들어 (0, -1, 0)이면 해가 바로 위에서 아래로 비춥니다.
     * 방향의 길이는 상관없습니다. 여기서 정규화합니다.
     */
    public void setDirectionalLight(Vector3f direction, Vector3f color, Vector3f ambient) {
        if (direction.lengthSquared() == 0f) {
            throw new IllegalArgumentException("빛의 방향은 0 벡터일 수 없습니다.");
        }
        lightDirection.set(direction).normalize();
        lightColor.set(color);
        ambientColor.set(ambient);
    }

    /**
     * 색과 깊이 버퍼를 지우고, 이 프레임 동안 유지되는 카메라와 빛 유니폼을 넣습니다.
     * {@code aspect}는 창의 프레임버퍼 비율이라, 크기를 바꾸면 다음 프레임의 투영이 따라갑니다.
     */
    public void begin(Camera camera, float aspect) {
        glClearColor(clearRed, clearGreen, clearBlue, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        camera.updateProjection(aspect);
        shader.use();
        shader.setMat4("uView", camera.viewMatrix());
        shader.setMat4("uProjection", camera.projectionMatrix());
        shader.setVec3("uLightDirection", lightDirection);
        shader.setVec3("uLightColor", lightColor);
        shader.setVec3("uAmbientColor", ambientColor);
    }

    public void draw(Mesh mesh, Matrix4f model, Vector3f color) {
        shader.setMat4("uModel", model);
        shader.setVec3("uColor", color);
        mesh.draw();
    }

    public void destroy() {
        if (shader != null) {
            shader.destroy();
            shader = null;
        }
    }
}
