package pro.fazeclan.river.foundyou.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import pro.fazeclan.river.foundyou.FoundYou;

public class ConfigCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("config")
                .requires(ctx -> ctx.getSender().hasPermission("found_you.admin.config"))
                .then(
                        Commands.literal("reload")
                                .executes(ctx -> {
                                    FoundYou.getInstance().reloadConfig();

                                    return Command.SINGLE_SUCCESS;
                                })
                );
    }

}
