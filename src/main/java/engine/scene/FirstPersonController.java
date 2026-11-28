package engine.scene;

import engine.core.Window;
import engine.input.Input;
import engine.input.Key;
import engine.physics.CollisionWorld;
import engine.render.Camera;

/**
 * 눈높이로 바닥을 걷는 1인칭 조작입니다.
 *
 * <p>마우스는 고개의 좌우(yaw)와 위아래(pitch)만 바꿉니다. WASD는 그 yaw가 가리키는
 * 수평 방향으로 움직이고, 보는 각도가 위든 아래든 높이는 바뀌지 않습니다.
 * 항상 {@link #EYE_HEIGHT}에 둡니다.
 *
 * <p>이동량은 속력(미터/초)에 프레임 시간(초)을 곱합니다. 그래서 프레임이 빠르든
 * 느리든 같은 거리를 갑니다. W와 D를 같이 눌러도 대각선이 더 빠르지 않도록,
 * 방향 벡터를 길이 1로 만든 뒤에 속력을 곱합니다.
 *
 * <p>커서가 잡혀 있을 때만 시점과 이동이 적용됩니다.
 * ESC는 커서를 풀고, 풀린 상태에서 한 번 더 누르면 창을 닫습니다.
 * 풀린 상태에서 왼쪽 클릭을 하면 커서를 다시 잡습니다.
 */
public final class FirstPersonController {

    /** 바닥에서의 눈높이, 미터. 1단계는 이 높이에서 씬을 바라봤습니다. */
    public static final float EYE_HEIGHT = 1.7f;

    private static final float WALK_METERS_PER_SECOND = 4.0f;
    private static final float SPRINT_METERS_PER_SECOND = 7.5f;
    private static final float MOUSE_DEGREES_PER_PIXEL = 0.12f;
    private static final float RADIUS = 0.32f;
    private static final float STEP_HEIGHT = 0.45f;
    private static final float GRAVITY = 16f;
    private static final float JUMP_SPEED = 5.4f;

    private final Camera camera;
    private final Input input;
    private final Window window;
    private CollisionWorld world;
    private float feetY;
    private float velocityY;
    private boolean controlsEnabled = true;
    private boolean captureSuspended;

    public FirstPersonController(Camera camera, Input input, Window window) {
        this.camera = camera;
        this.input = input;
        this.window = window;
        System.out.println("WASD 이동, Shift 달리기, Space 점프. ESC로 커서를 풀고, 한 번 더 누르면 종료.");
    }

    public void setCollision(CollisionWorld world) {
        this.world = world;
    }

    /** 대화 중에는 이동만 멈춥니다. 시점과 ESC는 그대로입니다. */
    public void setControlsEnabled(boolean enabled) {
        this.controlsEnabled = enabled;
    }

    /** 선택지가 있을 때 클릭으로 고르도록 커서 재포획을 멈춥니다. */
    public void setCaptureSuspended(boolean suspended) {
        this.captureSuspended = suspended;
    }

    public void place(float x, float z, float yawDegrees, float pitchDegrees) {
        feetY = groundAt(x, z, 4f);
        velocityY = 0f;
        camera.setPose(x, feetY + EYE_HEIGHT, z, yawDegrees, pitchDegrees);
    }

    public void update(float deltaSeconds) {
        if (input.wasPressed(Key.ESCAPE)) {
            if (input.isCursorCaptured()) {
                input.releaseCursor();
            } else {
                window.requestClose();
            }
            return;
        }

        if (!input.isCursorCaptured()) {
            if (!captureSuspended && input.wasMouseLeftPressed()) {
                input.captureCursor();
            }
            if (!input.isCursorCaptured()) {
                return;
            }
        }

        // 화면에서 오른쪽은 yaw 증가, 위쪽은 pitch 증가입니다.
        // 마우스 y는 아래로 갈수록 커지므로 pitch에서는 부호를 뒤집습니다.
        camera.addYaw(input.mouseDeltaX() * MOUSE_DEGREES_PER_PIXEL);
        camera.addPitch(-input.mouseDeltaY() * MOUSE_DEGREES_PER_PIXEL);
        if (!controlsEnabled) {
            camera.setHeight(feetY + EYE_HEIGHT);
            return;
        }

        float yawRadians = (float) Math.toRadians(camera.yawDegrees());
        float forwardX = (float) Math.sin(yawRadians);
        float forwardZ = (float) -Math.cos(yawRadians);
        float rightX = (float) Math.cos(yawRadians);
        float rightZ = (float) Math.sin(yawRadians);

        float moveX = 0f;
        float moveZ = 0f;
        if (input.isDown(Key.W)) {
            moveX += forwardX;
            moveZ += forwardZ;
        }
        if (input.isDown(Key.S)) {
            moveX -= forwardX;
            moveZ -= forwardZ;
        }
        if (input.isDown(Key.D)) {
            moveX += rightX;
            moveZ += rightZ;
        }
        if (input.isDown(Key.A)) {
            moveX -= rightX;
            moveZ -= rightZ;
        }

        float lengthSquared = moveX * moveX + moveZ * moveZ;
        float dx = 0f;
        float dz = 0f;
        if (lengthSquared > 0f) {
            float speed = input.isDown(Key.SHIFT) ? SPRINT_METERS_PER_SECOND : WALK_METERS_PER_SECOND;
            float scale = speed * deltaSeconds / (float) Math.sqrt(lengthSquared);
            dx = moveX * scale;
            dz = moveZ * scale;
        }
        float x = camera.x();
        float z = camera.z();
        if (!blocked(x + dx, z)) {
            x += dx;
        }
        if (!blocked(x, z + dz)) {
            z += dz;
        }

        boolean grounded = Math.abs(feetY - groundAt(x, z, feetY)) < 0.05f && velocityY <= 0f;
        if (grounded && input.wasPressed(Key.SPACE)) {
            velocityY = JUMP_SPEED;
        }
        velocityY -= GRAVITY * deltaSeconds;
        feetY += velocityY * deltaSeconds;
        float ground = groundAt(x, z, feetY);
        if (velocityY <= 0f && feetY <= ground) {
            feetY = ground;
            velocityY = 0f;
        }
        camera.setPose(x, feetY + EYE_HEIGHT, z, camera.yawDegrees(), camera.pitchDegrees());
    }

    private boolean blocked(float x, float z) {
        return world != null && world.blocked(x, z, feetY, RADIUS, STEP_HEIGHT);
    }

    private float groundAt(float x, float z, float feet) {
        if (world == null) {
            return 0f;
        }
        float ground = world.groundY(x, z, RADIUS, feet, STEP_HEIGHT);
        return ground == Float.NEGATIVE_INFINITY ? feet - 2f : ground;
    }

    public float x() {
        return camera.x();
    }

    public float y() {
        return camera.y();
    }

    public float z() {
        return camera.z();
    }

    public float yawDegrees() {
        return camera.yawDegrees();
    }

    public float pitchDegrees() {
        return camera.pitchDegrees();
    }
}
