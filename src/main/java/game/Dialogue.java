package game;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import engine.input.Input;
import engine.input.Key;
import engine.scene.FirstPersonController;
import engine.scene.GameObject;
import engine.ui.Font;
import engine.ui.Ui;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** JSON 대사. 줄, 선택지, 한 번 말한 뒤 바뀌는 플래그. */
public final class Dialogue {

    private static final float CHARS_PER_SECOND = 32f;
    private static final float REACH = 2.6f;

    private final Map<String, Script> scripts;
    private final Set<String> flags = new HashSet<>();
    private final Ui ui;
    private final Font font;

    private Npc active;
    private String nodeId;
    private int lineIndex;
    private float shown;
    private boolean choosing;
    private boolean recapture;
    private float pointerX;
    private float pointerY;

    public Dialogue(Ui ui, Font font) {
        this.ui = ui;
        this.font = font;
        this.scripts = load();
    }

    public Script script(String id) {
        Script script = scripts.get(id);
        if (script == null) {
            throw new IllegalStateException("대사가 없습니다: " + id);
        }
        return script;
    }

    public void update(
            float dt,
            Input input,
            FirstPersonController player,
            List<Npc> npcs,
            int viewW,
            int viewH,
            float mouseX,
            float mouseY) {
        this.pointerX = mouseX;
        this.pointerY = mouseY;
        if (active != null) {
            shown += dt * CHARS_PER_SECOND;
            player.setControlsEnabled(false);
            boolean choices = choosing && current().choices != null && !current().choices.isEmpty();
            player.setCaptureSuspended(choices);
            if (choices && input.isCursorCaptured()) {
                input.releaseCursor();
            }
            if (input.wasPressed(Key.ESCAPE)) {
                input.consumePress(Key.ESCAPE);
                close(input, player);
                return;
            }
            if (choices) {
                handleChoices(input, viewW, viewH);
                return;
            }
            if (pressedAdvance(input)) {
                advance(input, player);
            }
            return;
        }
        player.setControlsEnabled(true);
        player.setCaptureSuspended(false);
        Npc looked = lookingAt(player, npcs);
        if (looked != null && input.wasPressed(Key.E)) {
            input.consumePress(Key.E);
            open(looked, input);
        }
    }

    public void render(Input input, FirstPersonController player, List<Npc> npcs, int viewW, int viewH, float mouseX, float mouseY) {
        if (active == null) {
            Npc looked = lookingAt(player, npcs);
            if (looked != null) {
                String prompt = "E  말하기";
                float width = font.width(prompt);
                ui.text(font, prompt, (viewW - width) * 0.5f, viewH - 220f, 1f, 0.95f, 0.8f);
            }
            return;
        }
        float x = 36f;
        float y = viewH - 210f;
        float w = viewW - 72f;
        float h = 180f;
        ui.panel(x, y, w, h, 0.08f, 0.09f, 0.12f, 0.88f);
        ui.text(font, active.script.name, x + 28f, y + 18f, 1f, 0.82f, 0.45f);
        String line = visibleLine();
        ui.text(font, line, x + 28f, y + 62f, 0.95f, 0.95f, 0.93f);
        if (choosing && current().choices != null) {
            float cy = y + 112f;
            int index = 0;
            for (Choice choice : current().choices) {
                String label = (index + 1) + ". " + choice.text;
                float bw = font.width(label) + 28f;
                boolean hot = ui.hit(x + 28f, cy, bw, 32f, mouseX, mouseY);
                ui.text(font, label, x + 28f, cy, hot ? 1f : 0.8f, hot ? 0.95f : 0.86f, hot ? 0.55f : 0.75f);
                cy += 34f;
                index++;
            }
        }
    }

    public void debugOpen(Npc npc) {
        open(npc, null);
        shown = 1000f;
    }

    private void open(Npc npc, Input input) {
        active = npc;
        Script script = npc.script;
        nodeId = flags.contains(script.met) && script.again != null ? script.again : script.first;
        lineIndex = 0;
        shown = 0f;
        choosing = false;
        recapture = input != null && input.isCursorCaptured();
    }

    private void close(Input input, FirstPersonController player) {
        Script script = active.script;
        if (script.met != null) {
            flags.add(script.met);
        }
        active = null;
        choosing = false;
        player.setControlsEnabled(true);
        player.setCaptureSuspended(false);
        if (recapture) {
            input.captureCursor();
        }
    }

