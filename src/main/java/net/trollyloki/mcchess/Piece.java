package net.trollyloki.mcchess;

import it.unimi.dsi.fastutil.chars.Char2CharFunction;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Piece {

    private final @NotNull Color color;
    private final @NotNull Type type;

    /**
     * Instantiates a new piece.
     *
     * @param color color
     * @param type type
     */
    public Piece(@NotNull Color color, @NotNull Type type) {
        this.color = color;
        this.type = type;
    }

    /**
     * Gets the color of this piece.
     *
     * @return color
     */
    public @NotNull Color getColor() {
        return color;
    }

    /**
     * Gets the type of this piece.
     *
     * @return type
     */
    public @NotNull Type getType() {
        return type;
    }

    /**
     * Gets the letter used to represent this piece in FEN.
     *
     * @return letter
     */
    public char getLetter() {
        return color.convertLetter(type.getLetter());
    }

    /**
     * Gets a piece from its representation in FEN.
     *
     * @param letter letter
     * @return piece
     * @throws IllegalArgumentException if the letter is not a valid representation of a piece
     */
    public static @NotNull Piece fromLetter(char letter) {
        Type type = Type.fromLetter(letter);
        Color color = Character.isLowerCase(letter) ? Color.BLACK : Color.WHITE;
        return new Piece(color, type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Piece piece = (Piece) o;
        return color == piece.color && type == piece.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color, type);
    }

    public enum Type {
        KING('K'), QUEEN('Q'), ROOK('R'), BISHOP('B'), KNIGHT('N'), PAWN('P');

        private static final Map<Character, Type> LETTER_MAP = new HashMap<>();
        static {
            for (Type type : values())
                LETTER_MAP.put(type.letter, type);
        }

        private final char letter;

        Type(char letter) {
            this.letter = letter;
        }

        public char getLetter() {
            return letter;
        }

        public static @NotNull Type fromLetter(char letter) {
            letter = Character.toUpperCase(letter);
            if (!LETTER_MAP.containsKey(letter))
                throw new IllegalArgumentException("Invalid FEN letter");
            return LETTER_MAP.get(letter);
        }

    }

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

}
