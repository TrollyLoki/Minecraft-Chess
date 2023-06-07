package net.trollyloki.mcchess.board;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SquareIterator implements Iterator<Square> {

    private int file, rank;

    public SquareIterator(int file, int rank) {
        this.file = file;
        this.rank = rank;
    }

    public SquareIterator() {
        this(0, 0);
    }

    @Override
    public boolean hasNext() {
        return file < 8 && rank < 8;
    }

    @Override
    public Square next() {
        if (!hasNext())
            throw new NoSuchElementException();

        Square current = new Square(file, rank);
        if (++rank >= 8) {
            rank = 0;
            file++;
        }
        return current;
    }

}
