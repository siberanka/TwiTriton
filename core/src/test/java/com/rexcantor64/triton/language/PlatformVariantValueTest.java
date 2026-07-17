package com.rexcantor64.triton.language;

import com.google.gson.JsonParser;
import com.rexcantor64.triton.api.language.Language;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformVariantValueTest {

    @Test
    void preservesSharedStringFormat() {
        PlatformVariantValue value = parse("\"&aShared text\"");

        assertEquals("&aShared text", value.resolve(language("tr_TR"), language("en_US")).orElse(null));
    }

    @Test
    void resolvesSelectedFallbackMainAndDefaultLanguagesInOrder() {
        PlatformVariantValue value = parse("{"
                + "\"tr_TR\":\"selected\","
                + "\"en_GB\":\"fallback\","
                + "\"en_US\":\"main\","
                + "\"default\":\"default\"}");

        assertEquals("selected", value.resolve(language("tr_TR", "en_GB"), language("en_US")).orElse(null));
        assertEquals("fallback", value.resolve(language("de_DE", "en_GB"), language("en_US")).orElse(null));
        assertEquals("main", value.resolve(language("de_DE"), language("en_US")).orElse(null));
        assertEquals("default", value.resolve(language("de_DE"), language("fr_FR")).orElse(null));
    }

    @Test
    void languageNamesAreCaseInsensitive() {
        PlatformVariantValue value = parse("{\"TR_tr\":\"localized\"}");

        assertEquals("localized", value.resolve(language("tr_TR"), language("en_US")).orElse(null));
    }

    @Test
    void rejectsUnsupportedJsonShapesAndSkipsInvalidObjectEntries() {
        assertFalse(PlatformVariantValue.fromJson(JsonParser.parseString("[\"invalid\"]")).isPresent());
        assertFalse(PlatformVariantValue.fromJson(JsonParser.parseString("42")).isPresent());

        PlatformVariantValue partiallyValid = parse("{\"tr_TR\":42,\"en_US\":\"valid\"}");
        assertEquals("valid", partiallyValid.resolve(language("en_US"), language("tr_TR")).orElse(null));
        assertTrue(PlatformVariantValue.fromJson(JsonParser.parseString("{\"tr_TR\":42}")).isEmpty());
    }

    private static PlatformVariantValue parse(String json) {
        return PlatformVariantValue.fromJson(JsonParser.parseString(json)).orElseThrow(AssertionError::new);
    }

    private static Language language(String name, String... fallbacks) {
        return new TestLanguage(name, Arrays.asList(fallbacks));
    }

    private static class TestLanguage implements Language {
        private final String name;
        private final List<String> fallbacks;

        private TestLanguage(String name, List<String> fallbacks) {
            this.name = name;
            this.fallbacks = fallbacks;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public List<String> getMinecraftCodes() {
            return Collections.singletonList(this.name);
        }

        @Override
        public String getDisplayName() {
            return this.name;
        }

        @Override
        public String getRawDisplayName() {
            return this.name;
        }

        @Override
        public String getFlagCode() {
            return "gb";
        }

        @Override
        public List<String> getFallbackLanguages() {
            return this.fallbacks;
        }

        @Override
        public Language getLanguage() {
            return this;
        }
    }
}
