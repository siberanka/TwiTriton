package com.rexcantor64.triton.utils;

import lombok.val;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.junit.jupiter.api.Test;

import static com.rexcantor64.triton.utils.ComponentUtils.SECTION_CHAR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ComponentUtilsTest {

    @Test
    public void testSplitByNewLineWithoutExtras() {
        Component component = Component.text("First line\nSecond line").color(NamedTextColor.BLUE);

        val result = ComponentUtils.splitByNewLine(component);

        Component[] expected = new Component[]{
                Component.text("First line").color(NamedTextColor.BLUE),
                Component.text("Second line").color(NamedTextColor.BLUE)
        };

        assertEquals(expected.length, result.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].compact(), result.get(i).compact());
        }
    }

    @Test
    public void testSplitByNewLineWithExtras() {
        Component component = Component.text()
                .decorate(TextDecoration.ITALIC)
                .append(
                        Component.text()
                                .content("First line \nSecond ")
                                .color(NamedTextColor.BLACK),
                        Component.text()
                                .content("li")
                                .color(NamedTextColor.RED)
                                .decorate(TextDecoration.BOLD)
                                .append(
                                        Component.text("ne\nThird line")
                                                .decorate(TextDecoration.UNDERLINED)
                                )
                )
                .asComponent();

        val result = ComponentUtils.splitByNewLine(component);

        ComponentLike[] expected = new ComponentLike[]{
                Component.text()
                        .decorate(TextDecoration.ITALIC)
                        .append(
                        Component.text()
                                .content("First line ")
                                .color(NamedTextColor.BLACK)
                ),
                Component.text()
                        .decorate(TextDecoration.ITALIC)
                        .append(
                        Component.text()
                                .content("Second ")
                                .color(NamedTextColor.BLACK),
                        Component.text()
                                .content("li")
                                .color(NamedTextColor.RED)
                                .decorate(TextDecoration.BOLD)
                                .append(
                                        Component.text("ne")
                                                .decorate(TextDecoration.UNDERLINED)
                                )
                ),
                Component.text()
                        .decorate(TextDecoration.ITALIC)
                        .append(
                        Component.text()
                                .color(NamedTextColor.RED)
                                .decorate(TextDecoration.BOLD)
                                .append(
                                        Component.text("Third line")
                                                .decorate(TextDecoration.UNDERLINED)
                                )
                )
        };

        assertEquals(expected.length, result.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].asComponent().compact(), result.get(i).compact());
        }
    }

    @Test
    public void testSplitByNewLineWithSlashNAtTheEnd() {
        Component component = Component.text()
                .append(
                        Component.text("First line!\n").color(NamedTextColor.DARK_PURPLE),
                        Component.text("Second Line").color(NamedTextColor.GRAY)
                )
                .asComponent();

        val result = ComponentUtils.splitByNewLine(component);

        ComponentLike[] expected = new ComponentLike[]{
                Component.text()
                        .append(
                        Component.text("First line!").color(NamedTextColor.DARK_PURPLE)
                ),
                Component.text()
                        .append(
                        Component.text("").color(NamedTextColor.DARK_PURPLE),
                        Component.text("Second Line").color(NamedTextColor.GRAY)
                )
        };

        assertEquals(expected.length, result.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].asComponent().compact(), result.get(i).compact());
        }
    }

    @Test
    public void testHasLegacyFormattingWithLegacyText() {
        String input = SECTION_CHAR + "aHello World!";

        assertTrue(ComponentUtils.hasLegacyFormatting(input));
    }

    @Test
    public void testHasLegacyFormattingWithoutLegacyText() {
        String input = "Hello World!";

        assertFalse(ComponentUtils.hasLegacyFormatting(input));
    }

    @Test
    public void testUnflattenLegacyFormattingFromSimpleComponent() {
        Component component = Component.text("Hello " + SECTION_CHAR + "bWorld!");

        Component result = ComponentUtils.unflattenLegacyFormatting(component);

        Component expected = Component.text("Hello ")
                .append(
                        Component.text("World!").color(NamedTextColor.AQUA)
                );

        assertEquals(result.compact(), expected.compact());
    }

    @Test
    public void testUnflattenLegacyFormattingFromNestedComponent() {
        Component component = Component.keybind()
                .keybind("test")
                .append(
                        Component.text()
                                .content("Hello " + SECTION_CHAR + "cWorld!")
                                .color(NamedTextColor.GREEN)
                                .append(
                                        Component.text()
                                                .content(" Lorem " + SECTION_CHAR + "0Ipsum")
                                                .color(NamedTextColor.BLUE)
                                )
                )
                .asComponent();

        Component result = ComponentUtils.unflattenLegacyFormatting(component);

        Component expected = Component.keybind()
                .keybind("test")
                .append(
                        Component.text()
                                .content("Hello ")
                                .color(NamedTextColor.GREEN)
                                .append(
                                        Component.text()
                                                .content("World!")
                                                .color(NamedTextColor.RED),
                                        Component.text()
                                                .content(" Lorem ")
                                                .color(NamedTextColor.BLUE)
                                                .append(
                                                        Component.text()
                                                                .content("Ipsum")
                                                                .color(NamedTextColor.BLACK)
                                                )
                                )
                )
                .asComponent();

        assertEquals(result.compact(), expected.compact());
    }

    @Test
    public void testClickEventSafetyUtilities() {
        Component normal = Component.text("Hello world");
        assertFalse(ComponentUtils.hasClickEvents(normal));

        Component withClick = Component.text("Click me")
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/say hello"));
        assertTrue(ComponentUtils.hasClickEvents(withClick));

        Component nestedWithClick = Component.text("Hello ")
                .append(Component.text("world").clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/say nested")));
        assertTrue(ComponentUtils.hasClickEvents(nestedWithClick));

        Component hoverWithClick = Component.text("Hover me")
                .hoverEvent(net.kyori.adventure.text.event.HoverEvent.showText(Component.text("Inside hover").clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/say hover"))));
        assertTrue(ComponentUtils.hasClickEvents(hoverWithClick));

        Component strippedClick = ComponentUtils.stripClickEvents(withClick);
        assertFalse(ComponentUtils.hasClickEvents(strippedClick));

        Component strippedNested = ComponentUtils.stripClickEvents(nestedWithClick);
        assertFalse(ComponentUtils.hasClickEvents(strippedNested));

        Component strippedHover = ComponentUtils.stripClickEvents(hoverWithClick);
        assertFalse(ComponentUtils.hasClickEvents(strippedHover));

        Component mixed = Component.text("Text ")
                .clickEvent(net.kyori.adventure.text.event.ClickEvent.suggestCommand("/msg player"))
                .append(Component.text("hacker").clickEvent(net.kyori.adventure.text.event.ClickEvent.runCommand("/op hacker")));

        Component strippedMixed = ComponentUtils.stripRunCommandClickEvents(mixed);
        assertTrue(ComponentUtils.hasClickEvents(strippedMixed));
        org.junit.jupiter.api.Assertions.assertNotNull(strippedMixed.clickEvent());
        assertEquals(net.kyori.adventure.text.event.ClickEvent.Action.SUGGEST_COMMAND, strippedMixed.clickEvent().action());
        org.junit.jupiter.api.Assertions.assertNull(strippedMixed.children().get(0).clickEvent());
    }

    @Test
    public void testSanitizationUtilities() {
        String inputWithDelimiters = "Hello \uE400 world \uE501!";
        String sanitized = ComponentUtils.sanitizeDelimiters(inputWithDelimiters);
        assertEquals("Hello  world !", sanitized);

        Component compWithDelimiters = Component.text("Text \uE400")
                .append(Component.text("Child \uE802"));
        Component sanitizedComp = ComponentUtils.sanitizeComponent(compWithDelimiters);
        assertEquals("Text ", ((TextComponent) sanitizedComp).content());
        assertEquals("Child ", ((TextComponent) sanitizedComp.children().get(0)).content());
    }

}


