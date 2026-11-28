package engine.ui;

import static org.lwjgl.opengl.GL33C.GL_BLEND;
import static org.lwjgl.opengl.GL33C.GL_CULL_FACE;
import static org.lwjgl.opengl.GL33C.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL33C.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL33C.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE0;
import static org.lwjgl.opengl.GL33C.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL33C.GL_TRIANGLES;
import static org.lwjgl.opengl.GL33C.glActiveTexture;
import static org.lwjgl.opengl.GL33C.glBindTexture;
import static org.lwjgl.opengl.GL33C.glBindVertexArray;
import static org.lwjgl.opengl.GL33C.glBlendFunc;
import static org.lwjgl.opengl.GL33C.glBufferData;
import static org.lwjgl.opengl.GL33C.glBufferSubData;
import static org.lwjgl.opengl.GL33C.glDisable;
import static org.lwjgl.opengl.GL33C.glDrawArrays;
import static org.lwjgl.opengl.GL33C.glEnable;
import static org.lwjgl.opengl.GL33C.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL33C.glGenBuffers;
import static org.lwjgl.opengl.GL33C.glGenVertexArrays;
import static org.lwjgl.opengl.GL33C.glVertexAttribPointer;
import static org.lwjgl.opengl.GL15C.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15C.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15C.glBindBuffer;
import static org.lwjgl.opengl.GL11C.GL_FLOAT;

import engine.render.Shader;
import engine.ui.Font.Glyph;
import java.nio.FloatBuffer;
import org.lwjgl.system.MemoryUtil;

/** 화면 좌표(왼쪽 위가 0,0)로 패널과 글자를 그립니다. */
public final class Ui {

    private static final int MAX_FLOATS = 48 * 256;

    private final Shader shader;
    private final int vao;
    private final int vbo;
    private final FloatBuffer vertices = MemoryUtil.memAllocFloat(MAX_FLOATS);
    private int screenWidth = 1;
    private int screenHeight = 1;

    public Ui() {
        shader = Shader.load("/shaders/ui.vert", "/shaders/ui.frag");
        vao = glGenVertexArrays();
        vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, MAX_FLOATS * Float.BYTES, GL_DYNAMIC_DRAW);
        int stride = 4 * Float.BYTES;
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 2L * Float.BYTES);
        glBindVertexArray(0);
    }

    public void begin(int width, int height) {
        screenWidth = Math.max(1, width);
        screenHeight = Math.max(1, height);
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        shader.use();
        shader.setVec2("uScreen", screenWidth, screenHeight);
    }

    public void end() {
        glDisable(GL_BLEND);
        glEnable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
    }

    public void panel(float x, float y, float w, float h, float r, float g, float b, float a) {
        vertices.clear();
        quad(x, y, w, h, 0, 0, 1, 1);
        draw(null, r, g, b, a, false);
    }

    public void text(Font font, String text, float x, float y, float r, float g, float b) {
        vertices.clear();
        float pen = x;
        float baseline = y + font.pixelHeight();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            Glyph glyph = font.glyph(cp);
            if (glyph.width > 0) {
                float left = pen + glyph.xOffset;
                float top = baseline + glyph.yOffset;
                quad(left, top, glyph.width, glyph.height, glyph.u0, glyph.v1, glyph.u1, glyph.v0);
            }
            pen += glyph.advance;
        }
        if (vertices.position() == 0) {
            return;
        }
        draw(font, r, g, b, 1f, true);
    }

    public boolean hit(float x, float y, float w, float h, float mx, float my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public void destroy() {
        shader.destroy();
        MemoryUtil.memFree(vertices);
    }

    private void quad(float x, float y, float w, float h, float u0, float v0, float u1, float v1) {
        put(x, y, u0, v0);
        put(x + w, y, u1, v0);
        put(x + w, y + h, u1, v1);
        put(x, y, u0, v0);
        put(x + w, y + h, u1, v1);
        put(x, y + h, u0, v1);
    }

    private void put(float x, float y, float u, float v) {
        vertices.put(x).put(y).put(u).put(v);
    }

    private void draw(Font font, float r, float g, float b, float a, boolean textured) {
        vertices.flip();
        shader.setVec4("uColor", r, g, b, a);
        shader.setInt("uTextured", textured ? 1 : 0);
        glActiveTexture(GL_TEXTURE0);
        if (textured) {
            glBindTexture(GL_TEXTURE_2D, font.texture());
            shader.setInt("uTexture", 0);
        }
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, vertices);
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, vertices.limit() / 4);
        glBindVertexArray(0);
    }
}
