package pro.fazeclan.river.foundyou.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.alexdev.unlimitednametags.api.UNTPaperAPI;
import org.alexdev.unlimitednametags.config.Settings;
import org.bukkit.entity.Player;
import pro.fazeclan.river.foundyou.FoundYou;
import pro.fazeclan.river.foundyou.command.arguments.RoleArgument;
import pro.fazeclan.river.foundyou.role.Faction;
import pro.fazeclan.river.foundyou.role.Role;
import pro.fazeclan.river.foundyou.role.RoleManager;
import pro.fazeclan.river.foundyou.util.RoleUtil;
import pro.fazeclan.river.foundyou.util.TextUtil;
import pro.fazeclan.river.jarona.util.NametagUtil;

import java.util.ArrayList;

public class PlayerCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("player")
                .requires(ctx -> ctx.getSender().hasPermission("found_you.admin.player"))
                .then(
                        Commands.literal("add")
                                .then(Commands.argument("game_player", ArgumentTypes.player())
                                        .then(Commands.argument("players", ArgumentTypes.players())
                                                .then(
                                                        Commands.argument("role", new RoleArgument())
                                                                .executes(ctx -> {
                                                                    var source = ctx.getSource();
                                                                    var nametagManager = UNTPaperAPI.getInstance();
                                                                    var manager = FoundYou.getInstance().getRoleManager();
                                                                    var tr = ctx.getArgument("game_player", PlayerSelectorArgumentResolver.class);
                                                                    var gamePlayer = tr.resolve(source).getFirst();

                                                                    var tr2 = ctx.getArgument("players", PlayerSelectorArgumentResolver.class);
                                                                    var players = tr2.resolve(source);

                                                                    var role = ctx.getArgument("role", Role.class);

                                                                    for (Player player : players) {
                                                                        assignPlayer(player, gamePlayer, role, manager, nametagManager);

                                                                        player.sendMessage(TextUtil.formatComponent(
                                                                                "<green>You've been added into an ongoing game!</green>"
                                                                        ));
                                                                        source.getSender().sendMessage(TextUtil.formatComponent(
                                                                                "<green>Added " + player.getName() + " to a game!</green>"
                                                                        ));
                                                                    }

                                                                    return Command.SINGLE_SUCCESS;
                                                                })
                                                )
                                                .executes(ctx -> {
                                                    var source = ctx.getSource();
                                                    var nametagManager = UNTPaperAPI.getInstance();
                                                    var manager = FoundYou.getInstance().getRoleManager();
                                                    var tr = ctx.getArgument("game_player", PlayerSelectorArgumentResolver.class);
                                                    var gamePlayer = tr.resolve(source).getFirst();

                                                    var tr2 = ctx.getArgument("players", PlayerSelectorArgumentResolver.class);
                                                    var players = tr2.resolve(source);

                                                    for (Player player : players) {
                                                        assignPlayer(player, gamePlayer, manager.getRandomUnlimitedRole(Faction.RUNNERS), manager, nametagManager);

                                                        player.sendMessage(TextUtil.formatComponent(
                                                                "<green>You've been added into an ongoing game!</green>"
                                                        ));
                                                        source.getSender().sendMessage(TextUtil.formatComponent(
                                                                "<green>Added " + player.getName() + " to a game!</green>"
                                                        ));
                                                    }

                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )

                )
                .then(
                        Commands.literal("remove")
                                .then(Commands.argument("players", ArgumentTypes.players())
                                        .executes(ctx -> {
                                            var source = ctx.getSource();
                                            var nametagManager = UNTPaperAPI.getInstance();
                                            var tr2 = ctx.getArgument("players", PlayerSelectorArgumentResolver.class);
                                            var players = tr2.resolve(source);

                                            for (Player player : players) {
                                                RoleUtil.removeRoles(player);

                                                player.kill();
                                                player.spigot().respawn();
                                                nametagManager.clearForcedNametag(player);
                                                NametagUtil.showPlayerNametagToAll(player);
                                                player.sendMessage(TextUtil.formatComponent(
                                                        "<green>You've been removed from an ongoing game!</green>"
                                                ));
                                                source.getSender().sendMessage(TextUtil.formatComponent(
                                                        "<green>Removed " + player.getName() + " from a game!</green>"
                                                ));
                                            }

                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                );
    }

    private static void assignPlayer(Player player, Player gamePlayer, Role role, RoleManager manager, UNTPaperAPI api) {
        RoleUtil.removeRoles(player);
        RoleUtil.assignRole(player, role);

        player.teleport(gamePlayer);
        api.setForcedNametag(player, MiniMessage.miniMessage().deserialize("<green>" + player.getName() + "</green>"));
        api.setNametagSeeThrough(player, false);
        NametagUtil.hidePlayerNametagWithGlowToAll(player, NamedTextColor.GREEN);
    }

}
