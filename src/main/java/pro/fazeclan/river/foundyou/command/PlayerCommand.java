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
import org.bukkit.entity.Player;
import pro.fazeclan.river.foundyou.FoundYou;
import pro.fazeclan.river.foundyou.command.arguments.RoleArgument;
import pro.fazeclan.river.foundyou.role.Faction;
import pro.fazeclan.river.foundyou.role.Role;
import pro.fazeclan.river.foundyou.util.GameFunctions;
import pro.fazeclan.river.foundyou.util.RoleUtil;
import pro.fazeclan.river.foundyou.util.TextUtil;
import pro.fazeclan.river.jarona.util.NametagUtil;

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
                                                                    var tr = ctx.getArgument("game_player", PlayerSelectorArgumentResolver.class);
                                                                    var gamePlayer = tr.resolve(source).getFirst();

                                                                    var tr2 = ctx.getArgument("players", PlayerSelectorArgumentResolver.class);
                                                                    var players = tr2.resolve(source);

                                                                    var role = ctx.getArgument("role", Role.class);

                                                                    for (Player player : players) {
                                                                        GameFunctions.addPlayer(player, role, gamePlayer.getLocation());

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
                                                    var manager = FoundYou.getInstance().getRoleManager();
                                                    var tr = ctx.getArgument("game_player", PlayerSelectorArgumentResolver.class);
                                                    var gamePlayer = tr.resolve(source).getFirst();

                                                    var tr2 = ctx.getArgument("players", PlayerSelectorArgumentResolver.class);
                                                    var players = tr2.resolve(source);

                                                    for (Player player : players) {
                                                        GameFunctions.addPlayer(player, manager.getRandomUnlimitedRole(Faction.RUNNERS), gamePlayer.getLocation());

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
                                            var tr2 = ctx.getArgument("players", PlayerSelectorArgumentResolver.class);
                                            var players = tr2.resolve(source);

                                            for (Player player : players) {
                                                GameFunctions.removePlayer(player);

                                                player.kill();
                                                player.spigot().respawn();
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

}
