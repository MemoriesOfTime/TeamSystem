package cn.lanink.teamsystem.form;

import cn.lanink.teamsystem.Team;
import cn.lanink.teamsystem.TeamSystem;
import cn.lanink.teamsystem.form.element.ResponseElementButton;
import cn.lanink.teamsystem.form.windows.AdvancedFormWindowCustom;
import cn.lanink.teamsystem.form.windows.AdvancedFormWindowModal;
import cn.lanink.teamsystem.form.windows.AdvancedFormWindowSimple;
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
public class FormCreate {

    private FormCreate() {
        throw new RuntimeException("FormCreate类不允许实例化");
    }

    public static void showMain(@NotNull Player player) {
        AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple("组队系统");
        if (TeamSystem.getInstance().getTeamByPlayer(player) == null) {
            simple.addButton(new ResponseElementButton("创建队伍")
                    .onClicked(FormCreate::showCreateTeam));
            simple.addButton(new ResponseElementButton("加入队伍")
                    .onClicked(FormCreate::showJoinTeam));
        }else {
            simple.addButton(new ResponseElementButton("我的队伍")
                    .onClicked(FormCreate::showMyTeam));
            simple.addButton(new ResponseElementButton("退出队伍")
                    .onClicked((p) -> showQuitTeamConfirm(null, p)));
        }
        player.showFormWindow(simple);
    }

    public static void showCreateTeam(@NotNull Player player) {
        AdvancedFormWindowCustom custom = new AdvancedFormWindowCustom("创建队伍");
        int id;
        do {
            id = TeamSystem.RANDOM.nextInt(Math.min(99999, Server.getInstance().getMaxPlayers()));
        }while (TeamSystem.getInstance().getTeams().containsKey(id));
        final int finalId = id;
        custom.addElement(new ElementLabel("队伍ID：" + finalId));
        custom.addElement(new ElementInput("队伍名称", "队伍名称", finalId + ""));
        custom.addElement(new ElementDropdown("队伍规模", Arrays.asList("2人", "3人", "4人", "5人")));

        custom.onResponded(((formResponseCustom, p) -> {
            Team team = new Team(finalId,
                    formResponseCustom.getInputResponse(1),
                    formResponseCustom.getDropdownResponse(2).getElementID() + 2,
                    player);
            TeamSystem.getInstance().getTeams().put(finalId, team);
            showMyTeam(player);
        }));
        player.showFormWindow(custom);
    }

    public static void showJoinTeam(@NotNull Player player) {
        AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple("组队系统");
        simple.addButton(new ResponseElementButton("查找队伍")
                .onClicked(FormCreate::showFindTeam));
        simple.addButton(new ResponseElementButton("队伍列表")
                .onClicked(FormCreate::showTeamList));
        player.showFormWindow(simple);
    }

    public static void showMyTeam(@NotNull Player player) {
        showTeamInfo(TeamSystem.getInstance().getTeamByPlayer(player), player);
    }

    public static void showTeamInfo(@NotNull Team team, @NotNull Player player) {
        AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple("队伍信息");
        StringBuilder content = new StringBuilder();
        content.append("队伍ID：").append(team.getId()).append("\n")
                .append("队伍名称：").append(team.getName()).append("\n")
                .append("队伍规模：").append(team.getMaxPlayers()).append("人\n")
                .append("队长：").append(team.getTeamLeader().getName()).append("\n")
                .append("队员：");
        if (team.getPlayers().size() > 1) {
            content.append("\n");
            for (Player p : team.getPlayers()) {
                if (p != team.getTeamLeader()) {
                    content.append(p.getName()).append("\n");
                }
            }
        }else {
            content.append("无");
        }
        content.append("\n\n");
        simple.setContent(content.toString());

        if (team.getPlayers().contains(player)) {
            if (team.getTeamLeader() == player) {
                simple.addButton(new ResponseElementButton("队长转让")
                        .onClicked((p) -> {
                            showTeamLeaderTransfer(team, p);
                        }));
                simple.addButton(new ResponseElementButton("查看申请")
                        .onClicked(p -> showTeamApplicationList(team, p)));
            }
            simple.addButton(new ResponseElementButton("传送功能")
                    .onClicked((p) -> {
                        //TODO
                    }));
        }else if (team.getPlayers().size() < team.getMaxPlayers()) {
            simple.addButton(new ResponseElementButton("申请加入")
                    .onClicked((p) -> {
                        team.getApplicationList().add(p);
                        team.getTeamLeader().sendMessage("有玩家申请加入你的队伍，请使用/team 查看申请！");
                        final AdvancedFormWindowSimple simple1 = new AdvancedFormWindowSimple("申请成功");
                        simple1.setContent("已发送申请加入队伍 " + team.getName() + " 请等待队长同意！\n\n");
                        simple1.addButton(new ElementButton("确认"));
                        p.showFormWindow(simple1);
                    }));
        }else {
            simple.addButton(new ElementButton("队伍已满"));
        }
        player.showFormWindow(simple);
    }

