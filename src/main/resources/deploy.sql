-- auto-generated definition
create schema s_team_sys;
use s_team_sys;

create table if not exists t_team_system
(
    id          int primary key,
    team_name   varchar(512) default '' not null,
    max_players int          default 0  not null,
    team_leader varchar(512)            not null,
    constraint t_team_system_team_leader_uindex
        unique (team_leader)
);

-- auto-generated definition
create table if not exists t_online_players
(
    id          int auto_increment
        primary key,
    player_name varchar(512) default '' not null,
    of_team     int                     null,
    quit_at     datetime                null,
    constraint t_online_players_player_name_uindex
        unique (player_name)
);


-- auto-generated definition
create table t_applies
(
    id        int auto_increment
        primary key,
    player_id int not null,
    team_id   int not null,
    constraint t_applies_t_online_players_id_fk
        foreign key (player_id) references t_online_players (id),
    constraint t_applies_t_team_system_id_fk
        foreign key (team_id) references t_team_system (id)
);

alter table t_online_players
    add constraint t_online_players_t_team_system_id_fk
        foreign key (of_team) references t_team_system (id);

alter table t_team_system
    add constraint t_team_system_t_online_players_id_fk
        foreign key (team_leader) references t_online_players (player_name);

