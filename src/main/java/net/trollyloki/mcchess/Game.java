package net.trollyloki.mcchess;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class Game {

    private static final int NONE = -1;

    private final @NotNull Board board;
    private @NotNull Piece.Color activeColor;
    private final @NotNull Set<Piece.Color> canShortCastle, canLongCastle;
    private int enPassantFile, enPassantRank;
    private int halfMoves, moveNumber;

    public Game(@NotNull Board board, @NotNull Piece.Color activeColor, @NotNull Set<Piece.Color> canShortCastle, @NotNull Set<Piece.Color> canLongCastle, int enPassantFile, int enPassantRank, int halfMoves, int moveNumber) {
        this.board = board;
        this.activeColor = activeColor;
        this.canShortCastle = new HashSet<>(canShortCastle);
        this.canLongCastle = new HashSet<>(canLongCastle);
        this.enPassantFile = enPassantFile;
        this.enPassantRank = enPassantRank;
        this.halfMoves = halfMoves;
        this.moveNumber = moveNumber;
    }

    public Game(@NotNull Board board) {
        this(board, Piece.Color.WHITE, Set.of(Piece.Color.values()), Set.of(Piece.Color.values()), NONE, NONE, 0, 1);
    }

    /**
     * Gets the board this game is being played on.
     *
     * @return board
     */
    public @NotNull Board getBoard() {
        return board;
    }

    /**
     * Gets the color that moves next.
     *
     * @return piece color
     */
    public @NotNull Piece.Color getActiveColor() {
        return activeColor;
    }

    /**
     * Sets the color that moves next.
     *
     * @param activeColor piece color
     */
    public void setActiveColor(@NotNull Piece.Color activeColor) {
        this.activeColor = activeColor;
    }

    /**
     * Perform a move specified by UCI LAN.
     *
     * @param move move LAN
     * @return {@code true} if a piece was moved, otherwise {@code false}
     */
    public boolean performMove(@NotNull String move) {
        move = move.toLowerCase(Locale.ROOT);

        // Parse string

        int fromFile = move.charAt(0) - 'a';
        int fromRank = move.charAt(1) - '1';

        int toFile = move.charAt(2) - 'a';
        int toRank = move.charAt(3) - '1';

        Piece.Type promotionPiece = null;
        if (move.length() > 4)
            promotionPiece = Piece.Type.fromLetter(move.charAt(4));

        // Find current pieces
        Optional<Piece> pieceOptional = board.getPieceAt(fromFile, fromRank);
        if (pieceOptional.isEmpty())
            return false;
        Piece piece = pieceOptional.get();
        Optional<Piece> toPiece = board.getPieceAt(toFile, toRank);
        boolean capturing = toPiece.isPresent();

        // Move piece
        if (!board.movePiece(fromFile, fromRank, toFile, toRank))
            return false;

        // Handle promotion
        if (promotionPiece != null)
            board.setPieceAt(toFile, toRank, new Piece(piece.getColor(), promotionPiece));

        // Handle en passant

        enPassantFile = NONE;
        enPassantRank = NONE;

        if (piece.getType() == Piece.Type.PAWN) {

            if (toFile != fromFile && !capturing) {
                capturing = true;

                int pawnRank = toRank + piece.getColor().opposite().getPawnDirection();

                board.getItemFrameAt(toFile, pawnRank).ifPresent(frame -> {
                    ItemStack existingItem = frame.getItem();
                    if (existingItem.getType() != Material.AIR) {
                        frame.setItem(null);
                        frame.getWorld().dropItem(frame.getLocation(), existingItem);
                    }
                });

            }

            if (Math.abs(toRank - fromRank) == 2) {
                enPassantFile = toFile;
                enPassantRank = (fromRank + toRank) / 2;
            }

        }

        // Handle castling

        if (piece.getType() == Piece.Type.KING) {
            if (move.startsWith("e1g1")) // white short castling
                board.movePiece(7, 0, 5, 0);
            else if (move.startsWith("e1c1")) // white long castling
                board.movePiece(0, 0, 3, 0);
            else if (move.startsWith("e8g8")) // black short castling
                board.movePiece(7, 7, 5, 7);
            else if (move.startsWith("e8c8")) // black long castling
                board.movePiece(0, 7, 3, 7);

            canShortCastle.remove(piece.getColor());
            canLongCastle.remove(piece.getColor());
        }

        if (piece.getType() == Piece.Type.ROOK && fromRank == piece.getColor().getBackRank()) {

            if (fromFile == 0)
                canLongCastle.remove(piece.getColor());
            else if (fromFile == 7)
                canShortCastle.remove(piece.getColor());

        }

        // Update move info

        if (capturing || piece.getType() == Piece.Type.PAWN)
            halfMoves = 0;
        else
            halfMoves++;

        activeColor = activeColor.opposite();
        if (activeColor == Piece.Color.WHITE)
            moveNumber++;

        return true;
    }

    public void validateCastling() {
        for (Piece.Color color : Piece.Color.values()) {
            if (canShortCastle.contains(color) || canLongCastle.contains(color)) {
                int backRank = color.getBackRank();
                boolean kingValid = board.isPieceAt(4, backRank, new Piece(color, Piece.Type.KING));
                if (!kingValid || !board.isPieceAt(7, backRank, new Piece(color, Piece.Type.ROOK)))
                    canShortCastle.remove(color);
                if (!kingValid || !board.isPieceAt(0, backRank, new Piece(color, Piece.Type.ROOK)))
                    canLongCastle.remove(color);
            }
        }
    }

    private boolean isEnPassantSquareValid() {
        if (enPassantFile == NONE && enPassantRank == NONE)
            return true;

        if (enPassantFile < 0 || enPassantFile >= Board.SIZE)
            return false;
        if (enPassantRank < 0 || enPassantRank >= Board.SIZE)
            return false;

        if (board.getPieceAt(enPassantFile, enPassantRank).isPresent())
            return false;

        int pawnRank = enPassantRank + activeColor.opposite().getPawnDirection();
        return board.isPieceAt(enPassantFile, pawnRank, new Piece(activeColor.opposite(), Piece.Type.PAWN));
    }

    public void validateEnPassantSquare() {
        if (!isEnPassantSquareValid()) {
            enPassantFile = NONE;
            enPassantRank = NONE;
        }
    }

    /**
     * Builds a FEN string from this game.
     *
     * @return FEN record
     * @see <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">Forsyth–Edwards Notation</a>
     */
    public @NotNull String toFEN() {
        validateCastling();
        validateEnPassantSquare();

        StringBuilder builder = new StringBuilder(board.toFEN());

        builder.append(' ');
        builder.append(activeColor.getLetter());

        builder.append(' ');
        if (canShortCastle.isEmpty() && canLongCastle.isEmpty()) {
            builder.append('-');
        } else {
            if (canShortCastle.contains(Piece.Color.WHITE))
                builder.append('K');
            if (canLongCastle.contains(Piece.Color.WHITE))
                builder.append('Q');
            if (canShortCastle.contains(Piece.Color.BLACK))
                builder.append('k');
            if (canLongCastle.contains(Piece.Color.BLACK))
                builder.append('q');
        }

        builder.append(' ');
        if (enPassantFile == NONE) {
            builder.append('-');
        } else {
            builder.append((char) ('a' + enPassantFile));
            builder.append((char) ('1' + enPassantRank));
        }

        builder.append(' ');
        builder.append(halfMoves);

        builder.append(' ');
        builder.append(moveNumber);

        return builder.toString();
    }

    /**
     * Creates a game from a FEN string.
     *
     * @param fen   FEN record
     * @param board board to load the game onto
     * @return game
     * @see <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">Forsyth–Edwards Notation</a>
     */
    public static @NotNull Game fromFEN(@NotNull String fen, @NotNull Board board) {
        String[] split = fen.split(" ");

        Piece.Color activeColor = Piece.Color.fromLetter(split[1].charAt(0));

        Set<Piece.Color> canShortCastle = new HashSet<>(2);
        Set<Piece.Color> canLongCastle = new HashSet<>(2);
        for (char letter : split[2].toCharArray()) {
            switch (letter) {
                case 'K' -> canShortCastle.add(Piece.Color.WHITE);
                case 'Q' -> canLongCastle.add(Piece.Color.WHITE);
                case 'k' -> canShortCastle.add(Piece.Color.BLACK);
                case 'q' -> canLongCastle.add(Piece.Color.BLACK);
            }
        }

        int enPassantFile = NONE;
        int enPassantRank = NONE;
        if (split[3].charAt(0) != '-') {
            enPassantFile = split[3].charAt(0) - 'a';
            enPassantRank = split[3].charAt(1) - '1';
        }

        int halfMoves = Integer.parseInt(split[4]);
        int moves = Integer.parseInt(split[5]);

        board.loadFromFEN(split[0]);

        return new Game(board, activeColor, canShortCastle, canLongCastle, enPassantFile, enPassantRank, halfMoves, moves);
    }

    @Override
    public String toString() {
        return "Game{" +
                "board=" + board +
                ", activeColor=" + activeColor +
                ", canShortCastle=" + canShortCastle +
                ", canLongCastle=" + canLongCastle +
                ", enPassantFile=" + enPassantFile +
                ", enPassantRank=" + enPassantRank +
                ", halfMoves=" + halfMoves +
                ", moves=" + moveNumber +
                '}';
    }

}
