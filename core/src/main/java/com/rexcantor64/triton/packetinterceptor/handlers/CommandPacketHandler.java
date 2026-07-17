package com.rexcantor64.triton.packetinterceptor.handlers;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTabComplete;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.MessageParser;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class CommandPacketHandler {

    private final @NotNull MessageParser parser;
    private final @NotNull MainConfig.FeatureSyntax syntax;

    public CommandPacketHandler(@NotNull MessageParser parser, @NotNull MainConfig config) {
        this.parser = parser;
        this.syntax = config.getChatSyntax();
    }

    public void onTabCompletePacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerTabComplete(event);
        boolean changed = false;

        for (WrapperPlayServerTabComplete.CommandMatch match : packet.getCommandMatches()) {
            val tooltip = match.getTooltip();
            if (!tooltip.isPresent()) {
                continue;
            }

            val result = parser.translateComponent(tooltip.get(), languagePlayer, syntax);
            if (result.isToRemove()) {
                match.setTooltip(Component.empty());
                changed = true;
            } else if (result.getResult().isPresent()) {
                match.setTooltip(result.getResultRaw());
                changed = true;
            }
        }

        if (changed) {
            event.markForReEncode(true);
        }
    }
}
