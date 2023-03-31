package net.trollyloki.mcchess.game.move;

import net.trollyloki.mcchess.Color;
import net.trollyloki.mcchess.board.Board;
import net.trollyloki.mcchess.board.Square;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class CastleMove implements Move {

    protected final @NotNull Color color;
    protected final boolean queenside;

    public CastleMove(@NotNull Color color, boolean queenside) {
        this.color = color;
        this.queenside = queenside;
    }

    public @NotNull Color getColor() {
        return color;
    }

    public boolean isQueenside() {
        return queenside;
    }

    @Override
    public boolean isPawnMoveOrCapture() {
        return false;
    }

    @Override
    public @NotNull Optional<Square> getEnPassantSquare() {
        return Optional.empty();
    }

    @Override
    public void play(@NotNull Board board) {
        int rank = color.getBackRank();
        if (queenside) {
            board.movePiece(new Square(4, rank), new Square(2, rank)); // king
            board.movePiece(new Square(0, rank), new Square(3, rank)); // rook
        } else {
            board.movePiece(new Square(4, rank), new Square(6, rank)); // king
            board.movePiece(new Square(7, rank), new Square(5, rank)); // rook
        }
    }

    @Override
    public @NotNull String toUCI() {
        return switch (color) {
            case WHITE -> queenside ? "e1c1" : "e1g1";
            case BLACK -> queenside ? "e8c8" : "e8g8";
        };
    }

    @Override
    public @NotNull String toSAN() {
        StringBuilder builder = new StringBuilder(5);
        builder.append("O-O");
        if (queenside)
            builder.append("-O");
        return builder.toString();
    }

    @Override
    public String toString() {
        return toSAN();
    }

}
