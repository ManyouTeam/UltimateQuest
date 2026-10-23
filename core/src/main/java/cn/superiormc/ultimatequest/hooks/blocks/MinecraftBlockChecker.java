package cn.superiormc.ultimatequest.hooks.blocks;

import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.managers.ErrorManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class MinecraftBlockChecker extends AbstractBlockHook {

    @Override
    public String getBlockId(Block block) {
        return block == null ? null : "minecraft:" + block.getType().name().toLowerCase();
    }

    @Override
    public boolean check(Block block, String materialString, Location location) {
        String[] parts = materialString.split(":");
        if (!isValidMaterialFormat(parts, 2)) {
            return false;
        }
        try {
            Material material = Material.getMaterial(parts[1].toUpperCase());
            if (material != null) {
                return material == block.getType();
            }
        } catch (Exception e) {
            if (ConfigManager.configManager.getBoolean("debug", false)) {
                ErrorManager.errorManager.sendErrorMessage("§cError: MinecraftBlockChecker: " + e.getMessage());
            }
        }
        return false;
    }

    @Override
    public void placeBlock(String materialString, Location location) {
        String[] parts = materialString.split(":");
        if (!isValidMaterialFormat(parts, 2)) {
            return;
        }
        try {
            Material material = Material.getMaterial(parts[1].toUpperCase());
            if (material != null) {
                location.getBlock().setType(material);
            }
        } catch (Exception e) {
            if (ConfigManager.configManager.getBoolean("debug", false)) {
                ErrorManager.errorManager.sendErrorMessage("§cError: MinecraftBlockChecker: " + e.getMessage());
            }
        }
    }

    @Override
    protected String getCheckerName() {
        return "minecraft";
    }
}
