package cn.superiormc.ultimatequest.spigot;

import cn.superiormc.ultimatequest.managers.ErrorManager;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import cn.superiormc.ultimatequest.utils.SpecialMethodUtil;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder.FormatRetention;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

public class SpigotMethodUtil implements SpecialMethodUtil {

    private static final Pattern TEXTURE_URL_PATTERN = Pattern.compile("\"url\"\\s*:\\s*\"(https?://textures\\.minecraft\\.net/texture/[^\"]+)\"");

    private final Map<UUID, Map<String, BossBar>> bossBarCache = new ConcurrentHashMap<>();

    private final Map<UUID, Map<String, Long>> bossBarGenerations = new ConcurrentHashMap<>();

    public Map<String, PlayerProfile> playerProfiles = Collections.synchronizedMap(new LinkedHashMap<>(256, 0.75f, true) {

        @Override
        protected boolean removeEldestEntry(Map.Entry<String, PlayerProfile> eldest) {
            return size() > 256;
        }
    });

    @Override
    public String methodID() {
        return "spigot";
    }

    @Override
    public void dispatchCommand(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }

    @Override
    public void dispatchCommand(Player player, String command) {
        Bukkit.dispatchCommand(player, command);
    }

    @Override
    public void dispatchOpCommand(Player player, String command) {
        boolean playerIsOp = player.isOp();
        try {
            player.setOp(true);
            Bukkit.dispatchCommand(player, command);
        } finally {
            player.setOp(playerIsOp);
        }
    }

    @Override
    public ItemStack getItemObject(Object object) {
        if (object instanceof ItemStack) {
            return (ItemStack) object;
        }
        return null;
    }

    @Override
    public Object makeItemToObject(ItemStack item) {
        return item;
    }

    @Override
    public void spawnEntity(Location location, EntityType entity) {
        location.getWorld().spawnEntity(location, entity);
    }

    @Override
    public void playerTeleport(Player player, Location location) {
        player.teleport(location);
    }

