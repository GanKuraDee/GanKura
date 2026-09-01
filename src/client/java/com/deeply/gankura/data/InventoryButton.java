package com.deeply.gankura.data;

import com.google.gson.annotations.Expose;

/**
 * 収納画面の周りに置くボタン1つ分。
 *
 * NotEnoughUpdates の Inventory Buttons と同じ持ち物にしてあるので、
 * あちらの共有コードやプリセットをそのまま読み込める
 */
public class InventoryButton {

    // 画面の左上(または anchor で指定した角)から数えた位置
    @Expose
    public int x;
    @Expose
    public int y;

    // プレイヤーの持ち物画面にだけ出すか
    @Expose
    public boolean playerInvOnly;

    // 位置の起点。true なら画面の右端・下端から数える。
    // 幅の違う画面でも同じ見た目になるようにするための仕組み
    @Expose
    public boolean anchorRight;
    @Expose
    public boolean anchorBottom;

    // 枠の絵柄(0〜4)
    @Expose
    public int backgroundIndex;

    @Expose
    public String command = "";
    @Expose
    public String icon = "";

    // Gson がこの引数なしのコンストラクタを使うので、既定値がそのまま入る
    public InventoryButton() {
    }

    public InventoryButton(int x, int y, String icon, boolean playerInvOnly,
                           boolean anchorRight, boolean anchorBottom, int backgroundIndex, String command) {
        this.x = x;
        this.y = y;
        this.icon = icon;
        this.playerInvOnly = playerInvOnly;
        this.anchorRight = anchorRight;
        this.anchorBottom = anchorBottom;
        this.backgroundIndex = backgroundIndex;
        this.command = command;
    }

    // コマンドの無いボタンは「置き場所だけ決まった空き枠」。エディタにしか出さない
    public boolean isActive() {
        return command != null && !command.trim().isEmpty();
    }

    public String commandOrEmpty() {
        return command == null ? "" : command;
    }

    public String iconOrEmpty() {
        return icon == null ? "" : icon;
    }

    // プリセットは使い回すので、読み込むときは複製して元を汚さないようにする
    public InventoryButton copy() {
        return new InventoryButton(x, y, iconOrEmpty(), playerInvOnly,
                anchorRight, anchorBottom, backgroundIndex, commandOrEmpty());
    }
}
