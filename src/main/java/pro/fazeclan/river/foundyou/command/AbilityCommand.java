package pro.fazeclan.river.foundyou.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import pro.fazeclan.river.foundyou.FoundYou;
import pro.fazeclan.river.foundyou.util.TextUtil;

public class AbilityCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("ability")
                .requires(ctx -> ctx.getSender().hasPermission("found_you.admin.ability"))
                .then(
                        Commands.literal("reload")
                                .executes(ctx -> {
                                    FoundYou.getInstance().getAbilityManager().reloadRegistry();
                                    ctx.getSource().getSender().sendMessage(TextUtil.formatComponent(
                                            "<green>Reloaded ability configurations!</green>"
                                    ));

                                    return Command.SINGLE_SUCCESS;
                                })
                );
    }

}
