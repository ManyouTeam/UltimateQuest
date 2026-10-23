package cn.superiormc.ultimatequest.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import java.util.Collection;
import java.util.List;

public final class PaperCommandRegistrar {

    private PaperCommandRegistrar() {

    }

    public static void bind(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> event.registrar().register("ultimatequest", "View and manage quests.", List.of("uq", "quests"), new UltimateQuestCommand()));
    }

    private static final class UltimateQuestCommand implements BasicCommand {

        private final MainCommand executor = new MainCommand();

        private final MainCommandTab tabCompleter = new MainCommandTab();

        @Override
        public void execute(CommandSourceStack source, String[] args) {
            executor.execute(source.getSender(), args);
        }

        @Override
        public Collection<String> suggest(CommandSourceStack source, String[] args) {
            return tabCompleter.suggest(source.getSender(), args);
        }
    }
}
