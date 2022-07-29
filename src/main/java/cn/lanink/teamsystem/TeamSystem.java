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

    /**
     * 获取所有的 Team
     * 此方法适用于跨服的情况
     */
    public IntObjectHashMap<Team> getTeams() {
        return Team.TeamManager.getTeams();
    }

    public IntObjectHashMap<Team> getLocalTeams() {
        return Team.TeamManager.getLocalTeams();
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
                this.database = Dao.INSTANCE.connect(
                        (String) sqlConfig.get("host"),
                        (int) sqlConfig.get("port"),
                        (String) sqlConfig.get("database"),
                        (String) sqlConfig.get("user"),
                        (String) sqlConfig.get("password")
                );
                if (Dao.INSTANCE.checkInit()) {
                    Dao.INSTANCE.initDatabase();
                }
            } catch (Exception e) {
                this.getLogger().error(language.translateString("info.connectToDatabaseFailed"), e);
                this.database = null;
            }
        }
        this.getServer().getPluginManager().registerEvents(new EventListener(), this);
        this.getLogger().info(language.translateString("info.pluginEnabled", VERSION));
    }

    @Override
    public void onDisable() {
        this.getLocalTeams().clear();
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

    public Team createTeam(int teamId, String teamName, int maxPlayer, Player leader) {
        return Team.TeamManager.createTeam(teamId, teamName, maxPlayer, leader);
    }


    public void quitTeam(@NotNull Player player) {
        quitTeam(player.getName());
    }

    /**
     * 当需要踢出一个已经下线的玩家时
     */
    public void quitTeam(@NotNull String playerName) {
        Team team = this.getTeamByPlayer(playerName);
        if (team == null) {
            return;
        }
        if (team.isTeamLeader(playerName)) {
            team.disband();
        }else {
            team.removePlayer(playerName);
        }
    }


    public Team getTeamByPlayer(@NotNull Player player) {
        return getTeamByPlayer(player.getName());
    }

    public Team getTeamByPlayer(@NotNull String playerName) {
        for (Team team : this.getTeams().values()) {
            if (team.getPlayers().contains(playerName)) {
                return team;
            }
        }
        return null;
    }
}
