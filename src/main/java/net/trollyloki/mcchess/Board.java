package net.trollyloki.mcchess;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

public class Board {

    public static final int SIZE = 8;

    private final @NotNull Location cornerLocation;
    private final @NotNull BlockFace attachmentFace;
    private final @NotNull Vector rankDirection, fileDirection;

    /**
     * Defines a new chess board.
     *
     * @param cornerLocation location of the a1 square of the board
     * @param attachmentFace block face that the item frames are attached to
     * @param rankDirection  vector pointing parallel to the ranks
     * @param fileDirection  vector pointing parallel to the files
     */
    public Board(@NotNull Location cornerLocation, @NotNull BlockFace attachmentFace, @NotNull Vector rankDirection, @NotNull Vector fileDirection) {
        if (cornerLocation.getWorld() == null)
            throw new IllegalArgumentException("World must not be null");
        if (!attachmentFace.isCartesian())
            throw new IllegalArgumentException("Block face must be cartesian");
        if (rankDirection.dot(fileDirection) != 0)
            throw new IllegalArgumentException("Ranks and files must be perpendicular");

        this.cornerLocation = cornerLocation.toCenterLocation();
        this.attachmentFace = attachmentFace;
        this.rankDirection = rankDirection.clone().normalize();
        this.fileDirection = fileDirection.clone().normalize();
    }

    private static void checkBounds(int file, int rank) throws IndexOutOfBoundsException {
        if (file < 0 || file >= SIZE)
            throw new IndexOutOfBoundsException("File must be between 0 and " + (SIZE - 1));
        if (rank < 0 || rank >= SIZE)
            throw new IndexOutOfBoundsException("Rank must be between 0 and " + (SIZE - 1));
    }

    /**
     * Gets the location of a square on this board.
     *
     * @param file file index
     * @param rank rank index
     * @return location
     */
    public @NotNull Location getLocation(int file, int rank) {
        checkBounds(file, rank);
        return cornerLocation.clone()
                .add(rankDirection.clone().multiply(file))
                .add(fileDirection.clone().multiply(rank));
    }

    /**
     * Gets the item frame for a square on this board.
     *
     * @param file file index
     * @param rank rank index
     * @return optional item frame
     */
    public @NotNull Optional<ItemFrame> getItemFrameAt(int file, int rank) {
        Location location = getLocation(file, rank);
        Block block = location.getBlock();
        for (ItemFrame itemFrame : location.getNearbyEntitiesByType(ItemFrame.class, 1,
                frame -> frame.getAttachedFace() == attachmentFace)) {

            if (itemFrame.getLocation().getBlock().getRelative(itemFrame.getAttachedFace()).equals(block))
                return Optional.of(itemFrame);

        }
        return Optional.empty();
    }

    /**
     * Gets the piece at a square on this board.
     *
     * @param file file index
     * @param rank rank index
     * @return optional piece
     */
    public @NotNull Optional<Piece> getPieceAt(int file, int rank) {
        return getItemFrameAt(file, rank)
                .map(frame -> frame.getItem().getType())
                .flatMap(ChessPlugin::getPieceFrom);
    }

    /**
     * Sets the piece at a square on this board.
     *
     * @param file file index
     * @param rank rank index
     * @param piece optional piece
     * @return {@code true} if the piece was set, otherwise {@code false}
     */
    public boolean setPieceAt(int file, int rank, @Nullable Piece piece) {
        Optional<ItemFrame> frame = getItemFrameAt(file, rank);
        if (frame.isEmpty())
            return false;

        frame.get().setItem(ChessPlugin.getItemFor(piece));
        return true;
    }

