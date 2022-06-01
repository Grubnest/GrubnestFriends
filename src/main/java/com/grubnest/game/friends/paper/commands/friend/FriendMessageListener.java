package com.grubnest.game.friends.paper.commands.friend;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.grubnest.game.core.paper.GrubnestCorePlugin;
import com.grubnest.game.friends.database.FriendDBManager;
import com.grubnest.game.friends.paper.FriendsBukkitPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.*;

/**
 * The FriendMessageListener class is used to receive and send requests to the proxy-side of the plugin
 *
 * @author NevaZyo
 * @version 1.0
 */
public class FriendMessageListener implements PluginMessageListener {

    private static FriendMessageListener INSTANCE;
    private final HashMap<UUID, FriendGUI> guis = new HashMap<>();


    /**
     * Private constructor (singleton)
     */
    private FriendMessageListener() {
    }

    /**
     * Triggered when a message is received on the channel "core:friendcommand" using Plugin Messaging
     *
     * @param channel Channel that the message was sent through.
     * @param player  Source of the message.
     * @param message The raw message that was sent.
     */
    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        if (!channel.equals("core:friendcommand")) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);

        String subChannel = in.readUTF();
        if (subChannel.equals("MakeGUI")) {
            //Bukkit.broadcastMessage("Making GUI");
            UUID playerUUID = UUID.fromString(in.readUTF());

            List<UUID> friends = new ArrayList<>();
            try {
                friends = FriendDBManager.getFriendsUUIDs(GrubnestCorePlugin.getInstance().getMySQL(), playerUUID.toString());
            } catch (SQLException e) {
                e.printStackTrace();
            }
            /*List<UUID> friends = new ArrayList<>();
            for (int i=0; i<213; i++)
            {
                friends.add(UUID.randomUUID());
            }*/

            Player p = Bukkit.getPlayer(playerUUID);
            if (friends.isEmpty()) {
                Objects.requireNonNull(p).sendMessage("You don't have any friend, do /friend <player> to add someone to your friends list.");
                return;
            }

            guis.put(playerUUID, new FriendGUI(playerUUID, friends));
        } else if (subChannel.equals("UpdateServersNames")) {
            //Bukkit.broadcastMessage("Updating servers names");
            UUID playerUUID = UUID.fromString(in.readUTF());
            List<String> serversNames = new ArrayList<>();
            boolean valid = true;
            while (valid) {
                try {
                    String server = in.readUTF();
                    serversNames.add(server);
                } catch (Exception e) {
                    valid = false;
                }
            }
            this.getOpenedGUIs().get(playerUUID).setCurrentPageServers(serversNames);
        } else {
            FriendsBukkitPlugin.getInstance().getLogger().info("ERROR: Read an unknown category name from PluginMessaging in core:friendcommand");
        }
    }

    /**
     * Singleton getter
     *
     * @return the instance (singleton)
     */
    public static FriendMessageListener getInstance() {
        if (INSTANCE == null)
            INSTANCE = new FriendMessageListener();
        return INSTANCE;
    }


    /**
     * Basic getter
     *
     * @return a map containing every opened GUIs and their owner
     */
    public HashMap<UUID, FriendGUI> getOpenedGUIs() {
        return this.guis;
    }
}
