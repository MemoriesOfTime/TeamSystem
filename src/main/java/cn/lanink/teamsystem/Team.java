package cn.lanink.teamsystem;

import cn.nukkit.Player;
import cn.nukkit.Server;
import com.smallaswater.easysql.mysql.data.SqlData;
import com.smallaswater.easysql.v3.mysql.manager.SqlManager;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * @author lt_name
 */
@Getter
public class Team {

    private final SqlManager sqlManager = TeamSystem.getInstance().getSqlManager();

    private final int id;
    private String name;
    private int maxPlayers;
    private String teamLeader;
    private final HashSet<String> players = new HashSet<>();
    private final HashSet<String> applicationList = new HashSet<>(); //申请列表

    public Team(int id, @NotNull String name, int maxPlayers, @NotNull Player teamLeader) {
        this.id = id;
        this.name = name;
        this.maxPlayers = maxPlayers;
        this.teamLeader = teamLeader.getName();
        this.players.add(teamLeader.getName());
        if (this.sqlManager != null) {
            SqlData data = new SqlData();
            data.put("id", this.id);
            data.put("name", this.name);
            data.put("maxPlayers", this.maxPlayers);
            data.put("teamLeader", this.teamLeader);
            data.put("players", setToString(this.players));
            data.put("applicationList", "");
            this.sqlManager.insertData("TeamSystem", data);
        }
    }

    public static String setToString(@NotNull Set<String> set) {
        StringBuilder builder = new StringBuilder();
        for (String string : set) {
            builder.append(string).append(";");
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }

    public boolean isTeamLeader(@NotNull Player teamLeader) {
        return this.teamLeader.equals(teamLeader.getName());
    }

    public void setTeamLeader(@NotNull Player teamLeader) {
        this.teamLeader = teamLeader.getName();
        if (this.sqlManager != null) {
            this.sqlManager.setData("TeamSystem",
                    new SqlData("teamLeader", this.teamLeader),
                    new SqlData("id", this.id));
        }
    }

    public Player getTeamLeader() {
        return Server.getInstance().getPlayer(this.teamLeader);
    }

    public void addPlayer(@NotNull Player player) {
        if (this.players.add(player.getName())) {
            if (this.sqlManager != null) {
                this.sqlManager.setData("TeamSystem",
                        new SqlData("players", setToString(this.players)),
                        new SqlData("id", this.id));
            }
        }
    }

    public void removePlayer(@NotNull Player player) {
        if (this.players.remove(player.getName())) {
            if (this.sqlManager != null) {
                this.sqlManager.setData("TeamSystem",
                        new SqlData("players", setToString(this.players)),
                        new SqlData("id", this.id));
            }
        }
    }

    public void addApplyForPlayer(@NotNull Player player) {
        if (this.applicationList.add(player.getName())) {
            if (this.sqlManager != null) {
                this.sqlManager.setData("TeamSystem",
                        new SqlData("applicationList", setToString(this.applicationList)),
                        new SqlData("id", this.id));
            }
        }
    }

    public void removeApplyForPlayer(@NotNull Player player) {
        if (this.applicationList.remove(player.getName())) {
            if (this.sqlManager != null) {
                this.sqlManager.setData("TeamSystem",
                        new SqlData("applicationList", setToString(this.applicationList)),
                        new SqlData("id", this.id));
            }
        }
    }

    /**
     * 解散队伍
     */
    public void disband() {
        //TODO 发送信息


        this.players.clear();
        this.applicationList.clear();
        if (this.sqlManager != null) {
            this.sqlManager.deleteData("TeamSystem", new SqlData("id", this.id));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Team)) {
            return false;
        }
        Team team = (Team) o;
        return id == team.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
