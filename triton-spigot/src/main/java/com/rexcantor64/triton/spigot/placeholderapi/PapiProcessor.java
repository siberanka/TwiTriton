package com.rexcantor64.triton.spigot.placeholderapi;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PapiProcessor {

    private static final String COLOR_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRrXx";

    /**
     * @see PlaceholderAPI#setPlaceholders(Player, String)
     * @since 4.0.0
     */
    public static @NotNull String replacePlaceholders(@NotNull String message, @NotNull Player player) {
        return PlaceholderAPI.setPlaceholders(player, message);
    }

    public static @NotNull String replacePlaceholders(@NotNull String message, @NotNull Player player,
                                                      @NotNull List<String> alternatePrefixes) {
        String result = bridgeAlternatePrefixes(message, alternatePrefixes);
        result = PlaceholderAPI.setPlaceholders(player, result);
        return PlaceholderAPI.setBracketPlaceholders(player, result);
    }

    private static @NotNull String bridgeAlternatePrefixes(@NotNull String message,
                                                           @NotNull List<String> alternatePrefixes) {
        String result = message;
        for (String prefix : alternatePrefixes) {
            if (prefix == null || prefix.length() != 1) {
                continue;
            }
            result = bridgeAlternatePrefix(result, prefix.charAt(0));
        }
        return result;
    }

    private static @NotNull String bridgeAlternatePrefix(@NotNull String message, char prefix) {
        Pattern pattern = Pattern.compile(Pattern.quote(String.valueOf(prefix)) + "([A-Za-z][A-Za-z0-9_]*)(?![A-Za-z0-9_])");
        Matcher matcher = pattern.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String placeholder = matcher.group(1);
            if (isColorCode(prefix, placeholder) || !isRegisteredPlaceholder(placeholder)) {
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group()));
                continue;
            }
            matcher.appendReplacement(buffer, Matcher.quoteReplacement("%" + placeholder + "%"));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static boolean isColorCode(char prefix, @NotNull String placeholder) {
        return prefix == '&' && placeholder.length() == 1 && COLOR_CODES.indexOf(placeholder.charAt(0)) != -1;
    }

    private static boolean isRegisteredPlaceholder(@NotNull String placeholder) {
        int paramsIndex = placeholder.indexOf('_');
        String identifier = paramsIndex == -1 ? placeholder : placeholder.substring(0, paramsIndex);
        return PlaceholderAPI.isRegistered(identifier);
    }
}
