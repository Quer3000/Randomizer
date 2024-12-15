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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import xyz.motz.randomizer.commands.*;
import xyz.motz.randomizer.listeners.CommandTabListener;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Randomizer extends JavaPlugin implements Listener {

    public List<Material> remaining = new ArrayList<>();
    public List<Material> remainingmobs = new ArrayList<>();


    @Getter
    public static Randomizer plugin;

    public boolean enabled = this.getConfig().getBoolean("activated");
    private FileConfiguration itemsConfig;
    private File itemsFile;

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
        itemsFile = new File(getDataFolder(), "items.yml");
        if (!itemsFile.exists()) {
            saveResource("items.yml", false);
        }
        itemsConfig = YamlConfiguration.loadConfiguration(itemsFile);

    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
            if (this.enabled) {
                e.setDropItems(false);
                if (e.getPlayer().getGameMode().equals(GameMode.SURVIVAL)) {
                    e.getBlock().getWorld().dropItemNaturally(e.getBlock().getLocation(),
                            new ItemStack(getPartner(e.getBlock().getType())));
                }
        }
    }

    // TNT EXPLOSIONS DROP THE RANDOMIZED ITEMS
    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        if (this.enabled) {
            event.setYield(0.0F);
            event.blockList().forEach(block -> {
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(getPartner(block.getType())));
                block.setType(Material.AIR); // Ensure the block is removed
            });
        }
    }
    // CREPER EXPLOSIONS DROP THE RANDOMIZED ITEMS
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

            event.getDrops().clear();
              event.getEntity().getWorld().dropItemNaturally(event.getEntity().getLocation(),
            new ItemStack(getPartnerMobs(event.getEntityType())));
            }
        }
    }


    public Material getPartner(Material mat) {
        Material randpart;
        try {
            randpart = Material.valueOf(this.itemsConfig.getString("partners." + mat.toString()));
        } catch (Exception e) {
            randpart = mat;
        }
        return randpart;
    }

    public Material getPartnerMobs(EntityType mob) {
        Material randpart;
        try {
            randpart = Material.valueOf(this.itemsConfig.getString("partners-mobs." + mob.toString()));
        } catch (Exception e) {
            randpart = null;
        }
        return randpart;
    }

}
