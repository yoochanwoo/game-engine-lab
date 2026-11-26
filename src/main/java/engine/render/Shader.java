package engine.render;

import static org.lwjgl.opengl.GL33C.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL33C.GL_FALSE;
import static org.lwjgl.opengl.GL33C.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL33C.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL33C.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL33C.glAttachShader;
import static org.lwjgl.opengl.GL33C.glCompileShader;
import static org.lwjgl.opengl.GL33C.glCreateProgram;
import static org.lwjgl.opengl.GL33C.glCreateShader;
import static org.lwjgl.opengl.GL33C.glDeleteProgram;
import static org.lwjgl.opengl.GL33C.glDeleteShader;
import static org.lwjgl.opengl.GL33C.glDetachShader;
import static org.lwjgl.opengl.GL33C.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL33C.glGetProgrami;
import static org.lwjgl.opengl.GL33C.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL33C.glGetShaderi;
import static org.lwjgl.opengl.GL33C.glGetUniformLocation;
import static org.lwjgl.opengl.GL33C.glLinkProgram;
import static org.lwjgl.opengl.GL33C.glShaderSource;
import static org.lwjgl.opengl.GL33C.glUniform1i;
import static org.lwjgl.opengl.GL33C.glUniform3f;
import static org.lwjgl.opengl.GL33C.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL33C.glUseProgram;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

/**
 * 디스크의 GLSL 파일을 컴파일해 GPU 프로그램으로 만듭니다.
 *
 * <p>셰이더는 정점마다, 그리고 픽셀마다 돌아가는 작은 프로그램입니다. 자바는
 * "무엇을 그릴지"(메시, 행렬, 색)만 넘기고, 화면 좌표로 바꾸는 계산은 셰이더가 합니다.
 * 파일은 {@code src/main/resources} 아래 두고, 클래스패스 경로로 읽습니다.
 *
 * <p>이 파일의 {@code gl*} 호출은 {@code org.lwjgl.opengl.GL33C}의 OpenGL 3.3 코어 함수입니다.
 */
public final class Shader {

    private int program;
    private final Map<String, Integer> uniformLocations = new HashMap<>();

    private Shader(int program) {
        this.program = program;
    }

    /**
     * @param vertexPath 예: {@code /shaders/basic.vert}
     * @param fragmentPath 예: {@code /shaders/basic.frag}
     */
    public static Shader load(String vertexPath, String fragmentPath) {
        int vertexShader = compile(GL_VERTEX_SHADER, readResource(vertexPath), vertexPath);
        int fragmentShader = compile(GL_FRAGMENT_SHADER, readResource(fragmentPath), fragmentPath);

        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);

        // 셰이더 객체는 프로그램에 링크된 뒤엔 더 이상 필요 없습니다.
        glDetachShader(program, vertexShader);
        glDetachShader(program, fragmentShader);
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);

        if (glGetProgrami(program, GL_LINK_STATUS) == GL_FALSE) {
            String log = glGetProgramInfoLog(program);
            glDeleteProgram(program);
            throw new IllegalStateException("셰이더 링크 실패\n" + log);
        }
        return new Shader(program);
    }

    public void use() {
        glUseProgram(program);
    }

    /** JOML 행렬은 OpenGL과 같이 열 우선(column-major)이라 전치하지 않고 넘깁니다. */
    public void setMat4(String name, Matrix4f value) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            value.get(buffer);
            glUniformMatrix4fv(location(name), false, buffer);
        }
    }

    public void setVec3(String name, Vector3f value) {
        glUniform3f(location(name), value.x, value.y, value.z);
    }

    public void setInt(String name, int value) {
        glUniform1i(location(name), value);
    }

    public void destroy() {
        if (program != 0) {
            glDeleteProgram(program);
            program = 0;
        }
    }

    private int location(String name) {
        Integer cached = uniformLocations.get(name);
        if (cached != null) {
            return cached;
        }
        int found = glGetUniformLocation(program, name);
        if (found < 0) {
            throw new IllegalStateException(
                    "유니폼을 찾지 못했습니다: " + name + " (셰이더에서 이름이 다르거나, 쓰이지 않아 최적화로 빠졌습니다)");
        }
        uniformLocations.put(name, found);
        return found;
    }

    private static int compile(int type, String source, String path) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException("셰이더 컴파일 실패: " + path + "\n" + log);
        }
        return shader;
    }

    private static String readResource(String path) {
        String classpath = path.startsWith("/") ? path : "/" + path;
        try (InputStream in = Shader.class.getResourceAsStream(classpath)) {
            if (in == null) {
                throw new IllegalStateException("셰이더 파일이 없습니다: " + classpath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("셰이더 파일을 읽지 못했습니다: " + classpath, exception);
        }
    }
}
