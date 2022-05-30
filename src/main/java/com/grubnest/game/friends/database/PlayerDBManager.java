package com.grubnest.game.friends.database;

import com.grubnest.game.core.databasehandler.MySQL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public interface PlayerDBManager
{

    /**
     * Creates a new table in the database if not already created
     *
     * @param mySql MySQL connection to use for the query
     */
    static void createTable(MySQL mySql)
    {
        try {
            Connection connection = mySql.getConnection();
            PreparedStatement statement = connection.prepareStatement("""
                    CREATE TABLE IF NOT EXISTS `player` (
                        uuid varchar(36) PRIMARY KEY,
                        username varchar(16)
                    )
                    """);
            statement.executeUpdate();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Tries to get the name of a player stored in the server's database based on their UUID
     * @param mySql MySQL connection to use for the query
     * @param uuid the UUID of the player you want to get the name of
     * @return the player's name, empty if none could be found in the server's database
     */
    static String getUsernameFromUUID(MySQL mySql, UUID uuid)
    {
        String username = "";
        String query = """
                SELECT username
                FROM player
                WHERE uuid=?
                """;
        try {
            Connection connection = mySql.getConnection();
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, uuid.toString());
            ResultSet rows = statement.executeQuery();
            if (rows.next())
                username = rows.getString("username");
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return username;
    }

    /**
     * Tries to get the name of a player stored in the cache based on their UUID
     * @param mySql MySQL connection to use for the query
     * @param username the username of the player you want to get the UUID of
     * @return the player's UUID, null if none could be found in the server's database
     */
    static UUID getUUIDFromUsername(MySQL mySql, String username)
    {
        UUID uuid = null;
        String query = """
                SELECT uuid
                FROM player
                WHERE LOWER(username)=?
                """;
        try {
            Connection connection = mySql.getConnection();
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, username.toLowerCase());
            ResultSet rows = statement.executeQuery();
            if (rows.next())
                uuid = UUID.fromString(rows.getString("uuid"));
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return uuid;
    }
}
