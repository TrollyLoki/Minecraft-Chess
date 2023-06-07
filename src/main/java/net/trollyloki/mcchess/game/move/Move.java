package net.trollyloki.mcchess.game.move;

import net.trollyloki.mcchess.Color;
import net.trollyloki.mcchess.board.Board;
import net.trollyloki.mcchess.board.Piece;
import net.trollyloki.mcchess.board.Square;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public interface Move {

    /**
     * Checks if this move is a pawn move or capture. (for fifty-move rule)
     *
     * @return {@code true} if this move is a pawn move or capture, otherwise {@code false}
     */
    boolean isPawnMoveOrCapture();

    /**
     * Gets the en passant target square created by this move.
     *
     * @return optional en passant target square
     */
    @NotNull Optional<Square> getEnPassantSquare();

    /**
     * Checks if it is possible to play this move on a certain board. This method does <strong>not</strong> take checks into account.
     *
     * @param board board
     * @return {@code true} if the move is possible, otherwise {@code false}
     */
    boolean isPossible(@NotNull Board board);

    /**
     * Plays this move on a board.
     *
     * @param board game
     */
    void play(@NotNull Board board);

    /**
     * Gets the string used to communicate this move over UCI.
     *
     * @return UCI LAN
     */
    @NotNull String toUCI();

    /**
     * Gets the Standard Algebraic Notation for this move
     *
     * @return SAN
     */
    @NotNull String toSAN();

    @Contract("_, _, _, _ -> new")
    static @NotNull Move normal(@NotNull Piece.Type pieceType, @NotNull Square from, @NotNull Square to, boolean capture) {
        return new NormalMove(pieceType, from, to, capture);
    }

    @Contract("_, _, _ -> new")
    static @NotNull Move normal(@NotNull Piece.Type pieceType, @NotNull Square from, @NotNull Square to) {
        return new NormalMove(pieceType, from, to, false);
    }

    @Contract("_, _, _, _ -> new")
    static @NotNull Move promotion(@NotNull Square from, @NotNull Square to, @NotNull Piece.Type promotionType, boolean capture) {
        return new PromotionMove(from, to, capture, promotionType);
    }

    @Contract("_, _, _ -> new")
    static @NotNull Move promotion(@NotNull Square from, @NotNull Square to, @NotNull Piece.Type promotionType) {
        return new PromotionMove(from, to, false, promotionType);
    }

    @Contract("_ -> new")
    static @NotNull Move shortCastle(@NotNull Color color) {
        return new CastleMove(color, false);
    }

    @Contract("_ -> new")
    static @NotNull Move longCastle(@NotNull Color color) {
        return new CastleMove(color, true);
    }

    @Contract("_ -> new")
    default @NotNull Move check(@NotNull CheckStatus checkStatus) {
        return new CheckMove(this, checkStatus);
    }

    /**
     * Parses a move from UCI LAN in the context of a board.
     *
     * @param uciMove UCI LAN
     * @param board   board, for context
     * @return move
     */
    static @NotNull Move fromUCI(@NotNull String uciMove, @NotNull Board board) {
        switch (uciMove) {
            case "e1g1" -> {
                return new CastleMove(Color.WHITE, false);
            }
            case "e1c1" -> {
                return new CastleMove(Color.WHITE, true);
            }
            case "e8g8" -> {
                return new CastleMove(Color.BLACK, false);
            }
            case "e8c8" -> {
                return new CastleMove(Color.BLACK, true);
            }
        }

        Square from = Square.fromString(uciMove.substring(0, 2));
        Piece.Type pieceType = board.getPieceAt(from).map(Piece::getType).orElseThrow();

        Square to = Square.fromString(uciMove.substring(2, 4));
        boolean capture = board.getPieceAt(to).isPresent()
                || pieceType == Piece.Type.PAWN && from.getFile() != to.getFile();

        if (uciMove.length() > 4)
            return new PromotionMove(from, to, capture, Piece.Type.fromLetter(uciMove.charAt(4)));
        else
            return new NormalMove(pieceType, from, to, capture);
    }

    /**
     * Parses a move from SAN in the context of a board.
     *
     * @param san   SAN
     * @param board board, for context
     * @return move
     */
    static @NotNull Move fromSAN(@NotNull String san, @NotNull Board board) {
        switch (san) {
            case "O-O" -> {
                return new CastleMove(board.getActiveColor(), false);
            }
            case "O-O-O" -> {
                return new CastleMove(board.getActiveColor(), true);
            }
        }

        Piece.Type pieceType;
        try {
            pieceType = Piece.Type.fromLetter(san.charAt(0));
            san = san.substring(1);
        } catch (IllegalArgumentException e) {
            pieceType = Piece.Type.PAWN;
        }

        CheckStatus checkStatus = switch (san.charAt(san.length() - 1)) {
            case '+' -> CheckStatus.CHECK;
            case '#' -> CheckStatus.CHECKMATE;
            default -> null;
        };
        if (checkStatus != null)
            san = san.substring(0, san.length() - 1);

        Piece.Type promotionType = null;
        if (san.charAt(san.length() - 2) == '=') {
            promotionType = Piece.Type.fromLetter(san.charAt(san.length() - 1));
            san = san.substring(0, san.length() - 2);
        }

        Square to = Square.fromString(san.substring(san.length() - 2));
        san = san.substring(0, san.length() - 2);

        boolean capture = false;
        if (san.charAt(san.length() - 1) == 'x') {
            capture = true;
            san = san.substring(0, san.length() - 1);
        }

        Square from = null;
        if (san.length() == 2) {
            from = Square.fromString(san);
        } else {

            Optional<Integer> file = Optional.empty();
            Optional<Integer> rank = Optional.empty();
            if (san.length() == 1) {
                file = Optional.of(san.charAt(0) - 'a').filter(Board::inBounds);
                if (file.isEmpty())
                    rank = Optional.of(san.charAt(0) - '1').filter(Board::inBounds);
            }

            Set<Square> candidates = new HashSet<>(64);
            if (file.isPresent()) {
                for (int r = 0; r < 8; r++)
                    candidates.add(new Square(file.get(), r));
            } else if (rank.isPresent()) {
                for (int f = 0; f < 8; f++)
                    candidates.add(new Square(f, rank.get()));
            } else {
                for (int f = 0; f < 8; f++)
                    for (int r = 0; r < 8; r++)
                        candidates.add(new Square(f, r));
            }

            Piece piece = new Piece(board.getActiveColor(), pieceType);
            candidates.removeIf(square -> !board.isPieceAt(square, piece));

            if (candidates.size() == 1) {
                from = candidates.stream().findAny().get();
            } else {
                for (Square candidate : candidates) {
                    if (board.isMovePossible(candidate, to)) {
                        from = candidate;
                        break;
                    }
                }
                if (from == null)
                    throw new IllegalArgumentException("No valid piece for " + san);
            }

        }

        Move move = promotionType != null
                ? new PromotionMove(from, to, capture, promotionType)
                : new NormalMove(pieceType, from, to, capture);
        return checkStatus != null ? new CheckMove(move, checkStatus) : move;
    }

}
