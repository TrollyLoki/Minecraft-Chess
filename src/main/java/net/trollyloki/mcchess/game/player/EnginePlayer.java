package net.trollyloki.mcchess.game.player;

import net.andreinc.neatchess.client.UCI;
import net.andreinc.neatchess.client.UCIResponse;
import net.andreinc.neatchess.client.model.BestMove;
import net.andreinc.neatchess.client.model.EngineInfo;
import net.trollyloki.mcchess.ChessPlugin;
import net.trollyloki.mcchess.game.Game;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.concurrent.CompletableFuture;

public class EnginePlayer implements ChessPlayer, AutoCloseable {

    private final @NotNull UCI engine;
    private boolean closed = false;

    private @NotNull String name;
    private final @NotNull EngineInfo engineInfo;

    private int depth = 0;
    private long moveTime = 0;
    private @NotNull Reference<Game> lastGame = new WeakReference<>(null);

    /**
     * Creates a new engine player.
     * <br>
     * <strong>Note:</strong> This starts a separate process on the machine!
     *
     * @param engine         path to the engine executable
     * @param defaultTimeout default command timeout in milliseconds
     */
    public EnginePlayer(@NotNull String engine, long defaultTimeout) {
        this.engine = new UCI(defaultTimeout);
        try {

            this.engineInfo = this.engine.start(engine).getResultOrThrow();
            this.name = this.engineInfo.getName();

        } catch (Exception e) {
            this.engine.close();
            throw e;
        }
    }

    /**
     * Creates a new engine player with a default command timeout of 60 seconds.
     * <br>
     * <strong>Note:</strong> This starts a separate process on the machine!
     *
     * @param engine path to the engine executable
     */
    public EnginePlayer(@NotNull String engine) {
        this(engine, 60000L);
    }

    @Override
    public @NotNull String getName() {
        return name;
    }

    /**
     * Sets the name of this engine player.
     *
     * @param name name
     */
    public void setName(@NotNull String name) {
        this.name = name;
    }

    public @NotNull EngineInfo getEngineInfo() {
        return engineInfo;
    }

    public void setOption(@NotNull String option, @NotNull String value) {
        engine.setOption(option, value);
    }

    public void setThreads(int threads) {
        setOption("Threads", String.valueOf(threads));
    }

    public void setHash(int mb) {
        setOption("Hash", String.valueOf(mb));
    }

    public void setLimitStrength(boolean limitStrength) {
        setOption("UCI_LimitStrength", String.valueOf(limitStrength));
    }

    public void setElo(int elo) {
        setOption("UCI_Elo", String.valueOf(elo));
    }

    /**
     * Sets the depth to search for best moves.
     * <br>
     * Overrides {@link #setMoveTime(long)}
     *
     * @param depth depth
     */
    public void setDepth(int depth) {
        this.depth = depth;
        this.moveTime = 0;
    }

    /**
     * Sets the time to search for best moves.
     * <br>
     * Overrides {@link #setDepth(int)}
     *
     * @param moveTime move time in milliseconds
     */
    public void setMoveTime(long moveTime) {
        this.moveTime = moveTime;
        this.depth = 0;
    }

    private UCIResponse<BestMove> bestMove() {
        if (depth != 0)
            return engine.bestMove(depth);
        else if (moveTime != 0)
            return engine.bestMove(moveTime);
        else
            return engine.bestMove(engine.getDefaultTimeout());
    }

    @Override
    public @NotNull CompletableFuture<Boolean> play(@NotNull Game game) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(ChessPlugin.getInstance(), () -> {
            String fen = game.toFEN();

            Bukkit.getScheduler().runTaskAsynchronously(ChessPlugin.getInstance(), () -> {
                try {

                    if (!lastGame.refersTo(game)) {
                        engine.uciNewGame();
                        lastGame = new WeakReference<>(game);
                    }
                    engine.positionFen(fen);

                    String bestMove = bestMove().getResultOrThrow().getCurrent();
                    Bukkit.getScheduler().runTask(ChessPlugin.getInstance(), () -> {
                        try {

                            game.performUciMove(bestMove);
                            future.complete(true);

                        } catch (Exception e) {
                            future.completeExceptionally(e);
                        }
                    });

                } catch (Exception e) {
                    future.completeExceptionally(e);
                }
            });

        });
        return future;
    }

    /**
     * Checks if the engine is closed.
     *
     * @return {@code true} if the engine process has been killed, otherwise {@code false}
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Kills the engine process.
     */
    @Override
    public void close() {
        if (closed)
            return;

        engine.close();
        closed = true;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

}
