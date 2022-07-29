package cn.lanink.teamsystem.utils;

import cn.lanink.gamecore.form.element.ResponseElementButton;
import cn.lanink.gamecore.form.windows.AdvancedFormWindowCustom;
import cn.lanink.gamecore.form.windows.AdvancedFormWindowModal;
import cn.lanink.gamecore.form.windows.AdvancedFormWindowSimple;
import cn.lanink.gamecore.utils.Language;
import cn.lanink.teamsystem.Team;
import cn.lanink.teamsystem.TeamSystem;
import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.form.element.ElementButton;
import cn.nukkit.form.element.ElementDropdown;
import cn.nukkit.form.element.ElementInput;
import cn.nukkit.form.element.ElementLabel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author lt_name
 */
public class FormHelper {

    private FormHelper() {
        throw new RuntimeException("FormCreate类不允许实例化");
    }

    public static void showMain(@NotNull Player player) {
        Language language = TeamSystem.getInstance().getLanguage();
        AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple(language.translateString("form.main.title"));
        if (TeamSystem.getInstance().getTeamByPlayer(player) == null) {
            simple.addButton(new ResponseElementButton(language.translateString("form.main.button.createTeam"))
                    .onClicked(FormHelper::showCreateTeam));
            simple.addButton(new ResponseElementButton(language.translateString("form.main.button.joinTeam"))
                    .onClicked(FormHelper::showJoinTeam));
        }else {
            simple.addButton(new ResponseElementButton(language.translateString("form.main.button.myTeam"))
                    .onClicked(FormHelper::showMyTeam));
            simple.addButton(new ResponseElementButton(language.translateString("form.main.button.quitTeam"))
                    .onClicked((p) -> showQuitTeamConfirm(null, p)));
        }
        player.showFormWindow(simple);
    }

    /**
     * 创建队伍页面
     *
     * @param player 打开GUI的玩家
     */
    public static void showCreateTeam(@NotNull Player player) {
        Language language = TeamSystem.getInstance().getLanguage();
        AdvancedFormWindowCustom custom = new AdvancedFormWindowCustom(language.translateString("form.create.title"));
        int id;
        do {
            id = TeamSystem.RANDOM.nextInt(Math.min(99999, Server.getInstance().getMaxPlayers()*3));  // 假定为 3 个服务器的最大人数
        }while (TeamSystem.getInstance().getTeams().containsKey(id));
        final int finalId = id;
        custom.addElement(new ElementLabel(language.translateString("general.teamID") +": " + finalId));
        custom.addElement(new ElementInput(language.translateString("general.teamName"), language.translateString("general.teamName") +": ", finalId + ""));
        custom.addElement(new ElementDropdown(language.translateString("general.teamSize"), Arrays.asList("2", "3", "4", "5")));

        custom.onResponded(((formResponseCustom, p) -> {
            TeamSystem.getInstance().createTeam(
                    finalId,
                    formResponseCustom.getInputResponse(1),
                    Integer.parseInt(formResponseCustom.getDropdownResponse(2).getElementContent()),
                    player
            );
            showMyTeam(player);
        }));
        custom.onClosed(FormHelper::showMain);
        player.showFormWindow(custom);
    }

    public static void showJoinTeam(@NotNull Player player) {
        Language language = TeamSystem.getInstance().getLanguage();
        AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple(language.translateString("form.join.title"));
        simple.addButton(new ResponseElementButton(language.translateString("form.join.button.searchTeam"))
                .onClicked(FormHelper::showFindTeam));
        simple.addButton(new ResponseElementButton(language.translateString("form.join.button.teamsList"))
                .onClicked(FormHelper::showTeamList));
        simple.onClosed(FormHelper::showMain);
        player.showFormWindow(simple);
    }

    public static void showMyTeam(@NotNull Player player) {
        showTeamInfo(TeamSystem.getInstance().getTeamByPlayer(player), player);
    }

