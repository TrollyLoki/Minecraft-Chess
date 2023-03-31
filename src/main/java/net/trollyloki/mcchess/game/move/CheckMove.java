package net.trollyloki.mcchess.game.move;

import org.jetbrains.annotations.NotNull;

public class CheckMove implements Move {

    protected final @NotNull Move move;
    protected final @NotNull CheckStatus checkStatus;

    public CheckMove(@NotNull Move move, @NotNull CheckStatus checkStatus) {
        this.move = move;
        this.checkStatus = checkStatus;
    }

    @Override
    public @NotNull String toUCI() {
        return move.toUCI();
    }

    @Override
    public @NotNull String toSAN() {
        return move.toSAN() + checkStatus.getPostfix();
    }

    @Override
    public String toString() {
        return toSAN();
    }

}
