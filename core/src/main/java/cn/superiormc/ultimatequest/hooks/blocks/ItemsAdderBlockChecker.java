package cn.superiormc.ultimatequest.hooks.blocks;

import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.managers.ErrorManager;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import dev.lone.itemsadder.api.CustomBlock;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class ItemsAdderBlockChecker extends AbstractBlockHook {

    @Override
    public String getBlockId(Block block) {
        if (!CommonUtil.checkPluginLoad("ItemsAdder")) {
            return null;
        }
        try {
            CustomBlock iaBlock = CustomBlock.byAlreadyPlaced(block);
            return iaBlock == null ? null : "itemsadder:" + iaBlock.getNamespacedID();
        } catch (Exception e) {
            if (ConfigManager.configManager.getBoolean("debug", false)) {
                ErrorManager.errorManager.sendErrorMessage("§cError: ItemsAdderBlockChecker: " + e.getMessage());
            }
            return null;
        }
    }

    @Override
    public boolean check(Block block, String materialString, Location location) {
        if (!CommonUtil.checkPluginLoad("ItemsAdder")) {
            ErrorManager.errorManager.sendErrorMessage("§cError: ItemsAdder is not loaded but you are using block from it!");
            return false;
        }
        String[] parts = materialString.split(":");
        if (!isValidMaterialFormat(parts, 3)) {
            return false;
        }
        try {
            CustomBlock iaBlock = CustomBlock.byAlreadyPlaced(block);
            if (iaBlock == null) {
                return false;
            }
            return (parts[1] + ":" + parts[2]).equals(iaBlock.getNamespacedID());
        } catch (Exception e) {
            if (ConfigManager.configManager.getBoolean("debug", false)) {
                ErrorManager.errorManager.sendErrorMessage("§cError: ItemsAdderBlockChecker: " + e.getMessage());
            }
            return false;
        }
    }

    @Override
    public void placeBlock(String materialString, Location location) {
        if (!CommonUtil.checkPluginLoad("ItemsAdder")) {
            ErrorManager.errorManager.sendErrorMessage("§cError: ItemsAdder is not loaded but you are using block from it!");
            return;
        }
        String[] parts = materialString.split(":");
        if (!isValidMaterialFormat(parts, 3)) {
            return;
        }
        try {
            CustomBlock.place(parts[1] + ":" + parts[2], location);
        } catch (Exception e) {
            if (ConfigManager.configManager.getBoolean("debug", false)) {
                ErrorManager.errorManager.sendErrorMessage("§cError: ItemsAdderBlockChecker: " + e.getMessage());
            }
        }
    }

    @Override
    protected String getCheckerName() {
        return "itemsadder";
    }
}
