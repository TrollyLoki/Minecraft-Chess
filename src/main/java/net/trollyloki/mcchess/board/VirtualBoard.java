package net.trollyloki.mcchess.board;

import net.trollyloki.mcchess.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public final class VirtualBoard extends Board {

    private final @Nullable Piece @NotNull [] pieceArray;

    /**
     * Creates a new virtual board.
     */
    public VirtualBoard() {
        this.pieceArray = new Piece[64];
        loadFEN(STANDARD_FEN);
    }

    private VirtualBoard(@Nullable Piece @NotNull [] pieceArray, @NotNull String site, @NotNull Color activeColor, @NotNull Set<Color> canShortCastle, @NotNull Set<Color> canLongCastle, @Nullable Square enPassantSquare, int halfMoves, int moveNumber) {
        super(site, activeColor, canShortCastle, canLongCastle, enPassantSquare, halfMoves, moveNumber);
        this.pieceArray = pieceArray.clone();
        validateCastling();
    }

    VirtualBoard(@NotNull String site, @NotNull Color activeColor, @NotNull Set<Color> canShortCastle, @NotNull Set<Color> canLongCastle, @Nullable Square enPassantSquare, int halfMoves, int moveNumber) {
        super(site, activeColor, canShortCastle, canLongCastle, enPassantSquare, halfMoves, moveNumber);
        this.pieceArray = new Piece[64];
        validateCastling();
    }

    @Override
    public @NotNull VirtualBoard virtual() {
        return new VirtualBoard(pieceArray, site, activeColor, canShortCastle, canLongCastle, enPassantSquare, halfMoves, moveNumber);
    }

    private int getIndexForSquare(@NotNull Square square) {
        return 8 * square.getFile() + square.getRank();
    }

    @Override
    public @NotNull Optional<Piece> getPieceAt(@NotNull Square square) {
        return Optional.ofNullable(pieceArray[getIndexForSquare(square)]);
    }

    @Override
    public boolean setPieceAt(@NotNull Square square, @Nullable Piece piece) {
        pieceArray[getIndexForSquare(square)] = piece;
        return true;
    }

}
