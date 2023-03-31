package net.trollyloki.mcchess.game.move;

import net.trollyloki.mcchess.Color;
import net.trollyloki.mcchess.board.Board;
import net.trollyloki.mcchess.board.Piece;
import net.trollyloki.mcchess.board.Square;
import net.trollyloki.mcchess.game.Game;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public interface Move {

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
            return new PromotionMove(pieceType, from, to, capture, Piece.Type.fromLetter(uciMove.charAt(4)));
        else
            return new NormalMove(pieceType, from, to, capture);
    }

    /**
     * Parses a move from SAN in the context of a game.
     *
     * @param san  SAN
     * @param game game, for context
     * @return move
     */
    static @NotNull Move fromSAN(@NotNull String san, @NotNull Game game) {
        switch (san) {
            case "O-O" -> {
                return new CastleMove(game.getActiveColor(), false);
            }
            case "O-O-O" -> {
                return new CastleMove(game.getActiveColor(), true);
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

            Piece piece = new Piece(game.getActiveColor(), pieceType);
            candidates.removeIf(square -> !game.getBoard().isPieceAt(square, piece));

            if (candidates.size() == 1) {
                from = candidates.stream().findAny().get();
            } else {
                for (Square candidate : candidates) {

                    int fileDiff = to.getFile() - candidate.getFile();
                    int rankDiff = to.getRank() - candidate.getRank();
                    int absFileDiff = Math.abs(fileDiff);
                    int absRankDiff = Math.abs(rankDiff);

                    if (pieceType == Piece.Type.ROOK || pieceType == Piece.Type.QUEEN) {
                        if (fileDiff == 0 && game.getBoard().isFileOpen(candidate.getFile(), candidate.getRank(), to.getRank())
                                || rankDiff == 0 && game.getBoard().isRankOpen(candidate.getRank(), candidate.getFile(), to.getFile())) {
                            from = candidate;
                            break;
                        }
                    }

                    if (pieceType == Piece.Type.BISHOP || pieceType == Piece.Type.QUEEN) {
                        if (absFileDiff == absRankDiff && game.getBoard().isDiagonalOpen(candidate.getFile(), candidate.getRank(), to.getFile(), to.getRank())) {
                            from = candidate;
                            break;
                        }
                    } else if (pieceType == Piece.Type.KNIGHT) {
                        if (absFileDiff == 2 && absRankDiff == 1 || absFileDiff == 1 && absRankDiff == 2) {
                            from = candidate;
                            break;
                        }
                    } else if (pieceType == Piece.Type.KING) {
                        if (absFileDiff <= 1 && absRankDiff <= 1) {
                            from = candidate;
                            break;
                        }
                    } else if (pieceType == Piece.Type.PAWN) {
                        int pawnDirection = game.getActiveColor().getPawnDirection();
                        int pawnStartRank = game.getActiveColor().getBackRank() + pawnDirection;
                        if (absFileDiff == (capture ? 1 : 0) && (rankDiff == pawnDirection || !capture && rankDiff == 2 * pawnDirection && candidate.getRank() == pawnStartRank)) {
                            from = candidate;
                            break;
                        }
                    }

                }
                if (from == null)
                    throw new IllegalArgumentException("No valid piece for " + san);
            }

        }

        Move move = promotionType != null
                ? new PromotionMove(pieceType, from, to, capture, promotionType)
                : new NormalMove(pieceType, from, to, capture);
        return checkStatus != null ? new CheckMove(move, checkStatus) : move;
    }

}
