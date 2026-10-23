package cn.superiormc.ultimatequest.hooks.blocks;

import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.managers.ErrorManager;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import io.th0rgal.oraxen.api.OraxenBlocks;
import io.th0rgal.oraxen.mechanics.Mechanic;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class OraxenBlockChecker extends AbstractBlockHook {

    @Override
    public String getBlockId(Block block) {
        if (!CommonUtil.checkPluginLoad("Oraxen")) {
            return null;
        }
        try {
            Mechanic oraxenBlock = OraxenBlocks.getOraxenBlock(block.getLocation());
            return oraxenBlock == null ? null : "oraxen:" + oraxenBlock.getItemID();
        } catch (Exception e) {
            if (ConfigManager.configManager.getBoolean("debug", false)) {
                ErrorManager.errorManager.sendErrorMessage("§cError: OraxenBlockChecker: " + e.getMessage());
            }
            return null;
        }
    }

    @Override
    public boolean check(Block block, String materialString, Location location) {
        if (!CommonUtil.checkPluginLoad("Oraxen")) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Oraxen is not loaded but you are using block from it!");
            return false;
        }
        String[] parts = materialString.split(":");
        if (!isValidMaterialFormat(parts, 2)) {
            return false;
        }
        try {
            Mechanic oraxenBlock = OraxenBlocks.getOraxenBlock(block.getLocation());
            return oraxenBlock != null && parts[1].equals(oraxenBlock.getItemID());
        } catch (Exception e) {
            if (ConfigManager.configManager.getBoolean("debug", false)) {
                ErrorManager.errorManager.sendErrorMessage("§cError: OraxenBlockChecker: " + e.getMessage());
            }
            return false;
        }
    }

    @Override
    public void placeBlock(String materialString, Location location) {
        if (!CommonUtil.checkPluginLoad("Oraxen")) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Oraxen is not loaded but you are using block from it!");
            return;
        }
        String[] parts = materialString.split(":");
        if (!isValidMaterialFormat(parts, 2)) {
            return;
        }
        try {
            OraxenBlocks.place(parts[1], location);
        } catch (Exception e) {
            if (ConfigManager.configManager.getBoolean("debug", false)) {
                ErrorManager.errorManager.sendErrorMessage("§cError: OraxenBlockChecker: " + e.getMessage());
            }
        }
    }

    @Override
    protected String getCheckerName() {
        return "oraxen";
    }
}
