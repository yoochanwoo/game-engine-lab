package engine.render;

import static org.lwjgl.assimp.Assimp.AI_MATKEY_BASE_COLOR;
import static org.lwjgl.assimp.Assimp.AI_MATKEY_COLOR_DIFFUSE;
import static org.lwjgl.assimp.Assimp.AI_MATKEY_TWOSIDED;
import static org.lwjgl.assimp.Assimp.aiGetErrorString;
import static org.lwjgl.assimp.Assimp.aiGetMaterialColor;
import static org.lwjgl.assimp.Assimp.aiGetMaterialIntegerArray;
import static org.lwjgl.assimp.Assimp.aiImportFile;
import static org.lwjgl.assimp.Assimp.aiProcess_SortByPType;
import static org.lwjgl.assimp.Assimp.aiProcess_Triangulate;
import static org.lwjgl.assimp.Assimp.aiReturn_SUCCESS;

import engine.render.Model.Part;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.AIColor4D;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMaterial;
import org.lwjgl.assimp.AIMatrix4x4;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.AITexture;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.system.MemoryStack;

/**
 * glTF(.glb)와 Wavefront(.obj)를 {@link Model}로 바꿉니다.
 *
 * <p>두 형식의 바이너리 구조를 직접 읽지 않고 Assimp에 맡깁니다. Assimp가 둘 다
 * "노드 트리 + 메시 + 재질"이라는 같은 모습으로 돌려주기 때문에, 이 클래스는
 * 그 트리를 따라가며 삼각형을 꺼내기만 하면 됩니다. glTF를 직접 파싱하면
 * 버퍼 뷰, 액세서, 인덱스 타입까지 우리가 다뤄야 해서 이 단계의 코드가 훨씬 길어집니다.
 *
 * <p>파일에 들어 있는 노멀은 쓰지 않습니다. 삼각형마다 모서리 둘의 외적으로
 * 면 노멀을 다시 만들고, 그 면의 정점은 다른 면과 공유하지 않습니다. 그래서
 * 모델이 매끄러운 노멀로 저장돼 있어도 로우폴리처럼 각져 보입니다.
 *
 * <p>노드의 이동·회전·크기는 정점에 구워 넣습니다. 애니메이션은 아직 없고,
 * 서 있는 모델 하나를 통째로 두는 단계입니다. OpenGL 컨텍스트가 있는 스레드에서 호출해야 합니다.
 */
final class ModelLoader {

    private static final int FLAGS = aiProcess_Triangulate | aiProcess_SortByPType;

    private ModelLoader() {
    }

    static Model load(String path) {
        ModelFiles.Source source = ModelFiles.open(path);
        AIScene scene = aiImportFile(source.modelFile.toString(), FLAGS);
        if (scene == null) {
            throw new IllegalStateException("모델을 읽지 못했습니다: " + path + " (" + aiGetErrorString() + ")");
        }
        try {
            if (scene.mRootNode() == null) {
                throw new IllegalStateException("모델에 씬이 없습니다: " + path);
            }
            List<Part> parts = new ArrayList<>();
            Bounds bounds = new Bounds();
            Map<String, Texture> textures = new HashMap<>();
            Map<Integer, String> texturePaths = new HashMap<>();
            texturePaths.putAll(GltfTextures.byMaterial(source.modelFile));
            texturePaths.putAll(ObjMaterials.textures(source.modelFile));
            collect(scene.mRootNode(), new Matrix4f(), scene, source, parts, bounds, textures, texturePaths);
            if (parts.isEmpty()) {
                throw new IllegalStateException("모델에 삼각형이 없습니다: " + path);
            }
            System.out.println(String.format(
                    Locale.ROOT,
                    "모델 %s: 부분 %d개, 로컬 y %.2f ~ %.2f",
                    path,
                    parts.size(),
                    bounds.minY,
                    bounds.maxY));
            return new Model(
                    parts, bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ, path);
        } finally {
            scene.close();
        }
    }

    private static void collect(
            AINode node,
            Matrix4f parentWorld,
            AIScene scene,
            ModelFiles.Source source,
            List<Part> parts,
            Bounds bounds,
            Map<String, Texture> textures,
            Map<Integer, String> texturePaths) {
        Matrix4f world = new Matrix4f(parentWorld).mul(toMatrix(node.mTransformation()));
        IntBuffer meshIds = node.mMeshes();
        if (meshIds != null) {
            for (int i = 0; i < node.mNumMeshes(); i++) {
                AIMesh mesh = AIMesh.create(scene.mMeshes().get(meshIds.get(i)));
                Part part = buildPart(mesh, world, scene, source, bounds, textures, texturePaths);
                if (part != null) {
                    parts.add(part);
                }
            }
        }
        PointerBuffer children = node.mChildren();
        if (children != null) {
            for (int i = 0; i < node.mNumChildren(); i++) {
                collect(AINode.create(children.get(i)), world, scene, source, parts, bounds, textures, texturePaths);
            }
        }
    }

