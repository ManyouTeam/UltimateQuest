package cn.superiormc.ultimatequest.paper;

import cn.superiormc.ultimatequest.commands.PaperCommandRegistrar;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;

public final class UltimateQuestBootstrap implements PluginBootstrap {

    @Override
    public void bootstrap(BootstrapContext context) {
        PaperCommandRegistrar.bind(context);
    }
}
