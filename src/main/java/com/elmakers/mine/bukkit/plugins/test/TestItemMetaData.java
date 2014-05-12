package com.elmakers.mine.bukkit.plugins.test;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

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
        ItemMeta meta = heldItem.getItemMeta();
        if (meta.hasGlowEffect()) {
            sendMessage(player, ERROR_PREFIX + "This item is already glowing");
            return;
        }
        meta.setGlowEffect(true);
        heldItem.setItemMeta(meta);
        sendMessage(player, "Ooooh, shiny!");
    }

    private void onItemUnGlow(Player player, ItemStack heldItem)
    {
        ItemMeta meta = heldItem.getItemMeta();
        if (!meta.hasGlowEffect()) {
            sendMessage(player, ERROR_PREFIX + "This item isn't glowing");
            return;
        }
        if (meta.hasEnchants()) {
            sendMessage(player, ERROR_PREFIX + "Can't un-glow an enchanted item. Use " + ChatColor.WHITE + "/itemunenchant.");
            return;
        }
        meta.setGlowEffect(false);
        heldItem.setItemMeta(meta);
        sendMessage(player, "Awwwww");
    }

    private void onItemUnEnchant(Player player, ItemStack heldItem)
    {
        ItemMeta meta = heldItem.getItemMeta();
        if (!meta.hasEnchants()) {
            sendMessage(player, ERROR_PREFIX + "This item has no enchantments.");
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
        sendMessage(player, "Glowing: " + ChatColor.BLUE + (meta.hasGlowEffect() ? "yes" : "no"));
        if (meta.hasEnchants()) {
            sendMessage(player, "Has " + ChatColor.BLUE + meta.getEnchants().size() + CHAT_PREFIX + " enchantments");
        } else {
            sendMessage(player, "Has no enchantments");
        }
    }

    private void onItemFly(Player player, ItemStack heldItem)
    {
        sendMessage(player, "TODO!");
    }

    private void onItemUnFly(Player player, ItemStack heldItem)
    {
        sendMessage(player, "TODO!");
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

	}
}
