package engine.scene;

import engine.render.Model;
import org.joml.Vector3f;

/**
 * 씬에 놓인 물체 하나입니다. 모델, 변환, 색 배율을 묶습니다.
 *
 * <p>모델은 형태와 재질(기본 색, 텍스처)을 가지고, 어디에 있는지는 변환이 가집니다.
 * 같은 나무 모델을 여러 오브젝트가 가리킬 수 있습니다. 다른 파일로 바꾸려면
 * {@link #setModel}에 새 경로로 불러온 모델을 넣으면 됩니다.
 *
 * <p>{@link #color}는 재질 색에 곱하는 배율입니다. (1, 1, 1)이면 파일에 있는 색 그대로입니다.
 * 코드로 만든 흰 상자는 이 배율로 상자마다 다른 색을 냅니다.
 */
public final class GameObject {

    private Model model;
    private final Transform transform = new Transform();
    private final Vector3f color = new Vector3f(1f, 1f, 1f);

    public GameObject(Model model) {
        this.model = model;
    }

    public Model model() {
        return model;
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public Transform transform() {
        return transform;
    }

    public Vector3f color() {
        return color;
    }
}
