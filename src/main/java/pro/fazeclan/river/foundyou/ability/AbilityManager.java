package pro.fazeclan.river.foundyou.ability;

import lombok.Getter;
import org.bukkit.event.HandlerList;
import pro.fazeclan.river.foundyou.FoundYou;
import pro.fazeclan.river.foundyou.ability.definitions.*;

import java.util.HashMap;
import java.util.Map;

public class AbilityManager {

    @Getter
    private final Map<String, Ability> registry = new HashMap<>();

    public void registerAbilities() {
        register(new AdrenalineAbility());
        register(new AmbushAbility());
        register(new BrewerAbility());
        register(new ConcealAbility());
        register(new DashAbility());
        register(new DisplacementAbility());
        register(new EcholocateAbility());
        register(new EnragedShriekAbility());
        register(new LaserbeamAbility());
        register(new NoisemakerAbility());
        register(new ParryAbility());
        register(new QuickfireAbility());
        register(new QuiverAbility());
        register(new RewindAbility());
        register(new RollAbility());
        register(new ScrapshotAbility());
        register(new ExplodeAbility());
        register(new ShrinkAbility());
        register(new StalkAbility());
        register(new TerrifyAbility());
        register(new TrapAbility());
        register(new MuzzleAbility());
    }

    public void reloadRegistry() {
        for (Map.Entry<String, Ability> entry : registry.entrySet()) {
            HandlerList.unregisterAll(entry.getValue());
        }
        registry.clear();
        registerAbilities();
    }

    public <T extends Ability> T register(T ability) {
        registry.put(ability.getId(), ability);
        FoundYou.getInstance()
                .getServer()
                .getPluginManager()
                .registerEvents(ability, FoundYou.getInstance());
        return ability;
    }

    public void unregister(Ability ability) {
        registry.remove(ability.getId());
        HandlerList.unregisterAll(ability);
    }

}