    /**
     * 显示队伍信息
     *
     * @param team 队伍
     * @param player 打开GUI的玩家
     */
    public static void showTeamInfo(@NotNull Team team, @NotNull Player player) {
        Language language = TeamSystem.getInstance().getLanguage();
        AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple(language.translateString("form.info.title"));
        StringBuilder content = new StringBuilder();
        content.append(language.translateString("general.teamID")).append(": ").append(team.getId()).append("\n")
                .append(language.translateString("general.teamName")).append(": ").append(team.getName()).append("\n")
                .append(language.translateString("general.teamSize")).append(" ").append(team.getMaxPlayers()).append("\n")
                .append(language.translateString("general.leader")).append(" ").append(team.getTeamLeader().getName()).append("\n")
                .append(language.translateString("general.teammates")).append(" ");
        if (team.getPlayers().size() > 1) {
            content.append("\n");
            for (String p : team.getPlayers()) {
                if (!team.getTeamLeader().getName().equals(p)) {
                    content.append(p).append("\n");
                }
            }
        }else {
            content.append(language.translateString("general.empty"));
        }
        content.append("\n\n");
        simple.setContent(content.toString());

        if (team.getPlayers().contains(player.getName())) {
            if (team.getTeamLeader() == player) {
                simple.addButton(new ResponseElementButton(language.translateString("form.info.button.transfer"))
                        .onClicked((p) -> showTeamLeaderTransfer(team, p)));
                simple.addButton(new ResponseElementButton(language.translateString("form.info.button.checkApplications"))
                        .onClicked(p -> showTeamApplicationList(team, p)));
            }
            simple.addButton(new ResponseElementButton(language.translateString("form.info.button.teleport"))
                    .onClicked((p) -> showFindTeamPlayers(team,p)));
        }else if (team.getPlayers().size() < team.getMaxPlayers()) {
            simple.addButton(new ResponseElementButton(language.translateString("form.info.button.sendRequest")+"\n\n")
                    .onClicked((p) -> {
                        team.applyFrom(p);
                        team.getTeamLeader().sendMessage(language.translateString("tips.teamReceiveApplication"));
                        final AdvancedFormWindowSimple simple1 = new AdvancedFormWindowSimple(language.translateString("tips.requestApproved"));
                        simple1.setContent(language.translateString("form.info.sendApplicationSuccess", team.getName())+"\n\n");
                        simple1.addButton(new ElementButton(language.translateString("general.confirm")));
                        p.showFormWindow(simple1);
                    }));
        }else {
            simple.addButton(new ElementButton(language.translateString("tips.teamFull")));
        }
        player.showFormWindow(simple);
    }

    /**
     * 队长转让
     *
     * @param team 队伍
     * @param player 打开GUI的玩家
     */
    public static void showTeamLeaderTransfer(@NotNull Team team, @NotNull Player player) {
        Language language = TeamSystem.getInstance().getLanguage();
        if (team.getTeamLeader() != player) {
            AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple(language.translateString("general.error"));
            simple.setContent(language.translateString("tips.transfer_noPermission")+"\n\n");
            simple.addButton(new ResponseElementButton(language.translateString("general.return"))
                    .onClicked(FormHelper::showFindTeam));
            player.showFormWindow(simple);
            return;
        }
        AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple(language.translateString("form.transfer.title"));
        if (team.getPlayers().size() > 1) {
            simple.setContent(language.translateString("form.transfer.content.description"));
            for (String p : team.getPlayers()) {
                if (!team.getTeamLeader().getName().equals(p)) {
                    simple.addButton(new ResponseElementButton(p)
                            .onClicked(clickedPlayer -> {
                                team.setTeamLeader(Server.getInstance().getPlayer(p));
                                AdvancedFormWindowSimple successfulTransfer = new AdvancedFormWindowSimple(language.translateString("form.transfer.success.title"));
                                successfulTransfer.setContent(language.translateString("form.transfer.success.content", p)+"\n\n");
                                successfulTransfer.addButton(new ResponseElementButton(language.translateString("general.return"))
                                        .onClicked(cp -> showTeamInfo(team, cp)));
                                clickedPlayer.showFormWindow(successfulTransfer);
                            }));
                }
            }
        }else {
            simple.setContent(language.translateString("form.transfer.content.noPerson")+"\n\n");
        }
        simple.addButton(new ResponseElementButton(language.translateString("general.return"))
                .onClicked(p -> showTeamInfo(team, p)));
        player.showFormWindow(simple);
    }

    /**
     * 入队申请界面
     *
     * @param team 队伍
     * @param player 打开GUI的玩家
     */
    public static void showTeamApplicationList(@NotNull Team team, @NotNull Player player) {
        Language language = TeamSystem.getInstance().getLanguage();
        if (team.getTeamLeader() != player) {
            AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple(language.translateString("general.error"));
            simple.setContent(language.translateString("form.application.noPermission")+"\n\n");
            simple.addButton(new ResponseElementButton(language.translateString("general.return"))
                    .onClicked(FormHelper::showFindTeam));
            player.showFormWindow(simple);
            return;
        }
        AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple(language.translateString("form.application.title"));
        if (team.getApplicationList().isEmpty()) {
            simple.setContent(language.translateString("form.application.empty")+"\n\n");
        }else {
            for (String p : team.getApplicationList()) {
                simple.addButton(new ResponseElementButton(p)
                        .onClicked(cp -> {
                            AdvancedFormWindowModal modal = new AdvancedFormWindowModal(
                                    language.translateString("form.application.handle.title"),
                                    language.translateString("form.application.handle.content", p),
                                    language.translateString("general.approve"),
                                    language.translateString("general.refuse"));
                            modal.onClickedTrue(cp2 -> {
                                team.cancelApplyFrom(Server.getInstance().getPlayer(p));
                                team.addPlayer(Server.getInstance().getPlayer(p));
                                showTeamApplicationList(team, cp2);
                            });
                            modal.onClickedFalse(cp2 -> {
                                team.cancelApplyFrom(Server.getInstance().getPlayer(p));
                                showTeamApplicationList(team, cp2);
                            });
                            cp.showFormWindow(modal);
                        }));
            }
        }
        simple.addButton(new ResponseElementButton(language.translateString("general.return"))
                .onClicked(p -> showTeamInfo(team, p)));
        player.showFormWindow(simple);
    }

