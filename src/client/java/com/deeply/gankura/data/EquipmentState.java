package com.deeply.gankura.data;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

// Hypixel Skyblock の "/equipment" (Your Equipment and Stats) メニューでスキャンした装備を保持・永続化する。
// アイテムのNBTはワールドのレジストリ情報が無いと復元できないため、起動直後は生のSNBT文字列だけを保持し、
// ワールド参加時 (レジストリアクセスが手に入るタイミング) に hydrate() で実際のItemStackへ変換する。
public class EquipmentState {
    private static final Logger LOGGER = LoggerFactory.getLogger("GankuraEquipmentState");
    private static final File CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("gankura")
            .resolve("gankura_equipment.properties")
            .toFile();

    public static List<ItemStack> items = new ArrayList<>();

    private static List<String> pendingSnbt = null;
    private static boolean hydrated = false;

    static {
        loadRaw();
    }

    private static void loadRaw() {
        if (!CONFIG_FILE.exists()) return;

        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            properties.load(in);
            int count = Integer.parseInt(properties.getProperty("count", "0"));
            List<String> raw = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                String snbt = properties.getProperty("item" + i);
                if (snbt != null && !snbt.isEmpty()) raw.add(snbt);
            }
            pendingSnbt = raw;
        } catch (IOException | NumberFormatException e) {
            LOGGER.error("Failed to load saved equipment", e);
        }
    }

    // ワールド参加時に一度だけ呼び出し、保存されていたSNBTを実際のItemStackへ復元する
    public static void hydrate(RegistryWrapper.WrapperLookup registries) {
        if (hydrated || pendingSnbt == null) return;
        hydrated = true;

        List<ItemStack> loaded = new ArrayList<>();
        RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, registries);
        for (String snbt : pendingSnbt) {
            try {
                NbtCompound tag = StringNbtReader.readCompound(snbt);
                ItemStack.OPTIONAL_CODEC.parse(ops, tag).result().ifPresent(loaded::add);
            } catch (CommandSyntaxException e) {
                LOGGER.error("Failed to parse saved equipment item", e);
            }
        }
        pendingSnbt = null;

        // 今セッション中に既に /equipment がスキャンされていた場合はそちらを優先する
        if (items.isEmpty()) {
            items = loaded;
        }
    }

    // ゲーム終了時に、最後にスキャンした装備を保存する
    public static void save(RegistryWrapper.WrapperLookup registries) {
        File parentDir = CONFIG_FILE.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            LOGGER.error("Failed to create config directory: " + parentDir.getAbsolutePath());
            return;
        }

        Properties properties = new Properties();
        properties.setProperty("count", String.valueOf(items.size()));

        RegistryOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, registries);
        for (int i = 0; i < items.size(); i++) {
            final int index = i;
            ItemStack.OPTIONAL_CODEC.encodeStart(ops, items.get(i)).result()
                    .ifPresent(tag -> properties.setProperty("item" + index, tag.toString()));
        }

        try (FileOutputStream out = new FileOutputStream(CONFIG_FILE)) {
            properties.store(out, "GanKura last-scanned Skyblock Equipment (auto-saved on game close)");
        } catch (IOException e) {
            LOGGER.error("Failed to save equipment", e);
        }
    }
}
