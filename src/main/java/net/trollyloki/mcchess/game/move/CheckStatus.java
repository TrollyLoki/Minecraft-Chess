package net.trollyloki.mcchess.game.move;

import org.jetbrains.annotations.NotNull;

public enum CheckStatus {
    CHECK("+"), CHECKMATE("#");

    private final @NotNull String postfix;

    CheckStatus(@NotNull String postfix) {
        this.postfix = postfix;
    }

    public @NotNull String getPostfix() {
        return postfix;
    }

}