    /**
     * 队伍传送
     *
     * @param team 队伍
     * @param player 打开GUI的玩家
     */
    public static void showFindTeamPlayers(Team team, @NotNull Player player) {
        if (team == null) {
            team = TeamSystem.getInstance().getTeamByPlayer(player);
        }
        final Team finalTeam = team;
        Language language = TeamSystem.getInstance().getLanguage();
        AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple(language.translateString("form.teleport.select.title"));
        for(String player1 : team.getPlayers()){
            if(!player.getName().equals(player1)) {
                simple.addButton(new ResponseElementButton(player1)
                        .onClicked(player2 -> {
                            player2.sendMessage(language.translateString("tips.sendTeleportRequest"));
                            AdvancedFormWindowModal modal = new AdvancedFormWindowModal(
                                    language.translateString("form.teleport.handle.title"),
                                    language.translateString("form.teleport.handle.content", player2.getName()),
                                    language.translateString("general.approve"),
                                    language.translateString("general.refuse"));
                            modal.onClickedTrue(player2::teleport);
                            modal.onClickedFalse((cp2) -> {
                                AdvancedFormWindowSimple tip = new AdvancedFormWindowSimple(language.translateString("form.teleport.handle.title"));
                                tip.setContent(language.translateString("form.teleport.refused")+"\n\n");
                                tip.addButton(new ResponseElementButton(language.translateString("general.return"))
                                        .onClicked((cp3) -> showFindTeamPlayers(finalTeam, cp3)));
                                player2.showFormWindow(tip);
                            });
                            modal.onClosed((cp2) -> {
                                AdvancedFormWindowSimple tip = new AdvancedFormWindowSimple(language.translateString("form.teleport.handle.title"));
                                tip.setContent(language.translateString("form.teleport.refused")+"\n\n");
                                tip.addButton(new ResponseElementButton(language.translateString("general.return"))
                                        .onClicked((cp3) -> showFindTeamPlayers(finalTeam, cp3)));
                                player2.showFormWindow(tip);
                            });
                            Server.getInstance().getPlayer(player1).showFormWindow(modal);
                        }));
            }
        }
        simple.addButton(new ResponseElementButton(language.translateString("general.return"))
                .onClicked(p -> showTeamInfo(finalTeam, p)));
        player.showFormWindow(simple);
    }

    public static void showQuitTeamConfirm(Team team, @NotNull Player player) {
        if (team == null) {
            team = TeamSystem.getInstance().getTeamByPlayer(player);
        }
        Language language = TeamSystem.getInstance().getLanguage();
        AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple(language.translateString("form.quit.title"));
        if (team.getTeamLeader() == player) {
            simple.setContent(language.translateString("form.quit.ownerDescription")+"\n" +
                    language.translateString("form.quit.description", team.getName()) +"\n\n");
        }else {
            simple.setContent(language.translateString("form.quit.description", team.getName()));
        }
        simple.addButton(new ResponseElementButton(language.translateString("general.confirm"))
                .onClicked((p) -> TeamSystem.getInstance().quitTeam(p)));
        simple.addButton(new ResponseElementButton(language.translateString("general.return"))
                .onClicked(FormHelper::showMain));
        player.showFormWindow(simple);
    }

