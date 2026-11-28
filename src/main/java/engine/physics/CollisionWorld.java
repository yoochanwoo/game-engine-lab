package engine.physics;

import engine.scene.GameObject;
import java.util.ArrayList;
import java.util.List;

/** 정적 AABB. 수평은 원기둥, 수직은 발밑의 가장 높은 면. */
public final class CollisionWorld {

    private final List<Aabb> boxes = new ArrayList<>();

    public static CollisionWorld fromObjects(List<GameObject> objects) {
        CollisionWorld world = new CollisionWorld();
        for (GameObject object : objects) {
            world.boxes.add(Aabb.fromObject(object));
        }
        return world;
    }

    /** 몸통 높이와 겹치는 상자가 있으면 그 자리는 막힙니다. 단 높이는 올라갈 수 있습니다. */
    public boolean blocked(float x, float z, float feetY, float radius, float stepHeight) {
        float bodyTop = feetY + 1.6f;
        for (Aabb box : boxes) {
            if (box.maxY <= feetY + stepHeight) {
                continue;
            }
            if (box.minY >= bodyTop) {
                continue;
            }
            if (box.overlapsCircle(x, z, radius)) {
                return true;
            }
        }
        return false;
    }

    /** 설 수 있는 가장 높은 면. 없으면 아주 낮은 값. */
    public float groundY(float x, float z, float radius, float feetY, float stepHeight) {
        float best = Float.NEGATIVE_INFINITY;
        for (Aabb box : boxes) {
            if (box.maxY > feetY + stepHeight) {
                continue;
            }
            if (!box.overlapsCircle(x, z, radius)) {
                continue;
            }
            if (box.maxY > best) {
                best = box.maxY;
            }
        }
        return best;
    }
}
