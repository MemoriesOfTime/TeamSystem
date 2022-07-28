package cn.lanink.teamsystem.dao

import org.ktorm.database.Database

class Dao {

    companion object {
        fun connect(host: String, port: Int, database: String, user: String, password: String) : Database {
            return Database.connect("jdbc:mysql://$host:$port/$database", user=user, password=password)
        }
    }
}