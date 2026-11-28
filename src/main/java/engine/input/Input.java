package engine.input;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_A;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_D;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_E;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LAST;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_S;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_W;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.glfwSetCursorPosCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetMouseButtonCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowFocusCallback;

import engine.core.Window;
import java.util.Arrays;
import org.lwjgl.glfw.GLFWCursorPosCallback;
import org.lwjgl.glfw.GLFWKeyCallback;
import org.lwjgl.glfw.GLFWMouseButtonCallback;
import org.lwjgl.glfw.GLFWWindowFocusCallback;

/**
 * 키보드와 마우스의 현재 상태입니다.
 *
 * <p>GLFW는 키가 눌리거나 마우스가 움직일 때 콜백을 호출합니다. 그 순간 게임을
 * 움직이지 않고, 여기 배열에 "눌려 있다"만 적어 둡니다. 게임 루프는 프레임마다
 * 이 상태를 읽습니다(폴링). 콜백 안에서 카메라를 옮기면, 한 프레임에 콜백이
 * 여러 번 올 때 이동이 들쭉날쭉해집니다.
 *
 * <p>{@link #endFrame()}이 한 프레임만 유효한 값(방금 눌림, 마우스 이동량)을
 * 지웁니다. 누르고 있는 키는 떼기 전까지 유지됩니다.
 */
public final class Input {

    private final Window window;
    private final boolean[] down = new boolean[GLFW_KEY_LAST + 1];
    private final boolean[] pressed = new boolean[GLFW_KEY_LAST + 1];

    private boolean mouseLeftDown;
    private boolean mouseLeftPressed;
    private float mouseDeltaX;
    private float mouseDeltaY;
    private float mouseX;
    private float mouseY;
    private boolean cursorCaptured;

    private double lastCursorX;
    private double lastCursorY;
    private boolean haveCursor;

    private GLFWKeyCallback keyCallback;
    private GLFWMouseButtonCallback mouseButtonCallback;
    private GLFWCursorPosCallback cursorPosCallback;
    private GLFWWindowFocusCallback focusCallback;

    public Input(Window window) {
        this.window = window;
        long handle = window.handle();

        // 콜백 객체는 필드에 두어야 가비지 컬렉션되지 않습니다. Window와 같은 이유입니다.
        keyCallback = GLFWKeyCallback.create((win, key, scancode, action, mods) -> onKey(key, action));
        mouseButtonCallback = GLFWMouseButtonCallback.create(
                (win, button, action, mods) -> onMouseButton(button, action));
        cursorPosCallback = GLFWCursorPosCallback.create((win, xpos, ypos) -> onCursor(xpos, ypos));
        focusCallback = GLFWWindowFocusCallback.create((win, focused) -> {
            // 알트+탭처럼 창을 벗어나면 커서가 잡힌 채로 남지 않게 풉니다.
            if (!focused) {
                releaseCursor();
            }
        });

        glfwSetKeyCallback(handle, keyCallback);
        glfwSetMouseButtonCallback(handle, mouseButtonCallback);
        glfwSetCursorPosCallback(handle, cursorPosCallback);
        glfwSetWindowFocusCallback(handle, focusCallback);

        captureCursor();
    }

    public boolean isDown(Key key) {
        if (key == Key.SHIFT) {
            return down[GLFW_KEY_LEFT_SHIFT] || down[GLFW_KEY_RIGHT_SHIFT];
        }
        return down[code(key)];
    }

    /** 이번 프레임에 처음 눌렸으면 true입니다. 누르고 있는 동안 반복되지 않습니다. */
    public boolean wasPressed(Key key) {
        if (key == Key.SHIFT) {
            return pressed[GLFW_KEY_LEFT_SHIFT] || pressed[GLFW_KEY_RIGHT_SHIFT];
        }
        return pressed[code(key)];
    }

    public boolean isMouseLeftDown() {
        return mouseLeftDown;
    }

    public boolean wasMouseLeftPressed() {
        return mouseLeftPressed;
    }

    /** 지난 {@link #endFrame()} 이후 커서가 움직인 픽셀. 오른쪽이 양수입니다. */
    public float mouseDeltaX() {
        return mouseDeltaX;
    }

    /** 지난 {@link #endFrame()} 이후 커서가 움직인 픽셀. 아래쪽이 양수입니다. */
    public float mouseDeltaY() {
        return mouseDeltaY;
    }

    public float mouseX() {
        return mouseX;
    }

    public float mouseY() {
        return mouseY;
    }

    public void consumePress(Key key) {
        int glfwKey = key == Key.SHIFT ? GLFW_KEY_LEFT_SHIFT : code(key);
        pressed[glfwKey] = false;
        if (key == Key.SHIFT) {
            pressed[GLFW_KEY_RIGHT_SHIFT] = false;
        }
    }

