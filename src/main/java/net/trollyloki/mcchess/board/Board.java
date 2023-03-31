package net.trollyloki.mcchess.board;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;

public interface Board {

    /**
     * Checks if an index is within the bounds of the board.
     *
     * @param index file/rank index
     * @return {@code true} if the index is valid, otherwise {@code false}
     */
    static boolean inBounds(int index) {
        return index >= 0 && index < 8;
    }

    /**
     * Checks if a square is within the bounds of the board.
     *
     * @param square square
     * @return {@code true} if the file and rank index are valid, otherwise {@code false}
     */
    static boolean inBounds(@NotNull Square square) {
        return inBounds(square.getFile()) && inBounds(square.getRank());
    }

    static void checkBounds(int file, int rank) throws IndexOutOfBoundsException {
        if (file < 0 || file >= 8)
            throw new IndexOutOfBoundsException("File must be between 0 and 7");
        if (rank < 0 || rank >= 8)
            throw new IndexOutOfBoundsException("Rank must be between 0 and 7");
    }

    static void checkBounds(@NotNull Square square) throws IndexOutOfBoundsException {
        checkBounds(square.getFile(), square.getRank());
    }

    /**
     * Gets the site of this board.
     *
     * @return site string
     */
    @NotNull String getSite();

    /**
     * Sets the site of this board.
     *
     * @param site site string
     */
    void setSite(@NotNull String site);

    /**
     * Gets the piece at a square on this board.
     *
     * @param square square
     * @return optional piece
     */
    @NotNull Optional<Piece> getPieceAt(@NotNull Square square);

    /**
     * Checks if the piece at a square on this board is present and matches a predicate.
     *
     * @param square    square
     * @param predicate piece predicate
     * @return {@code true} if the predicate is met, otherwise {@code false}
     */
    default boolean isPieceAt(@NotNull Square square, @NotNull Predicate<Piece> predicate) {
        return getPieceAt(square).filter(predicate).isPresent();
    }

    /**
     * Checks if the piece at a square on this board is present and a certain piece.
     *
     * @param square square
     * @param piece  piece
     * @return {@code true} if the piece is there, otherwise {@code false}
     */
    default boolean isPieceAt(@NotNull Square square, @NotNull Piece piece) {
        return isPieceAt(square, piece::equals);
    }

    /**
     * Sets the piece at a square on this board.
     *
     * @param square square
     * @param piece  optional piece
     * @return {@code true} if the piece was set, otherwise {@code false}
     */
    boolean setPieceAt(@NotNull Square square, @Nullable Piece piece);

    default boolean isFileOpen(int file, int fromRank, int toRank) {
        int delta = toRank > fromRank ? 1 : -1;
        for (int r = fromRank; r < toRank; r += delta) {
            if (getPieceAt(new Square(file, r)).isPresent())
                return false;
        }
        return true;
    }

    default boolean isRankOpen(int rank, int fromFile, int toFile) {
        int delta = toFile > fromFile ? 1 : -1;
        for (int f = fromFile; f < toFile; f += delta) {
            if (getPieceAt(new Square(f, rank)).isPresent())
                return false;
        }
        return true;
    }

    default boolean isDiagonalOpen(int fromFile, int fromRank, int toFile, int toRank) {
        int fileDelta = toFile > fromFile ? 1 : -1;
        int rankDelta = toRank > fromRank ? 1 : -1;
        int f = fromFile;
        int r = fromRank;
        while (f < toFile || r < toRank) {
            if (getPieceAt(new Square(f, r)).isPresent())
                return false;
            f += fileDelta;
            r += rankDelta;
        }
        return true;
    }

    /**
     * Moves a piece on this board.
     *
     * @param from square to move from
     * @param to   square to move to
     * @return {@code true} if a piece was moved, otherwise {@code false}
     */
    default boolean movePiece(@NotNull Square from, @NotNull Square to) {
        Optional<Piece> fromPiece = getPieceAt(from);
        if (fromPiece.isEmpty())
            return false;

        setPieceAt(to, fromPiece.get());
        return true;
    }

    /**
     * Builds a FEN position string from this board.
     *
     * @return FEN piece placement data
     * @see <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">Forsyth–Edwards Notation</a>
     */
    @NotNull
    default String toFEN() {
        StringBuilder builder = new StringBuilder();
        for (int rank = 7; rank >= 0; rank--) {

            int emptySquares = 0;
            for (int file = 0; file < 8; file++) {

                Optional<Piece> piece = getPieceAt(new Square(file, rank));
                if (piece.isEmpty()) {
                    emptySquares++;
                    continue;
                }

                if (emptySquares != 0) {
                    builder.append(emptySquares);
                    emptySquares = 0;
                }
                builder.append(piece.get().getLetter());

            }

            if (emptySquares != 0)
                builder.append(emptySquares);

            if (rank != 0)
                builder.append('/');
        }
        return builder.toString();
    }

    /**
     * Loads a FEN position onto this board.
     *
     * @param position FEN piece placement data
     * @see <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">Forsyth–Edwards Notation</a>
     */
    default void loadFromFEN(@NotNull String position) {
        int rank = 7;
        for (String rankString : position.split("/")) {

            int file = 0;
            for (char letter : rankString.toCharArray()) {

                if (Character.isDigit(letter)) {
                    int emptySquares = Integer.parseInt(String.valueOf(letter));

                    for (int i = 0; i < emptySquares; i++) {
                        setPieceAt(new Square(file, rank), null);
                        file++;
                    }

                    continue;
                }

                setPieceAt(new Square(file, rank), Piece.fromLetter(letter));
                file++;
            }
            rank--;
        }
    }

}
