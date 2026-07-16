package com.rexcantor64.triton.language;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.rexcantor64.triton.api.language.Language;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

final class PlatformVariantValue {

    private final @Nullable String sharedValue;
    private final Map<String, String> localizedValues;

    private PlatformVariantValue(@Nullable String sharedValue, Map<String, String> localizedValues) {
        this.sharedValue = sharedValue;
        this.localizedValues = localizedValues;
    }

    static Optional<PlatformVariantValue> fromJson(@Nullable JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return Optional.empty();
        }

        if (element.isJsonPrimitive()) {
            JsonPrimitive primitive = element.getAsJsonPrimitive();
            return primitive.isString()
                    ? Optional.of(new PlatformVariantValue(primitive.getAsString(), Collections.emptyMap()))
                    : Optional.empty();
        }

        if (!element.isJsonObject()) {
            return Optional.empty();
        }

        Map<String, String> values = new LinkedHashMap<>();
        JsonObject object = element.getAsJsonObject();
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String languageName = normalize(entry.getKey());
            JsonElement value = entry.getValue();
            if (languageName.isEmpty() || value == null || !value.isJsonPrimitive()
                    || !value.getAsJsonPrimitive().isString()) {
                continue;
            }
            values.put(languageName, value.getAsString());
        }

        if (values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PlatformVariantValue(null, Collections.unmodifiableMap(values)));
    }

    Optional<String> resolve(@Nullable Language language, @Nullable Language mainLanguage) {
        if (this.sharedValue != null) {
            return Optional.of(this.sharedValue);
        }

        String value = getForLanguage(language == null ? null : language.getName());
        if (value != null) {
            return Optional.of(value);
        }

        if (language != null && language.getFallbackLanguages() != null) {
            for (String fallbackLanguage : language.getFallbackLanguages()) {
                value = getForLanguage(fallbackLanguage);
                if (value != null) {
                    return Optional.of(value);
                }
            }
        }

        value = getForLanguage(mainLanguage == null ? null : mainLanguage.getName());
        if (value != null) {
            return Optional.of(value);
        }

        return Optional.ofNullable(this.localizedValues.get("default"));
    }

    private @Nullable String getForLanguage(@Nullable String languageName) {
        if (languageName == null) {
            return null;
        }
        return this.localizedValues.get(normalize(languageName));
    }

    private static String normalize(@Nullable String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }
}
