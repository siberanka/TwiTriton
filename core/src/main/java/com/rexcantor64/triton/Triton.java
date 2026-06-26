package com.rexcantor64.triton;

import com.google.gson.JsonElement;
import com.rexcantor64.triton.api.language.LanguageParser;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.api.legacy.LegacyLanguageParser;
import com.rexcantor64.triton.bridge.BridgeManager;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.config.MessagesConfig;
import com.rexcantor64.triton.config.interfaces.Configuration;
import com.rexcantor64.triton.config.interfaces.ConfigurationProvider;
import com.rexcantor64.triton.config.interfaces.YamlConfiguration;
import com.rexcantor64.triton.debug.DumpManager;
import com.rexcantor64.triton.dependencies.Dependency;
import com.rexcantor64.triton.language.LanguageManager;
import com.rexcantor64.triton.language.TranslationManager;
import com.rexcantor64.triton.language.parser.AdventureParser;
import com.rexcantor64.triton.language.parser.LegacyParser;
import com.rexcantor64.triton.language.parser.MessageParser;
import com.rexcantor64.triton.loader.utils.LoaderFlag;
import com.rexcantor64.triton.logger.TritonLogger;
import com.rexcantor64.triton.migration.LanguageMigration;
import com.rexcantor64.triton.packetinterceptor.PacketEventsManager;
import com.rexcantor64.triton.player.PlayerManager;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import com.rexcantor64.triton.plugin.Platform;
import com.rexcantor64.triton.plugin.PluginLoader;
import com.rexcantor64.triton.storage.LocalStorage;
import com.rexcantor64.triton.storage.MysqlStorage;
import com.rexcantor64.triton.storage.Storage;
import com.rexcantor64.triton.utils.FileUtils;
import com.rexcantor64.triton.utils.TritonAPIUtils;
import com.rexcantor64.triton.web.TwinManager;
import lombok.Getter;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.CustomChart;
import org.bstats.charts.SimplePie;
import org.bstats.charts.SingleLineChart;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Getter
public abstract class Triton<P extends TritonLanguagePlayer<?>, B extends BridgeManager> implements com.rexcantor64.triton.api.Triton {

    // Main instances
    protected static Triton<?, ?> instance;
    protected PluginLoader loader;
    // File-related variables
    private File translationsFolder;
    // Configs
    private Configuration configYAML;
    private MainConfig config;
    private MessagesConfig messagesConfig;
    // Managers
    private LanguageManager languageManager;
    @Deprecated
    private final LanguageParser languageParser = new LegacyLanguageParser();
    private TranslationManager translationManager;
    private MessageParser messageParser;
    private TwinManager twinManager;
    protected final PlayerManager<P> playerManager;
    protected final B bridgeManager;
    private Storage storage;
    private TritonLogger logger;
    private DumpManager dumpManager;
    protected @Nullable PacketEventsManager packetEventsManager;

    protected Triton(PlayerManager<P> playerManager, B bridgeManager) {
        this.playerManager = playerManager;
        this.bridgeManager = bridgeManager;
    }

    public static Platform platform() {
        return instance.getLoader().getPlatform();
    }

    public static boolean isBungee() {
        return platform() == Platform.BUNGEE;
    }

    public static boolean isVelocity() {
        return platform() == Platform.VELOCITY;
    }

    public static boolean isProxy() {
        return platform().isProxy();
    }

    public static boolean isSpigot() {
        return platform() == Platform.SPIGOT;
    }

    public static Triton<?, ?> get() {
        return instance;
    }

    public void onLoad() {
        instance = this;
        TritonAPIUtils.register(instance);

        translationsFolder = new File(getDataFolder(), "translations");

        logger = loader.getTritonLogger();

        config = new MainConfig(this);
        configYAML = loadYAML("config", getConfigFileName());
        config.setup();

        if (config.isUsePacketEvents()) {
            val dependencyManager = Triton.get().getLoader().getDependencyManager();
            val isPacketEventsVendored = dependencyManager.hasLoaderFlag(LoaderFlag.VENDOR_PACKET_EVENTS);
            if (isPacketEventsVendored) {
                // load packet events dependency (netty and platform related modules are loaded later)
                dependencyManager.loadDependency(Dependency.PACKET_EVENTS_API);
            }
            this.initPacketEventsManager();
        }

        if (this.packetEventsManager != null) {
            this.packetEventsManager.onLoad();
        }
    }

