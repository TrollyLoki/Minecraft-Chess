package net.trollyloki.mcchess.game;

import net.trollyloki.mcchess.Color;
import net.trollyloki.mcchess.board.Board;
import net.trollyloki.mcchess.board.Piece;
import net.trollyloki.mcchess.board.Square;
import net.trollyloki.mcchess.game.player.ChessPlayer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class Game {

    private static final @NotNull String STANDARD_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private final @NotNull Board board;
    private @Nullable String initialFen;

    private @NotNull String event = "Minecraft Chess Game";
    private @NotNull LocalDateTime startTime = LocalDateTime.now();
    private int round = 1;

    private final @NotNull List<String> moves = new LinkedList<>();
    private @NotNull String result = "*";

    private final @NotNull Map<Color, ChessPlayer> players = new HashMap<>();

    private @NotNull Color activeColor;
    private final @NotNull Set<Color> canShortCastle, canLongCastle;
    private @Nullable Square enPassantSquare;
    private int halfMoves, moveNumber;

    public Game(@NotNull Board board, @NotNull Color activeColor, @NotNull Set<Color> canShortCastle, @NotNull Set<Color> canLongCastle, @Nullable Square enPassantSquare, int halfMoves, int moveNumber) {
        this.board = board;
        this.activeColor = activeColor;
        this.canShortCastle = new HashSet<>(canShortCastle);
        this.canLongCastle = new HashSet<>(canLongCastle);
        this.enPassantSquare = enPassantSquare;
        this.halfMoves = halfMoves;
        this.moveNumber = moveNumber;

        String fen = toFEN();
        if (!fen.equals(STANDARD_FEN))
            this.initialFen = toFEN();
    }

    public Game(@NotNull Board board) {
        this(board, Color.WHITE, Set.of(Color.values()), Set.of(Color.values()), null, 0, 1);
        validateCastling();
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

    /**
     * Perform a move specified by UCI LAN.
     *
     * @param move move LAN
     * @return {@code true} if a piece was moved, otherwise {@code false}
     */
    public boolean performMove(@NotNull String move) {
        move = move.toLowerCase(Locale.ROOT);

        // Parse string

        Square from = Square.fromString(move.substring(0, 2));
        Square to = Square.fromString(move.substring(2, 4));

        Piece.Type promotionPiece = null;
        if (move.length() > 4)
            promotionPiece = Piece.Type.fromLetter(move.charAt(4));

        // Find current pieces
        Optional<Piece> pieceOptional = board.getPieceAt(from);
        if (pieceOptional.isEmpty())
            return false;
        Piece piece = pieceOptional.get();
        Optional<Piece> toPiece = board.getPieceAt(to);
        boolean capturing = toPiece.isPresent();

        // Move piece
        if (!board.movePiece(from, to))
            return false;

        // Handle promotion
        if (promotionPiece != null)
            board.setPieceAt(to, new Piece(piece.getColor(), promotionPiece));

        // Handle en passant

        enPassantSquare = null;

        if (piece.getType() == Piece.Type.PAWN) {

            if (to.getFile() != from.getFile() && !capturing) {
                capturing = true;

                int pawnRank = to.getRank() + piece.getColor().opposite().getPawnDirection();

                board.getItemFrameFor(new Square(to.getFile(), pawnRank)).ifPresent(frame -> {
                    ItemStack existingItem = frame.getItem();
                    if (existingItem.getType() != Material.AIR) {
                        frame.setItem(null);
                        frame.getWorld().dropItem(frame.getLocation(), existingItem);
                    }
                });

            }

            if (Math.abs(to.getRank() - from.getRank()) == 2) {
                enPassantSquare = new Square(to.getFile(), (from.getRank() + to.getRank()) / 2);
            }

        }

        // Handle castling

        if (piece.getType() == Piece.Type.KING) {
            if (move.startsWith("e1g1")) // white short castling
                board.movePiece(new Square(7, 0), new Square(5, 0));
            else if (move.startsWith("e1c1")) // white long castling
                board.movePiece(new Square(0, 0), new Square(3, 0));
            else if (move.startsWith("e8g8")) // black short castling
                board.movePiece(new Square(7, 7), new Square(5, 7));
            else if (move.startsWith("e8c8")) // black long castling
                board.movePiece(new Square(0, 7), new Square(3, 7));

            canShortCastle.remove(piece.getColor());
            canLongCastle.remove(piece.getColor());
        }

        if (piece.getType() == Piece.Type.ROOK && from.getRank() == piece.getColor().getBackRank()) {

            if (from.getFile() == 0)
                canLongCastle.remove(piece.getColor());
            else if (from.getFile() == 7)
                canShortCastle.remove(piece.getColor());

        }

        // Update move info

        if (capturing || piece.getType() == Piece.Type.PAWN)
            halfMoves = 0;
        else
            halfMoves++;

        activeColor = activeColor.opposite();
        if (activeColor == Color.WHITE)
            moveNumber++;

        return true;
    }

    public void validateCastling() {
        for (Color color : Color.values()) {
            if (canShortCastle.contains(color) || canLongCastle.contains(color)) {
                int backRank = color.getBackRank();
                boolean kingValid = board.isPieceAt(new Square(4, backRank), new Piece(color, Piece.Type.KING));
                if (!kingValid || !board.isPieceAt(new Square(7, backRank), new Piece(color, Piece.Type.ROOK)))
                    canShortCastle.remove(color);
                if (!kingValid || !board.isPieceAt(new Square(0, backRank), new Piece(color, Piece.Type.ROOK)))
                    canLongCastle.remove(color);
            }
        }
    }

    private boolean isEnPassantSquareValid() {
        if (enPassantSquare == null)
            return true;

        if (!Board.inBounds(enPassantSquare))
            return false;

        if (board.getPieceAt(enPassantSquare).isPresent())
            return false;

        int pawnRank = enPassantSquare.getRank() + activeColor.opposite().getPawnDirection();
        return board.isPieceAt(new Square(enPassantSquare.getFile(), pawnRank), new Piece(activeColor.opposite(), Piece.Type.PAWN));
    }

    public void validateEnPassantSquare() {
        if (!isEnPassantSquareValid()) {
            enPassantSquare = null;
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
     * Creates a game from a FEN string.
     *
     * @param fen   FEN record
     * @param board board to load the game onto
     * @return game
     * @see <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">Forsyth–Edwards Notation</a>
     */
    public static @NotNull Game fromFEN(@NotNull String fen, @NotNull Board board) {
        String[] split = fen.split(" ");

        Color activeColor = Color.fromLetter(split[1].charAt(0));

        Set<Color> canShortCastle = new HashSet<>(2);
        Set<Color> canLongCastle = new HashSet<>(2);
        for (char letter : split[2].toCharArray()) {
            switch (letter) {
                case 'K' -> canShortCastle.add(Color.WHITE);
                case 'Q' -> canLongCastle.add(Color.WHITE);
                case 'k' -> canShortCastle.add(Color.BLACK);
                case 'q' -> canLongCastle.add(Color.BLACK);
            }
        }

        Square enPassantSquare = null;
        if (split[3].charAt(0) != '-')
            enPassantSquare = Square.fromString(split[3]);

        int halfMoves = Integer.parseInt(split[4]);
        int moves = Integer.parseInt(split[5]);

        board.loadFromFEN(split[0]);

        return new Game(board, activeColor, canShortCastle, canLongCastle, enPassantSquare, halfMoves, moves);
    }

    private static final @NotNull DateTimeFormatter
            PGN_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.ROOT),
            PGN_TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);

    /**
     * Saves this game in PGN format.
     *
     * @return PGN text
     */
    public @NotNull String toPGN() {
        StringBuilder builder = new StringBuilder();

        // Required tags
        builder.append("[Event \"").append(event).append("\"]\n");
        builder.append("[Site \"").append(board.getSite()).append("\"]\n");
        builder.append("[Date \"").append(startTime.format(PGN_DATE_FORMAT)).append("\"]\n");
        builder.append("[Round \"").append(round).append("\"]\n");
        builder.append("[White \"").append(
                players.containsKey(Color.WHITE) ? players.get(Color.WHITE).getName() : "White"
        ).append("\"]\n");
        builder.append("[Black \"").append(
                players.containsKey(Color.BLACK) ? players.get(Color.BLACK).getName() : "Black"
        ).append("\"]\n");
        builder.append("[Result \"").append(result).append("\"]\n");

        // Optional tags
        builder.append("[Time \"").append(startTime.format(PGN_TIME_FORMAT)).append("\"]\n");
        builder.append("[Mode \"ICS\"]\n");
        if (initialFen != null) {
            builder.append("[SetUp \"1\"]\n");
            builder.append("[FEN \"").append(initialFen).append("\"]\n");
        }

        // Movetext
        //TODO

        return builder.toString();
    }

    @Override
    public String toString() {
        return "Game{" +
                "board=" + board +
                ", activeColor=" + activeColor +
                ", canShortCastle=" + canShortCastle +
                ", canLongCastle=" + canLongCastle +
                ", enPassantSquare=" + enPassantSquare +
                ", halfMoves=" + halfMoves +
                ", moves=" + moveNumber +
                '}';
    }

}
