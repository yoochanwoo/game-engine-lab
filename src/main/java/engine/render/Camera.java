package engine.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 1인칭 원근 카메라입니다.
 *
 * <p>두 개의 행렬을 만듭니다.
 *
 * <ul>
 *   <li><b>view</b>: 눈의 위치와 고개 방향에서 만듭니다. 세계를 카메라 기준으로 옮깁니다.
 *   <li><b>projection</b>: 그 공간을 화면에 펴 붙입니다. 시야각, 창의 가로세로 비, 가까운/먼 클리핑.
 * </ul>
 *
 * <p>고개는 두 각도입니다. yaw는 수평 회전이고 0도면 -Z를 봅니다. pitch는 위아래이고
 * 0도면 수평입니다. pitch가 ±90도에 가까우면 바라보는 방향의 수평 성분이 0이 되어
 * view 행렬이 불안정해지므로 ±89도로 제한합니다.
 *
 * <p>물체 자체의 위치와 회전은 {@link engine.scene.Transform}의 model 행렬입니다.
 * 창 크기가 바뀌면 {@link #updateProjection(float)}에 새 비율을 넣으면 됩니다.
 * 렌더러가 매 프레임 창의 비율로 호출합니다.
 */
public final class Camera {

    private static final float NEAR_PLANE = 0.1f;
    private static final float FAR_PLANE = 100f;
    private static final float MAX_PITCH_DEGREES = 89f;
    private static final Vector3f WORLD_UP = new Vector3f(0f, 1f, 0f);

    private final Vector3f position = new Vector3f();
    private final Vector3f lookDirection = new Vector3f();
    private final Vector3f target = new Vector3f();
    private final Matrix4f view = new Matrix4f();
    private final Matrix4f projection = new Matrix4f();

    private float yawDegrees;
    private float pitchDegrees;
    private float fovRadians = (float) Math.toRadians(60.0);

    /** 눈 위치와 고개 각도를 정하고 view 행렬을 다시 만듭니다. 각도의 단위는 도입니다. */
    public void setPose(float x, float y, float z, float yawDegrees, float pitchDegrees) {
        position.set(x, y, z);
        this.yawDegrees = yawDegrees;
        this.pitchDegrees = clampPitch(pitchDegrees);
        rebuildView();
    }

    public void addYaw(float degrees) {
        yawDegrees += degrees;
        rebuildView();
    }

    public void addPitch(float degrees) {
        pitchDegrees = clampPitch(pitchDegrees + degrees);
        rebuildView();
    }

    /** 수평으로만 옮깁니다. 높이는 바꾸지 않습니다. */
    public void moveHorizontal(float dx, float dz) {
        position.x += dx;
        position.z += dz;
        rebuildView();
    }

    public void setHeight(float y) {
        position.y = y;
        rebuildView();
    }

    public void setFieldOfViewDegrees(float degrees) {
        this.fovRadians = (float) Math.toRadians(degrees);
    }

    /** 창 크기가 바뀌면 비율이 달라지므로, 프레임을 그리기 전에 다시 계산합니다. */
    public void updateProjection(float aspect) {
        float safeAspect = aspect <= 0f ? 1f : aspect;
        projection.setPerspective(fovRadians, safeAspect, NEAR_PLANE, FAR_PLANE);
    }

    public float x() {
        return position.x;
    }

    public float y() {
        return position.y;
    }

    public float z() {
        return position.z;
    }

    public float yawDegrees() {
        return yawDegrees;
    }

    public float pitchDegrees() {
        return pitchDegrees;
    }

    public Matrix4f viewMatrix() {
        return view;
    }

    public Matrix4f projectionMatrix() {
        return projection;
    }

    private void rebuildView() {
        float yawRadians = (float) Math.toRadians(yawDegrees);
        float pitchRadians = (float) Math.toRadians(pitchDegrees);
        float cosPitch = (float) Math.cos(pitchRadians);
        lookDirection.set(
                cosPitch * (float) Math.sin(yawRadians),
                (float) Math.sin(pitchRadians),
                cosPitch * (float) -Math.cos(yawRadians));
        target.set(position).add(lookDirection);
        // setLookAt은 카메라를 위한 view 행렬입니다. lookAt(곱하기)과 다릅니다.
        view.setLookAt(position, target, WORLD_UP);
    }

    private static float clampPitch(float degrees) {
        if (degrees > MAX_PITCH_DEGREES) {
            return MAX_PITCH_DEGREES;
        }
        if (degrees < -MAX_PITCH_DEGREES) {
            return -MAX_PITCH_DEGREES;
        }
        return degrees;
    }
}
