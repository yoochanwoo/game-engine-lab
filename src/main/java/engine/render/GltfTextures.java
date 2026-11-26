package engine.render;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * glTF 재질이 가리키는 이미지 경로만 파일에서 직접 읽습니다.
 *
 * <p>메시와 색은 Assimp가 읽고, 텍스처 파일 이름만 여기서 꺼냅니다. Assimp로
 * 그 문자열을 요청하면 텍스처가 있는 glTF에서 네이티브 라이브러리가 죽기 때문입니다.
 * .glb가 아니면 빈 맵을 돌려주고, OBJ는 Assimp 쪽 경로를 그대로 씁니다.
 */
final class GltfTextures {

    private static final int GLB_MAGIC = 0x46546C67;
    private static final int JSON_CHUNK = 0x4E4F534A;
    private static final Pattern INDEX = Pattern.compile("\"index\"\\s*:\\s*(\\d+)");
    private static final Pattern SOURCE = Pattern.compile("\"source\"\\s*:\\s*(\\d+)");
    private static final Pattern URI = Pattern.compile("\"uri\"\\s*:\\s*\"([^\"]*)\"");

    private GltfTextures() {
    }

    /** 재질 번호 → 모델 파일 기준 상대 경로. 텍스처가 없는 재질은 맵에 없습니다. */
    static Map<Integer, String> byMaterial(Path file) {
        String name = file.getFileName().toString().toLowerCase();
        if (!name.endsWith(".glb") && !name.endsWith(".gltf")) {
            return Map.of();
        }
        String json = readJson(file);
        if (json == null) {
            return Map.of();
        }
        List<String> materials = arrayObjects(json, "materials");
        List<String> textureObjects = arrayObjects(json, "textures");
        List<String> images = arrayObjects(json, "images");
        Map<Integer, String> paths = new HashMap<>();
        for (int materialIndex = 0; materialIndex < materials.size(); materialIndex++) {
            String material = materials.get(materialIndex);
            int textureKey = material.indexOf("\"baseColorTexture\"");
            if (textureKey < 0) {
                continue;
            }
            Matcher indexMatcher = INDEX.matcher(material.substring(textureKey));
            if (!indexMatcher.find()) {
                continue;
            }
            int textureIndex = Integer.parseInt(indexMatcher.group(1));
            if (textureIndex < 0 || textureIndex >= textureObjects.size()) {
                continue;
            }
            Matcher sourceMatcher = SOURCE.matcher(textureObjects.get(textureIndex));
            if (!sourceMatcher.find()) {
                continue;
            }
            int imageIndex = Integer.parseInt(sourceMatcher.group(1));
            if (imageIndex < 0 || imageIndex >= images.size()) {
                continue;
            }
            Matcher uriMatcher = URI.matcher(images.get(imageIndex));
            if (uriMatcher.find()) {
                paths.put(materialIndex, decodeUri(uriMatcher.group(1)));
            }
        }
        return paths;
    }

    private static String readJson(Path file) {
        try {
            byte[] bytes = Files.readAllBytes(file);
            String name = file.getFileName().toString().toLowerCase();
            if (name.endsWith(".gltf")) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
            if (bytes.length < 20) {
                return null;
            }
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            if (buffer.getInt() != GLB_MAGIC) {
                return null;
            }
            buffer.getInt(); // version
            buffer.getInt(); // length
            int chunkLength = buffer.getInt();
            int chunkType = buffer.getInt();
            if (chunkType != JSON_CHUNK || buffer.position() + chunkLength > bytes.length) {
                return null;
            }
            return new String(bytes, buffer.position(), chunkLength, StandardCharsets.UTF_8).trim();
        } catch (IOException exception) {
            throw new IllegalStateException("glTF JSON을 읽지 못했습니다: " + file, exception);
        }
    }

    /** JSON 배열 안의 객체({...})를 순서대로 꺼냅니다. 문자열 안의 괄호는 무시합니다. */
    private static List<String> arrayObjects(String json, String key) {
        int keyAt = json.indexOf("\"" + key + "\"");
        if (keyAt < 0) {
            return List.of();
        }
        int start = json.indexOf('[', keyAt);
        if (start < 0) {
            return List.of();
        }
        List<String> objects = new ArrayList<>();
        int depth = 0;
        boolean inString = false;
        boolean escape = false;
        int objectStart = -1;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (inString) {
                if (escape) {
                    escape = false;
                } else if (c == '\\') {
                    escape = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{') {
                if (depth == 0) {
                    objectStart = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objectStart >= 0) {
                    objects.add(json.substring(objectStart, i + 1));
                    objectStart = -1;
                }
            } else if (c == ']' && depth == 0) {
                break;
            }
        }
        return objects;
    }

    private static String decodeUri(String uri) {
        return uri.replace("\\/", "/").replace("%20", " ");
    }
}
