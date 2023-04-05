package net.trollyloki.mcchess;

import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import net.trollyloki.mcchess.board.PhysicalBoard;
import net.trollyloki.mcchess.board.Piece;
import net.trollyloki.mcchess.board.Square;
import net.trollyloki.mcchess.game.Game;
import net.trollyloki.mcchess.game.move.Move;
import org.bukkit.World;
import org.bukkit.entity.ItemFrame;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

public class GameListener implements Listener {

    private final @NotNull WeakHashMap<World, Set<Game>> games = new WeakHashMap<>();
    private final @NotNull WeakHashMap<Game, Square> fromSquares = new WeakHashMap<>();
    private final @NotNull WeakHashMap<Game, Piece.Type> fromTypes = new WeakHashMap<>();
    private final @NotNull WeakHashMap<Game, Square> capturedSquares = new WeakHashMap<>();

    /**
     * Registers a game to be handled by this listener.
     *
     * @param game game
     * @throws IllegalArgumentException if the game does not have a physical board
     */
    public void registerGame(@NotNull Game game) {
        if (!(game.getBoard() instanceof PhysicalBoard board))
            throw new IllegalArgumentException("Only games with physical boards can be registered");
        games.computeIfAbsent(board.getWorld(), k -> new HashSet<>()).add(game);
    }

    /**
     * Attempts to unregister a game handled by this listener.
     *
     * @param game game
     * @throws IllegalArgumentException if the game does not have a physical board
     */
    public void unregisterGame(@NotNull Game game) {
        if (!(game.getBoard() instanceof PhysicalBoard board))
            throw new IllegalArgumentException("Only games with physical boards can be registered");
        if (games.containsKey(board.getWorld()))
            games.get(board.getWorld()).remove(game);
    }

    private @NotNull Optional<GameSquare> getGameSquare(@NotNull ItemFrame frame) {
        if (!games.containsKey(frame.getWorld()))
            return Optional.empty();

        for (Game game : games.get(frame.getWorld())) {
            if (game.getBoard() instanceof PhysicalBoard board) {
                Optional<Square> square = board.getSquareOf(frame);
                if (square.isPresent())
                    return Optional.of(new GameSquare(game, square.get()));
            }
        }
        return Optional.empty();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemFrameChange(@NotNull PlayerItemFrameChangeEvent event) {
        if (event.getAction() == PlayerItemFrameChangeEvent.ItemFrameChangeAction.ROTATE)
            return;

        Optional<GameSquare> gameSquare = getGameSquare(event.getItemFrame());
        if (gameSquare.isEmpty())
            return;

        Game game = gameSquare.get().getGame();
        Square square = gameSquare.get().getSquare();

        Optional<Piece> optionalPiece = ChessPlugin.getPieceFrom(event.getItemStack().getType());
        if (optionalPiece.isEmpty())
            return;

        Piece piece = optionalPiece.get();

        switch (event.getAction()) {
            case PLACE -> {
                Move move = buildMove(game, square, piece);
                if (move == null)
                    return;
                game.postMove(move);
                capturedSquares.remove(game);
            }
            case REMOVE -> {
                if (piece.getColor() == game.getBoard().getActiveColor()) {
                    fromSquares.put(game, square);
                    fromTypes.put(game, piece.getType());
                } else {
                    capturedSquares.put(game, square);
                }
            }
        }
    }

    private @Nullable Move buildMove(@NotNull Game game, @NotNull Square toSquare, @NotNull Piece toPiece) {
        Square fromSquare = fromSquares.remove(game);
        Piece.Type fromType = fromTypes.remove(game);

        if (fromSquare == null || fromType == null || toSquare.equals(fromSquare))
            return null;

        Square capturedSquare = capturedSquares.remove(game);

        boolean capture = toSquare.equals(capturedSquare);
        if (!capture && fromType == Piece.Type.PAWN && toSquare.getFile() != fromSquare.getFile()) {
            // en passant
            capturedSquare = new Square(toSquare.getFile(), fromSquare.getRank());
            capture = true;
            game.getBoard().setPieceAt(capturedSquare, null);
        }

        if (toPiece.getType() != fromType)
            return Move.promotion(fromSquare, toSquare, toPiece.getType(), capture);

        if (fromType == Piece.Type.KING && fromSquare.getRank() == toPiece.getColor().getBackRank() && fromSquare.getFile() == 4) {
            // castling
            if (toSquare.getFile() == 6) {
                game.getBoard().movePiece(new Square(7, toSquare.getRank()), new Square(5, toSquare.getRank()));
                return Move.shortCastle(toPiece.getColor());
            } else if (toSquare.getFile() == 2) {
                game.getBoard().movePiece(new Square(0, toSquare.getRank()), new Square(3, toSquare.getRank()));
                return Move.longCastle(toPiece.getColor());
            }
        }

        return Move.normal(fromType, fromSquare, toSquare, capture);
    }

}
