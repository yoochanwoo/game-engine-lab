package engine.scene;

import engine.render.Mesh;
import org.joml.Vector3f;

/**
 * 씬에 놓인 물체 하나입니다. 메시, 변환, 색을 묶습니다.
 *
 * <p>메시는 형태만 가지고, 어디에 있는지는 변환이, 어떤 색인지는 이 오브젝트가 가집니다.
 * 같은 큐브 메시로 크기와 색이 다른 상자를 여러 개 만들 수 있습니다. 3단계에서
 * 모델 파일을 읽으면 {@link #mesh}가 가리키는 대상만 바꾸면 되고, 변환과 그리는
 * 경로는 그대로입니다.
 *
 * <p>색은 0에서 1 사이의 RGB입니다. 조명은 렌더러가 나중에 곱합니다.
 */
public final class GameObject {

    private Mesh mesh;
    private final Transform transform = new Transform();
    private final Vector3f color = new Vector3f(1f, 1f, 1f);

    public GameObject(Mesh mesh) {
        this.mesh = mesh;
    }

    public Mesh mesh() {
        return mesh;
    }

    /** 나중에 파일에서 읽은 모델로 큐브를 교체할 때 사용합니다. */
    public void setMesh(Mesh mesh) {
        this.mesh = mesh;
    }

    public Transform transform() {
        return transform;
    }

    public Vector3f color() {
        return color;
    }
}
