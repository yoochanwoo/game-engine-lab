package engine.core;

import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_SAMPLES;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL33C.GL_RENDERER;
import static org.lwjgl.opengl.GL33C.GL_VERSION;
import static org.lwjgl.opengl.GL33C.glGetString;
import static org.lwjgl.opengl.GL33C.glViewport;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.nio.IntBuffer;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

/**
 * GLFW 창과 OpenGL 3.3 코어 컨텍스트입니다.
 *
 * <p>게임 코드는 픽셀을 직접 다루지 않고, 이 클래스가 열어 둔 컨텍스트 위에서
 * 메시를 그립니다. 입력은 아직 ESC(창 닫기)만 받습니다. 2단계에서 WASD와 마우스가
 * 이 창의 콜백으로 들어올 예정입니다.
 *
 * <p>LWJGL 콜백 객체는 자바에서 필드로 붙들고 있어야 합니다. 지역 변수로만 두면
 * 가비지 컬렉터가 거두어 가고, 네이티브 쪽에서 죽은 콜백을 호출하다 죽을 수 있습니다.
 */
public final class Window {

    private final String title;
    private final int windowWidth;
    private final int windowHeight;

    private long handle;
    private int framebufferWidth;
    private int framebufferHeight;
    private boolean glfwReady;
    private boolean vsync = true;

    private GLFWErrorCallback errorCallback;
    private GLFWKeyCallback keyCallback;
    private GLFWFramebufferSizeCallback framebufferSizeCallback;

    public Window(String title, int windowWidth, int windowHeight) {
        this.title = title;
        this.windowWidth = windowWidth;
        this.windowHeight = windowHeight;
        this.framebufferWidth = windowWidth;
        this.framebufferHeight = windowHeight;
    }

    /** 창을 만들고 OpenGL 함수를 로드합니다. 메인 스레드에서 한 번만 호출합니다. */
    public void init() {
        errorCallback = GLFWErrorCallback.createPrint(System.err);
        glfwSetErrorCallback(errorCallback);

        if (!glfwInit()) {
            throw new IllegalStateException("GLFW를 초기화하지 못했습니다.");
        }
        glfwReady = true;

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        // macOS는 코어 프로파일에 이 힌트가 없으면 창 생성이 실패합니다. 다른 OS에서는 무해합니다.
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);

        handle = createWindow(4);
        if (handle == NULL) {
            // 소프트웨어 렌더러처럼 멀티샘플링을 거절하는 환경이 있어, 한 번 더 시도합니다.
            System.out.println("4x MSAA 창을 만들지 못했습니다. 샘플링 없이 다시 시도합니다.");
            handle = createWindow(0);
        }
        if (handle == NULL) {
            throw new IllegalStateException(
                    "창을 만들지 못했습니다. OpenGL 3.3을 지원하는 그래픽 드라이버가 필요합니다.");
        }

        glfwMakeContextCurrent(handle);
        // 컨텍스트가 현재 스레드에 붙은 뒤에야 OpenGL 함수 포인터를 읽을 수 있습니다.
        GL.createCapabilities();
        glfwSwapInterval(vsync ? 1 : 0);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer width = stack.mallocInt(1);
            IntBuffer height = stack.mallocInt(1);
            glfwGetFramebufferSize(handle, width, height);
            framebufferWidth = width.get(0);
            framebufferHeight = height.get(0);
        }
        glViewport(0, 0, framebufferWidth, framebufferHeight);

        // create()가 람다를 네이티브 콜백으로 감쌉니다. 필드를 유지해야 GC되지 않습니다.
        keyCallback = GLFWKeyCallback.create((window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_PRESS) {
                glfwSetWindowShouldClose(window, true);
            }
        });
        glfwSetKeyCallback(handle, keyCallback);

        framebufferSizeCallback = GLFWFramebufferSizeCallback.create((window, width, height) -> {
            framebufferWidth = width;
            framebufferHeight = height;
            glViewport(0, 0, width, height);
        });
        glfwSetFramebufferSizeCallback(handle, framebufferSizeCallback);

        glfwShowWindow(handle);
        System.out.println("OpenGL " + glGetString(GL_VERSION));
        System.out.println("GPU " + glGetString(GL_RENDERER));
        System.out.println("ESC 키로 창을 닫습니다.");
    }

    /** 창이 만들어진 뒤에 호출할 수 있습니다. 스크린샷처럼 수직 동기를 끄고 싶을 때 씁니다. */
    public void setVsync(boolean enabled) {
        this.vsync = enabled;
        if (handle != NULL) {
            glfwSwapInterval(enabled ? 1 : 0);
        }
    }

    public void pollEvents() {
        glfwPollEvents();
    }

    public void swapBuffers() {
        glfwSwapBuffers(handle);
    }

    public boolean shouldClose() {
        return glfwWindowShouldClose(handle);
    }

    public void requestClose() {
        glfwSetWindowShouldClose(handle, true);
    }

    public int framebufferWidth() {
        return framebufferWidth;
    }

    public int framebufferHeight() {
        return framebufferHeight;
    }

    /** 창이 최소화되면 높이가 0이 될 수 있습니다. 그릴 때 나눗셈에 쓰기 전에 확인합니다. */
    public float aspectRatio() {
        if (framebufferHeight <= 0) {
            return 1f;
        }
        return (float) framebufferWidth / (float) framebufferHeight;
    }

    public void destroy() {
        if (handle != NULL) {
            glfwDestroyWindow(handle);
            handle = NULL;
        }
        if (keyCallback != null) {
            keyCallback.free();
            keyCallback = null;
        }
        if (framebufferSizeCallback != null) {
            framebufferSizeCallback.free();
            framebufferSizeCallback = null;
        }
        if (glfwReady) {
            glfwTerminate();
            glfwReady = false;
        }
        if (errorCallback != null) {
            errorCallback.free();
            errorCallback = null;
        }
    }

    private long createWindow(int samples) {
        glfwWindowHint(GLFW_SAMPLES, samples);
        return glfwCreateWindow(windowWidth, windowHeight, title, NULL, NULL);
    }
}
