package xyz.motz.randomizer.main;

import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Interaction;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.world.LootGenerateEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.motz.randomizer.commands.*;
import xyz.motz.randomizer.listeners.CommandTabListener;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Randomizer extends JavaPlugin implements Listener {

    public List<Material> remaining = new ArrayList<>();
    public List<Material> remainingmobs = new ArrayList<>();


    @Getter
    public static Randomizer plugin;

    public boolean enabled = this.getConfig().getBoolean("activated");
    public FileConfiguration itemsConfig;
    public File itemsFile;

    public static Randomizer getPlugin() {
        return plugin;
    }

    public static void setPlugin(Randomizer plugin) {
        Randomizer.plugin = plugin;
    }

    @Override
    public void onLoad() {
        plugin = this;
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getPluginManager().registerEvents(new CommandTabListener(), this);

        getCommand("randomizer").setExecutor(new RandomizerCommand());
        this.getConfig().options().copyDefaults();
        saveDefaultConfig();


        // Initialize items.yml
        initializeItemsConfig();

    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (this.enabled) {
            if (e.getBlock().getType().equals(Material.KELP_PLANT)
                    || e.getBlock().getType().equals(Material.KELP)
                    || e.getBlock().getType().equals(Material.SUGAR_CANE)
                    || e.getBlock().getType().equals(Material.CACTUS)
                    || e.getBlock().getType().equals(Material.BAMBOO)
            ) {
                e.setDropItems(false);
                e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(),
                        new ItemStack(getPartner(e.getBlock().getType())));
                Block block = e.getBlock();
                dropItemsAbove(block);
            } else {
            e.setDropItems(false);
            if (e.getPlayer().getGameMode().equals(GameMode.SURVIVAL)) {
                e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(),
                        new ItemStack(getPartner(e.getBlock().getType())));
            }
            }
        }
    }

    //  FIXED CROPS; REDSTONE, FLOWERS; GRASS; FARN, BUTTONS AND LEVERSCROPS BEING DESTROYED BY WATER
    @EventHandler
    public void onWaterFlood(BlockFromToEvent event){
        if (this.enabled) {
            if (event.getBlock().getType() == Material.WATER || event.getBlock().getType() == Material.WATER_BUCKET){

            if (event.getToBlock().getType().isBlock() && !event.getToBlock().getType().equals(Material.AIR)
                    && !event.getToBlock().getType().equals(Material.WATER) && !event.getToBlock().getType().equals(Material.LAVA)
            ) {
                event.getToBlock().getWorld().dropItemNaturally(event.getToBlock().getLocation(),
                        new ItemStack(getPartner(event.getToBlock().getType())));
                event.getToBlock().getDrops().clear();
                event.getToBlock().setType(Material.AIR);
                event.setCancelled(true);
            }
            }
        }
    }

    // TODO: FIX CACTUS, BAMBOO; SUGARCANE; KELP BEING DESTROYED BY PISTONS (CACTUS PLACED A BLOCK NEXT TO IT) OR BREAKING THE BLOCK BELOW

    // FIX FOR CACTUS, KELP; SUGARCANE; BAMBOO breaking at the bottom.
    private void dropItemsAbove(Block block) {
        Block aboveBlock = block.getLocation().add(0, 1, 0).getBlock();
        while (aboveBlock.getType().equals(Material.KELP_PLANT)
                || aboveBlock.getType().equals(Material.KELP)
                || aboveBlock.getType().equals(Material.SUGAR_CANE)
                || aboveBlock.getType().equals(Material.CACTUS)
                || aboveBlock.getType().equals(Material.BAMBOO)
        ) {
            // Break the plant block naturally
            aboveBlock.getWorld().dropItemNaturally(aboveBlock.getLocation(),
                    new ItemStack(getPartner(aboveBlock.getType())));
            aboveBlock.getDrops().clear();
            aboveBlock.setType(Material.AIR);

            // CONTINUE WITH ALL BLOCKS ABOVE
            aboveBlock = aboveBlock.getLocation().add(0, 1, 0).getBlock();

        }
    }
    // Remove Dragon-EGG teleporting
    @EventHandler
    public void dragonEggTpEvent(BlockDropItemEvent event) {
        if (event.getBlock().getType().equals(Material.DRAGON_EGG)) {
            event.getBlock().getWorld().dropItemNaturally(event.getBlock().getLocation(),
                    new ItemStack(getPartner(event.getBlock().getType())));
            event.getBlock().getDrops().clear();
        }
    }


    // TNT EXPLOSIONS DROP THE RANDOMIZED ITEMS
    @EventHandler
    public void onBlockExplode(EntityExplodeEvent event) {
        if (this.enabled) {
            if (event.getEntityType().equals(EntityType.CREEPER) || event.getEntityType().equals(EntityType.PRIMED_TNT) || event.getEntityType().equals(EntityType.MINECART_TNT)) {
            event.setYield(0.0F);
            event.blockList().forEach(block -> {
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(getPartner(block.getType())));
                block.setType(Material.AIR); // Ensure the block is removed
            });
        }
        }
    }

    // CREEPER EXPLOSIONS DROP THE RANDOMIZED ITEMS
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (this.enabled) {
            event.setYield(0.0F);
            event.blockList().forEach(block -> {
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(getPartner(block.getType())));
                block.setType(Material.AIR);
            });
        }
    }

    @EventHandler // RECOGNISE WHEN FALLING BLOCKS ARE DESTROYED
    public void fallingBlocks(EntityDropItemEvent event) {
        if (this.enabled) {
            if (event.getEntityType().equals(EntityType.FALLING_BLOCK)) {
                event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(),
                        new ItemStack(getPartner(event.getItemDrop().getItemStack().getType())));
                event.getItemDrop().remove();
            } else {
                event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(),
                        new ItemStack(getPartnerMobs(event.getEntityType())));
                event.getItemDrop().remove();

            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (this.enabled) {
            if (!event.getEntityType().equals(EntityType.PLAYER)) {
                if (getPartnerMobs(event.getEntityType()) != null) {
                    event.getDrops().clear();
                    event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(),
                            new ItemStack(getPartnerMobs(event.getEntityType())));
                } else {
                    this.getLogger().warning("The EntityType " + event.getEntityType() + " is not supported!");
                }
            }
        }
    }

    // RANDOMIZED CRAFTING
    @EventHandler
    public void crafting(CraftItemEvent event) {
        if (this.enabled) {
            if (event.getInventory().getResult() != null) {
                event.getInventory().setResult(new ItemStack(getPartnerItem(event.getInventory().getResult().getType())));
            }
        }
    }

    // RANDOMIZED CHEST-LOOT#
    @EventHandler
    public void chestLoot(LootGenerateEvent event) {
        if (this.enabled) {
            List<Material> randomizedLoot = new ArrayList<>();
            int i;
            for (i = 0; i < event.getLoot().size(); i++) {
                randomizedLoot.add(getPartnerItem(event.getLoot().get(i).getType()));
            }
            event.getLoot().clear();
            for (i = randomizedLoot.size() - 1; i >= 0; i--) {
                event.getLoot().add(new ItemStack(randomizedLoot.get(i)));
            }
        }
    }

    public Material getPartner(Material mat) {
        Material randpart;
        try {
            randpart = Material.valueOf(this.itemsConfig.getString("blocks." + mat.toString()));
        } catch (Exception e) {
            randpart = mat;
        }
        return randpart;
    }

    public Material getPartnerMobs(EntityType mob) {
        Material randpart;
        try {
            randpart = Material.valueOf(this.itemsConfig.getString("mobs." + mob.toString()));
        } catch (Exception e) {
            randpart = null;
        }
        return randpart;
    }

    public Material getPartnerItem(Material mat) {
        Material randpart;
        try {
            randpart = Material.valueOf(this.itemsConfig.getString("items." + mat.toString()));
        } catch (Exception e) {
            randpart = mat;
        }
        return randpart;
    }

    // New method to initialize or reload items configuration
    public void initializeItemsConfig() {
        itemsFile = new File(getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            saveResource("items.yml", false);
        }
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
    }

    // Method to reload items configuration
    public void reloadItemsConfig() {
        if (itemsFile == null) {
            itemsFile = new File(getDataFolder(), "items.yml");
        }
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);
    }

    // Save items configuration
    public void saveItemsConfig() {
        try {
            itemsConfig.save(itemsFile);
        } catch (IOException e) {
            getLogger().severe("Could not save items configuration to " + itemsFile);
            e.printStackTrace();
        }
    }
}