package net.trollyloki.mcchess;

import net.kyori.adventure.text.Component;
import net.trollyloki.mcchess.board.Piece;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class ChessPlugin extends JavaPlugin {

    private static ChessPlugin instance;

    public static ChessPlugin getInstance() {
        return instance;
    }

    private static NamespacedKey pieceTypeKey;

    private static String defaultSite;
    private static String engineCommand;
    private static final @NotNull Map<Piece.Type, String> PIECE_NAMES = new HashMap<>();
    private static final @NotNull Map<Piece, Material> PIECE_TO_MATERIAL = new HashMap<>();
    private static final @NotNull Map<Material, Piece> MATERIAL_TO_PIECE = new HashMap<>();

    @Override
    public void onEnable() {
        instance = this;

        pieceTypeKey = new NamespacedKey(this, "piece_type");

        saveDefaultConfig();
        reloadConfig();

        //noinspection DataFlowIssue
        getCommand("chess").setExecutor(new ChessCommand());

    }

    @Override
    public void onDisable() {
        instance = null;
    }

    @Override
    public void reloadConfig() {
        super.reloadConfig();
        FileConfiguration config = getConfig();

        engineCommand = config.getString("engine");
        defaultSite = config.getString("default-site");

        PIECE_NAMES.clear();
        PIECE_TO_MATERIAL.clear();
        MATERIAL_TO_PIECE.clear();

        for (Piece.Type type : Piece.Type.values()) {

            {
                String path = "pieces.names.%s".formatted(
                        type.name().toLowerCase(Locale.ROOT)
                );
                String name = config.getString(path);
                if (name == null)
                    throw new IllegalArgumentException("Missing material in config: " + path);

                PIECE_NAMES.put(type, name);
            }

            for (Color color : Color.values()) {
                Piece piece = new Piece(color, type);

                String path = "pieces.materials.%s.%s".formatted(
                        color.name().toLowerCase(Locale.ROOT),
                        type.name().toLowerCase(Locale.ROOT)
                );
                String string = config.getString(path);
                if (string == null)
                    throw new IllegalArgumentException("Missing material in config: " + path);

                Material material = Material.valueOf(string.toUpperCase(Locale.ROOT));
                PIECE_TO_MATERIAL.put(piece, material);
                MATERIAL_TO_PIECE.put(material, piece);
            }
        }
    }

    public static String engine() {
        return engineCommand;
    }

    /**
     * Get the default site for this server.
     *
     * @return default site string
     */
    public static String getDefaultSite() {
        return defaultSite;
    }

    /**
     * Gets the name for a piece type.
     *
     * @param type piece type
     * @return name
     */
    public static @NotNull String getName(@NotNull Piece.Type type) {
        return PIECE_NAMES.get(type);
    }

    /**
     * Gets the material representing a piece.
     *
     * @param piece piece
     * @return material
     */
    public static @NotNull Material getMaterialFor(@NotNull Piece piece) {
        return PIECE_TO_MATERIAL.get(piece);
    }

    /**
     * Gets an item representing a piece.
     *
     * @param piece piece
     * @return item stack
     */
    @Contract("null -> null; !null -> !null")
    public static @Nullable ItemStack getItemFor(@Nullable Piece piece) {
        if (piece == null)
            return null;

        ItemStack item = new ItemStack(getMaterialFor(piece));
        item.editMeta(meta -> {
            meta.displayName(Component.text(getName(piece.getType())));
            meta.getPersistentDataContainer().set(pieceTypeKey, PersistentDataType.STRING, piece.getType().name());
        });
        return item;
    }

    /**
     * Gets the piece represented by a material.
     *
     * @param material material
     * @return optional piece
     */
    public static @NotNull Optional<Piece> getPieceFrom(@NotNull Material material) {
        return Optional.ofNullable(MATERIAL_TO_PIECE.get(material));
    }

}
