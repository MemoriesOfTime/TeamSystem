package cn.lanink.teamsystem;

import cn.lanink.teamsystem.form.FormCreate;
import cn.lanink.teamsystem.form.FormListener;
import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.plugin.PluginBase;
import com.google.gson.Gson;
import io.netty.util.collection.IntObjectHashMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

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

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
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
        if (team.getTeamLeader() == player) {
            this.getTeams().remove(team.getId());
        }else {
            team.getPlayers().remove(player);
        }
    }


    public Team getTeamByPlayer(@NotNull Player player) {
        for (Team team : this.teams.values()) {
            if (team.getPlayers().contains(player)) {
                return team;
            }
        }
        return null;
    }

}
