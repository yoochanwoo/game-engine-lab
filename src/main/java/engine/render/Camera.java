package engine.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 고정된 원근 카메라입니다.
 *
 * <p>두 개의 행렬을 만듭니다.
 *
 * <ul>
 *   <li><b>view</b>: 세계를 카메라 기준으로 옮깁니다. "눈이 어디 있고, 어디를 보는가".
 *   <li><b>projection</b>: 그 공간을 화면에 펴 붙입니다. 시야각과 창 비율, 가까운/먼 클리핑.
 * </ul>
 *
 * <p>물체 자체의 위치와 회전은 여기 없고, {@link engine.scene.Transform}의 model 행렬입니다.
 * 2단계의 1인칭 카메라는 이 클래스의 눈 위치와 바라보는 점만 매 프레임 바꾸면 됩니다.
 * 지금 데모는 {@link #lookAt}을 시작할 때 한 번만 호출합니다.
 */
public final class Camera {

    private static final float NEAR_PLANE = 0.1f;
    private static final float FAR_PLANE = 100f;
    private static final Vector3f WORLD_UP = new Vector3f(0f, 1f, 0f);

    private final Vector3f position = new Vector3f();
    private final Vector3f target = new Vector3f();
    private final Matrix4f view = new Matrix4f();
    private final Matrix4f projection = new Matrix4f();
    private float fovRadians = (float) Math.toRadians(60.0);

    /** 눈 위치와 바라보는 점을 정하고 view 행렬을 다시 만듭니다. */
    public void lookAt(float eyeX, float eyeY, float eyeZ, float targetX, float targetY, float targetZ) {
        position.set(eyeX, eyeY, eyeZ);
        target.set(targetX, targetY, targetZ);
        // setLookAt은 카메라를 위한 view 행렬입니다. lookAt(곱하기)과 다릅니다.
        view.setLookAt(position, target, WORLD_UP);
    }

    public void setFieldOfViewDegrees(float degrees) {
        this.fovRadians = (float) Math.toRadians(degrees);
    }

    /** 창 크기가 바뀌면 비율이 달라지므로, 프레임을 그리기 전에 다시 계산합니다. */
    public void updateProjection(float aspect) {
        float safeAspect = aspect <= 0f ? 1f : aspect;
        projection.setPerspective(fovRadians, safeAspect, NEAR_PLANE, FAR_PLANE);
    }

    public Matrix4f viewMatrix() {
        return view;
    }

    public Matrix4f projectionMatrix() {
        return projection;
    }
}
