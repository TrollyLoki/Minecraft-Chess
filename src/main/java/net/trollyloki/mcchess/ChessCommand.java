package net.trollyloki.mcchess;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ChessCommand implements CommandExecutor, TabCompleter {

    public static final String ADMIN_PERMISSION = "chess.admin";

    private final Map<UUID, Board> boards = new HashMap<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (args.length > 0) {

            if (args[0].equalsIgnoreCase("board")) {

                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Only players can use this command", NamedTextColor.RED));
                    return false;
                }

                if (args.length <= 2) {
                    sender.sendMessage(Component.text("Usage: /" + label + " board <file-direction> <attachment-face>", NamedTextColor.RED));
                    return false;
                }

                int i = 1;
                try {

                    BlockFace fileDirectionFace = BlockFace.valueOf(args[i].toUpperCase(Locale.ROOT));
                    if (!fileDirectionFace.isCartesian())
                        throw new IllegalArgumentException();

                    BlockFace attachmentFace = BlockFace.valueOf(args[++i].toUpperCase(Locale.ROOT));
                    if (!attachmentFace.isCartesian())
                        throw new IllegalArgumentException();

                    Vector fileDirection = fileDirectionFace.getDirection();
                    Vector rankDirection = attachmentFace.getDirection().crossProduct(fileDirection);
                    Location cornerLocation = player.getLocation().getBlock().getRelative(attachmentFace).getLocation();

                    Board board = new Board(cornerLocation, attachmentFace, rankDirection, fileDirection);
                    boards.put(player.getUniqueId(), board);
                    player.sendMessage(Component.text("Board registered", NamedTextColor.GREEN));
                    return true;

                } catch (IllegalArgumentException e) {
                    sender.sendMessage(Component.text(args[i] + " is not a valid block face. Options: north, south, east, west, up, or down", NamedTextColor.RED));
                    return false;
                }

            } else if (args[0].equalsIgnoreCase("debug") && sender.hasPermission(ADMIN_PERMISSION)) {

                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Only players can use this command", NamedTextColor.RED));
                    return false;
                }

                if (!boards.containsKey(player.getUniqueId())) {
                    player.sendMessage(Component.text("You have not registered a board", NamedTextColor.RED));
                    return false;
                }
                Board board = boards.get(player.getUniqueId());

                if (args.length > 1) {

                    if (args[1].equalsIgnoreCase("fen")) {

                        String fen = board.toFEN();
                        player.sendMessage(Component.text(fen)
                                .hoverEvent(Component.text("Click to copy", NamedTextColor.GRAY).asHoverEvent())
                                .clickEvent(ClickEvent.copyToClipboard(fen))
                        );
                        return true;

                    } else if (args[1].equalsIgnoreCase("board")) {

                        player.sendMessage(Component.text(board.toString()));
                        return true;

                    } else if (args[1].equalsIgnoreCase("move")) {

                        if (args.length == 2) {
                            sender.sendMessage(Component.text("Usage: /" + label + " debug move <move>", NamedTextColor.RED));
                            return false;
                        }

                        try {
                            board.performMove(args[2]);
                            return true;
                        } catch (IndexOutOfBoundsException e) {
                            player.sendMessage(Component.text("Invalid move: " + e.getMessage(), NamedTextColor.RED));
                            return false;
                        }

                    } else if (args[1].equalsIgnoreCase("load")) {

                        if (args.length == 2) {
                            sender.sendMessage(Component.text("Usage: /" + label + " debug load <fen>", NamedTextColor.RED));
                            return false;
                        }

                        try {
                            board.loadFromFEN(args[2]);
                            return true;
                        } catch (IndexOutOfBoundsException e) {
                            player.sendMessage(Component.text("Invalid FEN: " + e.getMessage(), NamedTextColor.RED));
                            return false;
                        }

                    }

                }

                sender.sendMessage(Component.text("Usage: /" + label + " debug <fen|board|move|load>", NamedTextColor.RED));
                return false;
            }

        }

        String options = "board";
        if (sender.hasPermission(ADMIN_PERMISSION))
            options += "|debug";
        sender.sendMessage(Component.text("Usage: /" + label + " <" + options + ">", NamedTextColor.RED));
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> options = new LinkedList<>();

        if (args.length <= 1) {

            options.add("board");
            if (sender.hasPermission(ADMIN_PERMISSION))
                options.add("debug");

        } else if (args[0].equalsIgnoreCase("board")) {

            if (args.length <= 3) {
                options.addAll(
                        Arrays.stream(BlockFace.values())
                                .filter(BlockFace::isCartesian)
                                .map(BlockFace::name).map(String::toLowerCase)
                                .toList()
                );
            }

        } else if (args[0].equalsIgnoreCase("debug") && sender.hasPermission(ADMIN_PERMISSION)) {

            if (args.length == 2) {

                options.add("fen");
                options.add("board");
                options.add("move");
                options.add("load");

            } else if (args[1].equalsIgnoreCase("move")) {

                if (args.length == 3) {

                    String current = args[2];
                    if (current.length() == 2)
                        options.add(current + "x");

                    if (
                            current.length() == 0
                                    || current.length() == 2
                                    || (current.length() == 3 && current.charAt(2) == 'x')
                    ) {

                        for (char c : "abcdefgh".toCharArray())
                            options.add(current + c);

                    } else if (
                            current.length() == 1
                                    || (current.length() == 3 && current.charAt(2) != 'x')
                                    || (current.length() == 4 && current.charAt(2) == 'x')
                    ) {

                        for (char c : "12345678".toCharArray())
                            options.add(current + c);

                    }

                }

            }

        }

        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        options.removeIf(option -> !option.toLowerCase(Locale.ROOT).startsWith(prefix));
        return options;
    }

}
