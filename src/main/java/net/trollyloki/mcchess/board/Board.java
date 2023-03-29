package net.trollyloki.mcchess.board;

import net.trollyloki.mcchess.ChessPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.Predicate;

public class Board {

    public static final int SIZE = 8;

    private final @NotNull Location cornerLocation;
    private final @NotNull BlockFace attachmentFace;
    private final @NotNull Vector rankDirection, fileDirection;

    private @NotNull String site = ChessPlugin.getDefaultSite();

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

    /**
     * Gets the site of this board.
     *
     * @return site string
     */
    public @NotNull String getSite() {
        return site;
    }

    /**
     * Sets the site of this board.
     *
     * @param site site string
     */
    public void setSite(@NotNull String site) {
        this.site = site;
    }

    private static boolean inBounds(int file, int rank) {
        return file >= 0 && file <= SIZE && rank >= 0 && rank < SIZE;
    }

    /**
     * Checks if a square is within the bounds of the board.
     *
     * @param square square
     * @return {@code true} if the file and rank index are valid
     */
    public static boolean inBounds(@NotNull Square square) {
        return inBounds(square.getFile(), square.getRank());
    }

    private static void checkBounds(int file, int rank) throws IndexOutOfBoundsException {
        if (file < 0 || file >= SIZE)
            throw new IndexOutOfBoundsException("File must be between 0 and " + (SIZE - 1));
        if (rank < 0 || rank >= SIZE)
            throw new IndexOutOfBoundsException("Rank must be between 0 and " + (SIZE - 1));
    }

    private static void checkBounds(@NotNull Square square) throws IndexOutOfBoundsException {
        checkBounds(square.getFile(), square.getRank());
    }

    /**
     * Gets the location of a square on this board.
     *
     * @param square square
     * @return location
     */
    public @NotNull Location getLocation(@NotNull Square square) {
        checkBounds(square);
        return cornerLocation.clone()
                .add(rankDirection.clone().multiply(square.getFile()))
                .add(fileDirection.clone().multiply(square.getRank()));
    }

    /**
     * Gets the square on this board at a location.
     *
     * @param location location
     * @return optional square, empty if the location is not on this board
     */
    public @NotNull Optional<Square> getSquareAt(@NotNull Location location) {
        Vector shifted = location.toBlockLocation().subtract(cornerLocation.toBlockLocation()).toVector();
        if (shifted.getY() != 0)
            return Optional.empty();

        int file = (int) shifted.dot(rankDirection);
        int rank = (int) shifted.dot(fileDirection);
        Square square = new Square(file, rank);

        return Optional.of(square).filter(Board::inBounds);
    }

    /**
     * Gets the square on this board that an item frame is for.
     *
     * @param itemFrame item frame
     * @return optional square, empty if the item frame is not on this board
     */
    public @NotNull Optional<Square> getSquareOf(@NotNull ItemFrame itemFrame) {
        if (itemFrame.getAttachedFace() != attachmentFace)
            return Optional.empty();

        Block block = itemFrame.getLocation().getBlock().getRelative(itemFrame.getAttachedFace());
        return getSquareAt(block.getLocation());
    }

    /**
     * Gets the item frame for a square on this board.
     *
     * @param square square
     * @return optional item frame
     */
    public @NotNull Optional<ItemFrame> getItemFrameFor(@NotNull Square square) {
        Location location = getLocation(square);
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
     * @param square square
     * @return optional piece
     */
    public @NotNull Optional<Piece> getPieceAt(@NotNull Square square) {
        return getItemFrameFor(square)
                .map(frame -> frame.getItem().getType())
                .flatMap(ChessPlugin::getPieceFrom);
    }

    /**
     * Checks if the piece at a square on this board is present and matches a predicate.
     *
     * @param square    square
     * @param predicate piece predicate
     * @return {@code true} if the predicate is met, otherwise {@code false}
     */
    public boolean isPieceAt(@NotNull Square square, @NotNull Predicate<Piece> predicate) {
        return getPieceAt(square).filter(predicate).isPresent();
    }

    /**
     * Checks if the piece at a square on this board is present and a certain piece.
     *
     * @param square square
     * @param piece  piece
     * @return {@code true} if the piece is there, otherwise {@code false}
     */
    public boolean isPieceAt(@NotNull Square square, @NotNull Piece piece) {
        return isPieceAt(square, piece::equals);
    }

    /**
     * Sets the piece at a square on this board.
     *
     * @param square square
     * @param piece  optional piece
     * @return {@code true} if the piece was set, otherwise {@code false}
     */
    public boolean setPieceAt(@NotNull Square square, @Nullable Piece piece) {
        Optional<ItemFrame> frame = getItemFrameFor(square);
        if (frame.isEmpty())
            return false;

        frame.get().setItem(ChessPlugin.getItemFor(piece));
        return true;
    }

    /**
     * Moves a piece on this board.
     *
     * @param from square to move from
     * @param to   square to move to
     * @return {@code true} if a piece was moved, otherwise {@code false}
     */
    public boolean movePiece(@NotNull Square from, @NotNull Square to) {
        Optional<ItemFrame> fromFrame = getItemFrameFor(from);
        if (fromFrame.isEmpty())
            return false;

        Optional<ItemFrame> toFrame = getItemFrameFor(to);
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

                Optional<Piece> piece = getPieceAt(new Square(file, rank));
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
                        setPieceAt(new Square(file, rank), null);
                        file++;
                    }

                    continue;
                }

                setPieceAt(new Square(file, rank), Piece.fromLetter(letter));
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
