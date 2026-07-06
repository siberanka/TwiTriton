package com.rexcantor64.triton.spigot.placeholderapi;

import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.spigot.SpigotTriton;
import com.rexcantor64.triton.utils.ComponentUtils;
import lombok.RequiredArgsConstructor;
import lombok.val;
import me.clip.placeholderapi.PlaceholderAPI;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Relational;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RequiredArgsConstructor
public class TritonPlaceholderHook extends PlaceholderExpansion implements Relational {

    private final SpigotTriton triton;
    // whether the "viewer" in a %rel_% placeholder should be swapped
    private final boolean swapRel;

    @Override
    public @NotNull String getIdentifier() {
        return swapRel ? "triton2" : "triton";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Rexcantor64";
    }

    @Override
    public @NotNull String getVersion() {
        return triton.getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    @Nullable
    public String onPlaceholderRequest(Player p, @NotNull String params) {
        Localized locale;
        if (p == null) {
            locale = triton.getLanguageManager().getMainLanguage();
        } else {
            locale = triton.getPlayerManager().get(p.getUniqueId());
        }
        val component = params.startsWith("plat_")
                ? translateNestedLanguagePlaceholders(
                        triton.getPlatformVariantManager()
                                .getTextComponentOr404(locale, params.substring("plat_".length()), triton.getConfig().getPlatformVariantsSyntax()),
                        locale
                )
                : triton.getTranslationManager().getTextComponentOr404(locale, params);
        val text = ComponentUtils.serializeToLegacy(component);

        return PlaceholderAPI.setPlaceholders(p, text);
    }

    private net.kyori.adventure.text.Component translateNestedLanguagePlaceholders(net.kyori.adventure.text.Component component,
                                                                                   Localized locale) {
        val result = triton.getMessageParser().translateComponent(component, locale, triton.getConfig().getChatSyntax());
        if (result.isToRemove()) {
            return net.kyori.adventure.text.Component.empty();
        }
        return result.getResult().orElse(component);
    }


    @Override
    public String onPlaceholderRequest(Player viewer2, Player viewer1, String params) {
        return onPlaceholderRequest(swapRel ? viewer2 : viewer1, params);
    }
}
