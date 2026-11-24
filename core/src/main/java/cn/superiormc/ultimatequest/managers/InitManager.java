package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.UltimateQuest;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import cn.superiormc.ultimatequest.utils.CommonUtil;

public final class InitManager extends AbstractManager {

    public static InitManager initManager;

    private boolean firstLoad;

    public InitManager() {
        initManager = this;
        File file = new File(UltimateQuest.instance.getDataFolder(), "config.yml");
        if (!file.exists()) {
            UltimateQuest.instance.saveDefaultConfig();
            firstLoad = true;
        }
        init();
    }

    public void init() {
        resourceOutput("config.yml", true);
        resourceOutputFolder("languages", true);
        resourceOutputFolder("menus", true);
        resourceOutputFolder("items", false);
        resourceOutputFolder("quests", false);
        resourceOutputFolder("tasks", true);
        resourceOutputFolder("categories", true);
        resourceOutputFolder("battlepasses", true);
        resourceOutputFolder("milestones", true);
        resourceOutputFolder("pools", true);
        migrateRewardPools();
        resourceOutputFolder("reward_pools", true);
        resourceOutputFolder("periods", true);
        resourceOutputFolder("scrolls", true);
    }

    private void migrateRewardPools() {
        File dataFolder = UltimateQuest.instance.getDataFolder();
        migrate(new File(dataFolder, "reward-pools"), new File(dataFolder, "reward_pools"));
    }

    private void migrate(File source, File target) {
        if (!source.exists()) {
            return;
        }
        try {
            if (source.isDirectory()) {
                Files.createDirectories(target.toPath());
                File[] children = source.listFiles();
                if (children == null) {
                    throw new IOException("Cannot list directory " + source);
                }
                for (File child : children) {
                    migrate(child, new File(target, child.getName()));
                }
                File[] remaining = source.listFiles();
                if (remaining != null && remaining.length == 0) {
                    Files.delete(source.toPath());
                }
            } else if (target.exists()) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Cannot migrate " + source + ": " + target
                    + " already exists. The old file was preserved; merge it manually into the new location.");
            } else {
                Files.createDirectories(target.toPath().getParent());
                Files.move(source.toPath(), target.toPath());
            }
        } catch (IOException exception) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Cannot migrate " + source + " to " + target + ": " + exception.getMessage());
        }
    }

    private void resourceOutputFolder(String folderName, boolean regenerate) {
        String normalizedFolder = folderName.endsWith("/") ? folderName : folderName + "/";
        File sourceFile;
        try {
            sourceFile = new File(UltimateQuest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException exception) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not extract resource folder "
                    + folderName + ": " + exception.getMessage());
            return;
        }

        try (JarFile jarFile = new JarFile(sourceFile)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                if (jarEntry.isDirectory()) {
                    continue;
                }
                String entryName = jarEntry.getName();
                if (!entryName.startsWith(normalizedFolder)) {
                    continue;
                }
                resourceOutput(entryName, regenerate);
            }
        } catch (IOException exception) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not extract resource folder "
                    + folderName + ": " + exception.getMessage());
        }
    }

    private void resourceOutput(String fileName, boolean regenerate) {
        File tempVal1 = new File(UltimateQuest.instance.getDataFolder(), fileName);
        if (!tempVal1.exists()) {
            if (!firstLoad && !regenerate) {
                return;
            }
            File tempVal2 = new File(fileName);
            if (tempVal2.getParentFile() != null) {
                CommonUtil.mkDir(tempVal2.getParentFile());
            }
            UltimateQuest.instance.saveResource(tempVal2.getPath(), false);
        }
    }

    public boolean isFirstLoad() {
        return firstLoad;
    }
}
