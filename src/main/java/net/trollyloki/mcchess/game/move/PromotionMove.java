package net.trollyloki.mcchess.game.move;

import net.trollyloki.mcchess.board.Board;
import net.trollyloki.mcchess.board.Piece;
import net.trollyloki.mcchess.board.Square;
import org.jetbrains.annotations.NotNull;

public class PromotionMove extends NormalMove {

    protected final @NotNull Piece.Type promotionType;

    public PromotionMove(@NotNull Square from, @NotNull Square to, boolean capture, @NotNull Piece.Type promotionType) {
        super(Piece.Type.PAWN, from, to, capture);
        this.promotionType = promotionType;
    }

    public @NotNull Piece.Type getPromotionType() {
        return promotionType;
    }

    @Override
    public void play(@NotNull Board board) {
        super.play(board);
        board.getPieceAt(to).map(Piece::getColor)
                .map(color -> new Piece(color, promotionType))
                .ifPresent(piece -> board.setPieceAt(to, piece));
    }

    @Override
    public @NotNull String toUCI() {
        return super.toUCI() + Character.toLowerCase(promotionType.getLetter());
    }

    @Override
    public @NotNull String toSAN() {
        return super.toSAN() + '=' + promotionType.getLetter();
    }

}
