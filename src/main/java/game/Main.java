package game;

import engine.core.Engine;
import engine.core.Window;
import java.nio.file.Path;

/**
 * 프로그램 진입점입니다. 창과 데모 게임을 만들어 엔진에 넘깁니다.
 *
 * <p>개발용으로 {@code --screenshot 파일.png}를 주면, 씬을 그린 뒤 그 경로에 저장하고
 * 바로 종료합니다. 평소 실행에서는 인자가 없고, ESC를 누를 때까지 창이 유지됩니다.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Path screenshot = parseScreenshotPath(args);
        Window window = new Window("Game Engine Lab", 1280, 720);
        Engine engine = new Engine(window, new DemoGame(), screenshot);
        engine.start();
    }

    private static Path parseScreenshotPath(String[] args) {
        Path screenshot = null;
        for (int i = 0; i < args.length; i++) {
            if (!"--screenshot".equals(args[i])) {
                System.err.println("알 수 없는 인자: " + args[i]);
                System.err.println("사용법: ./gradlew run --args=\"--screenshot scene.png\"");
                System.exit(1);
            }
            if (i + 1 >= args.length) {
                System.err.println("--screenshot 뒤에 PNG 경로가 필요합니다.");
                System.exit(1);
            }
            screenshot = Path.of(args[++i]);
        }
        return screenshot;
    }
}
