package net.trollyloki.mcchess.game.move;

import net.trollyloki.mcchess.board.Piece;
import net.trollyloki.mcchess.board.Square;
import org.jetbrains.annotations.NotNull;

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
