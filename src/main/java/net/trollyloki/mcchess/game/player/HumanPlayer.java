package net.trollyloki.mcchess.game.player;

import net.trollyloki.mcchess.game.Game;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
    public boolean play(@NotNull Game game) {
        return false;
    }

}
