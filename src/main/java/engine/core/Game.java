package engine.core;

/**
 * 엔진이 매 프레임 부르는 게임 쪽 진입점입니다.
 *
 * <p>창을 만들고 루프를 도는 일은 {@link Engine}이 맡고, 씬을 구성하고 그리는 일은
 * 이 인터페이스의 구현이 맡습니다. 지금은 {@code game.DemoGame} 하나만 있습니다.
 */
public interface Game {

    /** OpenGL 컨텍스트가 준비된 뒤 한 번 호출됩니다. 메시와 셰이더를 여기서 만듭니다. */
    void init(Window window);

    /**
     * 시뮬레이션을 한 번 진행합니다.
     *
     * @param deltaSeconds 이전 프레임 이후 지난 시간(초). 긴 멈춤은 엔진이 상한을 씌웁니다.
     */
    void update(float deltaSeconds);

    /** 현재 상태로 화면을 한 장 그립니다. */
    void render();

    /** 루프가 끝난 뒤 GPU 자원을 해제합니다. 창이 닫히기 전에 호출됩니다. */
    void destroy();
}
