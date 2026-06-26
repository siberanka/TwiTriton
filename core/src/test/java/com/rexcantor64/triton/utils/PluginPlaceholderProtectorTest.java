package com.rexcantor64.triton.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class PluginPlaceholderProtectorTest {

    @Test
    public void testProtectAndRestoreInternalPlaceholders() {
        String input = "Merhaba {oyuncu} %puan% $seviye &arena &a";

        String protectedText = PluginPlaceholderProtector.protect(input, Arrays.asList("$", "&"));

        assertNotEquals(input, protectedText);
        assertEquals("Merhaba {oyuncu} %puan% $seviye &arena &a", PluginPlaceholderProtector.restore(protectedText));
    }

    @Test
    public void testProtectAndRestoreComponents() {
        Component component = Component.text("Kazanan: {oyuncu}")
                .append(Component.text(" $ödül"))
                .clickEvent(ClickEvent.runCommand("/ödül {oyuncu}"));

        Component restored = PluginPlaceholderProtector.restore(
                PluginPlaceholderProtector.protect(component, Arrays.asList("$", "&"))
        );

        assertEquals(component, restored);
    }
}
