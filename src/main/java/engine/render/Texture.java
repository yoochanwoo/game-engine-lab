package engine.render;

import static org.lwjgl.opengl.GL33C.GL_NEAREST;
import static org.lwjgl.opengl.GL33C.GL_RGBA;
import static org.lwjgl.opengl.GL33C.GL_RGBA8;
import static org.lwjgl.opengl.GL33C.GL_REPEAT;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL33C.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.opengl.GL33C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL33C.glBindTexture;
import static org.lwjgl.opengl.GL33C.glDeleteTextures;
import static org.lwjgl.opengl.GL33C.glGenTextures;
import static org.lwjgl.opengl.GL33C.glPixelStorei;
import static org.lwjgl.opengl.GL33C.glTexImage2D;
import static org.lwjgl.opengl.GL33C.glTexParameteri;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.imageio.ImageIO;
import org.lwjgl.system.MemoryUtil;

/**
 * GPU에 올린 2D 이미지입니다. 모델의 재질이 가리키고, 여러 부분이 같은 이미지를 공유할 수 있습니다.
 *
 * <p>필터는 가까운 픽셀을 고르는 NEAREST입니다. 로우폴리 팩의 팔레트 텍스처가
 * 이웃 색과 섞이지 않게 하려는 선택입니다.
 */
public final class Texture {

    private int id;

    private Texture(int id) {
        this.id = id;
    }

    /** 재질에 이미지가 없을 때 묶는 1×1 흰색입니다. 셰이더가 텍스처를 곱해도 색이 유지됩니다. */
    public static Texture white() {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFFFFFFFF);
        return fromImage(image);
    }

    public static Texture fromBytes(byte[] encoded) {
        try {
            return fromStream(new ByteArrayInputStream(encoded));
        } catch (IOException exception) {
            throw new IllegalStateException("텍스처 이미지를 읽지 못했습니다.", exception);
        }
    }

    public static Texture fromStream(InputStream stream) throws IOException {
        BufferedImage image = ImageIO.read(stream);
        if (image == null) {
            throw new IOException("지원하지 않는 이미지입니다. PNG나 JPEG여야 합니다.");
        }
        return fromImage(image);
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, id);
    }

    public void destroy() {
        if (id != 0) {
            glDeleteTextures(id);
            id = 0;
        }
    }

    private static Texture fromImage(BufferedImage source) {
        int width = source.getWidth();
        int height = source.getHeight();
        // glTF는 이미지 위를 V=0으로 둡니다. 버퍼의 첫 줄이 OpenGL의 V=0이 되도록
        // 그림의 윗줄부터 넣습니다. OBJ는 로더에서 V를 뒤집습니다.
        ByteBuffer pixels = MemoryUtil.memAlloc(width * height * 4);
        try {
            int[] row = new int[width];
            for (int y = 0; y < height; y++) {
                source.getRGB(0, y, width, 1, row, 0, width);
                for (int x = 0; x < width; x++) {
                    int argb = row[x];
                    pixels.put((byte) ((argb >> 16) & 0xFF));
                    pixels.put((byte) ((argb >> 8) & 0xFF));
                    pixels.put((byte) (argb & 0xFF));
                    pixels.put((byte) ((argb >> 24) & 0xFF));
                }
            }
            pixels.flip();

            int id = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, id);
            glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
            glBindTexture(GL_TEXTURE_2D, 0);
            return new Texture(id);
        } finally {
            MemoryUtil.memFree(pixels);
        }
    }
}
