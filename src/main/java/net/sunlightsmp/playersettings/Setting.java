package net.sunlightsmp.playersettings;
import org.bukkit.Material;
public enum Setting {
  MOB_SPAWNS("mob-spawns","Mob Spawns",Material.ZOMBIE_HEAD),
  FAST_CRYSTAL("fast-crystal","Fast Crystal",Material.END_CRYSTAL),
  TP_REQUESTS("tp-requests","TP Requests",Material.ENDER_PEARL),
  NOTIFICATIONS("notifications","Notifications",Material.BELL),
  CHAT_MESSAGES("chat-messages","Chat Messages",Material.PAPER),
  COMBAT_ALERTS("combat-alerts","Combat Alerts",Material.IRON_SWORD),
  SERVER_ANNOUNCEMENTS("server-announcements","Server Announcements",Material.BEACON);
  private final String key,name; private final Material icon;
  Setting(String key,String name,Material icon){this.key=key;this.name=name;this.icon=icon;}
  public String key(){return key;} public String displayName(){return name;} public Material icon(){return icon;}
}