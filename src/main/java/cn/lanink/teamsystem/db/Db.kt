package cn.lanink.teamsystem.db

import cn.lanink.teamsystem.TeamSystem
import org.ktorm.database.Database
import org.ktorm.database.asIterable

object Db {

    fun connectMysql(host: String, port: Int, database: String, user: String, password: String): Database {
        return Database.connect("jdbc:mysql://$host:$port/$database", user = user, password = password)
    }

    fun checkInit() : Boolean {
        val set = setOf("t_team_system", "t_online_players", "t_applies")
        return set.size == TeamSystem.mysqlDb?.useConnection { conn ->
            conn.prepareStatement("SHOW TABLES;").use { stmt ->
                stmt.executeQuery().asIterable().map {
                    it.getString(1)
                }.count { name ->
                    set.contains(name)
                }
            }
        }
    }

    fun initDatabase() {
        val initSQL = """
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
                create table if not exists t_applies
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
            """.trimIndent()

        TeamSystem.mysqlDb?.useConnection { conn ->
            if (conn.createStatement().apply {
                    initSQL.split(";").filterNot {
                        it.trimIndent() == ""
                    }.forEach {
                        this.addBatch(it)
                    }
                }.executeBatch().isNotEmpty()) {
                TeamSystem.logger.info(TeamSystem.language.translateString("info.initDatabaseSuccess"))
            }
        }
    }

}
