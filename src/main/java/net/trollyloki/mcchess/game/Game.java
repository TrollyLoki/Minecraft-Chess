package net.trollyloki.mcchess.game;

import net.trollyloki.mcchess.Color;
import net.trollyloki.mcchess.board.Board;
import net.trollyloki.mcchess.board.Piece;
import net.trollyloki.mcchess.board.Square;
import net.trollyloki.mcchess.game.move.Move;
import net.trollyloki.mcchess.game.player.ChessPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Game {

    public static final @NotNull String STANDARD_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private final @NotNull Board board;

    private final @Nullable String initialFen;
    private final int initialMoveNumber;
    private final @NotNull Color initialActiveColor;

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
        this.initialFen = fen.equals(STANDARD_FEN) ? null : fen;
        this.initialMoveNumber = this.moveNumber;
        this.initialActiveColor = this.activeColor;
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

    public @NotNull String getEvent() {
        return event;
    }

    public void setEvent(@NotNull String event) {
        this.event = event;
    }

    public @NotNull LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime() {
        this.startTime = LocalDateTime.now();
    }

    public int getRound() {
        return round;
    }

    public void setRound(int round) {
        this.round = round;
    }

    public @UnmodifiableView List<String> getMoves() {
        return Collections.unmodifiableList(moves);
    }

    public @NotNull String getResult() {
        return result;
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

    public void setCanShortCastle(@NotNull Color color, boolean canCastle) {
        if (canCastle)
            canShortCastle.add(color);
        else
            canShortCastle.remove(color);
    }

    public void setCanLongCastle(@NotNull Color color, boolean canCastle) {
        if (canCastle)
            canLongCastle.add(color);
        else
            canLongCastle.remove(color);
    }

    public void setEnPassantSquare(@Nullable Square enPassantSquare) {
        this.enPassantSquare = enPassantSquare;
    }

    /**
     * Performs a move.
     *
     * @param move move
     */
    public void performMove(@NotNull Move move) {
        move.play(this.getBoard());
        moves.add(move.toSAN());

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

    /**
     * Performs a move specified by UCI LAN.
     *
     * @param uciMove UCI LAN
     */
    public void performUciMove(@NotNull String uciMove) {
        performMove(Move.fromUCI(uciMove, board));
    }

    /**
     * Performs a move specified by SAN.
     *
     * @param san SAN
     */
    public void performSanMove(@NotNull String san) {
        performMove(Move.fromSAN(san, this));
    }

    public void validateCastling() {
        for (Color color : Color.values()) {
            if (canShortCastle.contains(color) || canLongCastle.contains(color)) {
                int backRank = color.getBackRank();

                if (!board.isPieceAt(new Square(4, backRank), new Piece(color, Piece.Type.KING))) {
                    canShortCastle.remove(color);
                    canLongCastle.remove(color);
                    continue;
                }

                if (canShortCastle.contains(color) && !board.isPieceAt(new Square(7, backRank), new Piece(color, Piece.Type.ROOK)))
                    canShortCastle.remove(color);
                if (canLongCastle.contains(color) && !board.isPieceAt(new Square(0, backRank), new Piece(color, Piece.Type.ROOK)))
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
        builder.append('\n');

        if (initialActiveColor == Color.BLACK)
            builder.append(initialMoveNumber).append("...");

        int moveNumber = initialMoveNumber;
        Color activeColor = initialActiveColor;
        for (String san : moves) {
            if (activeColor == Color.WHITE) {
                if (moveNumber != initialMoveNumber)
                    builder.append(' ');
                builder.append(moveNumber).append('.');
            }

            builder.append(' ');
            builder.append(san);

            if (activeColor == Color.BLACK)
                moveNumber++;
            activeColor = activeColor.opposite();
        }

        if (!result.equals("*"))
            builder.append(' ').append(result);

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