    /**
     * Moves a piece on this board.
     *
     * @param fromFile file index to move from
     * @param fromRank rank index to move from
     * @param toFile file index to move to
     * @param toRank rank index to move to
     * @return {@code true} if a piece was moved, otherwise {@code false}
     */
    public boolean movePiece(int fromFile, int fromRank, int toFile, int toRank) {
        Optional<ItemFrame> fromFrame = getItemFrameAt(fromFile, fromRank);
        if (fromFrame.isEmpty())
            return false;

        Optional<ItemFrame> toFrame = getItemFrameAt(toFile, toRank);
        if (toFrame.isEmpty())
            return false;

        ItemStack item = fromFrame.get().getItem();
        if (item.getType() == Material.AIR)
            return false;

        fromFrame.get().setItem(null);

        ItemStack existingItem = toFrame.get().getItem();
        if (existingItem.getType() != Material.AIR)
            toFrame.get().getWorld().dropItem(toFrame.get().getLocation(), existingItem);

        toFrame.get().setItem(item);
        return true;
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
        Optional<Piece> fromPiece = getPieceAt(fromFile, fromRank);
        if (fromPiece.isEmpty())
            return false;
        Optional<Piece> toPiece = capturing ? getPieceAt(toFile, toRank) : Optional.empty();

        // Move piece
        if (!movePiece(fromFile, fromRank, toFile, toRank))
            return false;

        // Handle promotion
        if (promotionPiece != null)
            setPieceAt(toFile, toRank, new Piece(fromPiece.get().getColor(), promotionPiece));

        // Handle en passant
        if (capturing && toPiece.isEmpty()) {

            int pawnRank = toRank;
            if (fromPiece.get().getColor() == Piece.Color.BLACK)
                pawnRank++;
            else
                pawnRank--;

            getItemFrameAt(toFile, pawnRank).ifPresent(frame -> {
                ItemStack existingItem = frame.getItem();
                if (existingItem.getType() != Material.AIR) {
                    frame.setItem(null);
                    frame.getWorld().dropItem(frame.getLocation(), existingItem);
                }
            });

        }

        // Handle castling
        if (fromPiece.get().getType() == Piece.Type.KING) {
            if (move.startsWith("e1g1")) // white short castling
                movePiece(7, 0, 5, 0);
            else if (move.startsWith("e1c1")) // white long castling
                movePiece(0, 0, 3, 0);
            else if (move.startsWith("e8g8")) // black short castling
                movePiece(7, 7, 5, 7);
            else if (move.startsWith("e8c8")) // black long castling
                movePiece(0, 7, 3, 7);
        }

        return true;
    }

    /**
     * Builds a FEN position string from this board.
     *
     * @return FEN piece placement data
     * @see <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">Forsyth–Edwards Notation</a>
     */
    public @NotNull String toFEN() {
        StringBuilder builder = new StringBuilder();
        for (int rank = SIZE - 1; rank >= 0; rank--) {

            int emptySquares = 0;
            for (int file = 0; file < SIZE; file++) {

                Optional<Piece> piece = getPieceAt(file, rank);
                if (piece.isEmpty()) {
                    emptySquares++;
                    continue;
                }

                if (emptySquares != 0) {
                    builder.append(emptySquares);
                    emptySquares = 0;
                }
                builder.append(piece.get().getLetter());

            }

            if (emptySquares != 0)
                builder.append(emptySquares);

            if (rank != 0)
                builder.append('/');
        }
        return builder.toString();
    }

    /**
     * Loads a FEN position onto this board.
     *
     * @param position FEN piece placement data
     * @see <a href="https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation">Forsyth–Edwards Notation</a>
     */
    public void loadFromFEN(@NotNull String position) {
        int rank = SIZE - 1;
        for (String rankString : position.split("/")) {

            int file = 0;
            for (char letter : rankString.toCharArray()) {

                if (Character.isDigit(letter)) {
                    int emptySquares = Integer.parseInt(String.valueOf(letter));

                    for (int i = 0; i < emptySquares; i++) {
                        setPieceAt(file, rank, null);
                        file++;
                    }

                    continue;
                }

                setPieceAt(file, rank, Piece.fromLetter(letter));
                file++;
            }
            rank--;
        }
    }

    @Override
    public String toString() {
        return "Board{" +
                "cornerLocation=" + cornerLocation +
                ", attachmentFace=" + attachmentFace +
                ", rankDirection=" + rankDirection +
                ", fileDirection=" + fileDirection +
                '}';
    }

}