    /**
     * 队长转让
     *
     * @param team 队伍
     * @param player 玩家
     */
    public static void showTeamLeaderTransfer(@NotNull Team team, @NotNull Player player) {
        if (team.getTeamLeader() != player) {
            AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple("错误");
            simple.setContent("你不是队长，无法转让队长身份！\n\n");
            simple.addButton(new ResponseElementButton("返回")
                    .onClicked(FormCreate::showFindTeam));
            player.showFormWindow(simple);
            return;
        }
        AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple("队长转让");
        if (team.getPlayers().size() > 1) {
            simple.setContent("你要转让给谁？");
            for (Player p : team.getPlayers()) {
                if (p != team.getTeamLeader()) {
                    simple.addButton(new ResponseElementButton(p.getName())
                            .onClicked(clickedPlayer -> {
                                team.setTeamLeader(p);
                                AdvancedFormWindowSimple successfulTransfer = new AdvancedFormWindowSimple("转让成功");
                                successfulTransfer.setContent("你已成功把队长身份转让给 " + p.getName() + " ！\n\n");
                                successfulTransfer.addButton(new ResponseElementButton("返回")
                                        .onClicked(cp -> showTeamInfo(team, cp)));
                                clickedPlayer.showFormWindow(successfulTransfer);
                            }));
                }
            }
        }else {
            simple.setContent("你的队伍没有其他人！你不能转让队长身份给空气！\n\n");
        }
        simple.addButton(new ResponseElementButton("返回")
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
        if (team.getTeamLeader() != player) {
            AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple("错误");
            simple.setContent("你不是队长，无法处理入队申请！\n\n");
            simple.addButton(new ResponseElementButton("返回")
                    .onClicked(FormCreate::showFindTeam));
            player.showFormWindow(simple);
            return;
        }
        AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple("申请列表");
        if (team.getApplicationList().isEmpty()) {
            simple.setContent("暂无申请\n\n");
        }else {
            for (Player p : team.getApplicationList()) {
                simple.addButton(new ResponseElementButton(p.getName())
                        .onClicked(cp -> {
                            AdvancedFormWindowModal modal = new AdvancedFormWindowModal(
                                    "入队申请",
                                    p.getName() + " 申请加入队伍",
                                    "同意",
                                    "拒绝");
                            modal.onClickedTrue(cp2 -> {
                                team.getApplicationList().remove(p);
                                team.getPlayers().add(p);
                                showTeamApplicationList(team, cp2);
                            });
                            modal.onClickedFalse(cp2 -> {
                                team.getApplicationList().remove(p);
                                showTeamApplicationList(team, cp2);
                            });
                            cp.showFormWindow(modal);
                        }));
            }
        }
        simple.addButton(new ResponseElementButton("返回")
                .onClicked(p -> showTeamInfo(team, p)));
        player.showFormWindow(simple);
    }

    public static void showQuitTeamConfirm(Team team, @NotNull Player player) {
        if (team == null) {
            team = TeamSystem.getInstance().getTeamByPlayer(player);
        }

        AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple("退出队伍？");
        if (team.getTeamLeader() == player) {
            simple.setContent("您是队伍的队长，退出队伍会解散队伍！\n" +
                    "确定要退出队伍 " + team.getName() + " 吗？\n\n");
        }else {
            simple.setContent("确定要退出队伍 " + team.getName() + " 吗？");
        }
        simple.addButton(new ResponseElementButton("确认")
                .onClicked((p) -> TeamSystem.getInstance().quitTeam(p)));
        simple.addButton(new ResponseElementButton("返回")
                .onClicked(FormCreate::showMain));
        player.showFormWindow(simple);
    }

    public static void showFindTeam(@NotNull Player player) {
        AdvancedFormWindowCustom custom = new AdvancedFormWindowCustom("查找队伍");
        custom.addElement(new ElementDropdown("根据什么参数查找?", Arrays.asList("队伍ID", "队伍名称", "队长/队员名称")));
        custom.addElement(new ElementInput("参数", "id/teamName/player"));
        custom.onResponded((formResponseCustom, p) -> {
            String input = formResponseCustom.getInputResponse(1);
            if (input == null || input.trim().equals("")) {
                AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple("错误");
                simple.setContent("参数不能为空！\n\n");
                simple.addButton(new ResponseElementButton("返回")
                        .onClicked(FormCreate::showFindTeam));
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
                            AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple("查找失败");
                            simple.setContent("没有找到队伍ID为 " + id + " 的队伍\n\n");
                            simple.addButton(new ResponseElementButton("返回")
                                    .onClicked(FormCreate::showFindTeam));
                            p.showFormWindow(simple);
                            return;
                        }else {
                            showTeamInfo(team, p);
                        }
                    } catch (Exception e) {
                        AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple("错误");
                        simple.setContent("使用队伍ID查找时请输入数字！\n\n");
                        simple.addButton(new ResponseElementButton("返回")
                                .onClicked(FormCreate::showFindTeam));
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
                    AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple("查找失败");
                    simple.setContent("没有找到队伍名称为 " + input + " 的队伍\n\n");
                    simple.addButton(new ResponseElementButton("返回")
                            .onClicked(FormCreate::showFindTeam));
                    p.showFormWindow(simple);
                    break;
                case 2:
                default:
                    //队长/队员名称
                    Player findPlayer = Server.getInstance().getPlayer(input);
                    if (findPlayer == null) {
                        AdvancedFormWindowSimple findFailed = new AdvancedFormWindowSimple("查找失败");
                        findFailed.setContent("玩家 " + input + " 不存在或者不在线！\n\n");
                        findFailed.addButton(new ResponseElementButton("返回")
                                .onClicked(FormCreate::showFindTeam));
                        p.showFormWindow(findFailed);
                        return;
                    }
                    Team findTeam = TeamSystem.getInstance().getTeamByPlayer(findPlayer);
                    if (findTeam == null) {
                        AdvancedFormWindowSimple findFailed = new AdvancedFormWindowSimple("查找失败");
                        findFailed.setContent("没有找到队长或队员是 " + input + " 的队伍\n\n");
                        findFailed.addButton(new ResponseElementButton("返回")
                                .onClicked(FormCreate::showFindTeam));
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
        AdvancedFormWindowSimple simple = new AdvancedFormWindowSimple("队伍列表");
        ArrayList<Team> list = new ArrayList<>(TeamSystem.getInstance().getTeams().values());
        if (list.isEmpty()) {
            simple.setContent("还没有人创建队伍，快去创建一个吧！\n\n");
        }
        if (index > 1) {
            simple.addButton(new ResponseElementButton("上一页")
                    .onClicked(p -> showTeamList(p, Math.min(0, index - 1))));
        }
        int start = index * 10; //一页显示10个
        for (int i=0; i < 10; i++) {
            if (start >= list.size()) {
                break;
            }
            Team team = list.get(start);
            simple.addButton(new ResponseElementButton("ID:" + team.getId() + "\n名称:" + team.getName())
                    .onClicked(p -> showTeamInfo(team, p)));
            start++;
        }
        if (start < list.size()) {
            simple.addButton(new ResponseElementButton("下一页")
                    .onClicked(p -> showTeamList(p, index + 1)));
        }
        simple.addButton(new ResponseElementButton("返回").onClicked(FormCreate::showJoinTeam));
        player.showFormWindow(simple);
    }

}
