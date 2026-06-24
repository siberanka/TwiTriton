package com.rexcantor64.triton.api.config;

/**
 * Represents how Triton should look for placeholders.
 *
 * @since 1.0.0
 */
public interface FeatureSyntax {

    /**
     * Get the key of the tag that starts/ends the entire placeholder for a specific feature (chat, scoreboard, titles, etc).
     * Default is "lang".
     *
     * @return The key of the tag that starts/end the entire placeholder
     * @since 1.0.0
     */
    String getLang();

    /**
     * Get the key of the tag that starts/ends the variables of a placeholder for a specific feature (chat, scoreboard, titles, etc).
     * Default is "args".
     *
     * @return The key of the tag that starts/end the variables of a placeholder
     * @since 1.0.0
     * @deprecated The [args] tag is no longer needed as of Triton v4.0.0.
     * Simply using the [arg] tags without surrounding them with [args].
     */
    @Deprecated(since = "4.0.0")
    String getArgs();

    /**
     * Get the key of the tag that starts/ends a variable of a placeholder for a specific feature (chat, scoreboard, titles, etc).
     * Default is "arg".
     *
     * @return The key of the tag that starts/end a variable of a placeholder
     * @since 1.0.0
     */
    String getArg();

    /**
     * Whether safe-translations should be enforced on translations matching this syntax.
     * If true, Triton will strip click events from arguments and the final component if safe-translations is enabled in config.
     * If false, safe-translations will be skipped.
     *
     * @return Whether safe-translations should be enforced
     */
    default boolean isSafeTranslations() {
        return true;
    }

    /**
     * Creates a new FeatureSyntax that wraps an existing one but overrides isSafeTranslations to return the specified value.
     */
    static FeatureSyntax withSafeTranslations(FeatureSyntax parent, boolean safe) {
        return new FeatureSyntax() {
            @Override
            public String getLang() {
                return parent.getLang();
            }

            @Override
            public String getArgs() {
                return parent.getArgs();
            }

            @Override
            public String getArg() {
                return parent.getArg();
            }

            @Override
            public boolean isSafeTranslations() {
                return safe;
            }
        };
    }
}

