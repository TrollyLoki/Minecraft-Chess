package net.trollyloki.mcchess;

import net.andreinc.neatchess.client.UCI;
import net.andreinc.neatchess.client.exception.UCIRuntimeException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.trollyloki.mcchess.board.Board;
import net.trollyloki.mcchess.board.PhysicalBoard;
import net.trollyloki.mcchess.board.Piece;
import net.trollyloki.mcchess.game.Game;
import org.bukkit.Bukkit;
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
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class ChessCommand implements CommandExecutor, TabCompleter {

    public static final String ADMIN_PERMISSION = "chess.admin";

    private final Map<UUID, Board> boards = new HashMap<>();
    private final Map<UUID, Game> games = new HashMap<>();
    private final Map<UUID, UCI> engines = new HashMap<>();
    private final Map<UUID, CompletableFuture<?>> tasks = new HashMap<>();
    private final Set<UUID> cancelling = new HashSet<>();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {

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

                    Board board = new PhysicalBoard(cornerLocation, attachmentFace, rankDirection, fileDirection);
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

                        if (!games.containsKey(player.getUniqueId())) {
                            player.sendMessage(Component.text("You have not started a game", NamedTextColor.RED));
                            return false;
                        }
                        Game game = games.get(player.getUniqueId());

                        String fen = game.toFEN();
                        player.sendMessage(Component.text(fen)
                                .hoverEvent(Component.text("Click to copy", NamedTextColor.GRAY).asHoverEvent())
                                .clickEvent(ClickEvent.copyToClipboard(fen))
                        );
                        return true;

                    } else if (args[1].equalsIgnoreCase("board")) {

                        player.sendMessage(Component.text(board.toString()));
                        return true;

                    } else if (args[1].equalsIgnoreCase("game")) {

                        if (!games.containsKey(player.getUniqueId())) {
                            player.sendMessage(Component.text("You have not started a game", NamedTextColor.RED));
                            return false;
                        }
                        Game game = games.get(player.getUniqueId());

                        player.sendMessage(Component.text(game.toString()));
                        return true;

                    } else if (args[1].equalsIgnoreCase("turn")) {

                        if (!games.containsKey(player.getUniqueId())) {
                            player.sendMessage(Component.text("You have not started a game", NamedTextColor.RED));
                            return false;
                        }
                        Game game = games.get(player.getUniqueId());

                        if (args.length == 2) {
                            sender.sendMessage(Component.text("Usage: /" + label + " debug turn <white|black>", NamedTextColor.RED));
                            return false;
                        }

                        try {

                            game.setActiveColor(Color.valueOf(args[2].toUpperCase(Locale.ROOT)));
                            player.sendMessage(Component.text("Active color set to " + game.getActiveColor(), NamedTextColor.GREEN));
                            return true;

                        } catch (IllegalArgumentException e) {
                            sender.sendMessage(Component.text("Usage: /" + label + " debug turn <white|black>", NamedTextColor.RED));
                            return false;
                        }

                    } else if (args[1].equalsIgnoreCase("move")) {

                        if (!games.containsKey(player.getUniqueId())) {
                            player.sendMessage(Component.text("You have not started a game", NamedTextColor.RED));
                            return false;
                        }
                        Game game = games.get(player.getUniqueId());

                        if (args.length == 2) {
                            sender.sendMessage(Component.text("Usage: /" + label + " debug move <move>", NamedTextColor.RED));
                            return false;
                        }

                        try {
                            game.performMove(args[2]);
                            return true;
                        } catch (IndexOutOfBoundsException e) {
                            player.sendMessage(Component.text("Invalid move: " + e.getMessage(), NamedTextColor.RED));
                            return false;
                        }

                    } else if (args[1].equalsIgnoreCase("newgame")) {

                        games.put(player.getUniqueId(), new Game(board));
                        player.sendMessage(Component.text("New game started", NamedTextColor.GREEN));
                        return true;

                    } else if (args[1].equalsIgnoreCase("load")) {

                        if (args.length < 8) {
                            sender.sendMessage(Component.text("Usage: /" + label + " debug load <fen>", NamedTextColor.RED));
                            return false;
                        }

                        try {
                            games.put(player.getUniqueId(), Game.fromFEN(String.join(" ", Arrays.copyOfRange(args, 2, 8)), board));
                            player.sendMessage(Component.text("Game loaded from FEN", NamedTextColor.GREEN));
                            return true;
                        } catch (Exception e) {
                            player.sendMessage(Component.text("Invalid FEN: " + e.getMessage(), NamedTextColor.RED));
                            return false;
                        }

                    }

                }

                sender.sendMessage(Component.text("Usage: /" + label + " debug <fen|board|game|turn|move|newgame|load|engine>", NamedTextColor.RED));
                return false;

            } else if (args[0].equalsIgnoreCase("engine") && sender.hasPermission(ADMIN_PERMISSION)) {

                if (!(sender instanceof Player player)) {
                    sender.sendMessage(Component.text("Only players can use this command", NamedTextColor.RED));
                    return false;
                }

                if (!games.containsKey(player.getUniqueId())) {
                    player.sendMessage(Component.text("You have not started a game", NamedTextColor.RED));
                    return false;
                }
                Game game = games.get(player.getUniqueId());

                if (args.length > 1) {

                    if (args[1].equalsIgnoreCase("start")) {

                        if (engines.containsKey(player.getUniqueId())) {
                            player.sendMessage(Component.text("You have already started an engine", NamedTextColor.RED));
                            return false;
                        }

                        try {

                            UCI engine = new UCI();
                            engines.put(player.getUniqueId(), engine);
                            engine.start(ChessPlugin.engine());
                            sender.sendMessage(Component.text("Started engine", NamedTextColor.GREEN));
                            return true;

                        } catch (UCIRuntimeException e) {
                            e.printStackTrace();
                            engines.remove(player.getUniqueId());

                            sender.sendMessage(Component.text("Failed to start engine!", NamedTextColor.RED));
                            return false;
                        }

                    } else if (args[1].equalsIgnoreCase("move")) {

                        if (!engines.containsKey(player.getUniqueId())) {
                            player.sendMessage(Component.text("You have not started an engine", NamedTextColor.RED));
                            return false;
                        }
                        UCI engine = engines.get(player.getUniqueId());

                        if (args.length == 2) {
                            sender.sendMessage(Component.text("Usage: /" + label + " engine move <ms>", NamedTextColor.RED));
                            return false;
                        }

                        if (tasks.containsKey(player.getUniqueId()) && !tasks.get(player.getUniqueId()).isDone()) {
                            sender.sendMessage(Component.text("Please wait for your current action to complete", NamedTextColor.RED));
                            return false;
                        }

                        try {

                            long moveTime = Long.parseLong(args[2]);

                            player.sendMessage(Component.text("Moving...", NamedTextColor.YELLOW));
                            tasks.put(player.getUniqueId(), engineMove(game, engine, moveTime).whenComplete((m, e) -> {
                                if (e != null)
                                    player.sendMessage(Component.text("Error: " + e.getMessage(), NamedTextColor.RED));
                                else
                                    player.sendMessage(Component.text("Engine played " + m, NamedTextColor.GREEN));
                            }));

                            return true;

                        } catch (NumberFormatException e) {
                            sender.sendMessage(Component.text("Invalid milliseconds value: " + args[2], NamedTextColor.RED));
                            return false;
                        }

                    } else if (args[1].equalsIgnoreCase("play")) {

                        if (!engines.containsKey(player.getUniqueId())) {
                            player.sendMessage(Component.text("You have not started an engine", NamedTextColor.RED));
                            return false;
                        }
                        UCI engine = engines.get(player.getUniqueId());

                        if (args.length == 2) {
                            sender.sendMessage(Component.text("Usage: /" + label + " engine play <ms>", NamedTextColor.RED));
                            return false;
                        }

                        if (tasks.containsKey(player.getUniqueId()) && !tasks.get(player.getUniqueId()).isDone()) {
                            sender.sendMessage(Component.text("Please wait for your current action to complete", NamedTextColor.RED));
                            return false;
                        }

                        try {

                            long moveTime = Long.parseLong(args[2]);

                            player.sendMessage(Component.text("Playing...", NamedTextColor.YELLOW));
                            cancelling.remove(player.getUniqueId());

                            Supplier<CompletableFuture<?>> supplier = () -> engineMove(game, engine, moveTime).whenComplete((m, e) -> {
                                if (e != null)
                                    player.sendMessage(Component.text("Error: " + e.getMessage(), NamedTextColor.RED));
                                else {
                                    player.sendMessage(Component.text("Engine played " + m, NamedTextColor.GREEN));
                                }
                            });
                            playLoopHelper(player.getUniqueId(), supplier);
                            return true;

                        } catch (NumberFormatException e) {
                            sender.sendMessage(Component.text("Invalid milliseconds value: " + args[2], NamedTextColor.RED));
                            return false;
                        }

                    } else if (args[1].equalsIgnoreCase("cancel")) {

                        cancelling.add(player.getUniqueId());
                        sender.sendMessage(Component.text("Cancelling...", NamedTextColor.YELLOW));
                        return true;

                    } else if (args[1].equalsIgnoreCase("stop")) {

                        if (!engines.containsKey(player.getUniqueId())) {
                            player.sendMessage(Component.text("You have not started an engine", NamedTextColor.RED));
                            return false;
                        }
                        UCI engine = engines.get(player.getUniqueId());

                        try {

                            engine.close();
                            engines.remove(player.getUniqueId());
                            sender.sendMessage(Component.text("Stopped engine", NamedTextColor.GREEN));
                            return true;

                        } catch (UCIRuntimeException e) {
                            e.printStackTrace();

                            sender.sendMessage(Component.text("Failed to stop engine!", NamedTextColor.RED));
                            return false;
                        }

                    }

                }

                sender.sendMessage(Component.text("Usage: /" + label + " engine <start|move|play|cancel|stop>", NamedTextColor.RED));
                return false;

            }

        }

        String options = "board";
        if (sender.hasPermission(ADMIN_PERMISSION))
            options += "|debug|engine";
        sender.sendMessage(Component.text("Usage: /" + label + " <" + options + ">", NamedTextColor.RED));
        return false;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> options = new LinkedList<>();

        if (args.length <= 1) {

            options.add("board");
            if (sender.hasPermission(ADMIN_PERMISSION)) {
                options.add("debug");
                options.add("engine");
            }

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
                options.add("game");
                options.add("turn");
                options.add("move");
                options.add("newgame");
                options.add("load");

            } else if (args[1].equalsIgnoreCase("move")) {

                if (args.length == 3) {

                    String current = args[2];

                    if (current.length() == 0 || current.length() == 2) {

                        for (char c : "abcdefgh".toCharArray())
                            options.add(current + c);

                    } else if (current.length() == 1 || current.length() == 3) {

                        for (char c : "12345678".toCharArray())
                            options.add(current + c);

                    } else if (current.length() == 4) {

                        for (Piece.Type type : Piece.Type.values())
                            options.add(current + Character.toLowerCase(type.getLetter()));

                    }

                }

            } else if (args[1].equalsIgnoreCase("turn")) {

                if (args.length == 3) {

                    for (Color color : Color.values())
                        options.add(color.name().toLowerCase(Locale.ROOT));

                }

            }

        } else if (args[0].equalsIgnoreCase("engine") && sender.hasPermission(ADMIN_PERMISSION)) {

            if (args.length == 2) {

                options.add("start");
                options.add("move");
                options.add("play");
                options.add("cancel");
                options.add("stop");

            }

        }

        String prefix = args[args.length - 1].toLowerCase(Locale.ROOT);
        options.removeIf(option -> !option.toLowerCase(Locale.ROOT).startsWith(prefix));
        return options;
    }

    private void playLoopHelper(@NotNull UUID key, @NotNull Supplier<CompletableFuture<?>> futureSupplier) {
        tasks.put(key, futureSupplier.get().thenRun(() -> {
            if (!cancelling.contains(key))
                playLoopHelper(key, futureSupplier);
        }));
    }

    public static @NotNull CompletableFuture<String> engineMove(@NotNull Game game, @NotNull UCI engine, long moveTime) {
        String fen = game.toFEN();

        CompletableFuture<String> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTaskAsynchronously(ChessPlugin.getInstance(), () -> {
            try {

                engine.positionFen(fen).getResultOrThrow();
                String move = engine.bestMove(moveTime).getResultOrThrow().getCurrent();

                Bukkit.getScheduler().runTask(ChessPlugin.getInstance(), () -> {
                    try {
                        game.performMove(move);
                        future.complete(move);
                    } catch (Exception e) {
                        e.printStackTrace();
                        future.completeExceptionally(e);
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                future.completeExceptionally(e);
            }
        });
        return future;
    }

}