    public void onEnable() {
        languageManager = new LanguageManager(this);
        messagesConfig = new MessagesConfig();
        translationManager = new TranslationManager(this);

        LanguageMigration.migrate();

        reload();

        twinManager = new TwinManager(this);

        if (this.packetEventsManager != null) {
            this.packetEventsManager.onEnable();
        }
        com.rexcantor64.triton.bridge.BedrockBridge.init();
    }

    public void onDisable() {
        if (this.packetEventsManager != null) {
            this.packetEventsManager.onDisable();
        }
    }

    public void reload() {
        configYAML = loadYAML("config", getConfigFileName());
        config.setup();
        dumpManager = new DumpManager();
        logger.setLogLevel(config.getLogLevel());
        if (config.getParser().equalsIgnoreCase("legacy")) {
            messageParser = new LegacyParser();
        } else {
            messageParser = new AdventureParser();
        }
        messagesConfig.setup();
        setupStorage();
        languageManager.setup();
        translationManager.setup();
        if (this.packetEventsManager != null) {
            this.packetEventsManager.onReload();
        }
        startConfigRefreshTask();
    }

    /**
     * Initialize the platform's {@link PacketEventsManager} by setting the
     * {@link Triton#packetEventsManager} variable.
     * A platform is allowed to not do anything in case PacketEvents is not supported (yet).
     *
     * @since 4.0.0
     */
    protected abstract void initPacketEventsManager();

    /**
     * Preprocess a translation (i.e., the ones defined in the translations directory) when using the legacy parser.
     * This is used to support PlaceholderAPI placeholders on Spigot platforms,
     * but is an identity function on other platforms.
     *
     * @param translation The translation to process.
     * @param language    The language this translation corresponds to.
     * @return The transformed translation.
     */
    @Contract("_, _ -> param1")
    public @NotNull String preprocessLegacyParserTranslation(@NotNull String translation, @NotNull Localized language) {
        return translation;
    }

    @Contract("_, _ -> param1")
    public @NotNull String resolvePluginPlaceholdersBeforeTranslation(@NotNull String text, @NotNull Localized language) {
        return text;
    }

    @Contract("_, _ -> param1")
    public @NotNull Component resolvePluginPlaceholdersBeforeTranslation(@NotNull Component component, @NotNull Localized language) {
        return component;
    }

    @Contract("_, _ -> param1")
    public @NotNull String resolvePluginPlaceholdersAfterTranslation(@NotNull String text, @NotNull Localized language) {
        return text;
    }

    @Contract("_, _ -> param1")
    public @NotNull Component resolvePluginPlaceholdersAfterTranslation(@NotNull Component component, @NotNull Localized language) {
        return component;
    }

    public void refreshPlayers() {
        playerManager.getAll().stream()
                .filter(Objects::nonNull)
                .forEach(TritonLanguagePlayer::refreshAll);
    }

