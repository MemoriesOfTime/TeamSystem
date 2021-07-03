package cn.lanink.teamsystem.form.responsible;

import cn.lanink.teamsystem.form.element.ResponseElementButton;
import cn.nukkit.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * @author lt_name
 */
public interface ResponsibleElementButton extends ResponsibleElement {

    ResponseElementButton onClicked(@NotNull Consumer<Player> listener);

    boolean callClicked(@NotNull Player player);

}
