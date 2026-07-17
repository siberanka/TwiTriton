package com.rexcantor64.triton.commands;

import com.rexcantor64.triton.commands.handler.Command;
import com.rexcantor64.triton.commands.handler.CommandEvent;
import com.rexcantor64.triton.commands.handler.exceptions.NoPermissionException;
import com.rexcantor64.triton.commands.handler.exceptions.PlayerOnlyCommandException;
import com.rexcantor64.triton.commands.handler.exceptions.UnsupportedPlatformException;
import com.rexcantor64.triton.plugin.Platform;

import java.util.Collections;
import java.util.List;

public class OpenSelectorCommand implements Command {

    @Override
    public void handleCommand(CommandEvent event) throws NoPermissionException, PlayerOnlyCommandException, UnsupportedPlatformException {
        assertPlayersOnly(event);

        java.util.UUID uuid = event.getSender().getUUID();
        if (uuid != null && com.rexcantor64.triton.bridge.BedrockBridge.openLanguageSelectionForm(uuid)) {
            return;
        }

        if (event.getPlatform() != Platform.SPIGOT) {
            throw new UnsupportedPlatformException();
        }

        // Command handler is overridden on the triton-spigot module
    }

    @Override
    public List<String> handleTabCompletion(CommandEvent event) {
        return Collections.emptyList();
    }
}
