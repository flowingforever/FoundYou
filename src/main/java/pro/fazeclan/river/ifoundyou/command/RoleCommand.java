package pro.fazeclan.river.ifoundyou.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.tr7zw.nbtapi.NBT;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.entity.Player;
import pro.fazeclan.river.ifoundyou.IFoundYou;
import pro.fazeclan.river.ifoundyou.command.arguments.RoleArgument;
import pro.fazeclan.river.ifoundyou.dialog.RoleCreationDialog;
import pro.fazeclan.river.ifoundyou.role.Role;

public class RoleCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("role")
                .requires(ctx -> ctx.getSender().hasPermission("found_you.admin.role"))
                .then(
                        Commands.literal("create")
                                .executes(ctx -> {
                                    if (!(ctx.getSource().getSender() instanceof Player player)) {
                                        return Command.SINGLE_SUCCESS;
                                    }
                                    RoleCreationDialog.dialog(player);

                                    return Command.SINGLE_SUCCESS;
                                })
                )
                .then(
                        Commands.literal("reload")
                                .executes(ctx -> {
                                    var manager = IFoundYou.getInstance().getRoleManager();
                                    manager.reloadRegistry();

                                    return Command.SINGLE_SUCCESS;
                                })
                )
                .then(
                        Commands.literal("getitems")
                                .then(Commands.argument("role", new RoleArgument())
                                        .executes(ctx -> {
                                            if (!(ctx.getSource().getSender() instanceof Player player)) {
                                                return Command.SINGLE_SUCCESS;
                                            }

                                            var role = ctx.getArgument("role", Role.class);
                                            NBT.modify(player, nbt -> {
                                                try {
                                                    nbt.mergeCompound(NBT.parseNBT(role.getItems()));
                                                } catch (Exception ignored) {}
                                            });

                                            return Command.SINGLE_SUCCESS;
                                        })
                                )
                );
    }

}
