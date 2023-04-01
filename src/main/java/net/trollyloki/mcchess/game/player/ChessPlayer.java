package net.trollyloki.mcchess.game.player;

import net.trollyloki.mcchess.game.Game;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface ChessPlayer {

    /**
     * Gets the name of this player.
     *
     * @return name
     */
    @NotNull String getName();

    /**
     * Plays a move in a game.
     *
     * @param game game
     * @return {@code true} if a move was made, or {@code false} if the player has not moved yet
     */
    @NotNull CompletableFuture<Boolean> play(@NotNull Game game);

}
