package net.trollyloki.mcchess.game.move;

import net.trollyloki.mcchess.board.Board;
import net.trollyloki.mcchess.board.Square;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class CheckMove implements Move {

    protected final @NotNull Move move;
    protected final @NotNull CheckStatus checkStatus;

    public CheckMove(@NotNull Move move, @NotNull CheckStatus checkStatus) {
        this.move = move;
        this.checkStatus = checkStatus;
    }

    @Override
    public @NotNull Move check(@NotNull CheckStatus checkStatus) {
        return new CheckMove(move, checkStatus);
    }

    @Override
    public boolean isPawnMoveOrCapture() {
        return move.isPawnMoveOrCapture();
    }

    @Override
    public @NotNull Optional<Square> getEnPassantSquare() {
        return move.getEnPassantSquare();
    }

    @Override
    public void play(@NotNull Board board) {
        move.play(board);
    }

    @Override
    public @NotNull String toUCI() {
        return move.toUCI();
    }

    @Override
    public @NotNull String toSAN() {
        return move.toSAN() + checkStatus.getPostfix();
    }

    @Override
    public String toString() {
        return toSAN();
    }

}
