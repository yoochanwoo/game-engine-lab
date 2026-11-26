package engine.render;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 모델 경로를 실제 파일로 바꿉니다.
 *
 * <p>먼저 작업 폴더의 파일을 보고, 없으면 {@code src/main/resources} 안의 파일을 봅니다.
 * 그래서 {@code models/tree.glb}는 저장소에 넣어 둔 파일이고, {@code my/hero.glb}처럼
 * 프로젝트 옆에 둔 파일도 같은 {@code load} 호출로 열립니다.
 *
 * <p>텍스처는 모델 파일 기준의 상대 경로입니다. glTF가 {@code Textures/texture-a.png}라고
 * 적으면 모델 옆의 그 폴더를 찾습니다.
 */
final class ModelFiles {

    private ModelFiles() {
    }

    static Source open(String requested) {
        String normalized = requested.replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        Path direct = Path.of(normalized);
        if (Files.isRegularFile(direct)) {
            return new Source(direct.toAbsolutePath(), null);
        }

        String classpath = "/" + normalized;
        URL url = ModelFiles.class.getResource(classpath);
        if (url == null) {
            throw new IllegalStateException(
                    "모델 파일이 없습니다: " + requested + " (작업 폴더와 클래스패스 양쪽을 확인했습니다)");
        }
        String classpathDirectory = parent(normalized);
        if ("file".equals(url.getProtocol())) {
            try {
                return new Source(Path.of(url.toURI()), classpathDirectory);
            } catch (URISyntaxException exception) {
                throw new IllegalStateException("모델 경로를 해석하지 못했습니다: " + url, exception);
            }
        }

        String suffix = normalized.contains(".") ? normalized.substring(normalized.lastIndexOf('.')) : ".bin";
        try {
            Path temp = Files.createTempFile("model-", suffix);
            temp.toFile().deleteOnExit();
            try (InputStream in = url.openStream()) {
                Files.copy(in, temp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return new Source(temp, classpathDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("모델을 임시 파일로 풀지 못했습니다: " + requested, exception);
        }
    }

    private static String parent(String normalized) {
        int slash = normalized.lastIndexOf('/');
        if (slash < 0) {
            return "";
        }
        return normalized.substring(0, slash);
    }

    /**
     * Assimp가 연 모델 파일과, 그 옆의 텍스처를 찾기 위한 기준입니다.
     * {@code classpathDirectory}는 jar 안에서 상대 경로를 클래스패스로 다시 찾을 때 씁니다.
     */
    static final class Source {
        final Path modelFile;
        final String classpathDirectory;

        Source(Path modelFile, String classpathDirectory) {
            this.modelFile = modelFile;
            this.classpathDirectory = classpathDirectory;
        }

        byte[] readRelative(String relativePath) {
            String relative = relativePath.replace('\\', '/');
            Path besideModel = modelFile.getParent().resolve(relative).normalize();
            if (Files.isRegularFile(besideModel)) {
                try {
                    return Files.readAllBytes(besideModel);
                } catch (IOException exception) {
                    throw new IllegalStateException("텍스처를 읽지 못했습니다: " + besideModel, exception);
                }
            }
            if (classpathDirectory != null) {
                String classpath = classpathDirectory.isEmpty() ? relative : classpathDirectory + "/" + relative;
                try (InputStream in = ModelFiles.class.getResourceAsStream("/" + classpath)) {
                    if (in != null) {
                        return in.readAllBytes();
                    }
                } catch (IOException exception) {
                    throw new IllegalStateException("텍스처를 읽지 못했습니다: " + classpath, exception);
                }
            }
            throw new IllegalStateException(
                    "텍스처를 찾지 못했습니다: " + relativePath + " (모델 " + modelFile.getFileName() + " 옆을 확인하세요)");
        }
    }
}
