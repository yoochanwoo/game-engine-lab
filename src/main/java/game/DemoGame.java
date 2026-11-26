package game;

import engine.core.Game;
import engine.core.Window;
import engine.render.Camera;
import engine.render.Mesh;
import engine.render.Primitives;
import engine.render.Renderer;
import engine.scene.GameObject;
import java.util.ArrayList;
import java.util.List;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 1단계 데모 씬입니다. 풀빛 바닥 위에 색이 다른 상자 몇 개를 올려 두고,
 * 눈높이의 고정 카메라로 바라봅니다.
 *
 * <p>좌표의 1단위는 1미터로 둡니다. 카메라 높이가 1.7인 이유입니다.
 * 큐브 메시는 중심이 원점이라, 바닥에 붙이려면 y를 높이의 절반으로 둡니다.
 */
public final class DemoGame implements Game {

    private Window window;
    private Renderer renderer;
    private Camera camera;
    private Mesh cubeMesh;
    private Mesh groundMesh;
    private final List<GameObject> objects = new ArrayList<>();
    private final Matrix4f model = new Matrix4f();

    @Override
    public void init(Window window) {
        this.window = window;

        cubeMesh = Primitives.cube();
        groundMesh = Primitives.ground();

        renderer = new Renderer();
        renderer.init();
        renderer.setClearColor(0.62f, 0.78f, 0.92f);
        // 빛은 카메라가 있는 오른쪽 위에서 왼쪽 아래로 내려옵니다. 면마다 밝기가 달라집니다.
        renderer.setDirectionalLight(
                new Vector3f(-0.45f, -1.0f, -0.35f),
                new Vector3f(1.0f, 0.97f, 0.90f),
                new Vector3f(0.30f, 0.32f, 0.36f));

        camera = new Camera();
        camera.setFieldOfViewDegrees(55f);
        // 눈높이는 1.7m. 상자들이 모인 쪽을 살짝 내려다봅니다.
        camera.lookAt(4.6f, 1.7f, 6.0f, 0.0f, 0.55f, 0.2f);

        addGround();
        // 가운데 산호색 상자
        addCube(0.0f, 0.0f, 1.1f, 1.1f, 1.1f, 15f, 0.82f, 0.34f, 0.30f);
        // 오른쪽 키 큰 파란 상자. 옆면이 보이도록 y축으로 조금 돌립니다.
        addCube(1.9f, -1.2f, 0.85f, 2.3f, 0.85f, -18f, 0.28f, 0.48f, 0.76f);
        // 왼쪽 노란 상자
        addCube(-1.7f, 0.8f, 0.9f, 0.9f, 0.9f, 24f, 0.90f, 0.74f, 0.28f);
        // 왼쪽 앞의 녹색 상자. 다른 상자와 화면에서 겹치지 않게 카메라 쪽으로 둡니다.
        addCube(-2.6f, 2.3f, 0.8f, 0.8f, 0.8f, 18f, 0.32f, 0.62f, 0.40f);
        // 카메라 가까운 쪽의 낮은 주황 상자
        addCube(1.15f, 1.7f, 0.7f, 0.55f, 0.7f, 32f, 0.86f, 0.48f, 0.24f);
    }

    @Override
    public void update(float deltaSeconds) {
        // 1단계에서는 아무것도 움직이지 않습니다.
        // 2단계에서 이 deltaSeconds로 1인칭 카메라를 이동합니다.
    }

    @Override
    public void render() {
        renderer.begin(camera, window.aspectRatio());
        for (GameObject object : objects) {
            object.transform().writeModel(model);
            renderer.draw(object.mesh(), model, object.color());
        }
    }

    @Override
    public void destroy() {
        if (renderer != null) {
            renderer.destroy();
        }
        // 메시는 오브젝트 여러 개가 공유하므로, 오브젝트가 아니라 여기서 한 번만 지웁니다.
        if (cubeMesh != null) {
            cubeMesh.destroy();
        }
        if (groundMesh != null) {
            groundMesh.destroy();
        }
    }

    private void addGround() {
        GameObject ground = new GameObject(groundMesh);
        // ground 메시는 2x2입니다. 16x16미터 바닥이 되도록 8배로 키웁니다.
        ground.transform().scale().set(8f, 1f, 8f);
        ground.color().set(0.42f, 0.58f, 0.36f);
        objects.add(ground);
    }

    /**
     * 바닥에 붙어 있는 상자를 추가합니다. {@code height}의 절반이 중심 높이가 됩니다.
     * 회전은 y축(yaw)만 사용합니다. x축으로 돌리면 모서리가 바닥을 파고듭니다.
     */
    private void addCube(
            float x,
            float z,
            float width,
            float height,
            float depth,
            float yawDegrees,
            float red,
            float green,
            float blue) {
        GameObject box = new GameObject(cubeMesh);
        // 바닥과 아랫면이 같은 높이에 있으면 경계에서 깊이 싸움이 납니다. 1mm만 띄웁니다.
        box.transform().position().set(x, height * 0.5f + 0.001f, z);
        box.transform().rotation().set(0f, yawDegrees, 0f);
        box.transform().scale().set(width, height, depth);
        box.color().set(red, green, blue);
        objects.add(box);
    }
}
