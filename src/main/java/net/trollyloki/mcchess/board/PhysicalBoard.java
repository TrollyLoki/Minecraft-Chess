package net.trollyloki.mcchess.board;

import net.trollyloki.mcchess.ChessPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class PhysicalBoard extends Board {

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
    public PhysicalBoard(@NotNull Location cornerLocation, @NotNull BlockFace attachmentFace, @NotNull Vector rankDirection, @NotNull Vector fileDirection) {
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

        validateCastling();
    }

    /**
     * Gets the world this board is in.
     *
     * @return world
     */
    public @NotNull World getWorld() {
        return cornerLocation.getWorld();
    }

    /**
     * Gets the location of a square on this board.
     *
     * @param square square
     * @return location
     */
    public @NotNull Location getLocation(@NotNull Square square) {
        Board.checkBounds(square);
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

    @Override
    public @NotNull Optional<Piece> getPieceAt(@NotNull Square square) {
        return getItemFrameFor(square)
                .map(frame -> frame.getItem().getType())
                .flatMap(ChessPlugin::getPieceFrom);
    }

    @Override
    public boolean setPieceAt(@NotNull Square square, @Nullable Piece piece) {
        Optional<ItemFrame> frame = getItemFrameFor(square);
        if (frame.isEmpty())
            return false;

        ItemStack existingItem = frame.get().getItem();
        if (existingItem.getType() != Material.AIR)
            frame.get().getWorld().dropItem(frame.get().getLocation(), existingItem);

        frame.get().setItem(ChessPlugin.getItemFor(piece));
        return true;
    }

    @Override
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

    @Override
    public String toString() {
        return "PhysicalBoard{" +
                "cornerLocation=" + cornerLocation +
                ", attachmentFace=" + attachmentFace +
                ", rankDirection=" + rankDirection +
                ", fileDirection=" + fileDirection +
                ", fen=" + toFEN() +
                '}';
    }

}
