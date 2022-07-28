package cn.lanink.teamsystem;

import cn.lanink.gamecore.utils.Language;
import cn.lanink.teamsystem.dao.Dao;
import cn.lanink.teamsystem.utils.FormHelper;
import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import io.netty.util.collection.IntObjectHashMap;
import org.jetbrains.annotations.NotNull;
import org.ktorm.database.Database;

import java.util.HashMap;
import java.util.Random;

/**
 * @author lt_name
 */
public class TeamSystem extends PluginBase {

    public static final String VERSION = "?";
    public static boolean debug = false;

    public static final Random RANDOM = new Random();
    // public static final Gson GSON = new Gson();
    private static TeamSystem instance;

    public static TeamSystem getInstance() {
        return instance;
    }

    private final IntObjectHashMap<Team> teams = new IntObjectHashMap<>();

    public IntObjectHashMap<Team> getTeams() {
        return teams;
    }

    private Database database;

    public Database getDatabase() {
        return database;
    }

    private Language language;

    public Language getLanguage() {
        return language;
    }

    @Override
    public void onLoad() {
        instance = this;
        this.saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        String lang = new Config(this.getDataFolder()+"/config.yml", Config.YAML).getString("language", "zh_CN");
        Config config = new Config(Config.PROPERTIES);
        config.load(this.getResource("languages/"+lang+".properties"));
        this.language = new Language(config);
        if (this.getConfig().getBoolean("MySQL.enable")) {
            this.getLogger().info(language.translateString("info.connectingToDatabase"));
            HashMap<String, Object> sqlConfig = this.getConfig().get("MySQL", new HashMap<>());
            try {
                this.database = Dao.Companion.connect(
                        (String) sqlConfig.get("host"),
                        (int) sqlConfig.get("port"),
                        (String) sqlConfig.get("database"),
                        (String) sqlConfig.get("user"),
                        (String) sqlConfig.get("password")
                );
            } catch (Exception e) {
                this.getLogger().error(language.translateString("info.connectToDatabaseFailed"), e);
                this.database = null;
            }
        }
        this.getServer().getPluginManager().registerEvents(new EventListener(this), this);
        this.getLogger().info(language.translateString("info.pluginEnabled", VERSION));
    }

    @Override
    public void onDisable() {
        this.teams.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ("team".equalsIgnoreCase(command.getName())) {
            if (sender instanceof Player) {
                FormHelper.showMain((Player) sender);
            }else {
                sender.sendMessage(language.translateString("tips.useInGame"));
            }
            return true;
        }
        return false;
    }


    public void quitTeam(@NotNull Player player) {
        Team team = this.getTeamByPlayer(player);
        if (team == null) {
            return;
        }
        if (team.isTeamLeader(player)) {
            this.getTeams().remove(team.getId());
            team.disband();
        }else {
            team.removePlayer(player);
        }
    }


    public Team getTeamByPlayer(@NotNull Player player) {
        for (Team team : this.teams.values()) {
            if (team.getPlayers().contains(player.getName())) {
                return team;
            }
        }
        return null;
    }
}
