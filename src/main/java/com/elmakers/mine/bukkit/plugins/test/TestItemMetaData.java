package com.elmakers.mine.bukkit.plugins.test;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.PersistentMetadataValue;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.*;

public class TestItemMetaData extends JavaPlugin implements Listener
{
    private Map<String, LinkedList<MemoryConfiguration>> storedInventories = new HashMap<String, LinkedList<MemoryConfiguration>>();
    private static final ChatColor CHAT_PREFIX = ChatColor.AQUA;
    private static final ChatColor ERROR_PREFIX = ChatColor.RED;

    public void onEnable()
	{
		PluginManager pm = getServer().getPluginManager();
		pm.registerEvents(this, this);
	}

	public void onDisable()
    {
    }

    protected void sendMessage(CommandSender sender, String string)
    {
        sender.sendMessage(CHAT_PREFIX + string);
    }

    protected void sendError(CommandSender sender, String string)
    {
        sender.sendMessage(ERROR_PREFIX + string);
    }

    public void onInventoryPush(Player player)
    {
        Inventory inventory = player.getInventory();
        MemoryConfiguration stored = new MemoryConfiguration();
        stored.set("items", inventory.getContents());

        LinkedList<MemoryConfiguration> inventories = getStoredInventories(player, true);
        inventories.add(stored);

        inventory.clear();
        sendMessage(player, "Items stored. You have " + ChatColor.BLUE + inventories.size() + CHAT_PREFIX + " stored inventories on the stack.");
    }

    public void onInventoryPop(Player player)
    {
        LinkedList<MemoryConfiguration> inventories = getStoredInventories(player, false);
        if (inventories == null || inventories.size() == 0) {
            sendMessage(player, "No stored inventories for you");
            return;
        }

        World world = player.getWorld();
        Inventory inventory = player.getInventory();
        ItemStack[] contents = player.getInventory().getContents();
        int droppedItems = 0;
        for (ItemStack itemStack : contents)
        {
            if (itemStack == null || itemStack.getType() == Material.AIR) continue;
            droppedItems++;
            world.dropItemNaturally(player.getLocation(), itemStack);
        }
        inventory.clear();
        MemoryConfiguration stored = inventories.removeFirst();
        ItemStack[] items = (ItemStack[])stored.get("items");
        inventory.setContents(items);

        sendMessage(player, "Dropped " + ChatColor.BLUE + droppedItems + CHAT_PREFIX + " items and restored inventory.");
        sendMessage(player, "You have " + ChatColor.BLUE + inventories.size() + CHAT_PREFIX + " stored inventories on the stack.");
    }

    protected LinkedList<MemoryConfiguration> getStoredInventories(Player player, boolean create)
    {
        String playerId = player.getUniqueId().toString();
        LinkedList<MemoryConfiguration> inventories = storedInventories.get(playerId);
        if (inventories == null && create) {
            inventories = new LinkedList<MemoryConfiguration>();
            storedInventories.put(playerId, inventories);
        }
        return inventories;
    }

    private void onItemClone(Player player, ItemStack heldItem)
    {
        ItemStack newItem = heldItem.clone();
        World world = player.getWorld();
        world.dropItemNaturally(player.getLocation(), newItem);
        sendMessage(player, "Cloned your " + heldItem.getType().name());
    }

    private void onItemGlow(Player player, ItemStack heldItem)
    {
        sendMessage(player, "This was moved to a separate change and test");
        /*
        ItemMeta meta = heldItem.getItemMeta();
        if (meta.hasGlowEffect()) {
            sendError(player, "This item is already glowing");
            return;
        }
        meta.setGlowEffect(true);
        heldItem.setItemMeta(meta);
        sendMessage(player, "Ooooh, shiny!");
        */
    }

