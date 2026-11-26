package engine.render;

import static org.lwjgl.opengl.GL33C.GL_BACK;
import static org.lwjgl.opengl.GL33C.GL_NO_ERROR;
import static org.lwjgl.opengl.GL33C.GL_PACK_ALIGNMENT;
import static org.lwjgl.opengl.GL33C.GL_RGBA;
import static org.lwjgl.opengl.GL33C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL33C.glGetError;
import static org.lwjgl.opengl.GL33C.glPixelStorei;
import static org.lwjgl.opengl.GL33C.glReadBuffer;
import static org.lwjgl.opengl.GL33C.glReadPixels;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.lwjgl.system.MemoryUtil;

/**
 * 지금 그려진 프레임버퍼를 PNG로 저장합니다.
 *
 * <p>게임 플레이에는 쓰이지 않습니다. {@code --screenshot 경로.png}로 실행했을 때만
 * 엔진이 호출합니다. OpenGL의 원점은 왼쪽 아래라, 이미지로 저장할 때 위아래를 뒤집습니다.
 */
public final class Screenshot {

    private Screenshot() {
    }

    public static void save(Path path, int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("프레임버퍼 크기가 올바르지 않습니다: " + width + "x" + height);
        }

        int byteCount = width * height * 4;
        ByteBuffer pixels = MemoryUtil.memAlloc(byteCount);
        try {
            while (glGetError() != GL_NO_ERROR) {
                // 저장 전에 남아 있던 오류를 비워, 아래 검사가 이번 읽기만 보게 합니다.
            }
            glPixelStorei(GL_PACK_ALIGNMENT, 1);
            glReadBuffer(GL_BACK);
            glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            int error = glGetError();
            if (error != GL_NO_ERROR) {
                throw new IllegalStateException("프레임버퍼를 읽지 못했습니다. OpenGL error 0x"
                        + Integer.toHexString(error));
            }

            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < height; y++) {
                int sourceRow = (height - 1 - y) * width * 4;
                for (int x = 0; x < width; x++) {
                    int index = sourceRow + x * 4;
                    int red = pixels.get(index) & 0xFF;
                    int green = pixels.get(index + 1) & 0xFF;
                    int blue = pixels.get(index + 2) & 0xFF;
                    image.setRGB(x, y, (red << 16) | (green << 8) | blue);
                }
            }

            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (!ImageIO.write(image, "png", path.toFile())) {
                throw new IllegalStateException("PNG 인코더를 찾지 못했습니다: " + path);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("스크린샷을 저장하지 못했습니다: " + path, exception);
        } finally {
            MemoryUtil.memFree(pixels);
        }
    }
}
