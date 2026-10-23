package cn.superiormc.ultimatequest.hooks.blocks;

import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.managers.ErrorManager;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import com.nexomc.nexo.api.NexoBlocks;
import com.nexomc.nexo.mechanics.custom_block.CustomBlockMechanic;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class NexoBlockChecker extends AbstractBlockHook {

    @Override
    public String getBlockId(Block block) {
        if (!CommonUtil.checkPluginLoad("Nexo")) {
            return null;
        }
        try {
            CustomBlockMechanic nexoBlock = NexoBlocks.customBlockMechanic(block.getLocation());
            return nexoBlock == null ? null : "nexo:" + nexoBlock.getItemID();
        } catch (Exception e) {
            if (ConfigManager.configManager.getBoolean("debug", false)) {
                ErrorManager.errorManager.sendErrorMessage("§cError: NexoBlockChecker: " + e.getMessage());
            }
            return null;
        }
    }

    @Override
    public boolean check(Block block, String materialString, Location location) {
        if (!CommonUtil.checkPluginLoad("Nexo")) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Nexo is not loaded but you are using block from it as totem layout!");
            return false;
        }
        String[] parts = materialString.split(":");
        if (!isValidMaterialFormat(parts, 2)) {
            return false;
        }
        try {
            CustomBlockMechanic nexoBlock = NexoBlocks.customBlockMechanic(block.getLocation());
            return nexoBlock != null && parts[1].equals(nexoBlock.getItemID());
        } catch (Exception e) {
            if (ConfigManager.configManager.getBoolean("debug", false)) {
                ErrorManager.errorManager.sendErrorMessage("§cError: NexoBlockChecker: " + e.getMessage());
            }
            return false;
        }
    }

    @Override
    public void placeBlock(String materialString, Location location) {
        if (!CommonUtil.checkPluginLoad("Nexo")) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Nexo is not loaded but you are using block from it!");
            return;
        }
        String[] parts = materialString.split(":");
        if (!isValidMaterialFormat(parts, 2)) {
            return;
        }
        try {
            NexoBlocks.place(materialString, location);
        } catch (Exception e) {
            if (ConfigManager.configManager.getBoolean("debug", false)) {
                ErrorManager.errorManager.sendErrorMessage("§cError: NexoBlockChecker: " + e.getMessage());
            }
        }
    }

    @Override
    protected String getCheckerName() {
        return "nexo";
    }
}
