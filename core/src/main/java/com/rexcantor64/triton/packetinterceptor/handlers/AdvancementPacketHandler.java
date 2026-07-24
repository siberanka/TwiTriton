package com.rexcantor64.triton.packetinterceptor.handlers;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.advancements.AdvancementDisplay;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAdvancements;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.MessageParser;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.NotNullByDefault;

@RequiredArgsConstructor
@NotNullByDefault
public class AdvancementPacketHandler {

    private final MessageParser parser;
    private final MainConfig.FeatureSyntax syntax;

    public AdvancementPacketHandler(@NotNull MessageParser parser, @NotNull MainConfig config) {
        this.parser = parser;
        this.syntax = config.getAdvancementsSyntax();
    }

    public void onUpdateAdvancementsPacket(@NotNull PacketSendEvent event,
                                            @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerUpdateAdvancements(event);
        boolean changed = false;

        for (val holder : packet.getAddedAdvancements()) {
            val advancement = holder.getAdvancement();
            if (advancement == null || advancement.getDisplay() == null) {
                continue;
            }
            changed |= translateDisplay(advancement.getDisplay(), languagePlayer);
        }

        if (changed) {
            event.markForReEncode(true);
        }
    }

    boolean translateDisplay(@NotNull AdvancementDisplay display, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val title = parser.translateComponent(display.getTitle(), languagePlayer, syntax);
        title.getResultOrToRemove(Component::empty).ifPresent(display::setTitle);

        val description = parser.translateComponent(display.getDescription(), languagePlayer, syntax);
        description.getResultOrToRemove(Component::empty).ifPresent(display::setDescription);

        return !title.isUnchanged() || !description.isUnchanged();
    }
}
