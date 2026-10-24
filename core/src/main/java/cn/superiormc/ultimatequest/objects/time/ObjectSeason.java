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

    public String getId(Instant start) {
        return config.getString("season-prefix", "season") + "_" + start.getEpochSecond();
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
