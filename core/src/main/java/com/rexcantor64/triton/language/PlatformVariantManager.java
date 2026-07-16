package com.rexcantor64.triton.language;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.api.players.LanguagePlayer;
import com.rexcantor64.triton.bridge.BedrockBridge;
import com.rexcantor64.triton.language.item.TWINData;
import com.rexcantor64.triton.utils.FileUtils;
import lombok.Cleanup;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@RequiredArgsConstructor
public class PlatformVariantManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type COLLECTION_TYPE = new TypeToken<PlatformCollection>() {
    }.getType();

    private final Triton<?, ?> triton;
    private final Map<String, PlatformText> items = new HashMap<>();

    @Getter
    private int variantCount = 0;

    public synchronized void setup() {
        this.items.clear();
        this.variantCount = 0;

        if (!this.triton.getConfig().isPlatformVariants()) {
            this.triton.getLogger().logInfo("Platform Variant Manager disabled.");
            return;
        }

        String folderName = sanitizeFolderName(this.triton.getConfig().getPlatformVariantsFolder());
        File folder = new File(this.triton.getDataFolder(), folderName);
        if (!folder.exists()) {
            createSampleFolder(folder);
        }

        if (!folder.isDirectory()) {
            this.triton.getLogger().logError("There is a file named '%1' in the Triton folder that is not a folder.", folderName);
            return;
        }

        File[] files = folder.listFiles();
        if (files == null) {
            this.triton.getLogger().logError("An I/O error occurred while loading the %1 folder.", folderName);
            return;
        }

        for (File file : files) {
            if (TutorialFileManager.isTutorialFile(file)) {
                continue;
            }
            if (!file.getName().endsWith(".json")) {
                this.triton.getLogger().logWarning("Did not load file %1 because it is not a JSON file.", file.getName());
                continue;
            }

            try {
                PlatformCollection collection = GSON.fromJson(FileUtils.getReaderFromFile(file), COLLECTION_TYPE);
                if (collection == null || collection.getItems() == null) {
                    continue;
                }
                for (PlatformText item : collection.getItems()) {
                    if (item == null || item.getKey() == null || item.getKey().isEmpty() || item.isArchived()
                            || !"platform".equalsIgnoreCase(item.getType())) {
                        continue;
                    }
                    if (!item.normalizeVariants()) {
                        this.triton.getLogger().logWarning(
                                "Did not load platform variant '%1' from %2 because it has no valid Java or Bedrock values.",
                                item.getKey(), file.getName());
                        continue;
                    }
                    this.items.put(item.getKey(), item);
                    this.variantCount++;
                }
            } catch (JsonParseException e) {
                this.triton.getLogger().logError(e, "Failed to load platform variants collection %1 because it has invalid syntax.", file.getName());
            }
        }

        this.triton.getLogger().logInfo("Successfully setup the Platform Variant Manager! %1 variants loaded!", this.variantCount);
    }

    public @NotNull Optional<String> getTextString(@NotNull Localized localized, @NotNull String key) {
        PlatformText item = this.items.get(key);
        if (item == null) {
            return Optional.empty();
        }

        String platform = getPlatformKey(localized);
        com.rexcantor64.triton.api.language.Language language = localized.getLanguage();
        com.rexcantor64.triton.api.language.Language mainLanguage =
                this.triton.getLanguageManager().getMainLanguage();

        Optional<String> value = item.resolve(platform, language, mainLanguage);
        if (value.isPresent()) {
            return value;
        }

        String fallbackPlatform = platform.equals("bedrock") ? "java" : "bedrock";
        return item.resolve(fallbackPlatform, language, mainLanguage);
    }

    public @NotNull Component getTextComponentOr404(@NotNull Localized localized,
                                                    @NotNull String key,
                                                    @NotNull FeatureSyntax syntax,
                                                    Component... arguments) {
        return getTextString(localized, key)
                .map(string -> {
                    Component templateComponent = this.triton.getTranslationManager()
                            .handleTranslationType(string, localized.getLanguage());
                    boolean safeMode = this.triton.getConfig().isSafeTranslations() && syntax.isSafeTranslations();
                    Component[] processedArguments = arguments;
                    if (safeMode && arguments != null) {
                        processedArguments = new Component[arguments.length];
                        for (int i = 0; i < arguments.length; i++) {
                            processedArguments[i] = com.rexcantor64.triton.utils.ComponentUtils.sanitizeComponent(
                                    com.rexcantor64.triton.utils.ComponentUtils.stripClickEvents(arguments[i])
                            );
                        }
                    }
                    boolean hadClick = safeMode && com.rexcantor64.triton.utils.ComponentUtils.hasClickEvents(templateComponent);
                    Component finalComponent = this.triton.getMessageParser().replaceArguments(templateComponent, java.util.Arrays.asList(processedArguments));
                    if (safeMode && !hadClick) {
                        finalComponent = com.rexcantor64.triton.utils.ComponentUtils.stripClickEvents(finalComponent);
                    }
                    return finalComponent;
                })
                .orElseGet(() -> this.triton.getTranslationManager().getTranslationNotFoundComponent(key, arguments));
    }

    public boolean hasVariantSyntax(@NotNull String text) {
        return text.contains("[" + this.triton.getConfig().getPlatformVariantsSyntax().getLang() + "]");
    }

    private String getPlatformKey(@NotNull Localized localized) {
        if (localized instanceof LanguagePlayer) {
            LanguagePlayer player = (LanguagePlayer) localized;
            if (BedrockBridge.isBedrockPlayer(player.getUUID())) {
                return "bedrock";
            }
        }
        return "java";
    }

    static String sanitizeFolderName(String folderName) {
        if (folderName == null || folderName.trim().isEmpty()) {
            return "platforms";
        }
        String sanitized = folderName.trim().replace('\\', '/');
        if (sanitized.contains("/") || sanitized.equals(".") || sanitized.equals("..")) {
            return "platforms";
        }
        return sanitized;
    }

    private void createSampleFolder(File folder) {
        this.triton.getLogger().logDebug("Creating a sample platform variant at %1/default.json...", folder.getName());

        if (!folder.mkdirs()) {
            this.triton.getLogger().logError("Failed to create '%1' folder. Check if the server has the required permissions.", folder.getName());
            return;
        }

        PlatformCollection collection = new PlatformCollection();
        PlatformText sample = new PlatformText();
        sample.setKey("example.variant");
        sample.getVariants().put("java", new JsonPrimitive("&aJava player text with %1."));
        sample.getVariants().put("bedrock", new JsonPrimitive("&bBedrock player text with %1."));
        collection.getItems().add(sample);

        File sampleFile = new File(folder, "default.json");
        try {
            this.triton.getLogger().logDebug("Saving %1/default.json", folder.getName());
            @Cleanup val fileWriter = FileUtils.getWriterFromFile(sampleFile);
            GSON.toJson(collection, fileWriter);
            this.triton.getLogger().logDebug("Saved %1/default.json", folder.getName());
        } catch (Exception e) {
            this.triton.getLogger().logError(e, "Failed to save %1/default.json.", folder.getName());
        }
    }

    @Data
    private static class PlatformCollection {
        private ArrayList<PlatformText> items = new ArrayList<>();
    }

    @Data
    private static class PlatformText {
        private String key;
        private String type = "platform";
        private HashMap<String, JsonElement> variants = new HashMap<>();
        private TWINData _twin = null;
        private transient Map<String, PlatformVariantValue> normalizedVariants = Collections.emptyMap();

        private boolean normalizeVariants() {
            HashMap<String, PlatformVariantValue> normalized = new HashMap<>();
            if (variants == null) {
                this.normalizedVariants = normalized;
                return false;
            }
            for (Map.Entry<String, JsonElement> entry : variants.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                String platform = entry.getKey().trim().toLowerCase(Locale.ROOT);
                if (!platform.equals("java") && !platform.equals("bedrock")) {
                    continue;
                }
                PlatformVariantValue.fromJson(entry.getValue()).ifPresent(value ->
                        normalized.put(platform, value));
            }
            this.normalizedVariants = Collections.unmodifiableMap(normalized);
            return !normalized.isEmpty();
        }

        private Optional<String> resolve(String platform,
                                         com.rexcantor64.triton.api.language.Language language,
                                         com.rexcantor64.triton.api.language.Language mainLanguage) {
            PlatformVariantValue value = this.normalizedVariants.get(platform);
            if (value == null) {
                return Optional.empty();
            }
            return value.resolve(language, mainLanguage);
        }

        private boolean isArchived() {
            return _twin != null && _twin.isArchived();
        }
    }
}
