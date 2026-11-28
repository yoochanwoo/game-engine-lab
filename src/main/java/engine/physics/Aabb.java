package engine.physics;

import engine.render.Model;
import engine.scene.GameObject;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** 축에 정렬된 상자. 모델 로컬 경계를 월드 변환으로 감쌉니다. */
public final class Aabb {

    public float minX;
    public float minY;
    public float minZ;
    public float maxX;
    public float maxY;
    public float maxZ;

    public static Aabb fromObject(GameObject object) {
        Model model = object.model();
        Matrix4f matrix = object.transform().writeModel(new Matrix4f());
        Aabb box = new Aabb();
        box.minX = box.minY = box.minZ = Float.POSITIVE_INFINITY;
        box.maxX = box.maxY = box.maxZ = Float.NEGATIVE_INFINITY;
        float[] xs = {model.minX(), model.maxX()};
        float[] ys = {model.minY(), model.maxY()};
        float[] zs = {model.minZ(), model.maxZ()};
        Vector3f point = new Vector3f();
        for (float x : xs) {
            for (float y : ys) {
                for (float z : zs) {
                    matrix.transformPosition(point.set(x, y, z));
                    box.add(point.x, point.y, point.z);
                }
            }
        }
        return box;
    }

    public boolean overlapsCircle(float x, float z, float radius) {
        float closestX = Math.max(minX, Math.min(x, maxX));
        float closestZ = Math.max(minZ, Math.min(z, maxZ));
        float dx = x - closestX;
        float dz = z - closestZ;
        return dx * dx + dz * dz < radius * radius;
    }

    private void add(float x, float y, float z) {
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        minZ = Math.min(minZ, z);
        maxX = Math.max(maxX, x);
        maxY = Math.max(maxY, y);
        maxZ = Math.max(maxZ, z);
    }
}
