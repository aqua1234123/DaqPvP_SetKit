
package com.daqpvp;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.CommandExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;

import net.kyori.adventure.text.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main extends JavaPlugin implements Listener {

    private File dataFile;
    private File kitFile;
    private FileConfiguration dataConfig;
    private FileConfiguration kitConfig;

    private List<ItemStack> savedKit = new ArrayList<>();

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        this.getCommand("kit").setExecutor(new KitCommand());
        this.getCommand("setkit").setExecutor(new SetKitCommand());

        createDataFile();
        createKitFile();
        loadKit();

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    updateScoreboard(player);
                }
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    @Override
    public void onDisable() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            savePlayerData(player);
        }
        saveDataFile();
    }

    private void createDataFile() {
        dataFile = new File(getDataFolder(), "data.yml");
        if (!dataFile.exists()) {
            dataFile.getParentFile().mkdirs();
            try {
                dataFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
    }

    private void createKitFile() {
        kitFile = new File(getDataFolder(), "kit.yml");
        if (!kitFile.exists()) {
            kitFile.getParentFile().mkdirs();
            try {
                kitFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        kitConfig = YamlConfiguration.loadConfiguration(kitFile);
    }

    private void saveKit(Player player) {
        ItemStack[] items = player.getInventory().getContents();
        for (int i = 0; i < items.length; i++) {
            kitConfig.set("kit.slot" + i, items[i]);
        }
        try {
            kitConfig.save(kitFile);
            player.sendMessage("§aキットを保存しました！");
        } catch (IOException e) {
            player.sendMessage("§cキットの保存に失敗しました。");
        }
    }

    private void loadKit() {
        savedKit.clear();
        for (int i = 0; i < 36; i++) {
            ItemStack item = kitConfig.getItemStack("kit.slot" + i);
            if (item != null && item.getType() != Material.AIR) {
                savedKit.add(item);
            }
        }
    }

    private void saveDataFile() {
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void savePlayerData(Player player) {
        String path = "players." + player.getUniqueId();
        dataConfig.set(path + ".coins", getCoins(player));
        dataConfig.set(path + ".killstreak", getKillstreak(player));
        saveDataFile();
    }

    private void loadPlayerData(Player player) {
        String path = "players." + player.getUniqueId();
        if (!dataConfig.contains(path)) {
            dataConfig.set(path + ".coins", 0);
            dataConfig.set(path + ".killstreak", 0);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        loadPlayerData(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        savePlayerData(event.getPlayer());
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        setKillstreak(victim, 0);

        if (killer != null && killer != victim) {
            setKillstreak(killer, getKillstreak(killer) + 1);
            setCoins(killer, getCoins(killer) + 1);
        }
    }

    private void updateScoreboard(Player player) {
        Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = board.registerNewObjective("sidebar", "dummy", Component.text("§ddaqpvp japan"));
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int coins = Math.max(0, Math.min(10000, getCoins(player)));
        int ks = getKillstreak(player);

        obj.getScore("§a$ Coins: §f" + coins).setScore(2);
        obj.getScore("§6⚔ Killstreak: §f" + ks).setScore(1);

        player.setScoreboard(board);
    }

    private int getCoins(Player player) {
        return dataConfig.getInt("players." + player.getUniqueId() + ".coins", 0);
    }

    private void setCoins(Player player, int amount) {
        dataConfig.set("players." + player.getUniqueId() + ".coins", Math.max(0, Math.min(10000, amount)));
    }

    private int getKillstreak(Player player) {
        return dataConfig.getInt("players." + player.getUniqueId() + ".killstreak", 0);
    }

    private void setKillstreak(Player player, int amount) {
        dataConfig.set("players." + player.getUniqueId() + ".killstreak", Math.max(0, amount));
    }

    public class KitCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;

            if (getCoins(player) < 100) {
                player.sendMessage("§cコインが足りません！100コイン必要です。");
                return true;
            }

            setCoins(player, getCoins(player) - 100);

            for (ItemStack item : savedKit) {
                player.getInventory().addItem(item);
            }

            player.sendMessage("§aキットを購入しました！(100 Coins 消費)");

            return true;
        }
    }

    public class SetKitCommand implements CommandExecutor {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;

            if (!player.isOp()) {
                player.sendMessage("§cこのコマンドを使う権限がありません！");
                return true;
            }

            saveKit(player);
            loadKit();
            return true;
        }
    }
}