    @Override
    public SkullMeta setSkullMeta(SkullMeta meta, String skull) {
        if (!CommonUtil.getMajorVersion(19)) {
            return meta;
        }
        if (skull.length() > 16) {
            try {
                URL skinUrl = resolveSkinUrl(skull);
                if (skinUrl == null) {
                    return meta;
                }
                PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "custom_head");
                PlayerTextures textures = profile.getTextures();
                textures.setSkin(skinUrl);
                profile.setTextures(textures);
                meta.setOwnerProfile(profile);
            } catch (Exception e) {
                ErrorManager.errorManager.sendErrorMessage("§cError: SpigotMethodUtil: " + e.getMessage());
            }
        } else {
            if (!playerProfiles.containsKey(skull)) {
                playerProfiles.put(skull, Bukkit.getOfflinePlayer(skull).getPlayerProfile());
            }
            meta.setOwnerProfile(playerProfiles.get(skull));
        }
        return meta;
    }

    @Override
    public String serializeSkull(SkullMeta meta) {
        if (!CommonUtil.getMajorVersion(19)) {
            return null;
        }
        try {
            PlayerProfile ownerProfile = meta.getOwnerProfile();
            if (ownerProfile != null) {
                String name = ownerProfile.getName();
                if (name != null && !name.trim().isEmpty()) {
                    return name;
                }
                URL skinUrl = ownerProfile.getTextures().getSkin();
                if (skinUrl != null) {
                    return encodeSkinUrl(skinUrl);
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    @Override
    public void setItemName(ItemMeta meta, String name, Player player) {
        meta.setDisplayName(TextUtil.parse(name, player));
    }

    @Override
    public void setItemItemName(ItemMeta meta, String itemName, Player player) {
        if (itemName.isEmpty()) {
            meta.setItemName(" ");
        } else {
            meta.setItemName(TextUtil.parse(itemName, player));
        }
    }

    @Override
    public void setItemLore(ItemMeta meta, List<String> lores, Player player) {
        if (lores == null) {
            meta.setLore(null);
            return;
        }
        List<String> newLore = new ArrayList<>();
        for (String lore : lores) {
            for (String singleLore : lore.split("\\\\n")) {
                if (singleLore.isEmpty()) {
                    newLore.add(" ");
                    continue;
                }
                newLore.add(TextUtil.parse(singleLore, player));
            }
        }
        meta.setLore(newLore);
    }

    @Override
    public ItemMeta finalizeItemText(ItemMeta meta) {
        Map<String, Object> serialized = null;
        if (meta.hasDisplayName() && TextUtil.TRANSLATABLE_PATTERN.matcher(meta.getDisplayName()).find()) {
            serialized = new LinkedHashMap<>(meta.serialize());
            serialized.put("display-name", ComponentSerializer.toString(translatedComponents(meta.getDisplayName())));
        }
        List<String> lore = meta.getLore();
        if (lore != null && lore.stream().anyMatch(line -> TextUtil.TRANSLATABLE_PATTERN.matcher(line).find())) {
            if (serialized == null) {
                serialized = new LinkedHashMap<>(meta.serialize());
            }
            List<String> components = new ArrayList<>();
            for (String line : lore) {
                components.add(ComponentSerializer.toString(translatedComponents(line)));
            }
            serialized.put("lore", components);
        }
        if (serialized == null) {
            return meta;
        }
        serialized.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, "ItemMeta");
        Object converted = ConfigurationSerialization.deserializeObject(serialized);
        if (!(converted instanceof ItemMeta translated)) {
            throw new IllegalStateException("Could not deserialize translated quest item metadata");
        }
        return translated;
    }

    private static BaseComponent[] translatedComponents(String text) {
        String colored = TextUtil.colorize(text);
        Matcher matcher = TextUtil.TRANSLATABLE_PATTERN.matcher(colored);
        TextComponent root = new TextComponent("");
        root.setItalic(false);
        int start = 0;
        while (matcher.find()) {
            for (BaseComponent component : TextComponent.fromLegacyText(ChatColor.getLastColors(colored.substring(0, start))
                    + colored.substring(start, matcher.start()))) {
                root.addExtra(component);
            }
            TranslatableComponent translated = new TranslatableComponent(matcher.group(1));
            BaseComponent[] style = TextComponent.fromLegacyText(ChatColor.getLastColors(colored.substring(0, matcher.start())) + " ");
            translated.copyFormatting(style[style.length - 1], FormatRetention.ALL, true);
            root.addExtra(translated);
            start = matcher.end();
        }
        for (BaseComponent component : TextComponent.fromLegacyText(ChatColor.getLastColors(colored.substring(0, start)) + colored.substring(start))) {
            root.addExtra(component);
        }
        return new BaseComponent[] {root};
    }

    @Override
    public void sendChat(Player player, String text) {
        String parsed = TextUtil.withPAPI(text, player);
        if (TextUtil.TRANSLATABLE_PATTERN.matcher(parsed).find()) {
            if (player == null) {
                Bukkit.getConsoleSender().spigot().sendMessage(translatedComponents(parsed));
            } else {
                player.spigot().sendMessage(translatedComponents(parsed));
            }
            return;
        }
        if (player == null) {
            Bukkit.getConsoleSender().sendMessage(TextUtil.parse(text));
        } else {
            player.sendMessage(TextUtil.parse(text, player));
        }
    }

    @Override
    public void sendTitle(Player player, String title, String subTitle, int fadeIn, int stay, int fadeOut) {
        if (player != null) {
            player.sendTitle(TextUtil.parse(title, player), TextUtil.parse(subTitle, player), fadeIn, stay, fadeOut);
        }
    }

    @Override
    public void sendActionBar(Player player, String message) {
        if (player == null) {
            return;
        }
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(TextUtil.parse(message, player)));
    }

    @Override
    public void sendBossBar(Player player, String title, float progress, String color, String style) {
        sendBossBar(player, title, progress, color, style, null);
    }

    @Override
    public void sendBossBar(Player player, String title, float progress, String color, String style, String key) {
        if (player == null) {
            return;
        }
        String barKey = key == null || key.isEmpty() ? "temp-" + UUID.randomUUID() : key;
        Map<String, BossBar> playerBars = bossBarCache.computeIfAbsent(player.getUniqueId(), id -> new ConcurrentHashMap<>());
        BossBar bar = playerBars.get(barKey);
        title = TextUtil.parse(title, player);
        if (bar == null) {
            bar = Bukkit.createBossBar(title, color == null ? BarColor.WHITE : BarColor.valueOf(color.toUpperCase()), style == null ? BarStyle.SOLID : BarStyle.valueOf(style.toUpperCase()));
            bar.addPlayer(player);
            bar.setVisible(true);
            playerBars.put(barKey, bar);
        } else {
            bar.setTitle(title);
            bar.setColor(color == null ? BarColor.WHITE : BarColor.valueOf(color.toUpperCase()));
            bar.setStyle(style == null ? BarStyle.SOLID : BarStyle.valueOf(style.toUpperCase()));
        }
        bar.setProgress(Math.max(0.0, Math.min(1.0, progress)));
        Map<String, Long> generations = bossBarGenerations.computeIfAbsent(player.getUniqueId(), id -> new ConcurrentHashMap<>());
        long generation = generations.getOrDefault(barKey, 0L) + 1L;
        generations.put(barKey, generation);
        BossBar finalBar = bar;
        long finalGeneration = generation;
        UUID playerId = player.getUniqueId();
        SchedulerUtil.runTaskLater(player, () -> {
            Long currentGeneration = generations.get(barKey);
            if (currentGeneration != null && currentGeneration != finalGeneration) {
                return;
            }
            finalBar.removeAll();
            playerBars.remove(barKey);
            generations.remove(barKey);
            removeEmptyBossBarCaches(playerId, playerBars, generations);
        }, 60);
    }

    @Override
    public void clearBossBars(Player player) {
        Map<String, BossBar> playerBars = bossBarCache.remove(player.getUniqueId());
        if (playerBars != null) {
            for (BossBar bar : playerBars.values()) {
                bar.removeAll();
            }
        }
        bossBarGenerations.remove(player.getUniqueId());
    }

    private void removeEmptyBossBarCaches(UUID playerId, Map<String, BossBar> playerBars, Map<String, Long> generations) {
        if (playerBars != null && playerBars.isEmpty()) {
            bossBarCache.remove(playerId, playerBars);
        }
        if (generations != null && generations.isEmpty()) {
            bossBarGenerations.remove(playerId, generations);
        }
    }

    @Override
    public Inventory createNewInv(Player player, int size, String text, InventoryHolder holder) {
        return Bukkit.createInventory(holder, size, TextUtil.parse(text, player));
    }

    @Override
    public String legacyParse(String text) {
        if (text == null) {
            return "";
        }
        return TextUtil.colorize(text);
    }

    @Override
    public String getItemName(ItemMeta meta) {
        return meta.getDisplayName();
    }

    @Override
    public String getItemItemName(ItemMeta meta) {
        return meta.getItemName();
    }

    @Override
    public List<String> getItemLore(ItemMeta meta) {
        return meta.getLore();
    }

    @Override
    public String getEntityName(LivingEntity entity) {
        if (entity.getCustomName() != null) {
            return entity.getCustomName();
        }
        return entity.getName();
    }

    @Override
    public ItemStack editItemStack(ItemStack item, Player player, ConfigurationSection section, int amount, String... args) {
        return item;
    }

    @Override
    public ConfigurationSection serializeItemStack(ItemStack item) {
        return null;
    }

    private URL resolveSkinUrl(String skull) throws Exception {
        if (skull == null) {
            return null;
        }
        String trimmedSkull = skull.trim();
        if (trimmedSkull.isEmpty()) {
            return null;
        }
        if (trimmedSkull.startsWith("http://textures.minecraft.net/texture/") || trimmedSkull.startsWith("https://textures.minecraft.net/texture/")) {
            return new URL(trimmedSkull);
        }
        String json = new String(Base64.getDecoder().decode(trimmedSkull), StandardCharsets.UTF_8);
        Matcher matcher = TEXTURE_URL_PATTERN.matcher(json);
        if (matcher.find()) {
            return new URL(matcher.group(1));
        }
        return null;
    }

    private String encodeSkinUrl(URL skinUrl) {
        String textureJson = "{\"textures\":{\"SKIN\":{\"url\":\"" + skinUrl + "\"}}}";
        return Base64.getEncoder().encodeToString(textureJson.getBytes(StandardCharsets.UTF_8));
    }
}
