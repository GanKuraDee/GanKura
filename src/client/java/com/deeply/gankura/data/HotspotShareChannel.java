package com.deeply.gankura.data;

/** Hotspot の座標を流すチャット */
public enum HotspotShareChannel {
    PARTY("Party", "pc"),
    COOP("Co-op", "cc"),
    GUILD("Guild", "gc"),
    ALL("All", "ac");

    private final String label;
    private final String command;

    HotspotShareChannel(String label, String command) {
        this.label = label;
        this.command = command;
    }

    public String command() {
        return command;
    }

    @Override
    public String toString() {
        return label;
    }
}
