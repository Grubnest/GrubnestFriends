package com.grubnest.game.friends.database;

import com.grubnest.game.core.databasehandler.MySQL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public interface FriendDBManager
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
                    CREATE TABLE IF NOT EXISTS `friend` (
                        player_uuid varchar(36),
                        friend_uuid varchar(36),
                        PRIMARY KEY (player_uuid, friend_uuid)
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
     * Tries to add given friendUUID to the player's friends list
     *
     * @param mySql MySQL connection to use for the query
     * @param playerUUID UUID of the player you want to add the friend
     * @param friendUUID UUID of the friend you want to add
     */
    static void markAsFriend(MySQL mySql, String playerUUID, String friendUUID)
    {
        String query = """
                INSERT INTO friend
                	(player_uuid, friend_uuid)
                VALUES
                	(?, ?)
                """;

        try {
            Connection connection = mySql.getConnection();
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, playerUUID);
            statement.setString(2, friendUUID);
            statement.executeUpdate();
            statement.close();
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    /**
     * Tries to remove given friendUUID from the player's friends list
     *
     * @param mySql MySQL connection to use for the query
     * @param playerUUID UUID of the player you want to remove the friend from
     * @param friendUUID UUID of the player's friend you want to remove
     */
    static void removeFromFriendDB(MySQL mySql, String playerUUID, String friendUUID)
    {
        String query = """
                DELETE FROM friend
                WHERE player_uuid=? AND friend_uuid=?;
                """;
        try {
            Connection connection = mySql.getConnection();
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, playerUUID);
            statement.setString(2, friendUUID);
            statement.executeUpdate();
            statement.close();
            connection.close();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    /**
     * Returns a boolean indicating if the player has marked another as a friend
     *
     * @param mySql MySQL connection to use for the query
     * @param playerUUID the player's UUID you want to check
     * @param friendUUID UUID of the player's optional friend
     * @return true if friendUUID is in the player's friends list, false otherwise
     */
    static boolean isFriendAlready(MySQL mySql, String playerUUID, String friendUUID)
    {
        String query = """
                SELECT player_uuid
                FROM friend
                WHERE player_uuid=? AND friend_uuid=?
                """;
        boolean friendAlready = false;
        try {
            Connection connection = mySql.getConnection();
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, playerUUID);
            statement.setString(2, friendUUID);
            ResultSet rows = statement.executeQuery();
            friendAlready = rows.next();
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return friendAlready;
    }

    /**
     * Tries to get the player's friends UUIDs
     *
     * @param mySql MySQL connection to use for the query
     * @param playerUUID the player's UUID
     * @return list containing the player's friends UUIDs
     */
    static List<UUID> getFriendsUUIDs(MySQL mySql, String playerUUID)
    {
        String query = """
                SELECT friend_uuid
                FROM friend
                WHERE player_uuid=?
                """;
        List<UUID> friendsUUIDs = new ArrayList<>();
        try {
            Connection connection = mySql.getConnection();
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, playerUUID);
            ResultSet rows = statement.executeQuery();
            while (rows.next())
                friendsUUIDs.add(UUID.fromString(rows.getString("friend_uuid")));
            statement.close();
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return friendsUUIDs;
    }

}
