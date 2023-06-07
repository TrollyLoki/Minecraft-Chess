package net.trollyloki.mcchess.game.move;

import net.trollyloki.mcchess.Color;
import net.trollyloki.mcchess.board.Board;
import net.trollyloki.mcchess.board.Piece;
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

    private @NotNull Square getKingSquare() {
        return new Square(4, color.getBackRank());
    }

    private @NotNull Square getRookSquare() {
        return new Square(queenside ? 0 : 7, color.getBackRank());
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
    public boolean isPossible(@NotNull Board board) {
        if (queenside ? !board.canLongCastle(color) : !board.canShortCastle(color))
            return false;

        Square kingSquare = getKingSquare();
        Square rookSquare = getRookSquare();

        return board.isPieceAt(kingSquare, new Piece(color, Piece.Type.KING))
                && board.isPieceAt(rookSquare, new Piece(color, Piece.Type.ROOK))
                && board.isRankOpen(color.getBackRank(), rookSquare.getFile(), kingSquare.getFile());
    }

    @Override
    public void play(@NotNull Board board) {
        Square kingSquare = getKingSquare();
        Square rookSquare = getRookSquare();
        int direction = queenside ? -1 : 1;

        board.movePiece(kingSquare, kingSquare.relative(2 * direction, 0));
        board.movePiece(rookSquare, kingSquare.relative(direction, 0));
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