    private void onItemUnGlow(Player player, ItemStack heldItem)
    {
        sendMessage(player, "This was moved to a separate change and test");
        /*
        ItemMeta meta = heldItem.getItemMeta();
        if (!meta.hasGlowEffect()) {
            sendError(player, "This item isn't glowing");
            return;
        }
        if (meta.hasEnchants()) {
            sendError(player, "Can't un-glow an enchanted item. Use " + ChatColor.WHITE + "/itemunenchant.");
            return;
        }
        meta.setGlowEffect(false);
        heldItem.setItemMeta(meta);
        sendMessage(player, "Awwwww");
        */
    }

    private void onItemUnEnchant(Player player, ItemStack heldItem)
    {
        ItemMeta meta = heldItem.getItemMeta();
        if (!meta.hasEnchants()) {
            sendError(player, "This item has no enchantments.");
            return;
        }
        ArrayList<Enchantment> enchants = new ArrayList<Enchantment>(meta.getEnchants().keySet());
        for (Enchantment enchantment : enchants) {
            meta.removeEnchant(enchantment);
        }
        heldItem.setItemMeta(meta);
        sendMessage(player, "Your item feels less special");
    }

    private void onItemCheck(Player player, ItemStack heldItem)
    {
        ItemMeta meta = heldItem.getItemMeta();
        sendMessage(player, "Item Type: " + ChatColor.BLUE + heldItem.getType().name());
        sendMessage(player, "Glowing: " + ChatColor.BLUE + "I dunno!");
        if (meta.hasEnchants()) {
            sendMessage(player, "Has " + ChatColor.BLUE + meta.getEnchants().size() + CHAT_PREFIX + " enchantments");
        } else {
            sendMessage(player, "Has no enchantments");
        }
        if (meta.hasMetadata("test_fly")) {

            Collection<MetadataValue> values = meta.getMetadata("test_fly");
            MetadataValue flyMetadata = null;
            for (MetadataValue value : values) {
                if (value.getOwningPlugin() == this) {
                    flyMetadata = value;
                    break;
                }
            }

            if (flyMetadata == null) {
                sendError(player, "Found fly data, but it wasn't owned by this plugin");
            } else {
                sendMessage(player, " Hey, look, our custom fly data!");
                Map<String, Object> flyData = (Map<String, Object>)flyMetadata.value();
                if (flyData.containsKey("created")) {
                    sendMessage(player, "You made this item awesome at " + ChatColor.BLUE + flyData.get("created"));
                } else {
                    sendError(player, "Missing 'created' key");
                }
                if (flyData.containsKey("uses")) {
                    sendMessage(player, "You have used this item " + ChatColor.BLUE + flyData.get("uses") + CHAT_PREFIX + " times");
                } else {
                    sendError(player, "Missing 'uses' key");
                }

                if (flyData.containsKey("speed")) {
                    sendMessage(player, "Its speed is set to " + ChatColor.BLUE + flyData.get("speed") + CHAT_PREFIX + " ... with no way to change it");
                } else {
                    sendError(player, "Missing 'speed' key");
                }
            }
        } else {
            sendMessage(player, "Has no data");
        }
        try {
            sendMessage(player, "Raw: " + ChatColor.GRAY + meta);
        } catch (Throwable ex) {
            sendError(player, "An error occurred serializing item data. See server logs.");
            ex.printStackTrace();;
        }
    }

    private void onItemFly(Player player, ItemStack heldItem)
    {
        ItemMeta meta = heldItem.getItemMeta();
        if (meta.hasMetadata("test_fly")) {
            sendError(player, "That item is already a super awesome flying item");
            return;
        }
        Map<String, Object> flyData = new HashMap<String, Object>();
        Date now = new Date();
        flyData.put("created", now.toString());
        flyData.put("uses", 0);
        flyData.put("speed", 2.0);
        meta.setMetadata("test_fly", new PersistentMetadataValue(this, flyData));
        heldItem.setItemMeta(meta);

        if (!heldItem.getItemMeta().hasMetadata("test_fly")) {
            sendError(player, "Setting item data failed");
        } else {
            sendMessage(player, "Swing your item to fly");
            sendMessage(player, "Note that if you don't have flying enabled, you may get kicked");
            sendMessage(player, "Also note, you'll probably die when you land");
            sendMessage(player, ChatColor.ITALIC + "But, oh, what fun!");
        }
    }

