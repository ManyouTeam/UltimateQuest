package cn.superiormc.ultimatequest.objects.time;

import cn.superiormc.ultimatequest.utils.ResetTimeUtil;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.time.Instant;

public final class ObjectSeason {

    private final YamlConfiguration config;

    public ObjectSeason(YamlConfiguration config) {
        this.config = config;
    }

    public void apply(Instant start, Instant end) {
        config.set("starts-at", start.toString());
        config.set("ends-at", end.toString());
        config.set("season", config.getString("season-prefix", "season") + "_" + start.getEpochSecond());
        config.set("rotation", null);
    }

    public boolean isRotating() {
        ConfigurationSection rotation = config.getConfigurationSection("rotation");
        return rotation != null && rotation.getBoolean("enabled", false);
    }

    public Instant getPrevious(Instant now) {
        return ResetTimeUtil.previous(config.getConfigurationSection("rotation"), now);
    }

    public Instant getNext(Instant start) {
        return ResetTimeUtil.next(config.getConfigurationSection("rotation"), start);
    }

}
