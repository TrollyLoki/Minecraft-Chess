package net.trollyloki.mcchess.game;

import net.trollyloki.mcchess.ChessPlugin;
import net.trollyloki.mcchess.Color;
import net.trollyloki.mcchess.board.Board;
import net.trollyloki.mcchess.game.move.Move;
import net.trollyloki.mcchess.game.player.ChessPlayer;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class Game {

    private final @NotNull Board board;

    private final @Nullable String initialFen;
    private final int initialMoveNumber;
    private final @NotNull Color initialActiveColor;

    private @NotNull String event = "Minecraft Chess Game";
    private @NotNull LocalDateTime startTime = LocalDateTime.now();
    private int round = 1;

    private final @NotNull Map<Color, ChessPlayer> players = new HashMap<>();

    private final @NotNull List<Move> moves = new LinkedList<>();
    private @NotNull String result = "*";

    public Game(@NotNull Board board) {
        this.board = board;

        String fen = board.toFEN();
        this.initialFen = fen.equals(Board.STANDARD_FEN) ? null : fen;
        this.initialMoveNumber = board.getMoveNumber();
        this.initialActiveColor = board.getActiveColor();
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

    public @NotNull Optional<ChessPlayer> getPlayer(@NotNull Color color) {
        return Optional.ofNullable(players.get(color));
    }

    public void setPlayer(@NotNull Color color, @NotNull ChessPlayer player) {
        players.put(color, player);
    }

    public @UnmodifiableView List<Move> getMoves() {
        return Collections.unmodifiableList(moves);
    }

    public @NotNull String getResult() {
        return result;
    }

    public @NotNull CompletableFuture<Boolean> play() {
        Optional<ChessPlayer> player = getPlayer(board.getActiveColor());
        if (player.isEmpty())
            return CompletableFuture.completedFuture(false);

        return play(player.get()).thenApply(v -> true);
    }

    public @NotNull CompletableFuture<Move> play(@NotNull ChessPlayer player) {
        CompletableFuture<Move> future = new CompletableFuture<>();
        player.chooseMove(board).whenComplete((move, throwable) -> {
            if (move != null) {
                Bukkit.getScheduler().runTask(ChessPlugin.getInstance(), () -> {
                    playMove(move);
                    future.complete(move);
                });
            } else {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    public void playMove(@NotNull Move move) {
        board.performMove(move);
        postMove(move);
    }

    /**
     * Updates the state of this game assuming a given move has been made.
     *
     * @param move move
     */
    public void postMove(@NotNull Move move) {
        moves.add(move);
        getPlayer(board.getActiveColor()).ifPresent(player -> player.chooseMove(board, move));
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
        for (Move move : moves) {
            if (activeColor == Color.WHITE) {
                if (moveNumber != initialMoveNumber)
                    builder.append(' ');
                builder.append(moveNumber).append('.');
            }

            builder.append(' ');
            builder.append(move.toSAN());

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
                ", initialFen='" + initialFen + '\'' +
                ", event='" + event + '\'' +
                ", startTime=" + startTime +
                ", round=" + round +
                ", players=" + players +
                ", moves=" + moves +
                ", result='" + result + '\'' +
                '}';
    }

}
