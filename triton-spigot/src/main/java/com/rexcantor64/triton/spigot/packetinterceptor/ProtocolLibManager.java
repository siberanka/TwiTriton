package com.rexcantor64.triton.spigot.packetinterceptor;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.utility.MinecraftVersion;
import com.rexcantor64.triton.Triton;
import com.rexcantor64.triton.spigot.SpigotTriton;
import lombok.val;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

public class ProtocolLibManager {

    public static boolean isProtocolLibInstalled() {
        return Bukkit.getPluginManager().getPlugin("ProtocolLib") != null;
    }

    /**
     * Checks if ProtocolLib is enabled and if its version matches
     * the expected version.
     * Triton requires ProtocolLib 5.4.0 or later.
     *
     * @return Whether the plugin should continue loading
     * @since 3.8.2
     */
    public static boolean isProtocolLibAvailable() {
        return isProtocolLibAvailable(true);
    }

    public static boolean isProtocolLibAvailable(boolean required) {
        val protocolLib = Bukkit.getPluginManager().getPlugin("ProtocolLib");
        val logger = Triton.get().getLogger();
        val config = Triton.get().getConfig();
        if (!Bukkit.getPluginManager().isPluginEnabled(protocolLib)) {
            if (required) {
                logger.logError("ProtocolLib IS REQUIRED! Without ProtocolLib, Triton will not translate any messages.");
                logger.logError("For the plugin to work correctly, please download the latest version of ProtocolLib.");
            } else {
                logger.logWarning("ProtocolLib is installed but disabled. ProtocolLib-only translation modules will be unavailable.");
            }
            return false;
        }

        if (config.isIKnowWhatIAmDoing()) {
            return true;
        }

        try {
            // Field known to exist in build 717 (commit e726f6e)
            boolean ignore = MinecraftVersion.v1_21_5.atOrAbove();
        } catch (NoSuchFieldError ignore) {
            // Triton requires ProtocolLib 5.4.0 or later
            if (required) {
                logger.logError("ProtocolLib 5.4.0 or later is required! Older versions of ProtocolLib will only partially work or not work at all, and are therefore not recommended.");
                logger.logError("It is likely that you need the latest dev version, which you can download at https://triton.rexcantor64.com/protocollib");
                logger.logError("If you want to enable the plugin anyway, add `i-know-what-i-am-doing: true` to Triton's config.yml.");
            } else {
                logger.logWarning("ProtocolLib 5.4.0 or later is required for fallback modules. Signs and advancements will be unavailable.");
                logger.logWarning("Download the latest version from https://triton.rexcantor64.com/protocollib or enable `i-know-what-i-am-doing` at your own risk.");
            }
            return false;
        }

        return true;
    }

    public static @NotNull ProtocolLibRefresher registerProtocolLibListeners(boolean packetEventsPrimary) {
        val triton = SpigotTriton.asSpigot();
        ProtocolLibListener protocolLibListener;
        boolean useAsyncManager = triton.getConfig().isAsyncProtocolLib() && !packetEventsPrimary;
        if (useAsyncManager) {
            protocolLibListener = new ProtocolLibListener(triton, packetEventsPrimary, HandlerFunction.HandlerType.ASYNC);
        } else {
            protocolLibListener = new ProtocolLibListener(triton, packetEventsPrimary, HandlerFunction.HandlerType.ASYNC, HandlerFunction.HandlerType.SYNC);
        }

        // Use delayed task to try to be the last registered listener and therefore have the final say in packets
        triton.getScheduler().runSyncLater(() -> {
            if (useAsyncManager) {
                val asyncManager = ProtocolLibrary.getProtocolManager().getAsynchronousManager();
                asyncManager.registerAsyncHandler(protocolLibListener).start();
                asyncManager.registerAsyncHandler(new MotdPacketHandler()).start();
                ProtocolLibrary.getProtocolManager().addPacketListener(new ProtocolLibListener(triton, packetEventsPrimary, HandlerFunction.HandlerType.SYNC));
            } else {
                ProtocolLibrary.getProtocolManager().addPacketListener(protocolLibListener);
                ProtocolLibrary.getProtocolManager().addPacketListener(new MotdPacketHandler());
            }
            if (packetEventsPrimary && triton.getConfig().isAsyncProtocolLib()) {
                triton.getLogger().logInfo("PacketEvents is primary; ProtocolLib async queues are disabled for its narrow fallback.");
            }
            triton.getLogger().logInfo(packetEventsPrimary
                    ? "Registered ProtocolLib fallback listeners for PacketEvents"
                    : "Registered ProtocolLib listeners");
        }, 1L);

        return protocolLibListener;
    }
}
