package com.rexcantor64.triton.spigot.guiapi;

import com.rexcantor64.triton.spigot.SpigotTriton;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Secure, fail-closed GUI Manager implementing prompt.md section 5 guidelines.
 *
 * @since 4.1.0
 */
public class GuiManager implements Listener {

    private final Map<Inventory, OpenGuiInfo> open = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastClickTime = new ConcurrentHashMap<>();
    private static final long CLICK_DEBOUNCE_MS = 100L;

    public void add(Inventory inv, OpenGuiInfo gui) {
        open.put(inv, gui);
    }

    public void closeAllMenus() {
        for (Map.Entry<Inventory, OpenGuiInfo> entry : open.entrySet()) {
            try {
                entry.getKey().getViewers().forEach(viewer -> viewer.closeInventory());
            } catch (Throwable ignored) {
            }
        }
        open.clear();
        lastClickTime.clear();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;

        Inventory inv = e.getInventory();
        if (!(inv.getHolder() instanceof Gui) && !open.containsKey(inv)) {
            return;
        }

        OpenGuiInfo guiInfo = open.get(inv);
        if (guiInfo == null) {
            e.setCancelled(true);
            player.closeInventory();
            return;
        }

        Gui gui = guiInfo.getGui();
        // Fail-closed default for custom GUI interactions
        if (gui.isBlocked()) {
            e.setCancelled(true);
        }

        // Rate limiting (click debounce) to prevent fast double-click / shift-click dupe attempts
        long now = System.currentTimeMillis();
        long last = lastClickTime.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < CLICK_DEBOUNCE_MS) {
            e.setCancelled(true);
            return;
        }
        lastClickTime.put(player.getUniqueId(), now);

        if (e.getClickedInventory() == null) return;

        // Verify server-side slot content; never trust client representation alone
        int rawSlot = e.getRawSlot();
        if (rawSlot >= 0 && rawSlot < inv.getSize()) {
            ItemStack serverItem = inv.getItem(rawSlot);
            if (serverItem == null || serverItem.getType().isAir()) {
                if (gui.isBlocked()) {
                    e.setCancelled(true);
                }
            }
        }

        GuiButton btn;
        if (gui instanceof ScrollableGui sGui) {
            if (sGui.getMaxPages() > 1 && rawSlot >= 45) {
                e.setCancelled(true);
                if (rawSlot == 45 && guiInfo.getCurrentPage() > 1) {
                    sGui.open(player, guiInfo.getCurrentPage() - 1);
                } else if (rawSlot == 53 && guiInfo.getCurrentPage() < sGui.getMaxPages()) {
                    sGui.open(player, guiInfo.getCurrentPage() + 1);
                }
                return;
            }
            btn = sGui.getButton(rawSlot, guiInfo.getCurrentPage());
        } else {
            btn = gui.getButton(rawSlot);
        }

        if (btn == null) return;

        try {
            btn.getEvent().onClick(e);
        } catch (Throwable t) {
            e.setCancelled(true);
            SpigotTriton.asSpigot().getLogger().logError(t, "Error handling GUI button click for %1", player.getName());
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        open.remove(e.getInventory());
        lastClickTime.remove(e.getPlayer().getUniqueId());
    }
}
