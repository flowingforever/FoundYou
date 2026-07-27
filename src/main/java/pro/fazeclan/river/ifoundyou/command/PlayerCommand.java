package pro.fazeclan.river.ifoundyou.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.format.NamedTextColor;
import org.alexdev.unlimitednametags.api.UNTPaperAPI;
import org.alexdev.unlimitednametags.config.Settings;
import org.bukkit.entity.Player;
import pro.fazeclan.river.ifoundyou.IFoundYou;
import pro.fazeclan.river.ifoundyou.role.Faction;
import pro.fazeclan.river.ifoundyou.util.RoleUtil;
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
                                                .executes(ctx -> {
                                                    var nametagManager = UNTPaperAPI.getInstance();
                                                    var manager = IFoundYou.getInstance().getRoleManager();
                                                    var tr = ctx.getArgument("game_player", PlayerSelectorArgumentResolver.class);
                                                    var gamePlayer = tr.resolve(ctx.getSource()).getFirst();

                                                    var tr2 = ctx.getArgument("players", PlayerSelectorArgumentResolver.class);
                                                    var players = tr2.resolve(ctx.getSource());

                                                    for (Player player : players) {
                                                        RoleUtil.removeRoles(player);
                                                        RoleUtil.assignRole(player, manager.getRandomUnlimitedRole(Faction.RUNNERS));

                                                        player.teleport(gamePlayer);
                                                        nametagManager.modifyNametagProperty(player, current -> {
                                                            var groups = new ArrayList<>(current.displayGroups());
                                                            groups.clear();
                                                            groups.add(Settings.DisplayGroup
                                                                    .builder()
                                                                    .line("<green>" + player.getName() + "</green>")
                                                                    .scale(1f)
                                                                    .build()
                                                            );
                                                            return current.withDisplayGroups(groups);
                                                        });
                                                        nametagManager.hideNametag(player);
                                                        NametagUtil.hidePlayerNametagWithGlowToAll(player, NamedTextColor.GREEN);
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
                                            var nametagManager = UNTPaperAPI.getInstance();
                                            var tr2 = ctx.getArgument("players", PlayerSelectorArgumentResolver.class);
                                            var players = tr2.resolve(ctx.getSource());

                                            for (Player player : players) {
                                                RoleUtil.removeRoles(player);

                                                player.kill();
                                                player.spigot().respawn();
                                                nametagManager.removeNametagOverride(player);
                                                NametagUtil.showPlayerNametagToAll(player);
                                            }

                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                );
    }

}
