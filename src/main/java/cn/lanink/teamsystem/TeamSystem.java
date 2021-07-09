package cn.lanink.teamsystem;

import cn.lanink.teamsystem.form.FormCreate;
import cn.lanink.teamsystem.form.FormListener;
import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.plugin.PluginBase;
import com.google.gson.Gson;
import com.smallaswater.easysql.v3.mysql.manager.SqlManager;
import com.smallaswater.easysql.v3.mysql.utils.TableType;
import com.smallaswater.easysql.v3.mysql.utils.Types;
import com.smallaswater.easysql.v3.mysql.utils.UserData;
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

    @Override
    public void onLoad() {
        instance = this;
        this.saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        if (this.getConfig().getBoolean("MySQL.enable")) {
            this.getLogger().info("§a正在尝试连接数据库，请稍后...");
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
                this.getLogger().error("数据库连接失败!", e);
                this.sqlManager = null;
            }
        }
        this.getServer().getPluginManager().registerEvents(new FormListener(), this);
        this.getServer().getPluginManager().registerEvents(new EventListener(this), this);
        this.getLogger().info("TeamSystem 加载完成！当前版本：" + VERSION);
    }

    @Override
    public void onDisable() {
        //TODO
        this.teams.clear();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ("team".equalsIgnoreCase(command.getName())) {
            if (sender instanceof Player) {
                FormCreate.showMain((Player) sender);
            }else {
                sender.sendMessage("§cLT_Name：你知道吗？TeamSystem的所有操作都是GUI，这意味你只能在游戏内使用此命令！");
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
