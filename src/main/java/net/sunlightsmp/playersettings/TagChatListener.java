package net.sunlightsmp.playersettings;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public final class TagChatListener implements Listener {
    private final SunlightPlayerSettings plugin;
    public TagChatListener(SunlightPlayerSettings plugin){this.plugin=plugin;}

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        String prefix = plugin.getTagManager().displayTag(event.getPlayer().getUniqueId());
        if (prefix.isEmpty()) return;
        event.renderer((source, displayName, message, viewer) ->
            Component.text(prefix).append(displayName).append(Component.text(": ")).append(message));
    }
}
