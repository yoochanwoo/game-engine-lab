package engine.render;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 경로로 모델을 기억해 두는 곳입니다.
 *
 * <p>같은 경로를 다시 요청하면 파일을 다시 읽지 않고 이미 만든 {@link Model}을 돌려줍니다.
 * 그래서 나무 두 그루가 GPU 버퍼를 공유합니다. 다른 파일로 바꾸고 싶으면
 * {@code models.load("models/내모델.glb")}의 문자열만 바꾸면 됩니다.
 */
public final class Models {

    private final Map<String, Model> byPath = new HashMap<>();

    public Model load(String path) {
        Model cached = byPath.get(path);
        if (cached != null) {
            return cached;
        }
        Model loaded = ModelLoader.load(path);
        byPath.put(path, loaded);
        return loaded;
    }

    /** 파일에서 오지 않은 모델(바닥, 상자)도 끝날 때 같이 지우려고 등록합니다. */
    public Model store(String key, Model model) {
        byPath.put(key, model);
        return model;
    }

    public void destroy() {
        Set<Model> unique = new HashSet<>(byPath.values());
        for (Model model : unique) {
            model.destroy();
        }
        byPath.clear();
    }
}
