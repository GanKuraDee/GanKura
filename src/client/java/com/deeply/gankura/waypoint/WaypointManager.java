package com.deeply.gankura.waypoint;

import com.deeply.gankura.GanKura;
import com.deeply.gankura.data.GameState;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// ウェイポイントの読み書きと問い合わせ口。
// ModConfig(MoulConfig)とは別ファイルにしてあり、エリアやグループごと丸ごと持つ
public final class WaypointManager {

    private static final WaypointManager INSTANCE = new WaypointManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // エリアが読めないとき(SkyBlock外・遷移中)に使う置き場
    public static final String UNKNOWN_AREA = "Unknown";

    private final Path configPath = FabricLoader.getInstance().getConfigDir()
            .resolve("gankura").resolve("waypoints.json");
    private WaypointData data = new WaypointData();

    private WaypointManager() {
    }

    public static WaypointManager getInstance() {
        return INSTANCE;
    }

    public WaypointData data() {
        return data;
    }

    // 今いるエリア。タブリストの "Area: " 行から取った値をそのまま鍵に使う
    public static String currentArea() {
        if (!GameState.Server.isSkyblock()) return UNKNOWN_AREA;
        String area = GameState.Server.map;
        return area == null || area.isBlank() ? UNKNOWN_AREA : area;
    }

    public void load() {
        if (!Files.exists(configPath)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            WaypointData loaded = GSON.fromJson(reader, WaypointData.class);
            if (loaded != null) {
                loaded.sanitize();
                data = loaded;
            }
        } catch (IOException | RuntimeException e) {
            GanKura.LOGGER.error("Failed to read {}, falling back to defaults.", configPath, e);
        }
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            GanKura.LOGGER.error("Failed to write {}.", configPath, e);
        }
    }

    // 読み取り専用。書き換えるときは waypointsForEditing() を使う
    public List<Waypoint> waypoints(String area) {
        List<Waypoint> list = data.areas.get(area);
        return list == null ? Collections.emptyList() : Collections.unmodifiableList(list);
    }

    public List<Waypoint> waypointsForEditing(String area) {
        return data.waypointsOf(area);
    }

    // 1ブロックに置けるウェイポイントは1つだけ。グループが違っても重ねさせない
    public Waypoint findAt(String area, int x, int y, int z) {
        for (Waypoint waypoint : waypoints(area)) {
            if (waypoint.getX() == x && waypoint.getY() == y && waypoint.getZ() == z) {
                return waypoint;
            }
        }
        return null;
    }

    public Waypoint findAt(String area, BlockPos pos) {
        return findAt(area, pos.getX(), pos.getY(), pos.getZ());
    }

    public void add(String area, Waypoint waypoint) {
        data.waypointsOf(area).add(waypoint);
        save();
    }

    // 空になったエリアは残しておいても読みにくいだけなので消す
    public void pruneEmptyAreas() {
        data.areas.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    public List<String> areaNames() {
        return new ArrayList<>(data.areas.keySet());
    }

    // そのエリアの全グループ。既定グループを先頭に、作ったものと実際に使われているものを並べる
    public List<String> groups(String area) {
        List<String> groups = new ArrayList<>();
        groups.add(Waypoint.DEFAULT_GROUP);

        for (String group : data.groups.getOrDefault(area, List.of())) {
            if (!groups.contains(group)) groups.add(group);
        }

        for (Waypoint waypoint : waypoints(area)) {
            if (!groups.contains(waypoint.getGroup())) groups.add(waypoint.getGroup());
        }

        return groups;
    }

    // 中身が空のグループを先に作れるようにしておく。
    // 同じ名前が既にあるときは false
    public boolean addGroup(String area, String group) {
        if (groups(area).contains(group)) return false;

        data.groups.computeIfAbsent(area, key -> new ArrayList<>()).add(group);
        save();
        return true;
    }

    // グループ名の変更。中のウェイポイントも一緒に付いてくる。
    // 変更先の名前が既にある場合は、そのグループへ合流する
    public void renameGroup(String area, String group, String newName) {
        if (group.equals(Waypoint.DEFAULT_GROUP) || group.equals(newName) || newName.isEmpty()) return;

        List<String> groups = data.groups.computeIfAbsent(area, key -> new ArrayList<>());
        groups.remove(group);
        if (!groups.contains(newName)) groups.add(newName);

        for (Waypoint waypoint : waypointsForEditing(area)) {
            if (waypoint.getGroup().equals(group)) waypoint.setGroup(newName);
        }

        // 非表示にしていたなら、その状態も新しい名前へ引き継ぐ
        if (!data.isGroupEnabled(area, group)) {
            data.setGroupEnabled(area, group, true);
            data.setGroupEnabled(area, newName, false);
        }

        save();
    }

    // グループを消す。中のウェイポイントは既定グループへ戻すだけで、消えはしない
    public void removeGroup(String area, String group) {
        if (group.equals(Waypoint.DEFAULT_GROUP)) return;

        List<String> groups = data.groups.get(area);
        if (groups != null && groups.remove(group) && groups.isEmpty()) {
            data.groups.remove(area);
        }

        for (Waypoint waypoint : waypointsForEditing(area)) {
            if (waypoint.getGroup().equals(group)) waypoint.setGroup(Waypoint.DEFAULT_GROUP);
        }

        data.setGroupEnabled(area, group, true);
        save();
    }

    public List<Waypoint> waypointsOfGroup(String area, String group) {
        return waypoints(area).stream().filter(waypoint -> waypoint.getGroup().equals(group)).toList();
    }

    public boolean isGroupEnabled(String area, String group) {
        return data.isGroupEnabled(area, group);
    }

    public void setGroupEnabled(String area, String group, boolean enabled) {
        data.setGroupEnabled(area, group, enabled);
    }
}
