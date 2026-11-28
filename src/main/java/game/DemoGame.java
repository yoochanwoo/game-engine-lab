package game;

import engine.core.Game;
import engine.core.Window;
import engine.input.Input;
import engine.input.Key;
import engine.physics.CollisionWorld;
import engine.render.Camera;
import engine.render.Model;
import engine.render.Models;
import engine.render.Primitives;
import engine.render.Renderer;
import engine.scene.FirstPersonController;
import engine.scene.GameObject;
import engine.ui.Font;
import engine.ui.Ui;
import game.Dialogue.Npc;
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

    private static final float START_X = 0f;
    private static final float START_Z = 6.5f;
    private static final float START_YAW_DEGREES = 0f;
    private static final float START_PITCH_DEGREES = -12f;

    private final boolean rehearseForScreenshot;

    private Window window;
    private Input input;
    private Renderer renderer;
    private Camera camera;
    private FirstPersonController player;
    private Models models;
    private Font font;
    private Ui ui;
    private Dialogue dialogue;
    private final List<GameObject> objects = new ArrayList<>();
    private final List<Npc> npcs = new ArrayList<>();
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
        this.input = input;

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

        models = new Models();
        Model ground = models.store(
                "primitive/ground",
                Model.colored(Primitives.ground(), 0.46f, 0.62f, 0.38f, -1f, 0f, -1f, 1f, 0f, 1f));
        Model block = models.store(
                "primitive/block",
                Model.colored(Primitives.cube(), 1f, 1f, 1f, -0.5f, -0.5f, -0.5f, 0.5f, 0.5f, 0.5f));
        Model tree = models.load("models/tree.glb");
        Model rock = models.load("models/rock.glb");
        Model bench = models.load("models/bench.glb");
        Model character = models.load("models/character.glb");
        Model crate = models.load("models/crate.obj");

        addGround(ground);
        wall(block, 0f, -14f, 28f, 0.8f);
        wall(block, 0f, 14f, 28f, 0.8f);
        wall(block, -14f, 0f, 0.8f, 28f);
        wall(block, 14f, 0f, 0.8f, 28f);
        step(block, 3.4f, -1.0f, 2.6f, 1.5f, 0f);
        step(block, 3.4f, -2.4f, 2.4f, 1.5f, 0.28f);
        step(block, 3.4f, -4.3f, 3.4f, 2.6f, 0.56f);

        prop(tree, -3.2f, -2.2f, 10f, 1.8f);
        prop(tree, 0.4f, -6.2f, -15f, 1.5f);
        prop(tree, 6.2f, 1.4f, 30f, 1.6f);
        prop(tree, -6.5f, 2.5f, 0f, 1.4f);
        propOn(tree, 3.4f, -4.3f, 20f, 1.2f, 0.84f);
        prop(rock, -1.6f, 1.2f, 20f, 1.5f);
        prop(rock, 1.4f, -0.4f, -10f, 1.2f);
        prop(bench, -0.4f, -1.6f, 25f, 2.3f);
        prop(crate, 1.6f, 1.8f, -15f, 0.6f);

        font = new Font("/fonts/NanumGothic-Regular.ttf", 28);
        ui = new Ui();
        dialogue = new Dialogue(ui, font);
        spawnNpcs(character);

        player.setCollision(CollisionWorld.fromObjects(objects));
        player.place(START_X, START_Z, START_YAW_DEGREES, START_PITCH_DEGREES);
        if (rehearseForScreenshot) {
            rehearsePlayer(input);
            dialogue.debugOpen(npcs.get(0));
        }
    }

    private void spawnNpcs(Model character) {
        npcs.clear();
        npcs.add(placeNpc("mira", character, -2.2f, -1.4f));
        npcs.add(placeNpc("doyun", character, 2.6f, -2.2f));
        npcs.add(placeNpc("haneul", character, 5.4f, 2.2f));
    }

    private Npc placeNpc(String id, Model character, float x, float z) {
        GameObject object = new GameObject(character);
        object.transform().position().set(x, -character.minY() * 2.2f, z);
        object.transform().scale().set(2.2f, 2.2f, 2.2f);
        objects.add(object);
        return new Npc(id, dialogue.script(id), object);
    }

    @Override
    public void update(float deltaSeconds) {
        for (Npc npc : npcs) {
            npc.face(player.x(), player.z());
        }
        player.setCollision(CollisionWorld.fromObjects(objects));
        dialogue.update(
                deltaSeconds,
                input,
                player,
                npcs,
                window.framebufferWidth(),
                window.framebufferHeight(),
                scaleMouse(input.mouseX(), window.windowWidth(), window.framebufferWidth()),
                scaleMouse(input.mouseY(), window.windowHeight(), window.framebufferHeight()));
        player.update(deltaSeconds);
    }

    @Override
    public void render() {
        renderer.begin(camera, window.aspectRatio());
        for (GameObject object : objects) {
            object.transform().writeModel(model);
            renderer.draw(object.model(), model, object.color());
        }
        float mouseX = scaleMouse(input.mouseX(), window.windowWidth(), window.framebufferWidth());
        float mouseY = scaleMouse(input.mouseY(), window.windowHeight(), window.framebufferHeight());
        ui.begin(window.framebufferWidth(), window.framebufferHeight());
        dialogue.render(input, player, npcs, window.framebufferWidth(), window.framebufferHeight(), mouseX, mouseY);
        ui.end();
    }

    @Override
    public void destroy() {
        if (font != null) {
            font.destroy();
        }
        if (ui != null) {
            ui.destroy();
        }
        if (models != null) {
            models.destroy();
        }
        if (renderer != null) {
            renderer.destroy();
        }
    }

    private float scaleMouse(float windowCoord, int windowSize, int framebufferSize) {
        if (windowSize <= 0) {
            return windowCoord;
        }
        return windowCoord * framebufferSize / windowSize;
    }

    /**
     * 키보드와 마우스가 없는 환경에서, 실제 컨트롤러에 입력을 넣어 걸음을 재현합니다.
     * 80프레임 × 1/60초 동안 W+A를 누른 채 마우스를 오른쪽·아래로 조금 움직입니다.
     */
    private void rehearsePlayer(Input input) {
        float fixedDelta = 1f / 60f;
        input.debugSetDown(Key.W, true);
        for (int frame = 0; frame < 50; frame++) {
            input.debugAddMouseDelta(0.4f, 0.3f);
            player.update(fixedDelta);
            input.endFrame();
        }
        input.debugSetDown(Key.W, false);
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
        object.transform().scale().set(14f, 1f, 14f);
        objects.add(object);
    }

    private void prop(Model model, float x, float z, float yawDegrees, float scale) {
        propOn(model, x, z, yawDegrees, scale, 0f);
    }

    private void propOn(Model model, float x, float z, float yawDegrees, float scale, float floor) {
        GameObject object = new GameObject(model);
        object.transform().position().set(x, floor - model.minY() * scale, z);
        object.transform().rotation().set(0f, yawDegrees, 0f);
        object.transform().scale().set(scale, scale, scale);
        objects.add(object);
    }

    private void wall(Model block, float x, float z, float sx, float sz) {
        box(block, x, z, sx, 1.35f, sz, 0f, 0.34f, 0.36f, 0.30f);
    }

    private void step(Model block, float x, float z, float sx, float sz, float floor) {
        box(block, x, z, sx, 0.28f, sz, floor, 0.62f, 0.52f, 0.38f);
    }

    private void box(Model block, float x, float z, float sx, float sy, float sz, float floor, float r, float g, float b) {
        GameObject object = new GameObject(block);
        object.transform().scale().set(sx, sy, sz);
        object.transform().position().set(x, floor - block.minY() * sy, z);
        object.color().set(r, g, b);
        objects.add(object);
    }
}
