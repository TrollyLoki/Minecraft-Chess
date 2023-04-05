package net.trollyloki.mcchess;

import net.trollyloki.mcchess.board.Square;
import net.trollyloki.mcchess.game.Game;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class GameSquare {

    private final @NotNull Game game;
    private final @NotNull Square square;

    public GameSquare(@NotNull Game game, @NotNull Square square) {
        this.game = game;
        this.square = square;
    }

    public @NotNull Game getGame() {
        return game;
    }

    public @NotNull Square getSquare() {
        return square;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GameSquare that = (GameSquare) o;
        return Objects.equals(game, that.game) && Objects.equals(square, that.square);
    }

    @Override
    public int hashCode() {
        return Objects.hash(game, square);
    }

}
