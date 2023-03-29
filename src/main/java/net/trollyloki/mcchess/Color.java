package net.trollyloki.mcchess;

import it.unimi.dsi.fastutil.chars.Char2CharFunction;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public enum Color {
    WHITE('w', 0, 1, Character::toUpperCase),
    BLACK('b', 7, -1, Character::toLowerCase);

    private static final Map<Character, Color> LETTER_MAP = new HashMap<>();

    static {
        for (Color color : values())
            LETTER_MAP.put(color.letter, color);
    }

    private final char letter;
    private final int backRank, pawnDirection;
    private final @NotNull Char2CharFunction charFunction;

    Color(char letter, int backRank, int pawnDirection, @NotNull Char2CharFunction charFunction) {
        this.letter = letter;
        this.backRank = backRank;
        this.pawnDirection = pawnDirection;
        this.charFunction = charFunction;
    }

    public char getLetter() {
        return letter;
    }

    public static @NotNull Color fromLetter(char letter) {
        letter = Character.toLowerCase(letter);
        if (!LETTER_MAP.containsKey(letter))
            throw new IllegalArgumentException("Invalid FEN letter");
        return LETTER_MAP.get(letter);
    }

    public int getBackRank() {
        return backRank;
    }

    public int getPawnDirection() {
        return pawnDirection;
    }

    public char convertLetter(char c) {
        return charFunction.get(c);
    }

    public @NotNull Color opposite() {
        return switch (this) {
            case WHITE -> BLACK;
            case BLACK -> WHITE;
        };
    }

}