    public static void showFindTeam(@NotNull Player player) {
        Language language = TeamSystem.getInstance().getLanguage();
        AdvancedFormWindowCustom custom = new AdvancedFormWindowCustom(language.translateString("form.search.title"));
        custom.addElement(new ElementDropdown(language.translateString("form.search.dropdown.text"), Arrays.asList(language.translateString("general.teamID"), language.translateString("general.teamName"), language.translateString("form.search.dropdown.teamMemberName"))));
        custom.addElement(new ElementInput(language.translateString("form.search.input.title"), language.translateString("form.search.input.placeHolder")));
        custom.onResponded((formResponseCustom, p) -> {
            String input = formResponseCustom.getInputResponse(1);
            if (input == null || "".equals(input.trim())) {
                AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple(language.translateString("general.error"));
                simple.setContent(language.translateString("form.search.emptyParameterTip")+"\n\n");
                simple.addButton(new ResponseElementButton(language.translateString("general.return"))
                        .onClicked(FormHelper::showFindTeam));
                p.showFormWindow(simple);
                return;
            }
            switch (formResponseCustom.getDropdownResponse(0).getElementID()) {
                case 0:
                    //队伍ID
                    try {
                        int id = Integer.parseInt(input);
                        Team team = TeamSystem.getInstance().getTeams().get(id);
                        if (team == null) {
                            AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple(language.translateString("form.error.search.fail.title"));
                            simple.setContent(language.translateString("form.error.search.notFoundByID.content", id)+"\n\n");
                            simple.addButton(new ResponseElementButton(language.translateString("general.return"))
                                    .onClicked(FormHelper::showFindTeam));
                            p.showFormWindow(simple);
                            return;
                        }else {
                            showTeamInfo(team, p);
                        }
                    } catch (Exception e) {
                        AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple(language.translateString("general.error"));
                        simple.setContent(language.translateString("form.error.search.formatError.content")+"\n\n");
                        simple.addButton(new ResponseElementButton(language.translateString("general.return"))
                                .onClicked(FormHelper::showFindTeam));
                        p.showFormWindow(simple);
                        return;
                    }
                    break;
                case 1:
                    //队伍名称
                    for (Team team : TeamSystem.getInstance().getTeams().values()) {
                        if (team.getName().equalsIgnoreCase(input)) {
                            showTeamInfo(team, p);
                            return;
                        }
                    }
                    AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple(language.translateString("form.error.search.fail.title"));
                    simple.setContent(language.translateString("form.error.search.notFoundByName.content", input)+"\n\n");
                    simple.addButton(new ResponseElementButton(language.translateString("general.return"))
                            .onClicked(FormHelper::showFindTeam));
                    p.showFormWindow(simple);
                    break;
                case 2:
                default:
                    //队长/队员名称
                    Player findPlayer = Server.getInstance().getPlayer(input);
                    if (findPlayer == null) {
                        AdvancedFormWindowSimple findFailed = new AdvancedFormWindowSimple(language.translateString("form.error.search.fail.title"));
                        findFailed.setContent(language.translateString("form.error.search.playerNotFound", input)+"\n\n");
                        findFailed.addButton(new ResponseElementButton(language.translateString("general.return"))
                                .onClicked(FormHelper::showFindTeam));
                        p.showFormWindow(findFailed);
                        return;
                    }
                    Team findTeam = TeamSystem.getInstance().getTeamByPlayer(findPlayer);
                    if (findTeam == null) {
                        AdvancedFormWindowSimple findFailed = new AdvancedFormWindowSimple(language.translateString("form.error.search.fail.title"));
                        findFailed.setContent(language.translateString("form.error.search.playerHasNoTeam", input)+"\n\n");
                        findFailed.addButton(new ResponseElementButton(language.translateString("general.return"))
                                .onClicked(FormHelper::showFindTeam));
                        p.showFormWindow(findFailed);
                    }else {
                        showTeamInfo(findTeam, p);
                    }
                    break;
            }
        });
        player.showFormWindow(custom);
    }

    public static void showTeamList(@NotNull Player player) {
        showTeamList(player, 0);
    }

    public static void showTeamList(@NotNull Player player, int index) {
        Language language = TeamSystem.getInstance().getLanguage();
        AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple(language.translateString("form.list.title"));
        ArrayList<Team> list = new ArrayList<>(TeamSystem.getInstance().getTeams().values());
        if (list.isEmpty()) {
            simple.setContent(language.translateString("form.list.emptyContent")+"\n\n");
        }
        if (index > 1) {
            simple.addButton(new ResponseElementButton(language.translateString("general.page.back"))
                    .onClicked(p -> showTeamList(p, Math.max(0, index - 1))));
        }
        int start = index * 10; //一页显示10个
        for (int i=0; i < 10; i++) {
            if (start >= list.size()) {
                break;
            }
            Team team = list.get(start);
            simple.addButton(new ResponseElementButton("ID:" + team.getId() + "\n"+language.translateString("general.teamName")+":" + team.getName())
                    .onClicked(p -> showTeamInfo(team, p)));
            start++;
        }
        if (start < list.size()) {
            simple.addButton(new ResponseElementButton(language.translateString("general.page.next"))
                    .onClicked(p -> showTeamList(p, index + 1)));
        }
        simple.addButton(new ResponseElementButton(language.translateString("general.return")).onClicked(FormHelper::showJoinTeam));
        player.showFormWindow(simple);
    }

}
