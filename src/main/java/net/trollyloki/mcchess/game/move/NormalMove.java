package net.trollyloki.mcchess.game.move;

import net.trollyloki.mcchess.board.Board;
import net.trollyloki.mcchess.board.Piece;
import net.trollyloki.mcchess.board.Square;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class NormalMove implements Move {

    protected final @NotNull Piece.Type pieceType;
    protected final @NotNull Square from, to;
    protected final boolean capture;

    public NormalMove(@NotNull Piece.Type pieceType, @NotNull Square from, @NotNull Square to, boolean capture) {
        this.pieceType = pieceType;
        this.from = from;
        this.to = to;
        this.capture = capture;
    }

    public @NotNull Piece.Type getPieceType() {
        return pieceType;
    }

    public @NotNull Square getFrom() {
        return from;
    }

    public @NotNull Square getTo() {
        return to;
    }

    public boolean isCapture() {
        return capture;
    }

    @Override
    public boolean isPawnMoveOrCapture() {
        return capture || pieceType == Piece.Type.PAWN;
    }

    @Override
    public @NotNull Optional<Square> getEnPassantSquare() {
        if (pieceType == Piece.Type.PAWN && Math.abs(to.getRank() - from.getRank()) == 2)
            return Optional.of(new Square(to.getFile(), (from.getRank() + to.getRank()) / 2));
        else
            return Optional.empty();
    }

    @Override
    public boolean isPossible(@NotNull Board board) {
        return board.isMovePossible(from, to);
    }

    @Override
    public void play(@NotNull Board board) {
        board.movePiece(from, to);
        if (pieceType == Piece.Type.PAWN && capture && board.getPieceAt(to).isEmpty())
            board.setPieceAt(new Square(to.getFile(), from.getRank()), null);
    }

    @Override
    public @NotNull String toUCI() {
        return from.toString() + to;
    }

    @Override
    public @NotNull String toSAN() {
        StringBuilder builder = new StringBuilder(6);
        if (pieceType != Piece.Type.PAWN)
            builder.append(pieceType.getLetter());
        builder.append(from);
        if (capture)
            builder.append('x');
        builder.append(to);
        return builder.toString();
    }

    @Override
    public String toString() {
        return toSAN();
    }

}