    public void consumeMouseClick() {
        mouseLeftPressed = false;
    }

    public boolean isCursorCaptured() {
        return cursorCaptured;
    }

    /** 커서를 숨기고 창에 가둡니다. 마우스를 끝까지 밀어도 시점이 계속 돕니다. */
    public void captureCursor() {
        window.setCursorCaptured(true);
        cursorCaptured = true;
        // 잡는 순간 커서 위치가 튀면 카메라가 한 번에 돌아갑니다. 다음 이벤트는 기준점만 잡습니다.
        haveCursor = false;
        mouseDeltaX = 0f;
        mouseDeltaY = 0f;
    }

    /** 커서를 다시 보이게 합니다. 이 상태에서는 시점과 이동을 바꾸지 않습니다. */
    public void releaseCursor() {
        if (!cursorCaptured) {
            return;
        }
        window.setCursorCaptured(false);
        cursorCaptured = false;
        haveCursor = false;
        mouseDeltaX = 0f;
        mouseDeltaY = 0f;
    }

    /**
     * 키를 누른 것처럼 상태를 바꿉니다. 화면이 없는 환경에서 같은 이동 코드를
     * 태울 때 씁니다. 콜백을 거치지 않습니다.
     */
    public void debugSetDown(Key key, boolean held) {
        int glfwKey = key == Key.SHIFT ? GLFW_KEY_LEFT_SHIFT : code(key);
        if (held && !down[glfwKey]) {
            pressed[glfwKey] = true;
        }
        down[glfwKey] = held;
    }

    /** 이번 프레임 마우스 이동에 픽셀을 더합니다. {@link #mouseDeltaX()}와 같은 방향입니다. */
    public void debugAddMouseDelta(float dx, float dy) {
        mouseDeltaX += dx;
        mouseDeltaY += dy;
    }

    /** 프레임이 끝날 때 엔진이 호출합니다. 누르고 있는 키는 그대로 둡니다. */
    public void endFrame() {
        Arrays.fill(pressed, false);
        mouseLeftPressed = false;
        mouseDeltaX = 0f;
        mouseDeltaY = 0f;
    }

    public void destroy() {
        long handle = window.handle();
        if (handle != 0L) {
            glfwSetKeyCallback(handle, null);
            glfwSetMouseButtonCallback(handle, null);
            glfwSetCursorPosCallback(handle, null);
            glfwSetWindowFocusCallback(handle, null);
            if (cursorCaptured) {
                window.setCursorCaptured(false);
                cursorCaptured = false;
            }
        }
        if (keyCallback != null) {
            keyCallback.free();
            keyCallback = null;
        }
        if (mouseButtonCallback != null) {
            mouseButtonCallback.free();
            mouseButtonCallback = null;
        }
        if (cursorPosCallback != null) {
            cursorPosCallback.free();
            cursorPosCallback = null;
        }
        if (focusCallback != null) {
            focusCallback.free();
            focusCallback = null;
        }
    }

    private void onKey(int key, int action) {
        if (key < 0 || key > GLFW_KEY_LAST) {
            return;
        }
        if (action == GLFW_PRESS) {
            down[key] = true;
            pressed[key] = true;
        } else if (action == GLFW_RELEASE) {
            down[key] = false;
        }
        // GLFW_REPEAT는 누르고 있다는 뜻이 이미 down에 있으므로 무시합니다.
    }

    private void onMouseButton(int button, int action) {
        if (button != GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }
        if (action == GLFW_PRESS) {
            mouseLeftDown = true;
            mouseLeftPressed = true;
        } else if (action == GLFW_RELEASE) {
            mouseLeftDown = false;
        }
    }

    private void onCursor(double x, double y) {
        if (!haveCursor) {
            haveCursor = true;
            lastCursorX = x;
            lastCursorY = y;
            return;
        }
        float dx = (float) (x - lastCursorX);
        float dy = (float) (y - lastCursorY);
        lastCursorX = x;
        lastCursorY = y;
        mouseX = (float) x;
        mouseY = (float) y;
        if (cursorCaptured) {
            mouseDeltaX += dx;
            mouseDeltaY += dy;
        }
    }

    private static int code(Key key) {
        switch (key) {
            case W:
                return GLFW_KEY_W;
            case A:
                return GLFW_KEY_A;
            case S:
                return GLFW_KEY_S;
            case D:
                return GLFW_KEY_D;
            case SHIFT:
                return GLFW_KEY_LEFT_SHIFT;
            case SPACE:
                return GLFW_KEY_SPACE;
            case E:
                return GLFW_KEY_E;
            case ESCAPE:
                return GLFW_KEY_ESCAPE;
            default:
                throw new IllegalArgumentException("키가 연결되지 않았습니다: " + key);
        }
    }
}