    private static Part buildPart(
            AIMesh mesh,
            Matrix4f world,
            AIScene scene,
            ModelFiles.Source source,
            Bounds bounds,
            Map<String, Texture> textures,
            Map<Integer, String> texturePaths) {
        int vertexCount = mesh.mNumVertices();
        if (vertexCount == 0 || mesh.mNumFaces() == 0) {
            return null;
        }
        float[] px = new float[vertexCount];
        float[] py = new float[vertexCount];
        float[] pz = new float[vertexCount];
        float[] u = new float[vertexCount];
        float[] v = new float[vertexCount];
        readVertices(mesh, world, source, px, py, pz, u, v, bounds);

        List<Integer> corners = new ArrayList<>();
        AIFace.Buffer faces = mesh.mFaces();
        for (int faceIndex = 0; faceIndex < mesh.mNumFaces(); faceIndex++) {
            AIFace face = faces.get(faceIndex);
            int indexCount = face.mNumIndices();
            IntBuffer indices = face.mIndices();
            if (indexCount < 3) {
                continue;
            }
            int first = indices.get(0);
            // Assimp에 삼각화를 요청하지만, 남는 다각형은 부채꼴로 나눕니다.
            for (int k = 1; k < indexCount - 1; k++) {
                corners.add(first);
                corners.add(indices.get(k));
                corners.add(indices.get(k + 1));
            }
        }
        if (corners.isEmpty()) {
            return null;
        }

        boolean flipNormal = world.determinant() < 0f;
        float[] vertices = new float[corners.size() * Mesh.FLOATS_PER_VERTEX];
        int[] indexArray = new int[corners.size()];
        Vector3f edgeA = new Vector3f();
        Vector3f edgeB = new Vector3f();
        Vector3f normal = new Vector3f();
        for (int triangle = 0; triangle < corners.size(); triangle += 3) {
            int i0 = corners.get(triangle);
            int i1 = corners.get(triangle + 1);
            int i2 = corners.get(triangle + 2);
            edgeA.set(px[i1] - px[i0], py[i1] - py[i0], pz[i1] - pz[i0]);
            edgeB.set(px[i2] - px[i0], py[i2] - py[i0], pz[i2] - pz[i0]);
            edgeA.cross(edgeB, normal);
            if (normal.lengthSquared() < 1.0e-12f) {
                normal.set(0f, 1f, 0f);
            } else {
                normal.normalize();
            }
            if (flipNormal) {
                normal.negate();
            }
            putVertex(vertices, triangle, px[i0], py[i0], pz[i0], normal, u[i0], v[i0]);
            putVertex(vertices, triangle + 1, px[i1], py[i1], pz[i1], normal, u[i1], v[i1]);
            putVertex(vertices, triangle + 2, px[i2], py[i2], pz[i2], normal, u[i2], v[i2]);
            indexArray[triangle] = triangle;
            indexArray[triangle + 1] = triangle + 1;
            indexArray[triangle + 2] = triangle + 2;
        }

        Material material = readMaterial(scene, mesh.mMaterialIndex(), source, textures, texturePaths);
        return new Part(Mesh.upload(vertices, indexArray), material);
    }

    private static void readVertices(
            AIMesh mesh,
            Matrix4f world,
            ModelFiles.Source source,
            float[] px,
            float[] py,
            float[] pz,
            float[] u,
            float[] v,
            Bounds bounds) {
        AIVector3D.Buffer positions = mesh.mVertices();
        AIVector3D.Buffer texcoords = mesh.mTextureCoords(0);
        Vector3f point = new Vector3f();
        for (int i = 0; i < px.length; i++) {
            AIVector3D position = positions.get(i);
            point.set(position.x(), position.y(), position.z());
            world.transformPosition(point);
            px[i] = point.x;
            py[i] = point.y;
            pz[i] = point.z;
            bounds.include(point.x, point.y, point.z);
            if (texcoords != null) {
                AIVector3D uv = texcoords.get(i);
                u[i] = uv.x();
                // 파일의 V=0은 이미지 아래인 경우가 많습니다. 우리가 올린 텍스처는 위가 V=0이라 뒤집습니다.
                v[i] = 1f - uv.y();
            }
        }
    }

    private static void putVertex(
            float[] vertices,
            int vertexIndex,
            float x,
            float y,
            float z,
            Vector3f normal,
            float u,
            float v) {
        int offset = vertexIndex * Mesh.FLOATS_PER_VERTEX;
        vertices[offset] = x;
        vertices[offset + 1] = y;
        vertices[offset + 2] = z;
        vertices[offset + 3] = normal.x;
        vertices[offset + 4] = normal.y;
        vertices[offset + 5] = normal.z;
        vertices[offset + 6] = u;
        vertices[offset + 7] = v;
    }

