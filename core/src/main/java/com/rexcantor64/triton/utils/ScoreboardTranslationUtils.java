package com.rexcantor64.triton.utils;

import com.github.retrooper.packetevents.protocol.score.FixedScoreFormat;
import com.github.retrooper.packetevents.protocol.score.ScoreFormat;
import com.rexcantor64.triton.api.config.FeatureSyntax;
import com.rexcantor64.triton.api.language.Localized;
import com.rexcantor64.triton.language.parser.MessageParser;
import com.rexcantor64.triton.language.parser.TranslationResult;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@UtilityClass
public class ScoreboardTranslationUtils {

    /**
     * Translates the fixed component used by modern scoreboards for right-aligned score text.
     * Blank and styled formats do not contain translatable text and are left untouched.
     */
    public @NotNull TranslationResult<ScoreFormat> translateScoreFormat(
            @Nullable ScoreFormat scoreFormat,
            @NotNull MessageParser parser,
            @NotNull Localized language,
            @NotNull FeatureSyntax syntax
    ) {
        if (!(scoreFormat instanceof FixedScoreFormat)) {
            return TranslationResult.unchanged();
        }

        FixedScoreFormat fixed = (FixedScoreFormat) scoreFormat;
        return parser.translateComponent(fixed.getValue(), language, syntax)
                .map(ScoreFormat::fixedScore);
    }
}
