package net.trollyloki.mcchess.board;

import net.trollyloki.mcchess.ChessPlugin;
import net.trollyloki.mcchess.Color;
import net.trollyloki.mcchess.game.move.Move;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public abstract class Board implements Iterable<Square> {

    public static final @NotNull String STANDARD_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    protected @NotNull String site = ChessPlugin.getDefaultSite();

    protected @NotNull Color activeColor;
    protected @NotNull Set<Color> canShortCastle, canLongCastle;
    protected @Nullable Square enPassantSquare;
    protected int halfMoves, moveNumber;

    public Board(@NotNull Color activeColor, @NotNull Set<Color> canShortCastle, @NotNull Set<Color> canLongCastle, @Nullable Square enPassantSquare, int halfMoves, int moveNumber) {
        this.activeColor = activeColor;
        this.canShortCastle = new HashSet<>(canShortCastle);
        this.canLongCastle = new HashSet<>(canLongCastle);
        this.enPassantSquare = enPassantSquare;
        this.halfMoves = halfMoves;
        this.moveNumber = moveNumber;
    }

    public Board(@NotNull String site, @NotNull Color activeColor, @NotNull Set<Color> canShortCastle, @NotNull Set<Color> canLongCastle, @Nullable Square enPassantSquare, int halfMoves, int moveNumber) {
        this(activeColor, canShortCastle, canLongCastle, enPassantSquare, halfMoves, moveNumber);
        this.site = site;
    }

    public Board() {
        this(Color.WHITE, Set.of(Color.values()), Set.of(Color.values()), null, 0, 1);
    }

    public Board(@NotNull String site) {
        this();
        this.site = site;
    }

    /**
     * Creates a new virtual board with the same state as this board.
     *
     * @return new virtual board
     */
    @Contract("-> new")
    public @NotNull VirtualBoard virtual() {
        VirtualBoard board = new VirtualBoard(site, activeColor, canShortCastle, canLongCastle, enPassantSquare, halfMoves, moveNumber);
        for (Square square : this) {
            this.getPieceAt(square).ifPresent(piece ->
                    board.setPieceAt(square, piece));
        }
        return board;
    }

    /**
     * Checks if an index is within the bounds of the board.
     *
     * @param index file/rank index
     * @return {@code true} if the index is valid, otherwise {@code false}
     */
    public static boolean inBounds(int index) {
        return index >= 0 && index < 8;
    }

    /**
     * Checks if a square is within the bounds of the board.
     *
     * @param square square
     * @return {@code true} if the file and rank index are valid, otherwise {@code false}
     */
    public static boolean inBounds(@NotNull Square square) {
        return inBounds(square.getFile()) && inBounds(square.getRank());
    }

    public static void checkBounds(int file, int rank) throws IndexOutOfBoundsException {
        if (file < 0 || file >= 8)
            throw new IndexOutOfBoundsException("File must be between 0 and 7");
        if (rank < 0 || rank >= 8)
            throw new IndexOutOfBoundsException("Rank must be between 0 and 7");
    }

    public static void checkBounds(@NotNull Square square) throws IndexOutOfBoundsException {
        checkBounds(square.getFile(), square.getRank());
    }

    /**
     * Gets the site of this board.
     *
     * @return site string
     */
    public @NotNull String getSite() {
        return site;
    }

    /**
     * Sets the site of this board.
     *
     * @param site site string
     */
    public void setSite(@NotNull String site) {
        this.site = site;
    }

    @Override
    public @NotNull Iterator<Square> iterator() {
        return new SquareIterator();
    }

    /**
     * Gets the piece at a square on this board.
     *
     * @param square square
     * @return optional piece
     */
    public abstract @NotNull Optional<Piece> getPieceAt(@NotNull Square square);

    /**
     * Checks if the piece at a square on this board is present and matches a predicate.
     *
     * @param square    square
     * @param predicate piece predicate
     * @return {@code true} if the predicate is met, otherwise {@code false}
     */
    public boolean isPieceAt(@NotNull Square square, @NotNull Predicate<Piece> predicate) {
        return getPieceAt(square).filter(predicate).isPresent();
    }

    /**
     * Checks if the piece at a square on this board is present and a certain piece.
     *
     * @param square square
     * @param piece  piece
     * @return {@code true} if the piece is there, otherwise {@code false}
     */
    public boolean isPieceAt(@NotNull Square square, @NotNull Piece piece) {
        return isPieceAt(square, piece::equals);
    }

    /**
     * Sets the piece at a square on this board.
     *
     * @param square square
     * @param piece  optional piece
     * @return {@code true} if the piece was set, otherwise {@code false}
     */
    public abstract boolean setPieceAt(@NotNull Square square, @Nullable Piece piece);

    /**
     * Moves a piece on this board.
     *
     * @param from square to move from
     * @param to   square to move to
     * @return {@code true} if a piece was moved, otherwise {@code false}
     */
    public boolean movePiece(@NotNull Square from, @NotNull Square to) {
        Optional<Piece> fromPiece = getPieceAt(from);
        if (fromPiece.isEmpty())
            return false;

        setPieceAt(from, null);
        setPieceAt(to, fromPiece.get());
        return true;
    }

    /**
     * Gets the color that moves next.
     *
     * @return piece color
     */
    public @NotNull Color getActiveColor() {
        return activeColor;
    }

    /**
     * Sets the color that moves next.
     *
     * @param activeColor piece color
     */
    public void setActiveColor(@NotNull Color activeColor) {
        this.activeColor = activeColor;
    }

    public boolean canShortCastle(@NotNull Color color) {
        return canShortCastle.contains(color);
    }

    public void setCanShortCastle(@NotNull Color color, boolean canCastle) {
        if (canCastle)
            canShortCastle.add(color);
        else
            canShortCastle.remove(color);
    }

    public boolean canLongCastle(@NotNull Color color) {
        return canLongCastle.contains(color);
    }

    public void setCanLongCastle(@NotNull Color color, boolean canCastle) {
        if (canCastle)
            canLongCastle.add(color);
        else
            canLongCastle.remove(color);
    }

    public @NotNull Optional<Square> getEnPassantSquare() {
        return Optional.ofNullable(enPassantSquare);
    }

    public void setEnPassantSquare(@Nullable Square enPassantSquare) {
        this.enPassantSquare = enPassantSquare;
    }

    public int getHalfMoves() {
        return halfMoves;
    }

    public int getMoveNumber() {
        return moveNumber;
    }

    public boolean isFileOpen(int file, int fromRank, int toRank) {
        int delta = toRank > fromRank ? 1 : -1;
        for (int r = fromRank + delta; r != toRank; r += delta) {
            if (getPieceAt(new Square(file, r)).isPresent())
                return false;
        }
        return true;
    }

    public boolean isRankOpen(int rank, int fromFile, int toFile) {
        int delta = toFile > fromFile ? 1 : -1;
        for (int f = fromFile + delta; f != toFile; f += delta) {
            if (getPieceAt(new Square(f, rank)).isPresent())
                return false;
        }
        return true;
    }

    public boolean isDiagonalOpen(int fromFile, int fromRank, int toFile, int toRank) {
        int fileDelta = toFile > fromFile ? 1 : -1;
        int rankDelta = toRank > fromRank ? 1 : -1;
        int f = fromFile + fileDelta;
        int r = fromRank + rankDelta;
        while (f != toFile && r != toRank) {
            if (getPieceAt(new Square(f, r)).isPresent())
                return false;
            f += fileDelta;
            r += rankDelta;
        }
        return true;
    }

    /**
     * Checks if it is possible for a piece on this board to move to a certain square. This method does <strong>not</strong> take checks into account.
     *
     * @param from square the piece is currently at
     * @param to   square the piece is moving to
     * @return {@code true} if the move is possible, otherwise {@code false}
     */
    public boolean isMovePossible(@NotNull Square from, @NotNull Square to) {

        Optional<Piece> optionalPiece = getPieceAt(from);
        if (optionalPiece.isEmpty())
            return false;
        Piece piece = optionalPiece.get();

        Optional<Color> toColor = getPieceAt(to).map(Piece::getColor);
        if (toColor.filter(color -> color == piece.getColor()).isPresent())
            return false;

        int fileDiff = to.getFile() - from.getFile();
        int rankDiff = to.getRank() - from.getRank();
        int absFileDiff = Math.abs(fileDiff);
        int absRankDiff = Math.abs(rankDiff);

        if (piece.getType() == Piece.Type.ROOK || piece.getType() == Piece.Type.QUEEN) {
            if (fileDiff == 0 && isFileOpen(from.getFile(), from.getRank(), to.getRank())
                    || rankDiff == 0 && isRankOpen(from.getRank(), from.getFile(), to.getFile())) {
                return true;
            }
        }
        if (piece.getType() == Piece.Type.BISHOP || piece.getType() == Piece.Type.QUEEN) {
            return absFileDiff == absRankDiff && isDiagonalOpen(from.getFile(), from.getRank(), to.getFile(), to.getRank());
        } else if (piece.getType() == Piece.Type.KNIGHT) {
            return absFileDiff == 2 && absRankDiff == 1 || absFileDiff == 1 && absRankDiff == 2;
        } else if (piece.getType() == Piece.Type.KING) {
            return absFileDiff <= 1 && absRankDiff <= 1;
        } else if (piece.getType() == Piece.Type.PAWN) {
            int pawnDirection = piece.getColor().getPawnDirection();
            int pawnStartRank = piece.getColor().getBackRank() + pawnDirection;
            boolean capture = to.equals(enPassantSquare) || toColor.filter(color -> color != piece.getColor()).isPresent();
            return absFileDiff == (capture ? 1 : 0) &&
                    (rankDiff == pawnDirection || !capture && from.getRank() == pawnStartRank && rankDiff == 2 * pawnDirection);
        }

        return false;
    }

    /**
     * Finds a certain piece on this board.
     *
     * @param piece piece
     * @return optional square, empty if the piece is not on this board
     */
    public @NotNull Optional<Square> findPiece(@NotNull Piece piece) {
        for (Square square : this) {
            if (isPieceAt(square, piece))
                return Optional.of(square);
        }
        return Optional.empty();
    }

    /**
     * Checks if a certain king on this board is in check.
     *
     * @param color king color
     * @return {@code true} if the king is in check, otherwise {@code false}
     */
    public boolean isInCheck(@NotNull Color color) {

        Optional<Square> optionalSquare = findPiece(new Piece(color, Piece.Type.KING));
        if (optionalSquare.isEmpty())
            return false;
        Square kingSquare = optionalSquare.get();

        for (Square square : this) {
            if (isMovePossible(square, kingSquare))
                return true;
        }

        return false;
    }

    /**
     * Checks if a move is legal to play on this board.
     *
     * @param move move
     * @return {@code true} if the move is legal, otherwise {@code false}
     */
    public boolean isLegalMove(@NotNull Move move) {
        if (!move.isPossible(this))
            return false;

        VirtualBoard virtualBoard = virtual();
        move.play(virtualBoard);

        return !virtualBoard.isInCheck(activeColor);
    }

    /**
     * Performs a move.
     *
     * @param move move
     */
    public void performMove(@NotNull Move move) {
        if (!isLegalMove(move))
            ChessPlugin.getInstance().getLogger().warning("Playing illegal move " + move.toSAN());

        move.play(this);

        validateCastling();

        enPassantSquare = move.getEnPassantSquare().orElse(null);

        if (move.isPawnMoveOrCapture())
            halfMoves = 0;
        else
            halfMoves++;

        activeColor = activeColor.opposite();
        if (activeColor == Color.WHITE)
            moveNumber++;
    }

    public void validateCastling() {
        for (Color color : Color.values()) {
            if (canShortCastle.contains(color) || canLongCastle.contains(color)) {
                int backRank = color.getBackRank();

                if (!isPieceAt(new Square(4, backRank), new Piece(color, Piece.Type.KING))) {
                    canShortCastle.remove(color);
                    canLongCastle.remove(color);
                    continue;
                }

                if (canShortCastle.contains(color) && !isPieceAt(new Square(7, backRank), new Piece(color, Piece.Type.ROOK)))
                    canShortCastle.remove(color);
                if (canLongCastle.contains(color) && !isPieceAt(new Square(0, backRank), new Piece(color, Piece.Type.ROOK)))
                    canLongCastle.remove(color);

            }
        }
    }

    private boolean isEnPassantSquareValid() {
        if (enPassantSquare == null)
            return true;

        if (!Board.inBounds(enPassantSquare))
            return false;

        if (getPieceAt(enPassantSquare).isPresent())
            return false;

        int pawnRank = enPassantSquare.getRank() + activeColor.opposite().getPawnDirection();
        return isPieceAt(new Square(enPassantSquare.getFile(), pawnRank), new Piece(activeColor.opposite(), Piece.Type.PAWN));
    }

    public void validateEnPassantSquare() {
        if (!isEnPassantSquareValid()) {
            enPassantSquare = null;
        }
    }

    /**
     * Builds a FEN position string  from this board.
     *
     * @return FEN piece placement data
     * @see <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">Forsyth–Edwards Notation</a>
     */
    protected @NotNull String buildPositionString() {
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
    protected void applyPositionString(@NotNull String position) {
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

    /**
     * Builds a FEN string from this board.
     *
     * @return FEN record
     * @see <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">Forsyth–Edwards Notation</a>
     */
    public @NotNull String toFEN() {
        validateCastling();
        validateEnPassantSquare();

        StringBuilder builder = new StringBuilder(buildPositionString());

        builder.append(' ');
        builder.append(activeColor.getLetter());

        builder.append(' ');
        if (canShortCastle.isEmpty() && canLongCastle.isEmpty()) {
            builder.append('-');
        } else {
            if (canShortCastle.contains(Color.WHITE))
                builder.append('K');
            if (canLongCastle.contains(Color.WHITE))
                builder.append('Q');
            if (canShortCastle.contains(Color.BLACK))
                builder.append('k');
            if (canLongCastle.contains(Color.BLACK))
                builder.append('q');
        }

        builder.append(' ');
        if (enPassantSquare == null) {
            builder.append('-');
        } else {
            builder.append(enPassantSquare);
        }

        builder.append(' ');
        builder.append(halfMoves);

        builder.append(' ');
        builder.append(moveNumber);

        return builder.toString();
    }

    /**
     * Loads a FEN string onto this board.
     *
     * @param fen FEN record
     * @see <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">Forsyth–Edwards Notation</a>
     */
    public void loadFEN(@NotNull String fen) {
        String[] split = fen.split(" ");

        activeColor = Color.fromLetter(split[1].charAt(0));

        canShortCastle = new HashSet<>(2);
        canLongCastle = new HashSet<>(2);
        for (char letter : split[2].toCharArray()) {
            switch (letter) {
                case 'K' -> canShortCastle.add(Color.WHITE);
                case 'Q' -> canLongCastle.add(Color.WHITE);
                case 'k' -> canShortCastle.add(Color.BLACK);
                case 'q' -> canLongCastle.add(Color.BLACK);
            }
        }

        enPassantSquare = null;
        if (split[3].charAt(0) != '-')
            enPassantSquare = Square.fromString(split[3]);

        halfMoves = Integer.parseInt(split[4]);
        moveNumber = Integer.parseInt(split[5]);

        applyPositionString(split[0]);
    }

    @Override
    public String toString() {
        return toFEN();
    }

}