    private static Material readMaterial(
            AIScene scene,
            int materialIndex,
            ModelFiles.Source source,
            Map<String, Texture> textures,
            Map<Integer, String> texturePaths) {
        Vector3f color = new Vector3f(1f, 1f, 1f);
        boolean doubleSided = false;
        Texture texture = null;
        if (materialIndex >= 0 && materialIndex < scene.mNumMaterials()) {
            AIMaterial material = AIMaterial.create(scene.mMaterials().get(materialIndex));
            readColor(material, color);
            doubleSided = readTwoSided(material);
            texture = readTexture(scene, material, materialIndex, source, textures, texturePaths);
            // 텍스처가 있는데 기본 색이 검정이면 이미지를 지워 버립니다. 그런 값은
            // 색이 없다는 뜻으로 보고 흰색으로 곱합니다.
            if (texture != null && color.lengthSquared() < 1.0e-4f) {
                color.set(1f, 1f, 1f);
            }
        }
        Vector3f objColor = ObjMaterials.color(source.modelFile, materialIndex);
        if (objColor == null) {
            objColor = ObjMaterials.color(source.modelFile, materialIndex - 1);
        }
        if (objColor != null) {
            color.set(objColor);
        }
        return new Material(color, texture, doubleSided);
    }

    private static void readColor(AIMaterial material, Vector3f dest) {
        // heap에 만든 AIColor4D를 free()하면 이 Assimp 빌드가 같은 블록을 한 번 더 해제합니다.
        // 스택 메모리는 스코프가 끝날 때 풀로 돌아가서 그 문제가 없습니다.
        try (MemoryStack stack = MemoryStack.stackPush()) {
            AIColor4D color = AIColor4D.malloc(stack);
            if (aiGetMaterialColor(material, AI_MATKEY_BASE_COLOR, 0, 0, color) == aiReturn_SUCCESS
                    || aiGetMaterialColor(material, AI_MATKEY_COLOR_DIFFUSE, 0, 0, color) == aiReturn_SUCCESS) {
                dest.set(color.r(), color.g(), color.b());
            }
        }
    }

    private static boolean readTwoSided(AIMaterial material) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer value = stack.mallocInt(1);
            if (aiGetMaterialIntegerArray(material, AI_MATKEY_TWOSIDED, 0, 0, value, null) == aiReturn_SUCCESS) {
                return value.get(0) != 0;
            }
            return false;
        }
    }

    private static Texture readTexture(
            AIScene scene,
            AIMaterial material,
            int materialIndex,
            ModelFiles.Source source,
            Map<String, Texture> textures,
            Map<Integer, String> texturePaths) {
        // glTF 텍스처 경로는 파일의 JSON에서 읽습니다. Assimp로 그 문자열을 꺼내면
        // 이 버전의 네이티브 라이브러리가 메모리를 두 번 해제하며 죽습니다.
        // OBJ는 기본 재질이 0번에 하나 더 생기는 경우가 있어, 바로 앞 번호도 봅니다.
        String path = texturePaths.get(materialIndex);
        if (path == null) {
            path = texturePaths.get(materialIndex - 1);
        }
        if (path == null || path.isBlank()) {
            return null;
        }
        Texture cached = textures.get(path);
        if (cached != null) {
            return cached;
        }
        Texture loaded = path.startsWith("*")
                ? embeddedTexture(scene, path)
                : Texture.fromBytes(source.readRelative(path));
        textures.put(path, loaded);
        return loaded;
    }

    private static Texture embeddedTexture(AIScene scene, String path) {
        int index;
        try {
            index = Integer.parseInt(path.substring(1));
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("내장 텍스처 번호를 읽지 못했습니다: " + path, exception);
        }
        if (index < 0 || index >= scene.mNumTextures()) {
            throw new IllegalStateException("내장 텍스처가 없습니다: " + path);
        }
        AITexture texture = AITexture.create(scene.mTextures().get(index));
        if (texture.mHeight() == 0) {
            ByteBuffer compressed = texture.pcDataCompressed();
            byte[] bytes = new byte[compressed.remaining()];
            compressed.get(bytes);
            return Texture.fromBytes(bytes);
        }
        throw new IllegalStateException("압축되지 않은 내장 텍스처는 아직 지원하지 않습니다: " + path);
    }

    /** Assimp 행렬은 행 우선입니다. JOML {@code set}의 인자도 행 순서라 그대로 넣습니다. */
    private static Matrix4f toMatrix(AIMatrix4x4 source) {
        return new Matrix4f().set(
                source.a1(), source.a2(), source.a3(), source.a4(),
                source.b1(), source.b2(), source.b3(), source.b4(),
                source.c1(), source.c2(), source.c3(), source.c4(),
                source.d1(), source.d2(), source.d3(), source.d4());
    }

    private static final class Bounds {
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;

        void include(float x, float y, float z) {
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }
    }
}
