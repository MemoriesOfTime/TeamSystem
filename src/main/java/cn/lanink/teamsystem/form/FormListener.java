package cn.lanink.teamsystem.form;

import cn.lanink.teamsystem.form.windows.AdvancedFormWindowCustom;
import cn.lanink.teamsystem.form.windows.AdvancedFormWindowModal;
import cn.lanink.teamsystem.form.windows.AdvancedFormWindowSimple;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerFormRespondedEvent;

/**
 * @author lt_name
 */
public class FormListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onResponded(PlayerFormRespondedEvent event) {
        if (AdvancedFormWindowSimple.onEvent(event.getWindow(), event.getResponse(), event.getPlayer())) {
            return;
        }
        if (AdvancedFormWindowModal.onEvent(event.getWindow(), event.getResponse(), event.getPlayer())) {
            return;
        }
        AdvancedFormWindowCustom.onEvent(event.getWindow(), event.getResponse(), event.getPlayer());
    }

}
