package space.gorogoro.spectatorlock;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * 権限の無いプレイヤーが観戦モードに切り替えるのを止める。
 */
public class SpectatorLock extends JavaPlugin implements Listener {

  private static final String PERMISSION_BYPASS = "spectatorlock.bypass";
  private static final String PERMISSION_TELEPORT = "spectatorlock.teleport";
  private static final String PERMISSION_RELOAD = "spectatorlock.reload";

  // config.yml の設定。起動時とリロード時に一度だけ読み込む
  private boolean restrictWorldsEnabled;
  private Set<String> restrictedWorlds = Collections.emptySet();
  private boolean blockSpectatorTeleport;

  // 表示するメッセージ(config.yml の messages.<language>)。起動時とリロード時に一度だけ読み込む
  private static final String DEFAULT_LANGUAGE = "en";
  private static final String[] MESSAGE_KEYS = {
          "spectator-denied", "teleport-denied", "reloaded", "no-permission", "usage"
  };
  private final Map<String, String> messages = new HashMap<>();

  /**
   * JavaPlugin method onEnable.
   */
  @Override
  public void onEnable() {
    try {
      getLogger().info("The Plugin Has Been Enabled!");

      // If there is no setting file, it is created
      if(!getDataFolder().exists()){
        getDataFolder().mkdir();
      }

      File configFile = new File(getDataFolder(), "config.yml");
      if(!configFile.exists()){
        saveDefaultConfig();
      }
      loadSettings();

      getServer().getPluginManager().registerEvents(this, this);
    } catch (Exception e) {
      logStackTrace(e);
    }
  }

  /**
   * config.yml を読み込んで保持する。足りない項目があれば jar 内の既定値を追記する。
   */
  private void loadSettings() {
    if (!getConfig().getKeys(true).containsAll(getConfig().getDefaults().getKeys(true))) {
      getConfig().options().copyDefaults(true);
      saveConfig();
    }
    restrictWorldsEnabled = getConfig().getBoolean("restrict-worlds.enabled");
    List<String> worlds = getConfig().getStringList("restrict-worlds.worlds");
    restrictedWorlds = Collections.unmodifiableSet(new HashSet<>(worlds));
    blockSpectatorTeleport = getConfig().getBoolean("block-spectator-teleport");
    loadMessages();
  }

  /**
   * language で選んだ言語のメッセージを読み込む。無い言語なら英語にする。足りない項目は jar 内の英語で補う。
   */
  private void loadMessages() {
    String language = getConfig().getString("language");
    if (language == null || !getConfig().isConfigurationSection("messages." + language)) {
      getLogger().warning("Unknown language '" + language + "' in config.yml. Using '" + DEFAULT_LANGUAGE + "'.");
      language = DEFAULT_LANGUAGE;
    }
    messages.clear();
    for (String key : MESSAGE_KEYS) {
      String text = getConfig().getString("messages." + language + "." + key);
      if (text == null && getConfig().getDefaults() != null) {
        text = getConfig().getDefaults().getString("messages." + DEFAULT_LANGUAGE + "." + key);
      }
      messages.put(key, text != null ? text : key);
    }
  }

  /**
   * メッセージの {player} を置き換えて返す。
   */
  private String message(String key, String player) {
    String text = messages.getOrDefault(key, key);
    if (player != null) {
      text = text.replace("{player}", player);
    }
    return text;
  }

  /**
   * そのワールドで観戦モードを制限するか。ワールド限定が無効なら全ワールドで制限する。
   */
  private boolean isRestricted(World world) {
    return !restrictWorldsEnabled || restrictedWorlds.contains(world.getName());
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onGameModeChange(PlayerGameModeChangeEvent event) {
    if (event.getNewGameMode() != GameMode.SPECTATOR) {
      return;
    }
    PlayerGameModeChangeEvent.Cause cause = event.getCause();
    // ハードコアで死んだときの観戦モードはバニラの仕組みなので止めない
    if (cause == PlayerGameModeChangeEvent.Cause.HARDCORE_DEATH) {
      return;
    }
    Player player = event.getPlayer();
    if (!isRestricted(player.getWorld()) || player.hasPermission(PERMISSION_BYPASS)) {
      return;
    }
    event.setCancelled(true);
    Component denied = Component.text(message("spectator-denied", player.getName()), NamedTextColor.RED);
    if (cause == PlayerGameModeChangeEvent.Cause.COMMAND || cause == PlayerGameModeChangeEvent.Cause.GAMEMODE_SWITCHER) {
      // /gamemode やゲームモード切り替え画面では、実行した人(本人とは限らない)に表示される
      event.cancelMessage(denied);
    } else {
      player.sendMessage(denied);
    }
  }

  @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
  public void onTeleport(PlayerTeleportEvent event) {
    // 観戦メニューからのテレポートだけを対象にする
    if (!blockSpectatorTeleport || event.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) {
      return;
    }
    // ワールド限定が有効なら、行き先が制限するワールドのときだけ止める
    if (!isRestricted(event.getTo().getWorld()) || event.getPlayer().hasPermission(PERMISSION_TELEPORT)) {
      return;
    }
    event.setCancelled(true);
    event.getPlayer().sendMessage(Component.text(message("teleport-denied", null), NamedTextColor.RED));
  }

  @Override
  public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    if (!sender.hasPermission(PERMISSION_RELOAD)) {
      sender.sendMessage(Component.text(message("no-permission", null), NamedTextColor.RED));
      return true;
    }
    if (args.length != 1 || !args[0].equalsIgnoreCase("reload")) {
      sender.sendMessage(Component.text(message("usage", null), NamedTextColor.RED));
      return true;
    }
    reloadConfig();
    loadSettings();
    sender.sendMessage(Component.text(message("reloaded", null), NamedTextColor.GREEN));
    return true;
  }

  @Override
  public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
    if (args.length == 1 && sender.hasPermission(PERMISSION_RELOAD) && "reload".startsWith(args[0].toLowerCase())) {
      return List.of("reload");
    }
    return Collections.emptyList();
  }

  public void logStackTrace(Exception e){
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      pw.flush();
      getLogger().warning(sw.toString());
  }

  /**
   * JavaPlugin method onDisable.
   */
  @Override
  public void onDisable() {
    try {
      getLogger().info("The Plugin Has Been Disabled!");
    } catch (Exception e) {
      logStackTrace(e);
    }
  }
}
