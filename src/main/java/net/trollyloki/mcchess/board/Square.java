package net.trollyloki.mcchess.board;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Square {

    private final int file, rank;

    /**
     * @param file file index
     * @param rank rank index
     */
    public Square(int file, int rank) {
        this.file = file;
        this.rank = rank;
    }

    /**
     * Gets the file this square is on.
     *
     * @return file index
     */
    public int getFile() {
        return file;
    }

    /**
     * Gets the rank this square is on.
     *
     * @return rank index
     */
    public int getRank() {
        return rank;
    }

    /**
     * Gets a square relative to this square.
     *
     * @param fileDelta difference in file
     * @param rankDelta difference in rank
     * @return new square
     */
    @Contract("_, _ -> new")
    public @NotNull Square relative(int fileDelta, int rankDelta) {
        return new Square(file + fileDelta, rank + rankDelta);
    }

    /**
     * Converts a string like "a1" to a square.
     *
     * @param string string
     * @return square
     * @see #toString()
     */
    public static @NotNull Square fromString(@NotNull String string) {
        if (string.length() != 2)
            throw new IllegalArgumentException("String must be of length 2");
        return new Square(string.charAt(0) - 'a', string.charAt(1) - '1');
    }

    /**
     * Converts this square to a string.
     *
     * @return string
     * @see #fromString(String)
     */
    @Override
    public @NotNull String toString() {
        return new String(new char[]{(char) ('a' + file), (char) ('1' + rank)});
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Square square = (Square) o;
        return file == square.file && rank == square.rank;
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, rank);
    }

}
