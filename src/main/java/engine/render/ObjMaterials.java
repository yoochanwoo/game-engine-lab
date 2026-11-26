package engine.render;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.joml.Vector3f;

/**
 * OBJ 옆의 MTL에서 재질 색(Kd)과 텍스처(map_Kd)를 읽습니다.
 *
 * <p>재질은 파일에 나온 순서가 곧 번호입니다. Assimp도 그 순서로 재질을 만듭니다.
 * glTF 텍스처 문자열을 Assimp에 물어보면 이 빌드의 네이티브 코드가 죽어서,
 * OBJ는 텍스트라 여기서 직접 읽습니다.
 */
final class ObjMaterials {

    private ObjMaterials() {
    }

    static Map<Integer, String> textures(Path modelFile) {
        return read(modelFile).textures;
    }

    static Vector3f color(Path modelFile, int materialIndex) {
        return read(modelFile).colors.get(materialIndex);
    }

    private static Parsed read(Path modelFile) {
        String name = modelFile.getFileName().toString().toLowerCase();
        if (!name.endsWith(".obj")) {
            return Parsed.EMPTY;
        }
        try {
            String obj = Files.readString(modelFile);
            String mtlName = null;
            for (String raw : obj.split("\n")) {
                String line = raw.trim();
                if (line.startsWith("mtllib ")) {
                    mtlName = line.substring("mtllib ".length()).trim();
                    break;
                }
            }
            if (mtlName == null) {
                return Parsed.EMPTY;
            }
            Path mtl = modelFile.getParent().resolve(mtlName);
            if (!Files.isRegularFile(mtl)) {
                return Parsed.EMPTY;
            }
            Parsed parsed = new Parsed();
            int index = -1;
            for (String raw : Files.readString(mtl).split("\n")) {
                String line = raw.trim();
                if (line.startsWith("newmtl ")) {
                    index++;
                } else if (index < 0) {
                    continue;
                } else if (line.startsWith("Kd ")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 4) {
                        parsed.colors.put(index, new Vector3f(
                                Float.parseFloat(parts[1]),
                                Float.parseFloat(parts[2]),
                                Float.parseFloat(parts[3])));
                    }
                } else if (line.startsWith("map_Kd ")) {
                    parsed.textures.put(index, line.substring("map_Kd ".length()).trim());
                }
            }
            return parsed;
        } catch (IOException | NumberFormatException exception) {
            throw new IllegalStateException("MTL을 읽지 못했습니다: " + modelFile, exception);
        }
    }

    private static final class Parsed {
        private static final Parsed EMPTY = new Parsed();

        private final Map<Integer, String> textures = new HashMap<>();
        private final Map<Integer, Vector3f> colors = new HashMap<>();
    }
}
