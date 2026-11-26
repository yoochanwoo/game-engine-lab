# Game Engine Lab

직접 만들어 가는 작은 3D 게임 엔진입니다. 최종 목표는 PC용 1인칭 로우폴리 탐험 게임입니다. NPC와 대화하며 돌아다니는, ENA: Dream BBQ 같은 분위기를 지향합니다. Windows가 기본이고, macOS와 Linux에서도 같은 방법으로 실행할 수 있게 맞춰 두었습니다.

지금은 **2단계**입니다. 1단계의 씬을 눈높이(1.7m)의 1인칭 카메라로 걸어 다닙니다. 높이는 바뀌지 않고, 마우스로 고개를 돌립니다.

## 준비물

- **JDK 17 이상** (17 또는 21 모두 됩니다)
- 별도의 Gradle 설치는 필요 없습니다. 이 저장소의 Gradle Wrapper가 빌드를 대신합니다.
- OpenGL 3.3을 지원하는 그래픽 드라이버. 최근 10년 안의 내장 그래픽이면 충분합니다.

설치 확인:

```bash
java -version
```

`17` 또는 그 이상이면 됩니다.

## 실행

저장소 루트에서 실행합니다.

Windows:

```bat
gradlew.bat run
```

macOS / Linux:

```bash
./gradlew run
```

처음 실행하면 LWJGL과 JOML을 내려받느라 조금 걸립니다. 창이 뜨면 커서가 숨겨지고, 바로 걸어 다닐 수 있습니다. 창의 X 버튼으로도 닫힙니다.

가능하면 위의 Gradle 명령으로 실행하세요. macOS에서는 GLFW가 메인 스레드에서 창을 열어야 하는데, `run` 작업이 그 옵션(`-XstartOnFirstThread`)을 넣어 줍니다. VS Code 편집기의 Run 버튼은 이 옵션 없이 실행될 수 있습니다.

## 조작

시작하는 위치는 1단계 카메라와 같습니다. 상자들이 모인 쪽을 살짝 내려다보고 있습니다.

| 입력 | 동작 |
| --- | --- |
| W A S D | 바라보는 방향의 수평으로 이동합니다. 위를 봐도 떠오르지 않고, 높이는 1.7m입니다. |
| Shift | 더 빠르게 걷습니다. 왼쪽과 오른쪽 Shift 모두 됩니다. |
| 마우스 | 좌우는 yaw, 위아래는 pitch입니다. pitch는 약 ±89°에서 멈춥니다. |
| ESC | 커서를 풀어 창 밖으로 나갈 수 있습니다. 이때는 이동과 시점 변경이 멈춥니다. |
| ESC (커서가 풀린 상태) | 종료합니다. |
| 왼쪽 클릭 (커서가 풀린 상태) | 커서를 다시 잡고 플레이를 이어 갑니다. |

다른 창으로 전환하면 커서도 풀립니다. 게임 창을 다시 클릭하면 잡힙니다.

화면이 없는 환경에서 그린 결과를 파일로 남기려면:

```bash
./gradlew run --args="--screenshot scene.png"
```

시작 시점의 화면을 저장하고 종료합니다. `--rehearse`를 붙이면, 저장 전에 W+A와 마우스 이동을 짧게 재현해서 걸어서 돌아본 장면을 찍습니다. 플레이할 때는 필요 없습니다.

```bash
./gradlew run --args="--screenshot scene.png --rehearse"
```

## VS Code에서 열기

1. VS Code에서 **File → Open Folder**로 이 폴더를 엽니다.
2. 추천 확장을 설치합니다. 폴더를 열면 오른쪽 아래에 안내가 뜨고, `.vscode/extensions.json`에도 적혀 있습니다.
   - [Extension Pack for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack) (언어 지원, 디버거, Maven/Gradle 프로젝트 인식)
   - [Gradle for Java](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-gradle) (Gradle 작업 목록)
3. 프로젝트를 신뢰(Trust)하고, Gradle import가 끝날 때까지 기다립니다.
4. 터미널을 열고 `gradlew.bat run` (macOS/Linux는 `./gradlew run`)을 실행합니다.

JDK를 여러 개 설치했다면, VS Code의 `Java: Configure Java Runtime`에서 17 이상을 고르면 됩니다.

## 폴더 구조

```
src/main/java
  game/            데모 게임. 씬을 만들고 엔진을 시작합니다.
  engine/core/     창과 게임 루프.
  engine/input/    키보드, 마우스, 커서 잡기.
  engine/render/   셰이더, 메시, 카메라, 실제로 그리는 코드.
  engine/scene/    물체, 위치, 1인칭 조작.
src/main/resources/shaders
  basic.vert       정점 셰이더
  basic.frag       픽셀 셰이더
```

물체(`GameObject`)는 메시를 가리키기만 합니다. 같은 큐브 메시를 여러 상자가 공유하고, 위치와 색만 다릅니다. 3단계에서 `.glb`나 `.obj`를 읽으면 그 결과도 `Mesh`가 되고, 상자가 가리키는 메시만 바꾸면 됩니다. 그리는 코드는 그대로입니다.

## 클래스

