package game;

import engine.core.Game;
import engine.core.Window;
import engine.input.Input;
import engine.input.Key;
import engine.render.Camera;
import engine.render.Mesh;
import engine.render.Model;
import engine.render.Models;
import engine.render.Primitives;
import engine.render.Renderer;
import engine.scene.FirstPersonController;
import engine.scene.GameObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 데모 씬입니다. 풀빛 바닥 위에 파일에서 읽은 나무, 바위, 벤치, 사람과
 * OBJ 상자 하나를 올려 두고, 눈높이의 1인칭 카메라로 걸어 다닙니다.
 *
 * <p>좌표의 1단위는 1미터로 둡니다. 모델의 가장 낮은 점이 바닥(y=0)에 닿도록
 * {@link Model#minY()}로 높이를 맞춥니다.
 *
 * <p>처음 서 있는 자리와 고개는 1단계에서 고정 카메라가 보던 방향과 같습니다.
 */
public final class DemoGame implements Game {

    /** 1단계 {@code lookAt(4.6, 1.7, 6.0 → 0, 0.55, 0.2)}과 같은 방향입니다. */
    private static final float START_X = 4.6f;
    private static final float START_Z = 6.0f;
    private static final float START_YAW_DEGREES = -38.4f;
    private static final float START_PITCH_DEGREES = -8.8f;

    private final boolean rehearseForScreenshot;

    private Window window;
    private Renderer renderer;
    private Camera camera;
    private FirstPersonController player;
    private Models models;
    private final List<GameObject> objects = new ArrayList<>();
    private final Matrix4f model = new Matrix4f();

    public DemoGame() {
        this(false);
    }

    /**
     * @param rehearseForScreenshot true면 시작하자마자 앞과 왼쪽으로 조금 걷고
     *     고개를 돌린 뒤의 모습을 첫 화면으로 둡니다. 입력이 없는 환경에서
     *     1인칭 이동을 확인할 때 씁니다.
     */
    public DemoGame(boolean rehearseForScreenshot) {
        this.rehearseForScreenshot = rehearseForScreenshot;
    }

    @Override
    public void init(Window window, Input input) {
        this.window = window;

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
        player = new FirstPersonController(camera, input, window);
        player.place(START_X, START_Z, START_YAW_DEGREES, START_PITCH_DEGREES);
        if (rehearseForScreenshot) {
            rehearsePlayer(input);
        }

        models = new Models();
        Mesh groundMesh = Primitives.ground();
        Model ground = models.store(
                "primitive/ground",
                Model.colored(groundMesh, 0.42f, 0.58f, 0.36f, 0f, 0f));
        // 같은 경로를 두 번 불러도 파일은 한 번만 읽고, 두 오브젝트가 그 모델을 공유합니다.
        Model tree = models.load("models/tree.glb");
        Model rock = models.load("models/rock.glb");
        Model bench = models.load("models/bench.glb");
        Model character = models.load("models/character.glb");
        Model crate = models.load("models/crate.obj");

        addGround(ground);
        place(tree, -2.1f, -0.4f, 15f, 1.7f);
        place(tree, -0.4f, -1.5f, -20f, 1.2f);
        place(rock, -2.55f, 1.15f, 25f, 1.6f);
        place(bench, -1.15f, 1.7f, 35f, 2.4f);
        // 미니 캐릭터는 나중에 NPC로 쓸 자리입니다. 파일 속 키는 0.67m라 키워서 세웁니다.
        place(character, 0.15f, 0.55f, 210f, 2.2f);
        place(crate, -0.55f, 2.35f, -20f, 0.55f);
    }

    @Override
    public void update(float deltaSeconds) {
        player.update(deltaSeconds);
    }

    @Override
    public void render() {
        renderer.begin(camera, window.aspectRatio());
        for (GameObject object : objects) {
            object.transform().writeModel(model);
            renderer.draw(object.model(), model, object.color());
        }
    }

    @Override
    public void destroy() {
        if (models != null) {
            models.destroy();
        }
        if (renderer != null) {
            renderer.destroy();
        }
    }

    /**
     * 키보드와 마우스가 없는 환경에서, 실제 컨트롤러에 입력을 넣어 걸음을 재현합니다.
     * 80프레임 × 1/60초 동안 W+A를 누른 채 마우스를 오른쪽·아래로 조금 움직입니다.
     */
    private void rehearsePlayer(Input input) {
        float fixedDelta = 1f / 60f;
        input.debugSetDown(Key.W, true);
        input.debugSetDown(Key.A, true);
        for (int frame = 0; frame < 80; frame++) {
            input.debugAddMouseDelta(2f, 1f);
            player.update(fixedDelta);
            input.endFrame();
        }
        input.debugSetDown(Key.W, false);
        input.debugSetDown(Key.A, false);
        System.out.println(String.format(
                Locale.ROOT,
                "리허설 카메라: (%.2f, %.2f, %.2f) yaw %.1f pitch %.1f",
                player.x(),
                player.y(),
                player.z(),
                player.yawDegrees(),
                player.pitchDegrees()));
    }

    private void addGround(Model ground) {
        GameObject object = new GameObject(ground);
        // ground 메시는 2x2입니다. 16x16미터 바닥이 되도록 가로와 세로만 8배로 키웁니다.
        object.transform().scale().set(8f, 1f, 8f);
        objects.add(object);
    }

    /** 모델의 밑면이 바닥(y=0)에 닿게 놓고, 세 축을 같은 배율로 키웁니다. */
    private void place(Model model, float x, float z, float yawDegrees, float scale) {
        GameObject object = new GameObject(model);
        object.transform().position().set(x, -model.minY() * scale, z);
        object.transform().rotation().set(0f, yawDegrees, 0f);
        object.transform().scale().set(scale, scale, scale);
        objects.add(object);
    }
}
