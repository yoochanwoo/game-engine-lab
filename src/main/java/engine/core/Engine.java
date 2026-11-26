package engine.core;

import engine.input.Input;
import engine.render.Screenshot;
import java.nio.file.Path;

/**
 * 창을 연 뒤, 게임이 끝날 때까지 update와 render를 반복합니다.
 *
 * <p>시간 간격은 가변(delta time)입니다. 한 프레임이 얼마나 걸렸는지를 초 단위로
 * 재서 {@link Game#update}에 넘깁니다. 창을 드래그하는 동안처럼 프레임이 길게
 * 멈추면 다음 스텝이 폭주하지 않도록 간격을 0.05초(20fps에 해당하는 한 프레임)로
 * 자릅니다. 1인칭 이동은 이 초에 속력(미터/초)을 곱해 거리를 정합니다.
 */
public final class Engine {

    private static final float MAX_DELTA_SECONDS = 0.05f;

    private final Window window;
    private final Game game;
    private final Path screenshotPath;

    public Engine(Window window, Game game) {
        this(window, game, null);
    }

    /**
     * @param screenshotPath null이 아니면 몇 프레임을 그린 뒤 PNG로 저장하고 종료합니다.
     *     화면이 없는 환경에서 렌더 결과를 확인할 때 씁니다. 보통 실행에서는 null입니다.
     */
    public Engine(Window window, Game game, Path screenshotPath) {
        this.window = window;
        this.game = game;
        this.screenshotPath = screenshotPath;
    }

    public void start() {
        window.init();
        if (screenshotPath != null) {
            window.setVsync(false);
        }
        Input input = new Input(window);
        try {
            game.init(window, input);
            loop(input);
        } finally {
            try {
                game.destroy();
            } finally {
                try {
                    input.destroy();
                } finally {
                    window.destroy();
                }
            }
        }
    }

    private void loop(Input input) {
        if (screenshotPath != null) {
            // OS 마우스 이벤트가 리허설로 옮겨 둔 고개를 돌리기 전에 저장합니다.
            // 첫 그리기가 비는 드라이버가 있어 두 번 그립니다.
            game.render();
            game.render();
            Screenshot.save(screenshotPath, window.framebufferWidth(), window.framebufferHeight());
            System.out.println("스크린샷 저장: " + screenshotPath.toAbsolutePath());
            return;
        }

        long previousNanos = System.nanoTime();

        while (!window.shouldClose()) {
            long now = System.nanoTime();
            float deltaSeconds = (now - previousNanos) / 1_000_000_000f;
            previousNanos = now;
            if (deltaSeconds > MAX_DELTA_SECONDS) {
                deltaSeconds = MAX_DELTA_SECONDS;
            }

            window.pollEvents();
            if (window.framebufferHeight() == 0) {
                // 최소화된 동안 쌓인 마우스 이동이, 창을 되돌리는 순간 고개를 확 돌리지 않게 비웁니다.
                input.endFrame();
                continue;
            }

            game.update(deltaSeconds);
            input.endFrame();
            game.render();
            window.swapBuffers();
        }
    }
}
