package cn.lanink.teamsystem;

import cn.nukkit.Player;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;

/**
 * @author lt_name
 */
@Data
public class Team {

    private final int id;
    private String name;
    private int maxPlayers;
    private Player teamLeader;
    private final HashSet<Player> players = new HashSet<>();
    private final HashSet<Player> applicationList = new HashSet<>(); //申请列表

    public Team(int id, @NotNull String name, int maxPlayers, @NotNull Player teamLeader) {
        this.id = id;
        this.name = name;
        this.maxPlayers = maxPlayers;
        this.teamLeader = teamLeader;
        this.players.add(this.teamLeader);
    }

}
