package engine.render;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 파일 하나에서 온 모델입니다. 메시 여러 개와, 각 메시의 재질을 가집니다.
 *
 * <p>{@link engine.scene.GameObject}는 이 모델을 가리키기만 합니다. 같은 나무를
 * 두 그루 두려면 모델을 두 번 만들지 않고, 오브젝트 두 개가 같은 모델을 가리키면 됩니다.
 * 위치와 크기는 오브젝트의 {@link engine.scene.Transform}이 맡습니다.
 *
 * <p>{@link #minY()}는 모델 로컬 좌표에서 가장 낮은 점입니다. 바닥(y=0)에 붙이려면
 * 오브젝트 높이를 {@code -minY() * scale}로 두면 됩니다.
 */
public final class Model {

    private final List<Part> parts;
    private final float minY;
    private final float maxY;
    private final String source;

    Model(List<Part> parts, float minY, float maxY, String source) {
        this.parts = List.copyOf(parts);
        this.minY = minY;
        this.maxY = maxY;
        this.source = source;
    }

    /** 색만 있는 메시 하나입니다. 바닥과 상자처럼 코드로 만든 도형을 모델로 감쌀 때 씁니다. */
    public static Model colored(Mesh mesh, float red, float green, float blue, float minY, float maxY) {
        Material material = new Material(new org.joml.Vector3f(red, green, blue), null, false);
        return new Model(List.of(new Part(mesh, material)), minY, maxY, "primitive");
    }

    public List<Part> parts() {
        return parts;
    }

    public float minY() {
        return minY;
    }

    public float maxY() {
        return maxY;
    }

    public String source() {
        return source;
    }

    public void destroy() {
        Map<Texture, Boolean> textures = new IdentityHashMap<>();
        for (Part part : parts) {
            part.mesh.destroy();
            if (part.material.texture() != null) {
                textures.put(part.material.texture(), Boolean.TRUE);
            }
        }
        for (Texture texture : textures.keySet()) {
            texture.destroy();
        }
    }

    /** 모델 안의 한 덩어리. 메시 하나와 재질 하나입니다. */
    public static final class Part {
        private final Mesh mesh;
        private final Material material;

        Part(Mesh mesh, Material material) {
            this.mesh = mesh;
            this.material = material;
        }

        public Mesh mesh() {
            return mesh;
        }

        public Material material() {
            return material;
        }
    }
}
