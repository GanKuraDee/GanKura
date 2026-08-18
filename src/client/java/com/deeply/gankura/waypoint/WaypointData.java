package com.deeply.gankura.waypoint;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// waypoints.json にそのまま書き出される中身。
// ウェイポイントはエリア名(タブリストの "Area: " 行)ごとに分けて持つ
public class WaypointData {

    // 表示そのもののON/OFF
    public boolean enabled = true;
    // ブロックの上に名前を出すかどうか
    public boolean showNames = true;
    // エリア名 -> そのエリアのウェイポイント
    public Map<String, List<Waypoint>> areas = new LinkedHashMap<>();
    // エリア名 -> 非表示にしたグループ
    public Map<String, Set<String>> disabledGroups = new LinkedHashMap<>();
    // エリア名 -> 作ったグループ。まだ1つもウェイポイントが無くても消さずに残す
    public Map<String, List<String>> groups = new LinkedHashMap<>();

    public List<Waypoint> waypointsOf(String area) {
        return areas.computeIfAbsent(area, key -> new ArrayList<>());
    }

    public boolean isGroupEnabled(String area, String group) {
        Set<String> disabled = disabledGroups.get(area);
        return disabled == null || !disabled.contains(group);
    }

    public void setGroupEnabled(String area, String group, boolean enabled) {
        if (enabled) {
            Set<String> disabled = disabledGroups.get(area);
            if (disabled != null && disabled.remove(group) && disabled.isEmpty()) {
                disabledGroups.remove(area);
            }
        } else {
            disabledGroups.computeIfAbsent(area, key -> new LinkedHashSet<>()).add(group);
        }
    }

    // 手で編集されたJSONや古い形式でも落ちないよう、null をここで潰しておく
    public void sanitize() {
        if (areas == null) {
            areas = new LinkedHashMap<>();
        } else {
            areas.values().removeIf(list -> list == null);
            for (List<Waypoint> list : areas.values()) {
                list.removeIf(waypoint -> waypoint == null);
            }
        }

        if (disabledGroups == null) {
            disabledGroups = new LinkedHashMap<>();
        } else {
            disabledGroups.values().removeIf(disabled -> disabled == null);
        }

        if (groups == null) {
            groups = new LinkedHashMap<>();
        } else {
            groups.values().removeIf(names -> names == null);
        }
    }
}
