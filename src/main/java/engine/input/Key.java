package engine.input;

/**
 * 게임이 아는 키입니다. GLFW의 숫자 코드는 {@link Input} 안에만 둡니다.
 *
 * <p>NPC와 대화하는 키처럼 입력이 늘면 여기에 상수를 추가하고, {@code Input}에서
 * 실제 키와 연결하면 됩니다. 이동이나 충돌 코드는 이 이름만 보면 됩니다.
 */
public enum Key {
    W,
    A,
    S,
    D,
    /** 왼쪽 Shift와 오른쪽 Shift 둘 다입니다. */
    SHIFT,
    ESCAPE
}
