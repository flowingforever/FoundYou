package pro.fazeclan.river.foundyou.compat;

import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FYVoicechatPlugin implements VoicechatPlugin {
    private VoicechatServerApi serverApi;
    private final Map<UUID, Group> spectatorGroups; // game to spectator group map
    private final Map<UUID, Group> hunterGroups; // game to hunter group map

    public FYVoicechatPlugin() {
        this.spectatorGroups = new ConcurrentHashMap<>();
        this.hunterGroups = new ConcurrentHashMap<>();
    }

    @Override
    public String getPluginId() {
        return "found_you_voicechat";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, event -> {
            serverApi = event.getVoicechat();
        });
    }

    public void addSpectator(Player player) {
        if (serverApi == null) return;
        var connection = serverApi.getConnectionOf(player.getUniqueId());
        if (connection == null) return;
        var world = player.getWorld();
        try {
            var gameUUID = UUID.fromString(world.getKey().value());
            Group group;
            if (spectatorGroups.containsKey(gameUUID)) {
                group = spectatorGroups.get(gameUUID);
            } else {
                group = serverApi.groupBuilder()
                        .setHidden(true)
                        .setId(UUID.randomUUID())
                        .setName("Game Spectators")
                        .setPersistent(false)
                        .setType(Group.Type.OPEN)
                        .build();
                spectatorGroups.put(gameUUID, group);
            }
            connection.setGroup(group);
        } catch (RuntimeException ignored) {}
    }

    public void addHunter(Player player) {
        if (serverApi == null) return;
        var connection = serverApi.getConnectionOf(player.getUniqueId());
        if (connection == null) return;
        var world = player.getWorld();
        try {
            var gameUUID = UUID.fromString(world.getKey().value());
            Group group;
            if (hunterGroups.containsKey(gameUUID)) {
                group = hunterGroups.get(gameUUID);
            } else {
                group = serverApi.groupBuilder()
                        .setHidden(true)
                        .setId(UUID.randomUUID())
                        .setName("Game Hunters")
                        .setPersistent(false)
                        .setType(Group.Type.OPEN)
                        .build();
                hunterGroups.put(gameUUID, group);
            }
            connection.setGroup(group);
        } catch (RuntimeException ignored) {}
    }

    public void removePlayer(Player player) {
        if (serverApi == null) return;
        var connection = serverApi.getConnectionOf(player.getUniqueId());
        if (connection == null) return;
        connection.setGroup(null);
    }

}
