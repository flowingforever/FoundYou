package pro.fazeclan.river.foundyou;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.tr7zw.nbtapi.NBT;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import pro.fazeclan.river.foundyou.ability.AbilityManager;
import pro.fazeclan.river.foundyou.command.AbilityCommand;
import pro.fazeclan.river.foundyou.command.ConfigCommand;
import pro.fazeclan.river.foundyou.command.PlayerCommand;
import pro.fazeclan.river.foundyou.command.RoleCommand;
import pro.fazeclan.river.foundyou.compat.FYVoicechatPlugin;
import pro.fazeclan.river.foundyou.game.FoundYouGame;
import pro.fazeclan.river.foundyou.listener.AbilityListeners;
import pro.fazeclan.river.foundyou.listener.GameListeners;
import pro.fazeclan.river.foundyou.role.RoleManager;
import pro.fazeclan.river.jarona.Jarona;

import javax.annotation.Nullable;

public final class FoundYou extends JavaPlugin {

    @Getter
    private RoleManager roleManager;

    @Getter
    private AbilityManager abilityManager;

    @Nullable
    @Getter
    private FYVoicechatPlugin voicechatPlugin;

    @Override
    public void onLoad() {
        this.roleManager = new RoleManager();
        this.abilityManager = new AbilityManager();
    }

    @Override
    public void onEnable() {
        if (getServer().getPluginManager().isPluginEnabled("voicechat")) {
            var service = getServer().getServicesManager().load(BukkitVoicechatService.class);

            if (service != null) {
                voicechatPlugin = new FYVoicechatPlugin();
                service.registerPlugin(voicechatPlugin);
            }
        } else {
            getLogger().warning("Simple Voice Chat not loaded, nothing SVC related will work!");
        }

        NBT.preloadApi();

        var manager = Jarona.getInstance().getGameManager();
        manager.register(new FoundYouGame());

        saveDefaultConfig();

        this.roleManager.reloadRegistry();
        this.abilityManager.registerAbilities();

        getServer().getPluginManager().registerEvents(new GameListeners(), this);
        getServer().getPluginManager().registerEvents(new AbilityListeners(), this);

        var command = Commands.literal("foundyou")
                .then(RoleCommand.command())
                .then(ConfigCommand.command())
                .then(PlayerCommand.command())
                .then(AbilityCommand.command())
                .build();
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            commands.registrar().register(command);
        });
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public static FoundYou getInstance() {
        return JavaPlugin.getPlugin(FoundYou.class);
    }

    public static NamespacedKey getKey(String value) {
        return new NamespacedKey(getInstance(), value);
    }

}
