package cn.lanink.teamsystem;

import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerQuitEvent;

/**
 * @author lt_name
 */
public class EventListener implements Listener {

    private final TeamSystem teamSystem;

    public EventListener(TeamSystem teamSystem) {
        this.teamSystem = teamSystem;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        //this.teamSystem.quitTeam(event.getPlayer());
    }

}
