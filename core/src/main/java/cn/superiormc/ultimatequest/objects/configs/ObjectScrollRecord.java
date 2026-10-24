package cn.superiormc.ultimatequest.objects.configs;

import cn.superiormc.ultimatequest.managers.*;
import cn.superiormc.ultimatequest.objects.quests.*;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

public final class ObjectScrollRecord {

    private final YamlConfiguration record;

    public ObjectScrollRecord(YamlConfiguration record) {
        this.record = record;
    }

    public List<QuestDefinition> readQuests() {
        if (record.getInt("schema-version") != 3) {
            throw new IllegalArgumentException("Unsupported scroll record schema-version: " + record.get("schema-version"));
        }
        if (!record.getString("kind", "").equals("scroll")) {
            throw new IllegalArgumentException("Invalid scroll record kind: " + record.get("kind"));
        }
        if (!Set.of("ISSUED", "ACTIVE", "REDEEMING", "REDEEMED", "REVIEW").contains(record.getString("state", ""))) {
            throw new IllegalArgumentException("Invalid scroll record state: " + record.get("state"));
        }
        if (!record.isList("objective-ids")) {
            throw new IllegalArgumentException("Missing scroll objective IDs");
        }
        ObjectScroll.readDuration(record);
        QuestDefinition definition = ConfigManager.configManager.getScroll(record.getString("profile", "")).createDefinition(record.getStringList("objective-ids"));
        return List.of(definition);
    }
}
