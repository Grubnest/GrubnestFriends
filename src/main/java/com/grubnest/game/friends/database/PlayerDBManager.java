package com.grubnest.game.friends.database;

import com.grubnest.game.core.databasehandler.MySQL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * The PlayerDBManager interface allows you to manage the player database.
 * Each function requires a MySQL instance as parameter, which you can get from the core
 *
 * @author NevaZyo
 * @version 1.0 at 06/01/2022
 */
public interface PlayerDBManager {

    /**
     * Tries to get the name of a player stored in the server's database based on their UUID
     *
     * @param mySql MySQL connection to use for the query
     * @param uuid  the UUID of the player you want to get the name of
     * @return the player's name, empty if none could be found in the server's database
     */
    static String getUsernameFromUUID(MySQL mySql, UUID uuid) throws SQLException {
        String username = "";
        String query = """
                SELECT username
                FROM player
                WHERE uuid=?
                """;
        try (
                Connection connection = mySql.getConnection();
                PreparedStatement statement = connection.prepareStatement(query)
        ) {
            statement.setString(1, uuid.toString());
            ResultSet rows = statement.executeQuery();
            if (rows.next())
                username = rows.getString("username");
        } catch (SQLException e) {
            throw new SQLException("Error while trying to resolve a username from its UUID in player database");
        }
        return username;
    }

    /**
     * Tries to get the name of a player stored in the cache based on their UUID
     *
     * @param mySql    MySQL connection to use for the query
     * @param username the username of the player you want to get the UUID of
     * @return the player's UUID, null if none could be found in the server's database
     */
    static UUID getUUIDFromUsername(MySQL mySql, String username) throws SQLException {
        UUID uuid = null;
        String query = """
                SELECT uuid
                FROM player
                WHERE LOWER(username)=?
                """;
        try (
                Connection connection = mySql.getConnection();
                PreparedStatement statement = connection.prepareStatement(query)
        ) {
            statement.setString(1, username.toLowerCase());
            ResultSet rows = statement.executeQuery();
            if (rows.next())
                uuid = UUID.fromString(rows.getString("uuid"));
        } catch (SQLException e) {
            throw new SQLException("Error while trying to resolve a UUID from a username in player database");
        }
        return uuid;
    }
}
