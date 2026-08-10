package pro.fazeclan.river.foundyou.screen;

import de.tr7zw.nbtapi.NBT;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BundleContents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import pro.fazeclan.river.foundyou.FoundYou;
import pro.fazeclan.river.foundyou.role.Faction;
import pro.fazeclan.river.foundyou.role.Role;
import pro.fazeclan.river.jarona.invui.gui.*;
import pro.fazeclan.river.jarona.invui.item.BoundItem;
import pro.fazeclan.river.jarona.invui.item.Item;
import pro.fazeclan.river.jarona.invui.item.ItemBuilder;
import pro.fazeclan.river.jarona.invui.window.Window;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RolePreviewMenu {

    private static Map<String, Gui> cache = new ConcurrentHashMap(16);

    public static void createAndShowPreview(Player player, Role role) {

        ItemStack[] contents;
        Map<EquipmentSlot, ItemStack> equipment;

        if (cache.containsKey(role.getId())) {
            var preview = cache.get(role.getId());
            Window.builder()
                    .setTitle("Role Preview > " + role.getName())
                    .setUpperGui(preview)
                    .open(player);
            return;
        }

        var root = NBT.parseNBT(role.getItems());

        contents = new ItemStack[36];
        var invList = root.getCompoundList("Inventory");
        for (var itemTag : invList) {
            int slot = itemTag.getByte("Slot") & 0xFF;
            if (slot < contents.length) {
                contents[slot] = NBT.itemStackFromNBT(itemTag);
            }
        }

        equipment = new HashMap<>(5);
        var equipmentTag = root.getCompound("equipment");
        if (equipmentTag != null) {
            for (String key : equipmentTag.getKeys()) {
                if (Objects.equals(key, "offhand")) {
                    equipment.put(EquipmentSlot.OFF_HAND, NBT.itemStackFromNBT(equipmentTag.getCompound(key)));
                } else {
                    equipment.put(EquipmentSlot.valueOf(key.toUpperCase()), NBT.itemStackFromNBT(equipmentTag.getCompound(key)));
                }
            }
        }

        var gui = Gui.empty(9, 6);
        // equipment
        setSlot(gui, 0, equipment.get(EquipmentSlot.HEAD));
        setSlot(gui, 1, equipment.get(EquipmentSlot.CHEST));
        setSlot(gui, 2, equipment.get(EquipmentSlot.LEGS));
        setSlot(gui, 3, equipment.get(EquipmentSlot.FEET));
        setSlot(gui, 4, equipment.get(EquipmentSlot.OFF_HAND));

        // ability cycling
        var mm = MiniMessage.miniMessage();
        var manager = FoundYou.getInstance().getAbilityManager();
        var abilityList = role.getAbilities().stream()
                .map(name -> manager.getRegistry().get(name))
                .filter(Objects::nonNull)
                .toList();
        var displayList = abilityList.stream()
                .map(ability -> {
                    var item = ability.getDisplay();
                    item.editMeta(meta -> {
                        meta.customName(mm.deserialize("<!i>" + ability.getDefaultAbilityProperty("name", "")));
                        var lore = ability.getDefaultAbilityProperty("description", "");
                        meta.lore(Arrays.stream(lore.split("<newline>"))
                                .map(text -> mm.deserialize("<!i><white>" + text + "</white>"))
                                .toList());
                        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
                    });
                    return item;
                })
                .toList();

        var index = new AtomicInteger(0);

        var boundSelection = BoundItem.builder()
                .setItemProvider(p ->
                        new ItemBuilder(Material.WHITE_BUNDLE)
                                .setName("Ability Selection")
                                .set(
                                        DataComponentTypes.BUNDLE_CONTENTS,
                                        BundleContents.bundleContents().addAll(displayList).build()
                                )
                                .setLore(List.of(
                                        mm.deserialize("<dark_gray>(Hover over this and scroll!)</dark_gray>")
                                ))
                )
                .addBundleSelectHandler((item, gui1, player1, integer) -> {
                    if (integer == -1) return;
                    setSlot(gui1, 6, displayList.get(integer));
                    gui1.notifyWindows();
                });
        setSlot(gui, 6, displayList.get(index.get()));
        gui.setItem(7, boundSelection.build());

        // description showcase
        var description = getDescriptionItem(role);
        setSlot(gui, 8, description);

        // separator
        var separator = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        separator.editMeta(meta -> meta.setHideTooltip(true));
        for (int i = 0; i < 9; i++) {
            setSlot(gui, 9 + i, separator);
        }

        // inventory
        for (int i = 0; i < contents.length; i++) {
            setSlot(gui, 18 + i, contents[i]);
        }

        Window.builder()
                .setTitle("Role Preview > " + role.getName())
                .setUpperGui(gui)
                .open(player);

    }

    private static @NotNull ItemStack getDescriptionItem(Role role) {
        var mm = MiniMessage.miniMessage();

        var description = new ItemStack(Material.NAME_TAG);
        description.editMeta(meta -> {
            if (role.getFaction().equals(Faction.HUNTERS)) {
                meta.itemName(mm.deserialize("<red><b>HUNTER ROLE</b></red>"));
            } else {
                meta.itemName(mm.deserialize("<green><b>RUNNER ROLE</b></green>"));
            }
            var list = new ArrayList<Component>();

            list.add(Component.empty());
            list.add(mm.deserialize("<!i><white>" + role.getDescription() + "</white>"));
            list.add(Component.empty());

            if (role.isLimited()) {
                list.add(mm.deserialize("<!i><rainbow><b>SPECIAL!</b></rainbow> <red>Limited to " + role.getMaxPlayers() + " players max!</red>"));
            } else {
                list.add(mm.deserialize("<!i><green>No limit on players!</green>"));
            }
            meta.lore(list);
        });
        return description;
    }

    private static void setSlot(Gui gui, int slot, ItemStack stack) {
        gui.setItem(slot, Item.simple(Objects.requireNonNullElseGet(stack, () -> new ItemStack(Material.AIR))));
    }

}