    public Configuration loadYAML(String fileName, String internalFileName) {
        File f = FileUtils.getResource(fileName + ".yml", internalFileName + ".yml");
        if (fileName.equals("config")) {
            checkAndAppendConfigProperties(f);
        }
        try {
            val stream = new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8);
            return ConfigurationProvider.getProvider(YamlConfiguration.class).load(stream);
        } catch (Exception e) {
            logger.logError(e, "Failed to load %1.yml.", fileName);
            logger.logError("You'll likely receive more errors on console until the next restart.");
        }
        return null;
    }

    private void checkAndAppendConfigProperties(File configFile) {
        try {
            if (!configFile.exists()) return;
            List<String> lines = Files.readAllLines(configFile.toPath(), StandardCharsets.UTF_8);
            boolean hasDefaultType = false;
            boolean hasSafeTranslations = false;
            boolean hasPluginPlaceholders = false;
            int insertIndex = -1;
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i).trim();
                if (line.startsWith("default-translation-type:")) {
                    hasDefaultType = true;
                }
                if (line.startsWith("safe-translations:")) {
                    hasSafeTranslations = true;
                }
                if (line.startsWith("plugin-placeholders:")) {
                    hasPluginPlaceholders = true;
                }
                if (line.startsWith("message-parser:")) {
                    insertIndex = i;
                }
            }
            
            if (!hasDefaultType || !hasSafeTranslations || !hasPluginPlaceholders) {
                List<String> newLines = new ArrayList<>(lines);
                int targetIndex = insertIndex != -1 ? insertIndex + 1 : newLines.size();
                List<String> linesToAdd = new ArrayList<>();
                
                if (!hasDefaultType) {
                    linesToAdd.add("");
                    linesToAdd.add("# The default translation type/format to use when no prefix like [minimsg] or [triton_json] is specified.");
                    linesToAdd.add("# Available values:");
                    linesToAdd.add("# - legacy (standard Minecraft formatting codes like & and §)");
                    linesToAdd.add("# - minimessage (Kyori Adventure MiniMessage formatting)");
                    linesToAdd.add("default-translation-type: \"legacy\"");
                }
                
                if (!hasSafeTranslations) {
                    linesToAdd.add("");
                    linesToAdd.add("# A security feature that prevents click action (command execution) injection from untrusted arguments.");
                    linesToAdd.add("# If enabled, Triton will strip click actions from the final translated message if the original");
                    linesToAdd.add("# translation template did not contain any click actions.");
                    linesToAdd.add("safe-translations: true");
                }

                if (!hasPluginPlaceholders) {
                    linesToAdd.add("");
                    linesToAdd.add("# Resolve other plugins' placeholders before and after Triton translations.");
                    linesToAdd.add("# Spigot only: requires PlaceholderAPI. Supports %placeholder% and {placeholder} directly.");
                    linesToAdd.add("# The alternate-prefixes below bridge registered PlaceholderAPI identifiers from forms like $placeholder and &placeholder.");
                    linesToAdd.add("# Unknown placeholders and normal color codes such as &a are left untouched.");
                    linesToAdd.add("plugin-placeholders:");
                    linesToAdd.add("  enabled: false");
                    linesToAdd.add("  before-translation: true");
                    linesToAdd.add("  after-translation: true");
                    linesToAdd.add("  alternate-prefixes:");
                    linesToAdd.add("    - \"$\"");
                    linesToAdd.add("    - \"&\"");
                }
                
                newLines.addAll(targetIndex, linesToAdd);
                Files.write(configFile.toPath(), newLines, StandardCharsets.UTF_8);
                logger.logInfo("Successfully appended missing translation configuration options to config.yml!");
            }
        } catch (Exception e) {
            logger.logError(e, "Failed to update config.yml with missing options.");
        }
    }

    public abstract String getVersion();

    public abstract @NotNull JsonElement getPlatformDebugInfo();

    protected abstract void startConfigRefreshTask();

    public abstract void runAsync(Runnable runnable);

    public abstract File getDataFolder();

    /**
     * @return config filename inside the JAR without extension for the current platform
     * @since 4.0.0
     */
    protected abstract String getConfigFileName();

    private void setupStorage() {
        if (this.storage != null) {
            this.storage.unload();
        }
        if (config.getStorageType().equalsIgnoreCase("mysql")) {
            try {
                val mysqlStorage = new MysqlStorage(config.getDatabaseHost(), config.getDatabasePort(), config
                        .getDatabaseName(), config.getDatabaseUser(), config.getDatabasePassword(), config
                        .getDatabaseTablePrefix());
                this.storage = mysqlStorage;
                mysqlStorage.load();
                logger.logInfo("Loaded MySQL storage manager");
                return;
            } catch (Exception e) {
                if (this.storage != null) {
                    this.storage.unload();
                }
                logger.logError(e, "Failed to connect to database, falling back to local storage!");
            }
        }
        this.storage = new LocalStorage();
        this.storage.load();
        logger.logInfo("Loaded local storage manager");
    }

    protected @NotNull List<@NotNull CustomChart> getBStatsCustomCharts() {
        val charts = new ArrayList<CustomChart>();
        charts.add(new SingleLineChart(
                "active_placeholders",
                () -> this.getTranslationManager().getTranslationCount()
        ));
        charts.add(new SimplePie(
                "message_parser",
                () -> this.getConfig().getParser().equalsIgnoreCase("legacy") ? "Legacy" : "Adventure"
        ));
        charts.add(new SimplePie(
                "storage_backend",
                () -> this.getConfig().getStorageType().equalsIgnoreCase("mysql") ? "MySQL" : "Local"
        ));
        charts.add(new AdvancedPie(
                "translation_types",
                () -> {
                    val data = new HashMap<String, Integer>();
                    data.put("Text", Math.min(this.getTranslationManager().getTextTranslationCount(), 1));
                    data.put("Sign", Math.min(this.getTranslationManager().getSignTranslationCount(), 1));
                    data.put("Pattern", Math.min(this.getTranslationManager().getMatchesCount(), 1));
                    return data;
                }
        ));
        return charts;
    }

    public void openLanguagesSelectionGUI(com.rexcantor64.triton.api.players.LanguagePlayer p) {
    }

    public abstract UUID getPlayerUUIDFromString(String input);

}