| 클래스 | 하는 일 |
| --- | --- |
| `game.Main` | 프로그램 시작. 창과 데모 게임을 만들어 엔진에 넘깁니다. |
| `game.DemoGame` | 바닥과 상자 다섯 개를 배치하고, 1인칭 조작을 매 프레임 갱신합니다. |
| `engine.core.Window` | GLFW로 1280×720 창과 OpenGL 3.3 코어 컨텍스트를 엽니다. 크기가 바뀌면 뷰포트를 맞춥니다. |
| `engine.core.Engine` | 게임 루프. 지난 프레임 이후의 시간(delta)을 재고, update 다음 render를 반복합니다. |
| `engine.core.Game` | 게임이 구현하는 네 메서드: `init`, `update`, `render`, `destroy`. |
| `engine.input.Key` | 게임이 아는 키 이름. GLFW 번호는 여기 없습니다. |
| `engine.input.Input` | 콜백으로 모은 키·마우스 상태. 프레임마다 읽고, 프레임 끝에서 1회성 값을 지웁니다. |
| `engine.scene.FirstPersonController` | WASD, Shift, 마우스 시선을 카메라에 적용합니다. |
| `engine.render.Shader` | `resources/shaders`의 GLSL을 읽어 컴파일하고, 행렬과 색 유니폼을 넣습니다. |
| `engine.render.Mesh` | 정점 버퍼(VBO), 인덱스 버퍼(EBO), 정점 배열(VAO)을 GPU에 올립니다. |
| `engine.render.Primitives` | 코드로 만드는 큐브와 바닥. 면마다 노멀이 달라 각져 보입니다. |
| `engine.render.Camera` | 눈 위치와 yaw/pitch로 view 행렬을, 창 비율로 projection 행렬을 만듭니다. |
| `engine.render.Renderer` | 화면을 지우고, 방향광을 넣은 뒤 메시를 그립니다. |
| `engine.render.Screenshot` | `--screenshot`일 때만 쓰는 프레임 저장. |
| `engine.scene.Transform` | 위치, 회전(도), 크기를 model 행렬로 바꿉니다. |
| `engine.scene.GameObject` | 메시 하나, 변환 하나, 색 하나. |

## 한 프레임이 지나가는 길

1. GLFW가 그동안 쌓인 키와 마우스 이벤트를 콜백으로 넘기고, `Input`이 상태에 적습니다.
2. `Engine`이 이전 프레임과의 시간 차이를 초로 계산합니다.
3. `FirstPersonController`가 그 상태로 고개와 위치를 바꿉니다. 이동 거리 = 속력 × 초.
4. `Input.endFrame()`이 "방금 눌림"과 마우스 이동량을 지웁니다. 아직 누르고 있는 키는 남습니다.
5. `Renderer`가 색과 깊이 버퍼를 지우고, 창 비율로 projection을 다시 만듭니다.
6. 카메라의 view, projection과 빛의 방향을 셰이더에 넣습니다.
7. 물체마다 model 행렬과 색을 넣고 `Mesh`가 삼각형을 그립니다.
8. 정점 셰이더가 `projection * view * model * 위치`로 화면 좌표를 만들고, 프래그먼트 셰이더가 면의 노멀과 방향광으로 색을 정합니다.
9. 버퍼를 화면에 보여주고, 창이 닫힐 때까지 반복합니다.

## 시점과 입력

**yaw / pitch.** yaw는 수평으로 도는 각도입니다. 0도면 월드의 -Z를 보고, 마우스를 오른쪽으로 움직이면 +X 쪽으로 돕니다. pitch는 위아래입니다. 0도가 수평이고, 양수는 위를 봅니다. ±90도까지 가면 하늘이나 바닥만 남아 view 행렬이 깨지므로 약 ±89도에서 멈춥니다.

**view 행렬.** 눈의 위치에서 pitch와 yaw로 만든 방향(길이 1)을 더한 점을 `lookAt`의 목표로 둡니다. 물체의 model 행렬, 이 view, 창 비율로 만든 projection이 1단계와 같은 MVP입니다. 창을 늘리면 뷰포트와 projection의 가로세로 비가 같이 바뀌어 상자의 비율이 유지됩니다.

**delta time.** 속력은 초당 미터(걷기 4, Shift 7.5)입니다. 프레임이 0.016초면 약 6.4cm, 0.033초면 약 13cm를 갑니다. W와 A를 같이 눌러도 대각선이 더 빠르지 않게 방향을 길이 1로 맞춘 뒤에 곱합니다.

**콜백과 폴링.** 키를 누르는 순간은 콜백입니다. 그 콜백은 "W가 눌려 있다"만 기록하고 카메라를 움직이지 않습니다. 이동은 프레임에 한 번, 기록된 상태를 읽고 일어납니다. 방금 눌렸는지는 그 프레임에만 true이고, 프레임이 끝나면 지워집니다. 그래서 나중에 상호작용 키를 `Key`에 추가해도 같은 방식으로 한 번만 반응하게 할 수 있습니다.

OpenGL 관례대로 +Y가 위입니다. 시작할 때 카메라는 +Z 쪽에 서서 원점 쪽을 봅니다.

## 빌드만 확인

창을 열지 않고 컴파일만 하려면:

```bash
./gradlew build
```

Windows에서는 `gradlew.bat build`입니다.

## 창이 안 열릴 때

- `java -version`이 17 미만이면 JDK를 새로 설치합니다.
- "OpenGL 3.3" 오류가 나면 그래픽 드라이버를 업데이트합니다.
- macOS는 OpenGL을 더 이상 권장하지 않지만, 3.3 코어 프로파일은 아직 동작합니다. 반드시 `./gradlew run`으로 실행합니다.
- 첫 실행의 다운로드 오류는 네트워크 문제입니다. 같은 명령을 한 번 더 실행하면 이어서 받습니다.
