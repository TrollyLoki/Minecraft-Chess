package net.trollyloki.mcchess.game.player;

import net.trollyloki.mcchess.board.Board;
import net.trollyloki.mcchess.game.move.Move;
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
     * Chooses a move to play on a board.
     *
     * @param board board
     * @return future to completed with the chosen move
     */
    @NotNull CompletableFuture<Move> chooseMove(@NotNull Board board);

    /**
     * Chooses a move to play on a board.
     *
     * @param board board
     * @param opponentMove most recent move made by the opponent
     * @return future to completed with the chosen move
     */
    default @NotNull CompletableFuture<Move> chooseMove(@NotNull Board board, @NotNull Move opponentMove) {
        return chooseMove(board);
    }

}
