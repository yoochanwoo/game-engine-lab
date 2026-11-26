package engine.render;

import org.joml.Vector3f;

/**
 * 한 표면의 색입니다. 기본 색에, 있을 때만 텍스처를 곱합니다.
 *
 * <p>나무 몸통과 잎처럼 한 모델 안에서도 부분마다 재질이 다를 수 있습니다.
 * 텍스처가 없는 재질은 {@code texture}가 null이고, 그릴 때 흰 텍스처로 대체합니다.
 */
public final class Material {

    private final Vector3f baseColor;
    private final Texture texture;
    private final boolean doubleSided;

    public Material(Vector3f baseColor, Texture texture, boolean doubleSided) {
        this.baseColor = baseColor;
        this.texture = texture;
        this.doubleSided = doubleSided;
    }

    /** 0에서 1 사이의 RGB. 텍스처가 있으면 그 색에 곱해집니다. */
    public Vector3f baseColor() {
        return baseColor;
    }

    public Texture texture() {
        return texture;
    }

    /** 나뭇잎 카드처럼 뒷면도 보여야 하면 true입니다. */
    public boolean doubleSided() {
        return doubleSided;
    }
}
