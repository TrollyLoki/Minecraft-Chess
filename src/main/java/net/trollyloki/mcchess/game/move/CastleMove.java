package net.trollyloki.mcchess.game.move;

import net.trollyloki.mcchess.Color;
import org.jetbrains.annotations.NotNull;

public class CastleMove implements Move {

    protected final @NotNull Color color;
    protected final boolean queenside;

    public CastleMove(@NotNull Color color, boolean queenside) {
        this.color = color;
        this.queenside = queenside;
    }

    public @NotNull Color getColor() {
        return color;
    }

    public boolean isQueenside() {
        return queenside;
    }

    @Override
    public @NotNull String toUCI() {
        return switch (color) {
            case WHITE -> queenside ? "e1c1" : "e1g1";
            case BLACK -> queenside ? "e8c8" : "e8g8";
        };
    }

    @Override
    public @NotNull String toSAN() {
        StringBuilder builder = new StringBuilder(5);
        builder.append("O-O");
        if (queenside)
            builder.append("-O");
        return builder.toString();
    }

    @Override
    public String toString() {
        return toSAN();
    }

}
