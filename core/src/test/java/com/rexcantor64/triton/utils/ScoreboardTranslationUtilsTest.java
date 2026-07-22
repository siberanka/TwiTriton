package com.rexcantor64.triton.utils;

import com.github.retrooper.packetevents.protocol.score.FixedScoreFormat;
import com.github.retrooper.packetevents.protocol.score.ScoreFormat;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.test.DefaultFeatureSyntax;
import com.rexcantor64.triton.test.MockAdventureParser;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreboardTranslationUtilsTest {

    private final MockAdventureParser parser = new MockAdventureParser();
    private final DefaultFeatureSyntax syntax = new DefaultFeatureSyntax();
    private final Localized localized = () -> {
        throw new IllegalStateException("Language lookup is unavailable during testing");
    };

    @Test
    void translatesTabRightAlignedFixedScoreText() {
        ScoreFormat format = ScoreFormat.fixedScore(
                Component.text("12.81[lang]scoreboard.format_million[/lang]")
        );

        var result = ScoreboardTranslationUtils.translateScoreFormat(format, parser, localized, syntax);

        assertTrue(result.isChanged());
        FixedScoreFormat translated = (FixedScoreFormat) result.getResult().orElseThrow();
        assertEquals(
                Component.empty()
                        .append(Component.text("12.81"))
                        .append(Component.text("replaced(scoreboard.format_million)")),
                translated.getValue()
        );
    }

    @Test
    void turnsDisabledFixedScoreTextIntoRemoval() {
        ScoreFormat format = ScoreFormat.fixedScore(Component.text("[lang]disabled.line[/lang]"));

        var result = ScoreboardTranslationUtils.translateScoreFormat(format, parser, localized, syntax);

        assertTrue(result.isToRemove());
    }

    @Test
    void leavesNonTextScoreFormatsUntouched() {
        ScoreFormat styled = ScoreFormat.styledScore(Style.style(NamedTextColor.GREEN));

        assertTrue(ScoreboardTranslationUtils.translateScoreFormat(styled, parser, localized, syntax).isUnchanged());
        assertTrue(ScoreboardTranslationUtils.translateScoreFormat(
                ScoreFormat.blankScore(),
                parser,
                localized,
                syntax
        ).isUnchanged());
    }
}
