package com.rexcantor64.triton.utils;

import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PluginPlaceholderProtector {

    private static final char START = '\uE900';
    private static final char END = '\uE901';
    private static final Pattern RESTORE_PATTERN = Pattern.compile(START + "([0-9A-Fa-f]+)" + END);
    private static final String IDENTIFIER = "[\\p{L}_][\\p{L}\\p{N}_.:-]*";
    private static final String IDENTIFIER_LONG = "[\\p{L}_][\\p{L}\\p{N}_.:-]+";

    private PluginPlaceholderProtector() {
    }

    public static @NotNull String protect(@NotNull String text, @NotNull List<String> alternatePrefixes) {
        String pattern = buildPattern(alternatePrefixes);
        if (pattern.isEmpty()) {
            return text;
        }
        Matcher matcher = Pattern.compile(pattern).matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(encode(matcher.group())));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public static @NotNull Component protect(@NotNull Component component, @NotNull List<String> alternatePrefixes) {
        return ComponentUtils.transformTextContent(component, text -> protect(text, alternatePrefixes));
    }

    public static @NotNull String restore(@NotNull String text) {
        Matcher matcher = RESTORE_PATTERN.matcher(text);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(decode(matcher.group(1))));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    public static @NotNull Component restore(@NotNull Component component) {
        return ComponentUtils.transformTextContent(component, PluginPlaceholderProtector::restore);
    }

    private static @NotNull String buildPattern(@NotNull List<String> alternatePrefixes) {
        StringBuilder pattern = new StringBuilder();
        appendAlternative(pattern, "%" + IDENTIFIER + "%");
        appendAlternative(pattern, "\\{" + IDENTIFIER + "\\}");

        for (String prefix : alternatePrefixes) {
            if (prefix == null || prefix.length() != 1) {
                continue;
            }
            String identifierPattern = prefix.charAt(0) == '&' ? IDENTIFIER_LONG : IDENTIFIER;
            appendAlternative(pattern, Pattern.quote(prefix) + identifierPattern);
        }

        return pattern.toString();
    }

    private static void appendAlternative(@NotNull StringBuilder pattern, @NotNull String alternative) {
        if (pattern.length() > 0) {
            pattern.append('|');
        }
        pattern.append('(').append(alternative).append(')');
    }

    private static @NotNull String encode(@NotNull String text) {
        StringBuilder encoded = new StringBuilder(text.length() * 4 + 2);
        encoded.append(START);
        for (int i = 0; i < text.length(); i++) {
            String hex = Integer.toHexString(text.charAt(i)).toUpperCase();
            for (int pad = hex.length(); pad < 4; pad++) {
                encoded.append('0');
            }
            encoded.append(hex);
        }
        encoded.append(END);
        return encoded.toString();
    }

    private static @NotNull String decode(@NotNull String encoded) {
        if (encoded.length() % 4 != 0) {
            return encoded;
        }
        StringBuilder decoded = new StringBuilder(encoded.length() / 4);
        for (int i = 0; i < encoded.length(); i += 4) {
            decoded.append((char) Integer.parseInt(encoded.substring(i, i + 4), 16));
        }
        return decoded.toString();
    }
}
