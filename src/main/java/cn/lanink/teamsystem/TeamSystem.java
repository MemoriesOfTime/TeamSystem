package cn.lanink.teamsystem;

import cn.lanink.gamecore.utils.Language;
import cn.lanink.teamsystem.utils.FormHelper;
import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.Config;
import com.google.gson.Gson;
import com.smallaswater.easysql.mysql.utils.TableType;
import com.smallaswater.easysql.mysql.utils.Types;
import com.smallaswater.easysql.mysql.utils.UserData;
import com.smallaswater.easysql.v3.mysql.manager.SqlManager;
import io.netty.util.collection.IntObjectHashMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Random;

/**
 * @author lt_name
 */
public class TeamSystem extends PluginBase {

    public static final String VERSION = "?";
    public static boolean debug = false;

    public static final Random RANDOM = new Random();
    public static final Gson GSON = new Gson();

    @Getter
    private static TeamSystem instance;

    @Getter
    private final IntObjectHashMap<Team> teams = new IntObjectHashMap<>();

    @Getter
    private SqlManager sqlManager;

    @Getter
    private Language language;

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
                this.sqlManager = new SqlManager(this,
                        new UserData(
                                (String) sqlConfig.get("user"),
                                (String) sqlConfig.get("passWorld"),
                                (String) sqlConfig.get("host"),
                                (int) sqlConfig.get("port"),
                                (String) sqlConfig.get("database")
                        )
                );
                this.sqlManager.enableWallFilter();
                if (!this.sqlManager.isExistTable("TeamSystem")) {
                    this.sqlManager.createTable("TeamSystem",
                            new TableType("id", Types.INT.setValue("primary key")),
                            new TableType("name", Types.VARCHAR),
                            new TableType("maxPlayers", Types.INT.setValue("not null")),
                            new TableType("teamLeader", Types.VARCHAR),
                            new TableType("players", Types.TEXT),
                            new TableType("applicationList", Types.TEXT.setValue(""))
                    );
                }
            } catch (Exception e) {
                this.getLogger().error(language.translateString("info.connectToDatabaseFailed"), e);
                this.sqlManager = null;
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
