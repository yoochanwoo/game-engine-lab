package engine.scene;

import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * 한 물체의 위치, 회전, 크기입니다.
 *
 * <p>메시 정점은 "자기 중심이 원점인 로컬 좌표"로 만들어져 있습니다. 이 클래스가
 * 그 로컬 좌표를 세계 좌표로 보내는 model 행렬을 만듭니다. 적용 순서는
 * 크기, 회전(Z, X, Y), 이동입니다. 회전 값은 도(degree)이고, 행렬을 만들 때만
 * 라디안으로 바꿉니다.
 *
 * <p>반환된 벡터를 {@code position().set(...)}처럼 직접 고치면 다음 프레임의
 * 행렬에 반영됩니다. 따로 커밋하는 단계는 없습니다.
 */
public final class Transform {

    private final Vector3f position = new Vector3f();
    private final Vector3f rotationDegrees = new Vector3f();
    private final Vector3f scale = new Vector3f(1f, 1f, 1f);

    public Vector3f position() {
        return position;
    }

    /** 오일러 각, 단위는 도. x가 pitch, y가 yaw, z가 roll입니다. */
    public Vector3f rotation() {
        return rotationDegrees;
    }

    public Vector3f scale() {
        return scale;
    }

    /** {@code dest}를 model 행렬로 덮어씁니다. 매 프레임 새 행렬을 만들지 않으려고 받습니다. */
    public Matrix4f writeModel(Matrix4f dest) {
        return dest.identity()
                .translate(position)
                .rotateY((float) Math.toRadians(rotationDegrees.y))
                .rotateX((float) Math.toRadians(rotationDegrees.x))
                .rotateZ((float) Math.toRadians(rotationDegrees.z))
                .scale(scale);
    }
}