    private void onItemUnFly(Player player, ItemStack heldItem)
    {
        if (!heldItem.hasMetadata()) {
            sendError(player, "That item is not special at all!");
            return;
        }
        ItemMeta meta = heldItem.getItemMeta();

        if (!meta.hasMetadata("test_fly")) {
            sendError(player, "That item is not a super awesome flying item");
            return;
        }
        meta.removeMetadata("test_fly", this);
        heldItem.setItemMeta(meta);
        if (heldItem.hasMetadata()) {
            meta = heldItem.getItemMeta();
            if (meta.hasMetadata("test_fly")) {
                sendError(player, "Failed to un-fly the item!");
            } else {
                sendMessage(player, "Item can no longer fly, but still seems to be special in some way.");
            }
        } else {
            sendMessage(player, "Your item won't make you fly anymore");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] arg)
    {
        // All of these commands work with the Player's held item
        if (!(sender instanceof Player)) {
            sendMessage(sender, ERROR_PREFIX + "This command only works in-game");
            return true;
        }

        Player player = (Player)sender;

        if (label.equalsIgnoreCase("invpush")) {
            onInventoryPush(player);
            return true;
        } else if (label.equalsIgnoreCase("invpop")) {
            onInventoryPop(player);
            return true;
        }

        ItemStack heldItem = player.getItemInHand();
        if (heldItem == null || heldItem.getType() == Material.AIR)
        {
            sendMessage(sender, ERROR_PREFIX + "You must hold an item first");
            return true;
        }

        if (label.equalsIgnoreCase("itemglow")) {
            onItemGlow(player, heldItem);
            return true;
        } else  if (label.equalsIgnoreCase("itemunglow")) {
            onItemUnGlow(player, heldItem);
            return true;
        } else  if (label.equalsIgnoreCase("itemunenchant")) {
            onItemUnEnchant(player, heldItem);
            return true;
        } else  if (label.equalsIgnoreCase("itemcheck")) {
            onItemCheck(player, heldItem);
            return true;
        } else  if (label.equalsIgnoreCase("itemfly")) {
            onItemFly(player, heldItem);
            return true;
        } else  if (label.equalsIgnoreCase("itemunfly")) {
            onItemUnFly(player, heldItem);
            return true;
        } else  if (label.equalsIgnoreCase("itemclone")) {
            onItemClone(player, heldItem);
            return true;
        }

        return false;
    }

    @EventHandler
	public void onPlayerInteract(PlayerInteractEvent event)
    {
        if (event.getAction() != Action.LEFT_CLICK_AIR && event.getAction() != Action.LEFT_CLICK_BLOCK)
        {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = player.getItemInHand();

        // Note the pre-check does not unpack ItemMeta
        if (item == null || !item.hasMetadata())
        {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (!meta.hasMetadata("test_fly"))
        {
            return;
        }

        Collection<MetadataValue> values = meta.getMetadata("test_fly");
        MetadataValue flyMetadata = null;
        for (MetadataValue value : values) {
            if (value.getOwningPlugin() == this) {
                flyMetadata = value;
                break;
            }
        }

        if (flyMetadata == null) {
            sendError(player, "Found fly data, but it wasn't owned by this plugin");
            return;
        }

        Map<String, Object> flyData = (Map<String, Object>)flyMetadata.value();
        Double speed = (Double)flyData.get("speed");
        if (speed == null) {
            sendError(player, "Found fly data with an invalid speed. No flying for you!");
            return;
        }

        Vector targetVelocity = player.getLocation().getDirection().normalize();
        targetVelocity.setY(targetVelocity.getY() + 0.75);
        targetVelocity = targetVelocity.normalize().multiply(speed);
        player.setVelocity(targetVelocity);

        Integer flyCount = (Integer)flyData.get("uses");
        flyCount = flyCount == null ? 1 : flyCount + 1;
        flyData.put("uses", flyCount);

        meta.setMetadata("test_fly", new PersistentMetadataValue(this, flyData));
        item.setItemMeta(meta);
	}
}
