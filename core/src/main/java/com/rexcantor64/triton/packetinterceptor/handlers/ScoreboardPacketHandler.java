package com.rexcantor64.triton.packetinterceptor.handlers;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.score.ScoreFormat;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerScoreboardObjective;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerResetScore;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTeams;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateScore;
import com.rexcantor64.triton.config.MainConfig;
import com.rexcantor64.triton.language.parser.MessageParser;
import com.rexcantor64.triton.player.TritonLanguagePlayer;
import com.rexcantor64.triton.utils.ScoreboardTranslationUtils;
import lombok.RequiredArgsConstructor;
import lombok.val;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class ScoreboardPacketHandler {

    private final @NotNull MessageParser parser;
    private final @NotNull MainConfig.FeatureSyntax syntax;

    public ScoreboardPacketHandler(@NotNull MessageParser parser, @NotNull MainConfig config) {
        this.parser = parser;
        this.syntax = config.getScoreboardSyntax();
    }

    public void onTeamsPacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        WrapperPlayServerTeams teams = new WrapperPlayServerTeams(event);

        val action = teams.getTeamMode();
        if (action == WrapperPlayServerTeams.TeamMode.REMOVE) {
            languagePlayer.getPacketEventsRefresh().discardScoreboardTeam(teams.getTeamName());
        }

        if (action != WrapperPlayServerTeams.TeamMode.CREATE && action != WrapperPlayServerTeams.TeamMode.UPDATE) {
            // we are only interested in new/update actions
            return;
        }

        val infoOpt = teams.getTeamInfo();
        if (!infoOpt.isPresent()) {
            // this should never happen since we have filtered by the actions before
            return;
        }
        val info = infoOpt.get();

        val originalDisplayName = info.getDisplayName();
        val originalPrefix = info.getPrefix();
        val originalSuffix = info.getSuffix();

        // display name
        parser.translateComponent(
                        originalDisplayName,
                        languagePlayer,
                        syntax
                )
                .getResultOrToRemove(Component::empty)
                .ifPresent(result -> {
                    info.setDisplayName(result);
                    event.markForReEncode(true);
                });
        // prefix
        parser.translateComponent(
                        originalPrefix,
                        languagePlayer,
                        syntax
                )
                .getResultOrToRemove(Component::empty)
                .ifPresent(result -> {
                    info.setPrefix(result);
                    event.markForReEncode(true);
                });
        // suffix
        parser.translateComponent(
                        originalSuffix,
                        languagePlayer,
                        syntax
                )
                .getResultOrToRemove(Component::empty)
                .ifPresent(result -> {
                    info.setSuffix(result);
                    event.markForReEncode(true);
                });

        if (event.needsReEncode()) {
            val teamInfoCopy = new WrapperPlayServerTeams.ScoreBoardTeamInfo(
                    originalDisplayName,
                    originalPrefix,
                    originalSuffix,
                    info.getTagVisibility(),
                    info.getCollisionRule(),
                    info.getColor(),
                    info.getOptionData()
            );

            languagePlayer.getPacketEventsRefresh().saveScoreboardTeam(teams.getTeamName(), teamInfoCopy);
        } else {
            languagePlayer.getPacketEventsRefresh().discardScoreboardTeam(teams.getTeamName());
        }
    }

    public void onObjectivePacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerScoreboardObjective(event);

        val action = packet.getMode();
        if (action == WrapperPlayServerScoreboardObjective.ObjectiveMode.REMOVE) {
            languagePlayer.getPacketEventsRefresh().discardScoreboardObjective(packet.getName());
        }

        if (action != WrapperPlayServerScoreboardObjective.ObjectiveMode.CREATE && action != WrapperPlayServerScoreboardObjective.ObjectiveMode.UPDATE) {
            // we are only interested in new/update actions
            return;
        }

        val originalDisplayName = packet.getDisplayName();
        val originalScoreFormat = packet.getScoreFormat();

        parser.translateComponent(
                        originalDisplayName,
                        languagePlayer,
                        syntax
                )
                .getResultOrToRemove(Component::empty)
                .ifPresent(result -> {
                    packet.setDisplayName(result);
                    event.markForReEncode(true);
                });

        ScoreboardTranslationUtils.translateScoreFormat(originalScoreFormat, parser, languagePlayer, syntax)
                .getResultOrToRemove(() -> ScoreFormat.fixedScore(Component.empty()))
                .ifPresent(result -> {
                    packet.setScoreFormat(result);
                    event.markForReEncode(true);
                });

        if (event.needsReEncode()) {
            languagePlayer.getPacketEventsRefresh().saveScoreboardObjective(
                    packet.getName(),
                    originalDisplayName,
                    packet.getRenderType(),
                    originalScoreFormat
            );
        } else {
            languagePlayer.getPacketEventsRefresh().discardScoreboardObjective(packet.getName());
        }
    }

    public void onUpdateScorePacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerUpdateScore(event);
        val entityName = packet.getEntityName();
        val objectiveName = packet.getObjectiveName();

        if (packet.getAction() == WrapperPlayServerUpdateScore.Action.REMOVE_ITEM) {
            languagePlayer.getPacketEventsRefresh().discardScoreboardScore(entityName, objectiveName);
            return;
        }

        val originalDisplayName = packet.getEntityDisplayName();
        val originalScoreFormat = packet.getScoreFormat();
        boolean changed = false;

        if (originalDisplayName != null) {
            val result = parser.translateComponent(originalDisplayName, languagePlayer, syntax);
            result.getResultOrToRemove(Component::empty).ifPresent(packet::setEntityDisplayName);
            changed = !result.isUnchanged();
        }

        val scoreFormatResult = ScoreboardTranslationUtils.translateScoreFormat(
                originalScoreFormat,
                parser,
                languagePlayer,
                syntax
        );
        scoreFormatResult
                .getResultOrToRemove(() -> ScoreFormat.fixedScore(Component.empty()))
                .ifPresent(packet::setScoreFormat);
        changed |= !scoreFormatResult.isUnchanged();

        if (changed) {
            event.markForReEncode(true);
            languagePlayer.getPacketEventsRefresh().saveScoreboardScore(
                    entityName,
                    objectiveName,
                    packet.getValue(),
                    originalDisplayName,
                    originalScoreFormat
            );
        } else {
            languagePlayer.getPacketEventsRefresh().discardScoreboardScore(entityName, objectiveName);
        }
    }

    public void onResetScorePacket(@NotNull PacketSendEvent event, @NotNull TritonLanguagePlayer<?> languagePlayer) {
        val packet = new WrapperPlayServerResetScore(event);
        languagePlayer.getPacketEventsRefresh().discardScoreboardScore(packet.getTargetName(), packet.getObjective());
    }

}
