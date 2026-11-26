package game;

import engine.core.Engine;
import engine.core.Window;
import java.nio.file.Path;

/**
 * 프로그램 진입점입니다. 창과 데모 게임을 만들어 엔진에 넘깁니다.
 *
 * <p>개발용으로 {@code --screenshot 파일.png}를 주면, 씬을 그린 뒤 그 경로에 저장하고
 * 바로 종료합니다. {@code --rehearse}를 같이 주면 저장 전에 앞으로 걷고 고개를
 * 돌립니다. 평소 실행에서는 인자가 없고, 커서를 두 번 ESC 할 때까지 창이 유지됩니다.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Launch launch = parse(args);
        Window window = new Window("Game Engine Lab", 1280, 720);
        Engine engine = new Engine(window, new DemoGame(launch.rehearse), launch.screenshot);
        engine.start();
    }

    private static Launch parse(String[] args) {
        Launch launch = new Launch();
        for (int i = 0; i < args.length; i++) {
            if ("--screenshot".equals(args[i])) {
                if (i + 1 >= args.length) {
                    System.err.println("--screenshot 뒤에 PNG 경로가 필요합니다.");
                    usage();
                }
                launch.screenshot = Path.of(args[++i]);
            } else if ("--rehearse".equals(args[i])) {
                launch.rehearse = true;
            } else {
                System.err.println("알 수 없는 인자: " + args[i]);
                usage();
            }
        }
        return launch;
    }

    private static void usage() {
        System.err.println("사용법: ./gradlew run --args=\"[--screenshot 파일.png] [--rehearse]\"");
        System.exit(1);
    }

    private static final class Launch {
        private Path screenshot;
        private boolean rehearse;
    }
}
