package pro.fazeclan.river.ifoundyou.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import pro.fazeclan.river.jarona.Jarona;

public class ConditionCommand {

    public static LiteralArgumentBuilder<CommandSourceStack> command() {
        return Commands.literal("conditions")
                .requires(ctx -> ctx.getSender().hasPermission("found_you.admin.conditions"))
                .then(
                        Commands.literal("reset")
                                .executes(ctx -> {
                                    var manager = Jarona.getInstance().getConditionManager();
                                    for (Player player : Bukkit.getOnlinePlayers()) {
                                        var conditions = manager.getPlayerConditions(player);
                                        var keys = conditions.getConditionMap().keySet();
                                        for (String c : keys) {
                                            manager.getPlayerConditions(player).remove(c);
                                        }
                                    }
                                    Jarona.getInstance().getConditionManager().getPlayerConditionMap().clear();

                                    return Command.SINGLE_SUCCESS;
                                })
                );
    }

}