    private void advance(Input input, FirstPersonController player) {
        Node node = current();
        String line = node.lines.get(lineIndex);
        if (shown < line.length()) {
            shown = line.length();
            return;
        }
        if (lineIndex + 1 < node.lines.size()) {
            lineIndex++;
            shown = 0f;
            return;
        }
        if (node.choices != null && !node.choices.isEmpty()) {
            choosing = true;
            return;
        }
        if (node.next != null) {
            nodeId = node.next;
            lineIndex = 0;
            shown = 0f;
            return;
        }
        close(input, player);
    }

    private void handleChoices(Input input, int viewW, int viewH) {
        if (input.wasPressed(Key.E) || input.wasPressed(Key.SPACE)) {
            return;
        }
        if (!input.wasMouseLeftPressed()) {
            return;
        }
        float x = 64f;
        float y = viewH - 210f + 112f;
        int index = 0;
        for (Choice choice : current().choices) {
            String label = (index + 1) + ". " + choice.text;
            float bw = font.width(label) + 28f;
            if (ui.hit(x, y, bw, 32f, pointerX, pointerY)) {
                input.consumeMouseClick();
                choose(choice);
                return;
            }
            y += 34f;
            index++;
        }
    }

    private void choose(Choice choice) {
        choosing = false;
        nodeId = choice.next;
        lineIndex = 0;
        shown = 0f;
    }

    private boolean pressedAdvance(Input input) {
        if (input.wasPressed(Key.E)) {
            input.consumePress(Key.E);
            return true;
        }
        if (input.wasPressed(Key.SPACE)) {
            input.consumePress(Key.SPACE);
            return true;
        }
        if (input.wasMouseLeftPressed()) {
            input.consumeMouseClick();
            return true;
        }
        return false;
    }

    private String visibleLine() {
        String line = current().lines.get(lineIndex);
        int count = Math.min(line.length(), (int) shown);
        return line.substring(0, count);
    }

    private Node current() {
        Node node = active.script.nodes.get(nodeId);
        if (node == null) {
            throw new IllegalStateException("대사 노드가 없습니다: " + nodeId);
        }
        return node;
    }

    private Npc lookingAt(FirstPersonController player, List<Npc> npcs) {
        float yaw = (float) Math.toRadians(player.yawDegrees());
        float forwardX = (float) Math.sin(yaw);
        float forwardZ = (float) -Math.cos(yaw);
        Npc best = null;
        float bestDist = REACH;
        for (Npc npc : npcs) {
            float dx = npc.object.transform().position().x - player.x();
            float dz = npc.object.transform().position().z - player.z();
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            if (dist > bestDist || dist < 0.001f) {
                continue;
            }
            float dot = (dx / dist) * forwardX + (dz / dist) * forwardZ;
            if (dot > 0.55f) {
                best = npc;
                bestDist = dist;
            }
        }
        return best;
    }

    private static Map<String, Script> load() {
        try (InputStream in = Dialogue.class.getResourceAsStream("/dialogue/npcs.json")) {
            if (in == null) {
                throw new IllegalStateException("dialogue/npcs.json 이 없습니다.");
            }
            return new Gson().fromJson(
                    new InputStreamReader(in, StandardCharsets.UTF_8),
                    new TypeToken<Map<String, Script>>() {}.getType());
        } catch (Exception exception) {
            throw new IllegalStateException("대사를 읽지 못했습니다.", exception);
        }
    }

    public static final class Script {
        public String name;
        public String first;
        public String again;
        public String met;
        public Map<String, Node> nodes;
    }

    public static final class Node {
        public List<String> lines;
        public String next;
        public List<Choice> choices;
    }

    public static final class Choice {
        public String text;
        public String next;
    }

    public static final class Npc {
        public final String id;
        public final Script script;
        public final GameObject object;

        public Npc(String id, Script script, GameObject object) {
            this.id = id;
            this.script = script;
            this.object = object;
        }

        public void face(float playerX, float playerZ) {
            float dx = playerX - object.transform().position().x;
            float dz = playerZ - object.transform().position().z;
            float yaw = (float) Math.toDegrees(Math.atan2(dx, -dz));
            object.transform().rotation().y = yaw;
        }
    }
}
