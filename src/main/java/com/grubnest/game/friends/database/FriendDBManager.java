package com.grubnest.game.friends.database;

import com.grubnest.game.core.databasehandler.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The FriendDBManager interface allows you to manage the friend database.
 * Each function requires a MySQL instance as parameter, which you can get from the core
 *
 * @author NevaZyo
 * @version 1.0 at 06/01/2022
 */
public interface FriendDBManager {

    /**
     * Creates a new table in the database if not already created
     */
    static void createTable() throws SQLException {
        try (
                Connection connection = DatabaseManager.getInstance().getMySQL().getConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        CREATE TABLE IF NOT EXISTS `friend` (
                            player_uuid varchar(36),
                            friend_uuid varchar(36),
                            PRIMARY KEY (player_uuid, friend_uuid)
                        )
                        """)
        ) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException("Error while trying to create database friend table");
        }
    }

    /**
     * Tries to add given friendUUID to the player's friends list
     *
     * @param playerUUID UUID of the player you want to add the friend
     * @param friendUUID UUID of the friend you want to add
     */
    static void markAsFriend(String playerUUID, String friendUUID) throws SQLException {
        String query = """
                INSERT INTO friend
                	(player_uuid, friend_uuid)
                VALUES
                	(?, ?)
                """;

        try (
                Connection connection = DatabaseManager.getInstance().getMySQL().getConnection();
                PreparedStatement statement = connection.prepareStatement(query)
        ) {
            statement.setString(1, playerUUID);
            statement.setString(2, friendUUID);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException("Error while trying to mark a player as friend in the database");
        }
    }

    /**
     * Tries to remove given friendUUID from the player's friends list
     *
     * @param playerUUID UUID of the player you want to remove the friend from
     * @param friendUUID UUID of the player's friend you want to remove
     */
    static void removeFromFriendDB(String playerUUID, String friendUUID) throws SQLException {
        String query = """
                DELETE FROM friend
                WHERE player_uuid=? AND friend_uuid=?;
                """;

        try (
                Connection connection = DatabaseManager.getInstance().getMySQL().getConnection();
                PreparedStatement statement = connection.prepareStatement(query)
        ) {
            statement.setString(1, playerUUID);
            statement.setString(2, friendUUID);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException("Error while trying to remove a player from friend database");
        }
    }

    /**
     * Returns a boolean indicating if the player has marked another as a friend
     *
     * @param playerUUID the player's UUID you want to check
     * @param friendUUID UUID of the player's optional friend
     * @return true if friendUUID is in the player's friends list, false otherwise
     */
    static boolean isFriendAlready(String playerUUID, String friendUUID) throws SQLException {
        String query = """
                SELECT player_uuid
                FROM friend
                WHERE player_uuid=? AND friend_uuid=?
                """;

        boolean friendAlready;
        try (
                Connection connection = DatabaseManager.getInstance().getMySQL().getConnection();
                PreparedStatement statement = connection.prepareStatement(query)
        ) {
            statement.setString(1, playerUUID);
            statement.setString(2, friendUUID);
            ResultSet rows = statement.executeQuery();
            friendAlready = rows.next();
        } catch (SQLException e) {
            throw new SQLException("Error while trying to check if a player is friend with another");
        }
        return friendAlready;
    }

    /**
     * Tries to get the player's friends UUIDs
     *
     * @param playerUUID the player's UUID
     * @return list containing the player's friends UUIDs
     */
    static Optional<List<UUID>> getFriendsUUIDs(String playerUUID) throws SQLException {
        String query = """
                SELECT friend_uuid
                FROM friend
                WHERE player_uuid=?
                """;
        List<UUID> friendsUUIDs = null;
        try (
                Connection connection = DatabaseManager.getInstance().getMySQL().getConnection();
                PreparedStatement statement = connection.prepareStatement(query)
        ) {
            statement.setString(1, playerUUID);
            ResultSet rows = statement.executeQuery();
            while (rows.next()) {
                if (friendsUUIDs == null) {
                    friendsUUIDs = new ArrayList<>();
                }
                friendsUUIDs.add(UUID.fromString(rows.getString("friend_uuid")));
            }
        } catch (SQLException e) {
            throw new SQLException("Error while trying to get the friends list of a player");
        }
        return Optional.ofNullable(friendsUUIDs);
    }

}
