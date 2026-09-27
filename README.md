# Sunlight SMP Player Settings

Paper 1.21.11 plugin with a per-player /settings GUI.

Included toggles:
- Mob Spawns
- Fast Crystal
- TP Requests
- Notifications
- Chat Messages
- Combat Alerts
- Server Announcements

Player choices are stored by UUID in the plugin's players.yml and survive restarts.

Build with Java 21 and Maven. The output JAR is target/SunlightPlayerSettings.jar.

Important: this plugin stores and exposes the preferences. Existing TP request, Fast Crystal, mob-spawn, notification, or other custom systems must check the matching preference before acting. The settings plugin cannot automatically control another plugin's behavior without an integration.