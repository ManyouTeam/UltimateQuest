package cn.superiormc.ultimatequest.api.progression;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.bukkit.entity.Player;

public interface ProgressionService {

    Optional<ProgressionView> progress(UUID player, String type, String id);

    AutoCloseable subscribe(Consumer<ProgressionEvent> listener);

    void addExperience(Player player, String pass, long amount);

    void claimAll(Player player, String type, String id);
}
