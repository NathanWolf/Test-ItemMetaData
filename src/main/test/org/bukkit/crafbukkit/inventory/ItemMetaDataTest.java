package org.bukkit.craftbukkit.inventory;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.metadata.PersistentMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.support.DummyPlugin;
import org.bukkit.support.AbstractTestingBase;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class ItemMetaDataTest extends AbstractTestingBase {
    private Plugin pluginX = new DummyPlugin("pluginx");
    private Plugin pluginY = new DummyPlugin("pluginy");

    private static final String TEST_STRING_VALUE = "MY_TEST string 123";
    private static final int TEST_INTEGER_VALUE = 12345;
    private static final double TEST_DOUBLE_VALUE = 12345.1234;

    private class LinkedObject
    {
        public LinkedObject next;

        public LinkedObject()
        {
            // Create a circular reference
            this.next = new LinkedObject(this);
        }

        public LinkedObject(LinkedObject next)
        {
            this.next = next;
        }
    }

    @Test(expected=IllegalArgumentException.class)
    public void testAddInvalidData() {
        ItemStack testStack = new ItemStack(Material.STICK);
        testStack = CraftItemStack.asCraftCopy(testStack);
        ItemMeta itemMeta = testStack.getItemMeta();
        itemMeta.setMetadata("Testing", new FixedMetadataValue(pluginX, TEST_STRING_VALUE));
        testStack.setItemMeta(itemMeta);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testAddNestedInvalidData() {
        ItemStack testStack = new ItemStack(Material.STICK);
        testStack = CraftItemStack.asCraftCopy(testStack);
        ItemMeta itemMeta = testStack.getItemMeta();

        LinkedObject objectInstance = new LinkedObject();
        List<Object> list = new ArrayList<Object>();
        list.add(objectInstance);
        itemMeta.setMetadata("Testing", new PersistentMetadataValue(pluginX, list));

        testStack.setItemMeta(itemMeta);
    }

    @Test
    public void testAddSerializeableData() {
        ItemStack testStoredStack = new ItemStack(Material.DIAMOND_SWORD);
        testStoredStack = CraftItemStack.asCraftCopy(testStoredStack);
        ItemMeta itemMeta = testStoredStack.getItemMeta();
        itemMeta.setDisplayName(TEST_STRING_VALUE);
        List<String> lore = new ArrayList<String>();
        lore.add(TEST_STRING_VALUE + "_1");
        lore.add(TEST_STRING_VALUE + "_2");
        itemMeta.setLore(lore);
        itemMeta.addEnchant(Enchantment.DURABILITY, 1, false);
        itemMeta.setMetadata("testing_embedded_data", new PersistentMetadataValue(pluginY, TEST_STRING_VALUE));
        testStoredStack.setItemMeta(itemMeta);

        ItemStack testStack = new ItemStack(Material.STICK);
        testStack = CraftItemStack.asCraftCopy(testStack);
        itemMeta = testStack.getItemMeta();
        assertThat(itemMeta.hasMetadata(), is(false));
        assertThat(itemMeta.hasMetadata("test_serializeable"), is(false));

        SerializeableObject serializeable = new SerializeableObject();
        serializeable.map.put("Test-String", TEST_STRING_VALUE);
        serializeable.map.put("Test-Integer", TEST_INTEGER_VALUE);
        serializeable.map.put("Test-Double", TEST_DOUBLE_VALUE);
        serializeable.map.put("Test-ItemStack", testStoredStack);
        itemMeta.setMetadata("test_serializeable", new PersistentMetadataValue(pluginX, serializeable));
        assertThat(itemMeta.hasMetadata(), is(true));
        assertThat(itemMeta.hasMetadata("test_serializeable"), is(true));

        testStack.setItemMeta(itemMeta);
        assertThat(testStack.hasMetadata("test_serializeable"), is(true));
        assertThat(testStack.hasMetadata("test_not_there"), is(false));
        assertThat(testStack.hasMetadata("test_serializeable", pluginX), is(true));
        assertThat(testStack.hasMetadata("test_serializeable", pluginY), is(false));

        ItemStack cloneStack = testStack.clone();
        ItemMeta newMeta = cloneStack.getItemMeta();
        assertThat(newMeta.hasMetadata(), is(true));
        assertThat(newMeta.hasMetadata("test_serializeable"), is(true));

        List<MetadataValue> values = newMeta.getMetadata("test_serializeable");
        assertThat(values.size(), is(1));
        MetadataValue value = values.get(0);
        assertThat(value.getOwningPlugin(), is(pluginX));
        Object testObject = value.value();
        assertThat(testObject instanceof SerializeableObject, is(true));
        SerializeableObject deserialized = (SerializeableObject)testObject;
        assertThat(deserialized.map.size(), is(serializeable.map.size()));
        assertThat(deserialized.map.get("Test-Integer") instanceof Integer, is(true));
        Integer intValue = (Integer)deserialized.map.get("Test-Integer");
        assertThat(intValue, is(TEST_INTEGER_VALUE));
        assertThat(deserialized.map.get("Test-Double") instanceof Double, is(true));
        Double doubleValue = (Double)deserialized.map.get("Test-Double");
        assertThat(doubleValue, is(TEST_DOUBLE_VALUE));
        Object deserializedItem = deserialized.map.get("Test-ItemStack");
        assertThat(deserializedItem instanceof ItemStack, is(true));
        ItemStack deserializedItemStack = (ItemStack)deserializedItem;
        assertThat(deserializedItemStack.getType(), is(Material.DIAMOND_SWORD));
        ItemMeta deserializedMeta = deserializedItemStack.getItemMeta();
        assertThat(deserializedMeta.hasMetadata(), is(true));
        assertThat(deserializedMeta.hasEnchants(), is(true));
    }

    @Test
    public void testDataTypes() {
        ItemStack testStack = new ItemStack(Material.STICK);
        testStack = CraftItemStack.asCraftCopy(testStack);
        ItemMeta itemMeta = testStack.getItemMeta();
        assertThat(itemMeta.hasMetadata(), is(false));

        itemMeta.setMetadata("test-string", new PersistentMetadataValue(pluginX, TEST_STRING_VALUE));
        itemMeta.setMetadata("test-integer", new PersistentMetadataValue(pluginX, TEST_INTEGER_VALUE));
        itemMeta.setMetadata("test-long", new PersistentMetadataValue(pluginX, (long)TEST_INTEGER_VALUE));
        itemMeta.setMetadata("test-short", new PersistentMetadataValue(pluginX, (short)TEST_INTEGER_VALUE));
        itemMeta.setMetadata("test-byte", new PersistentMetadataValue(pluginX, (byte)TEST_INTEGER_VALUE));
        itemMeta.setMetadata("test-float", new PersistentMetadataValue(pluginX, (float)TEST_DOUBLE_VALUE));
        itemMeta.setMetadata("test-double", new PersistentMetadataValue(pluginX, TEST_DOUBLE_VALUE));

        List<String> testStringList = new ArrayList<String>();
        testStringList.add(TEST_STRING_VALUE);
        testStringList.add(TEST_STRING_VALUE);

        itemMeta.setMetadata("test-list", new PersistentMetadataValue(pluginX, testStringList));

        List<List<String>> testNestedList = new ArrayList<List<String>>();
        testNestedList.add(testStringList);
        testNestedList.add(testStringList);

        itemMeta.setMetadata("test-nested-list", new PersistentMetadataValue(pluginX, testNestedList));

        Map<String, String> testMapOfString = new HashMap<String, String>();
        testMapOfString.put("test-1", TEST_STRING_VALUE);
        testMapOfString.put("test-2", TEST_STRING_VALUE);

        itemMeta.setMetadata("test-string-map", new PersistentMetadataValue(pluginX, testMapOfString));

        Map<String, List<String>> testMapOfLists = new HashMap<String, List<String>>();
        testMapOfLists.put("test-1", testStringList);
        testMapOfLists.put("test-2", testStringList);

        itemMeta.setMetadata("test-list-map", new PersistentMetadataValue(pluginX, testMapOfLists));

        assertThat(itemMeta.hasMetadata(), is(true));

        testStack.setItemMeta(itemMeta);
        assertThat(testStack.hasMetadata("test-not-there"), is(false));
        assertThat(testStack.hasMetadata("test-string"), is(true));
        assertThat(testStack.hasMetadata("test-integer", pluginX), is(true));
        assertThat(testStack.hasMetadata("test-integer", pluginY), is(false));
        assertThat(testStack.hasMetadata("test-long", pluginX), is(true));
        assertThat(testStack.hasMetadata("test-short", pluginX), is(true));
        assertThat(testStack.hasMetadata("test-byte", pluginX), is(true));
        assertThat(testStack.hasMetadata("test-float", pluginX), is(true));
        assertThat(testStack.hasMetadata("test-double", pluginX), is(true));
        assertThat(testStack.hasMetadata("test-list", pluginX), is(true));
        assertThat(testStack.hasMetadata("test-nested-list", pluginX), is(true));
        assertThat(testStack.hasMetadata("test-string-map", pluginX), is(true));
        assertThat(testStack.hasMetadata("test-list-map", pluginX), is(true));

        ItemStack cloneStack = testStack.clone();
        ItemMeta newMeta = cloneStack.getItemMeta();
        assertThat(newMeta.hasMetadata(), is(true));
        MetadataValue testString = newMeta.getMetadata("test-string", pluginX);
        assertThat(testString.asString(), is(TEST_STRING_VALUE));
        MetadataValue testInteger = newMeta.getMetadata("test-integer", pluginX);
        assertThat(testInteger.asInt(), is(TEST_INTEGER_VALUE));
        MetadataValue tetLong = newMeta.getMetadata("test-long", pluginX);
        assertThat(tetLong.asLong(), is((long)TEST_INTEGER_VALUE));
        MetadataValue testShort = newMeta.getMetadata("test-short", pluginX);
        assertThat(testShort.asShort(), is((short)TEST_INTEGER_VALUE));
        MetadataValue testByte = newMeta.getMetadata("test-byte", pluginX);
        assertThat(testByte.asByte(), is((byte)TEST_INTEGER_VALUE));
        MetadataValue testFloat = newMeta.getMetadata("test-float", pluginX);
        assertThat(testFloat.asFloat(), is((float)TEST_DOUBLE_VALUE));
        MetadataValue testDouble = newMeta.getMetadata("test-double", pluginX);
        assertThat(testDouble.asDouble(), is(TEST_DOUBLE_VALUE));

        PersistentMetadataValue metaList = (PersistentMetadataValue)newMeta.getMetadata("test-list", pluginX);
        List<Object> testObjectList = new ArrayList<Object>(testStringList);
        assertThat(metaList.asList(), is(testObjectList));

        PersistentMetadataValue metaNestedList = (PersistentMetadataValue)newMeta.getMetadata("test-nested-list", pluginX);
        testObjectList = new ArrayList<Object>(testNestedList);
        assertThat(metaNestedList.asList(), is(testObjectList));

        PersistentMetadataValue metaStringMap = (PersistentMetadataValue)newMeta.getMetadata("test-string-map", pluginX);
        Map<String, Object> testObjectMap = new HashMap<String, Object>(testMapOfString);
        assertThat(metaStringMap.asMap(), is(testObjectMap));

        PersistentMetadataValue metaListMap = (PersistentMetadataValue)newMeta.getMetadata("test-list-map", pluginX);
        testObjectMap = new HashMap<String, Object>(testMapOfLists);
        assertThat(metaListMap.asMap(), is(testObjectMap));
    }

    @Test
    public void testSerializeObject() {
        SerializeableObject object = new SerializeableObject();
        List<ItemStack> items = new ArrayList<ItemStack>();
        final int ITEM_COUNT = 64;
        for (int i = 0; i < ITEM_COUNT; i++) {
            ItemStack testStack = new ItemStack(Material.DIAMOND_SWORD);
            testStack = CraftItemStack.asCraftCopy(testStack);
            ItemMeta itemMeta = testStack.getItemMeta();
            itemMeta.setMetadata("testing", new PersistentMetadataValue(pluginX, TEST_STRING_VALUE));
            itemMeta.addEnchant(Enchantment.DURABILITY, 1, false);
            testStack.setItemMeta(itemMeta);

            items.add(testStack);
            object.list.add(testStack.clone());
        }
        object.map.put("items", items);
        object.map.put("item_count", items.size());

        List<String> players = new ArrayList<String>();
        players.add("One Player");
        players.add("Two Player");
        object.map.put("players", players);

        YamlConfiguration yamlConfig = new YamlConfiguration();
        yamlConfig.set("test_object", object);
        String testString = yamlConfig.saveToString();
        assertThat(testString, is(not(isEmptyString())));

        YamlConfiguration loadConfig = new YamlConfiguration();
        try {
            loadConfig.loadFromString(testString);
        } catch (Throwable ex) {
            throw new RuntimeException(testString, ex);
        }

        Object loadedObject = loadConfig.get("test_object");
        assertThat(loadedObject, is(not(nullValue())));
        assertThat(loadedObject instanceof SerializeableObject, is(true));

        SerializeableObject deserialized = (SerializeableObject)loadedObject;

        assertThat(deserialized.map.size(), is(object.map.size()));
        assertThat(deserialized.list.size(), is(object.list.size()));
        assertThat(deserialized.map.containsKey("items"), is(true));
        assertThat(deserialized.map.containsKey("item_count"), is(true));
        assertThat(deserialized.map.containsKey("players"), is(true));
        assertThat(deserialized.map.get("item_count"), is(object.map.get("item_count")));
        assertThat(deserialized.map.get("players"), is(object.map.get("players")));

        assertThat(deserialized.map.get("items"), is(object.map.get("items")));

        Object testList = deserialized.map.get("items");
        assertThat(testList instanceof List, is(true));

        List<ItemStack> itemList = (List<ItemStack>)testList;
        assertThat(itemList.size(), is(ITEM_COUNT));
    }

    @Test
    public void testSerializeItemStack() {
        ItemStack testStack = new ItemStack(Material.DIAMOND_SWORD);
        testStack = CraftItemStack.asCraftCopy(testStack);
        ItemMeta itemMeta = testStack.getItemMeta();
        itemMeta.setMetadata("testing", new PersistentMetadataValue(pluginX, TEST_STRING_VALUE));
        itemMeta.addEnchant(Enchantment.DURABILITY, 1, false);
        testStack.setItemMeta(itemMeta);

        YamlConfiguration yamlConfig = new YamlConfiguration();
        yamlConfig.set("item", testStack);
        String testString = yamlConfig.saveToString();
        assertThat(testString, is(not(isEmptyString())));

        YamlConfiguration loadConfig = new YamlConfiguration();
        try {
            loadConfig.loadFromString(testString);
        } catch (Throwable ex) {
            throw new RuntimeException(testString, ex);
        }

        ItemStack deserializedItem = yamlConfig.getItemStack("item");
        assertThat(deserializedItem, is(not(nullValue())));
        assertThat(itemMeta.hasMetadata(), is(true));
        assertThat(itemMeta.hasEnchants(), is(true));
        assertThat(itemMeta.hasMetadata("testing"), is(true));

        List<MetadataValue> values = itemMeta.getMetadata("testing");
        assertThat(values.size(), is(1));
        MetadataValue value = values.get(0);
        assertThat(value.getOwningPlugin(), is(pluginX));
        assertThat(value.asString(), is(TEST_STRING_VALUE));
    }

    @Test
    public void testAddRemoveData() {
        ItemStack testStack = new ItemStack(Material.STICK);
        testStack = CraftItemStack.asCraftCopy(testStack);
        ItemMeta itemMeta = testStack.getItemMeta();
        assertThat(itemMeta.hasMetadata(), is(false));
        itemMeta.setMetadata("testing", new PersistentMetadataValue(pluginX, TEST_STRING_VALUE));
        assertThat(itemMeta.hasMetadata(), is(true));
        assertThat(itemMeta.hasMetadata("testing"), is(true));
        assertThat(itemMeta.hasMetadata("testing2"), is(false));
        assertThat(itemMeta.hasMetadata("testing", pluginX), is(true));
        assertThat(itemMeta.hasMetadata("testing", pluginY), is(false));
        testStack.setItemMeta(itemMeta);

        ItemMeta newMeta = testStack.getItemMeta();
        assertThat(newMeta.hasMetadata(), is(true));

        // Test single-call interface
        MetadataValue singleValue = newMeta.getMetadata("testing", pluginX);
        assertThat(singleValue, is(not(nullValue())));
        assertThat(singleValue.getOwningPlugin(), is(pluginX));
        assertThat(singleValue.asString(), is(TEST_STRING_VALUE));

        MetadataValue missingValue = newMeta.getMetadata("testing", pluginY);
        assertThat(missingValue, is(nullValue()));

        List<MetadataValue> values = newMeta.getMetadata("testing");
        assertThat(values.size(), is(1));
        MetadataValue value = values.get(0);
        assertThat(value.getOwningPlugin(), is(pluginX));
        assertThat(value.asString(), is(TEST_STRING_VALUE));
        newMeta.removeMetadata("testing", pluginX);
        testStack.setItemMeta(newMeta);

        ItemMeta cleanedMeta = testStack.getItemMeta();
        assertThat(cleanedMeta.hasMetadata(), is(false));
        assertThat(cleanedMeta.hasMetadata("testing"), is(false));
    }

    @Test
    public void multiPluginTest() {
        ItemStack testStack = new ItemStack(Material.DIAMOND_SWORD);
        testStack = CraftItemStack.asCraftCopy(testStack);
        ItemMeta itemMeta = testStack.getItemMeta();

        itemMeta.setMetadata("testing", new PersistentMetadataValue(pluginX, TEST_STRING_VALUE));
        itemMeta.setMetadata("testing", new PersistentMetadataValue(pluginY, TEST_INTEGER_VALUE));

        assertThat(itemMeta.hasMetadata(), is(true));
        assertThat(itemMeta.hasMetadata("testing"), is(true));

        testStack.setItemMeta(itemMeta);

        ItemStack cloned = testStack.clone();

        ItemMeta clonedMeta = cloned.getItemMeta();
        assertThat(clonedMeta.hasMetadata("testing"), is(true));

        List<MetadataValue> values = clonedMeta.getMetadata("testing");
        assertThat(values.size(), is(2));

        for (MetadataValue value : values) {
            if (value.getOwningPlugin() == pluginX) {
                assertThat(value.asString(), is(TEST_STRING_VALUE));
            } else {
                assertThat(value.asInt(), is(TEST_INTEGER_VALUE))          ;
            }
        }

        clonedMeta.removeMetadata("testing", pluginX);

        values = clonedMeta.getMetadata("testing");
        assertThat(values.size(), is(1));

        cloned.setItemMeta(clonedMeta);
        assertThat(cloned.hasMetadata("testing"), is(true));
        cloned = cloned.clone();
        clonedMeta = cloned.getItemMeta();
        assertThat(clonedMeta.hasMetadata(), is(true));
        assertThat(clonedMeta.hasMetadata("testing"), is(true));

        values = clonedMeta.getMetadata("testing");
        assertThat(values.size(), is(1));
        MetadataValue value = values.get(0);
        assertThat(value.getOwningPlugin(), is(pluginY));
        assertThat(value.asInt(), is(TEST_INTEGER_VALUE));

        clonedMeta.removeMetadata("testing", pluginY);
        assertThat(clonedMeta.hasMetadata(), is(false));
        assertThat(clonedMeta.hasMetadata("testing"), is(false));
        cloned.setItemMeta(clonedMeta);
    }

    @Test
    public void testEquality() {
        ItemStack testStack1 = new ItemStack(Material.DIAMOND_SWORD);
        testStack1 = CraftItemStack.asCraftCopy(testStack1);

        ItemStack testStack2 = new ItemStack(Material.DIAMOND_SWORD);
        testStack2 = CraftItemStack.asCraftCopy(testStack2);

        assertThat(testStack1, is(testStack2));

        ItemMeta meta1 = testStack1.getItemMeta();
        meta1.setMetadata("testing", new PersistentMetadataValue(pluginX, TEST_STRING_VALUE));
        testStack1.setItemMeta(meta1);

        assertThat(testStack1, is(not(testStack2)));

        ItemMeta meta2 = testStack2.getItemMeta();
        meta2.setMetadata("testing", new PersistentMetadataValue(pluginX, TEST_STRING_VALUE));
        testStack2.setItemMeta(meta2);

        assertThat(testStack1, is(testStack2));
    }

    @Test
    public void testDataAndEnchants() {
        ItemStack testStack = new ItemStack(Material.DIAMOND_SWORD);
        testStack = CraftItemStack.asCraftCopy(testStack);
        ItemMeta itemMeta = testStack.getItemMeta();
        assertThat(itemMeta.hasMetadata(), is(false));
        assertThat(itemMeta.hasEnchants(), is(false));

        itemMeta.setMetadata("testing", new PersistentMetadataValue(pluginX, TEST_STRING_VALUE));

        assertThat(itemMeta.hasMetadata(), is(true));
        assertThat(itemMeta.hasEnchants(), is(false));

        itemMeta.addEnchant(Enchantment.DURABILITY, 1, false);

        assertThat(itemMeta.hasMetadata(), is(true));
        assertThat(itemMeta.hasEnchants(), is(true));

        testStack.setItemMeta(itemMeta);

        ItemMeta newMeta = testStack.getItemMeta();
        assertThat(newMeta.hasMetadata(), is(true));
        assertThat(newMeta.hasEnchants(), is(true));
        assertThat(newMeta.hasEnchant(Enchantment.DURABILITY), is(true));
        assertThat(newMeta.hasMetadata("testing"), is(true));

        List<MetadataValue> values = newMeta.getMetadata("testing");
        assertThat(values.size(), is(1));
        MetadataValue value = values.get(0);
        assertThat(value.getOwningPlugin(), is(pluginX));
        assertThat(value.asString(), is(TEST_STRING_VALUE));

        newMeta.removeMetadata("testing", pluginX);
        testStack.setItemMeta(newMeta);

        ItemMeta cleanedMeta = testStack.getItemMeta();
        assertThat(cleanedMeta.hasMetadata(), is(false));
        assertThat(cleanedMeta.hasMetadata("testing"), is(false));
        assertThat(newMeta.hasEnchants(), is(true));
        assertThat(newMeta.hasEnchant(Enchantment.DURABILITY), is(true));
    }
}
