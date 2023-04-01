package net.trollyloki.mcchess.game.player;

import net.trollyloki.mcchess.game.Game;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class HumanPlayer implements ChessPlayer {

    private final @NotNull Player player;

    /**
     * Creates a new human player
     *
     * @param player Bukkit player
     */
    public HumanPlayer(@NotNull Player player) {
        this.player = player;
    }

    @Override
    public @NotNull String getName() {
        return player.getName();
    }

    @Override
    public @NotNull CompletableFuture<Boolean> play(@NotNull Game game) {
        return CompletableFuture.completedFuture(false);
    }

}
