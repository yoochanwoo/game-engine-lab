package engine.ui;

import static org.lwjgl.opengl.GL33C.GL_LINEAR;
import static org.lwjgl.opengl.GL33C.GL_R8;
import static org.lwjgl.opengl.GL33C.GL_RED;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL33C.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL33C.glBindTexture;
import static org.lwjgl.opengl.GL33C.glDeleteTextures;
import static org.lwjgl.opengl.GL33C.glGenTextures;
import static org.lwjgl.opengl.GL33C.glPixelStorei;
import static org.lwjgl.opengl.GL33C.glTexImage2D;
import static org.lwjgl.opengl.GL33C.glTexParameteri;
import static org.lwjgl.opengl.GL33C.glTexSubImage2D;
import static org.lwjgl.opengl.GL11C.GL_UNPACK_ALIGNMENT;
import static org.lwjgl.stb.STBTruetype.stbtt_FreeBitmap;
import static org.lwjgl.stb.STBTruetype.stbtt_GetCodepointBitmap;
import static org.lwjgl.stb.STBTruetype.stbtt_GetCodepointHMetrics;
import static org.lwjgl.stb.STBTruetype.stbtt_GetFontOffsetForIndex;
import static org.lwjgl.stb.STBTruetype.stbtt_InitFont;
import static org.lwjgl.stb.STBTruetype.stbtt_ScaleForPixelHeight;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.MemoryUtil.memAlloc;
import static org.lwjgl.system.MemoryUtil.memFree;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.stb.STBTTFontinfo;
import org.lwjgl.system.MemoryStack;

/** 쓰는 글자만 아틀라스에 굽습니다. 한글은 문자가 많아 미리 전부 만들지 않습니다. */
public final class Font {

    private static final int ATLAS = 1024;

    private final ByteBuffer ttf;
    private final STBTTFontinfo info;
    private final float scale;
    private final int pixelHeight;
    private final Map<Integer, Glyph> glyphs = new HashMap<>();
    private final int texture;
    private int penX = 1;
    private int penY = 1;
    private int rowHeight;

    public Font(String resourcePath, int pixelHeight) {
        this.pixelHeight = pixelHeight;
        byte[] bytes = read(resourcePath);
        ttf = memAlloc(bytes.length);
        ttf.put(bytes).flip();
        info = STBTTFontinfo.malloc();
        if (!stbtt_InitFont(info, ttf, stbtt_GetFontOffsetForIndex(ttf, 0))) {
            throw new IllegalStateException("폰트를 열지 못했습니다: " + resourcePath);
        }
        scale = stbtt_ScaleForPixelHeight(info, pixelHeight);
        texture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texture);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_R8, ATLAS, ATLAS, 0, GL_RED, GL_UNSIGNED_BYTE, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public int texture() {
        return texture;
    }

    public int pixelHeight() {
        return pixelHeight;
    }

    public Glyph glyph(int codepoint) {
        Glyph existing = glyphs.get(codepoint);
        if (existing != null) {
            return existing;
        }
        Glyph created = rasterize(codepoint);
        glyphs.put(codepoint, created);
        return created;
    }

    public float width(String text) {
        float width = 0f;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            width += glyph(cp).advance;
        }
        return width;
    }

    public void destroy() {
        glDeleteTextures(texture);
        info.free();
        memFree(ttf);
    }

    private Glyph rasterize(int codepoint) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer advance = stack.mallocInt(1);
            IntBuffer leftSide = stack.mallocInt(1);
            stbtt_GetCodepointHMetrics(info, codepoint, advance, leftSide);
            float advancePx = advance.get(0) * scale;
            if (codepoint == ' ') {
                return new Glyph(0, 0, 0, 0, 0, 0, 0, 0, advancePx);
            }
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            IntBuffer xOff = stack.mallocInt(1);
            IntBuffer yOff = stack.mallocInt(1);
            ByteBuffer bitmap = stbtt_GetCodepointBitmap(info, scale, scale, codepoint, width, height, xOff, yOff);
            int w = width.get(0);
            int h = height.get(0);
            if (bitmap == null || w == 0 || h == 0) {
                if (bitmap != null) {
                    stbtt_FreeBitmap(bitmap, NULL);
                }
                return new Glyph(0, 0, 0, 0, 0, 0, 0, 0, advancePx);
            }
            if (penX + w + 1 >= ATLAS) {
                penX = 1;
                penY += rowHeight + 1;
                rowHeight = 0;
            }
            if (penY + h + 1 >= ATLAS) {
                stbtt_FreeBitmap(bitmap, NULL);
                throw new IllegalStateException("글자 아틀라스가 가득 찼습니다.");
            }
            ByteBuffer flipped = memAlloc(w * h);
            try {
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        flipped.put(y * w + x, bitmap.get((h - 1 - y) * w + x));
                    }
                }
                flipped.rewind();
                glBindTexture(GL_TEXTURE_2D, texture);
                glTexSubImage2D(GL_TEXTURE_2D, 0, penX, penY, w, h, GL_RED, GL_UNSIGNED_BYTE, flipped);
            } finally {
                memFree(flipped);
                stbtt_FreeBitmap(bitmap, NULL);
            }
            Glyph glyph = new Glyph(
                    penX / (float) ATLAS,
                    penY / (float) ATLAS,
                    (penX + w) / (float) ATLAS,
                    (penY + h) / (float) ATLAS,
                    w,
                    h,
                    xOff.get(0),
                    yOff.get(0),
                    advancePx);
            penX += w + 1;
            rowHeight = Math.max(rowHeight, h);
            return glyph;
        }
    }

    private static byte[] read(String resourcePath) {
        try (InputStream in = Font.class.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("폰트 파일이 없습니다: " + resourcePath);
            }
            return in.readAllBytes();
        } catch (IOException exception) {
            throw new IllegalStateException("폰트를 읽지 못했습니다: " + resourcePath, exception);
        }
    }

    public static final class Glyph {
        public final float u0;
        public final float v0;
        public final float u1;
        public final float v1;
        public final int width;
        public final int height;
        public final int xOffset;
        public final int yOffset;
        public final float advance;

        Glyph(float u0, float v0, float u1, float v1, int width, int height, int xOffset, int yOffset, float advance) {
            this.u0 = u0;
            this.v0 = v0;
            this.u1 = u1;
            this.v1 = v1;
            this.width = width;
            this.height = height;
            this.xOffset = xOffset;
            this.yOffset = yOffset;
            this.advance = advance;
        }
    }
}
