package net.trollyloki.mcchess;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class Game {

    private @NotNull
    final Board board;
    private @NotNull Piece.Color activeColor;
    private @NotNull
    final Set<Piece.Color> canShortCastle, canLongCastle;
    private int enPassantFile, enPassantRank;
    private int halfMoves, moves;

    public Game(@NotNull Board board, @NotNull Piece.Color activeColor, @NotNull Set<Piece.Color> canShortCastle, @NotNull Set<Piece.Color> canLongCastle, int enPassantFile, int enPassantRank, int halfMoves, int moves) {
        this.board = board;
        this.activeColor = activeColor;
        this.canShortCastle = new HashSet<>(canShortCastle);
        this.canLongCastle = new HashSet<>(canLongCastle);
        this.enPassantFile = enPassantFile;
        this.enPassantRank = enPassantRank;
        this.halfMoves = halfMoves;
        this.moves = moves;
    }

    public Game(@NotNull Board board) {
        this(board, Piece.Color.WHITE, Set.of(Piece.Color.values()), Set.of(Piece.Color.values()), -1, -1, 0, 1);
    }

    /**
     * Perform a move specified by UCI LAN.
     *
     * @param move move LAN
     * @return {@code true} if a piece was moved, otherwise {@code false}
     */
    public boolean performMove(@NotNull String move) {
        move = move.toLowerCase(Locale.ROOT);
        int i = 0;

        // Parse string

        int fromFile = move.charAt(i++) - 'a';
        int fromRank = move.charAt(i++) - '1';

        boolean capturing = false;
        if (move.charAt(i) == 'x') {
            capturing = true;
            i++;
        } else if (move.charAt(i) == '-') {
            i++;
        }

        int toFile = move.charAt(i++) - 'a';
        int toRank = move.charAt(i++) - '1';

        Piece.Type promotionPiece = null;
        if (i < move.length())
            promotionPiece = Piece.Type.fromLetter(move.charAt(i));

        // Find current pieces
        Optional<Piece> pieceOptional = board.getPieceAt(fromFile, fromRank);
        if (pieceOptional.isEmpty())
            return false;
        Piece piece = pieceOptional.get();
        Optional<Piece> toPiece = capturing ? board.getPieceAt(toFile, toRank) : Optional.empty();

        // Move piece
        if (!board.movePiece(fromFile, fromRank, toFile, toRank))
            return false;

        // Handle promotion
        if (promotionPiece != null)
            board.setPieceAt(toFile, toRank, new Piece(piece.getColor(), promotionPiece));

        // Handle en passant

        if (piece.getType() == Piece.Type.PAWN && Math.abs(toRank - fromRank) == 2) {
            enPassantFile = toFile;
            enPassantRank = (fromRank + toRank) / 2;
        } else {
            enPassantFile = -1;
            enPassantRank = -1;
        }

        if (capturing && toPiece.isEmpty()) {

            int pawnRank = toRank;
            if (piece.getColor() == Piece.Color.BLACK)
                pawnRank++;
            else
                pawnRank--;

            board.getItemFrameAt(toFile, pawnRank).ifPresent(frame -> {
                ItemStack existingItem = frame.getItem();
                if (existingItem.getType() != Material.AIR) {
                    frame.setItem(null);
                    frame.getWorld().dropItem(frame.getLocation(), existingItem);
                }
            });

        }

        // Handle castling

        if (piece.getType() == Piece.Type.KING) {
            if (move.startsWith("e1g1")) // white short castling
                board.movePiece(7, 0, 5, 0);
            else if (move.startsWith("e1c1")) // white long castling
                board.movePiece(0, 0, 3, 0);
            else if (move.startsWith("e8g8")) // black short castling
                board.movePiece(7, 7, 5, 7);
            else if (move.startsWith("e8c8")) // black long castling
                board.movePiece(0, 7, 3, 7);

            canShortCastle.remove(piece.getColor());
            canLongCastle.remove(piece.getColor());
        }

        if (piece.getType() == Piece.Type.ROOK &&
                (piece.getColor() == Piece.Color.WHITE && fromRank == 0 || piece.getColor() == Piece.Color.BLACK && fromRank == Board.SIZE - 1)
        ) {

            if (fromFile == 0)
                canLongCastle.remove(piece.getColor());
            else if (fromFile == Board.SIZE - 1)
                canShortCastle.remove(piece.getColor());

        }

        // Update move info

        if (capturing || piece.getType() == Piece.Type.PAWN)
            halfMoves = 0;
        else
            halfMoves++;

        switch (piece.getColor()) {
            case WHITE -> activeColor = Piece.Color.BLACK;
            case BLACK -> {
                activeColor = Piece.Color.WHITE;
                moves++;
            }
        }

        return true;
    }

    /**
     * Builds a FEN string from this game.
     *
     * @return FEN record
     * @see <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">Forsyth–Edwards Notation</a>
     */
    public @NotNull String toFEN() {
        StringBuilder builder = new StringBuilder(board.toFEN());

        builder.append(' ');
        builder.append(activeColor == Piece.Color.BLACK ? 'b' : 'w');

        builder.append(' ');
        if (canShortCastle.isEmpty() && canLongCastle.isEmpty()) {
            builder.append('-');
        } else {
            if (canShortCastle.contains(Piece.Color.WHITE))
                builder.append('K');
            if (canLongCastle.contains(Piece.Color.WHITE))
                builder.append('Q');
            if (canShortCastle.contains(Piece.Color.BLACK))
                builder.append('k');
            if (canLongCastle.contains(Piece.Color.BLACK))
                builder.append('q');
        }

        builder.append(' ');
        if (enPassantFile == -1) {
            builder.append('-');
        } else {
            builder.append((char) ('a' + enPassantFile));
            builder.append((char) ('1' + enPassantRank));
        }

        builder.append(' ');
        builder.append(halfMoves);

        builder.append(' ');
        builder.append(moves);

        return builder.toString();
    }

    /**
     * Creates a game from a FEN string.
     *
     * @param fen   FEN record
     * @param board board to load the game onto
     * @return game
     * @see <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">Forsyth–Edwards Notation</a>
     */
    public static @NotNull Game fromFEN(@NotNull String fen, @NotNull Board board) {
        String[] split = fen.split(" ");

        Piece.Color activeColor = split[1].charAt(0) == 'b' ? Piece.Color.BLACK : Piece.Color.WHITE;

        Set<Piece.Color> canShortCastle = new HashSet<>(2);
        Set<Piece.Color> canLongCastle = new HashSet<>(2);
        for (char letter : split[2].toCharArray()) {
            switch (letter) {
                case 'K' -> canShortCastle.add(Piece.Color.WHITE);
                case 'Q' -> canLongCastle.add(Piece.Color.WHITE);
                case 'k' -> canShortCastle.add(Piece.Color.BLACK);
                case 'q' -> canLongCastle.add(Piece.Color.BLACK);
            }
        }

        int enPassantFile = -1;
        int enPassantRank = -1;
        if (split[3].charAt(0) != '-') {
            enPassantFile = split[3].charAt(0) - 'a';
            enPassantRank = split[3].charAt(1) - '1';
        }

        int halfMoves = Integer.parseInt(split[4]);
        int moves = Integer.parseInt(split[5]);

        board.loadFromFEN(split[0]);

        return new Game(board, activeColor, canShortCastle, canLongCastle, enPassantFile, enPassantRank, halfMoves, moves);
    }

    @Override
    public String toString() {
        return "Game{" +
                "board=" + board +
                ", activeColor=" + activeColor +
                ", canShortCastle=" + canShortCastle +
                ", canLongCastle=" + canLongCastle +
                ", enPassantFile=" + enPassantFile +
                ", enPassantRank=" + enPassantRank +
                ", halfMoves=" + halfMoves +
                ", moves=" + moves +
                '}';
    }

}
