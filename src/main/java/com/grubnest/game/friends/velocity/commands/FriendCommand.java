package com.grubnest.game.friends.velocity.commands;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.grubnest.game.core.databasehandler.utils.DataUtils;
import com.grubnest.game.friends.api.FriendsAPI;
import com.grubnest.game.friends.velocity.FriendsVelocityPlugin;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * The FriendCommand class allows the player to add another player to their friends list or to display their friends status
 * The command /friend is registered on the proxy-side of the GrubnestFriends plugin,
 * and sends a request to the bukkit-side class called FriendsMessageListener (by using PluginMessaging)
 * to open a GUI to the player showing their friends' status if no argument is typed
 *
 * @author NevaZyo
 * @version 1.0 at 06/01/2022
 */
public class FriendCommand implements SimpleCommand {
    private static FriendCommand INSTANCE = null;
    private final HashMap<String, Date> cooldowns = new HashMap<>();
    private final ChannelIdentifier identifier;

    /**
     * Private constructor (singleton)
     */
    private FriendCommand() {
        this.identifier = MinecraftChannelIdentifier.from("core:friendcommand");
        FriendsVelocityPlugin.getInstance().getServer().getChannelRegistrar().register(this.identifier);
        FriendsVelocityPlugin.getInstance().getServer().getEventManager().register(FriendsVelocityPlugin.getInstance(), this);
    }

    /**
     * What should be executed
     *
     * @param invocation the invocation context
     */
    @Override
    public void execute(Invocation invocation) {

        final CommandSource source = invocation.source();
        if (!(source instanceof Player)) {
            return;
        }

        Player sender = (Player) source;

        String[] args = invocation.arguments();
        if (args.length > 1) {
            sender.sendMessage(Component.text("Too many arguments"));
            return;
        }

        if (args.length == 0) {
            makeFriendGUI(sender);
            return;
        }

        if (args[0].equalsIgnoreCase(sender.getUsername())) {
            sender.sendMessage(Component.text("Feeling lonely? Join our discord server!"));
            return;
        }

        Optional<UUID> friendUUIDOpt;
        friendUUIDOpt = DataUtils.getIDFromUsername(args[0]);
        if (friendUUIDOpt.isEmpty()) {
            sender.sendMessage(Component.text("Couldn't find this player.", TextColor.color(255, 85, 85)));
            return;
        }

        UUID friendUUID = friendUUIDOpt.get();

        String[] key = new String[2];
        key[0] = sender.getUniqueId().toString();
        key[1] = friendUUID.toString();

        try {
            if (FriendsAPI.isFriendAlready(key[0], key[1])) {
                sender.sendMessage(Component.text("You've already marked this player as a friend.", TextColor.color(255, 85, 85)));
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            FriendsAPI.markAsFriend(key[0], key[1]);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        sender.sendMessage(Component.text("Added to your friends list!", TextColor.color(85, 255, 85)));

        if (!cooldowns.containsKey(Arrays.toString(key))) {
            sendFriendNotification(sender, FriendsVelocityPlugin.getInstance().getServer().getPlayer(friendUUID));
        } else if ((new Date().getTime() - cooldowns.get(Arrays.toString(key)).getTime()) / 1000 >= 10) {
            sendFriendNotification(sender, FriendsVelocityPlugin.getInstance().getServer().getPlayer(friendUUID));
        }
    }

    /**
     * Sends a notification to the player added to the sender's friends list
     *
     * @param sender   the player that typed the command
     * @param receiver the player added as a friend by the sender
     */
    private void sendFriendNotification(Player sender, Optional<Player> receiver) {
        if (receiver.isPresent()) {
            String[] key = new String[2];
            key[0] = sender.getUniqueId().toString();
            key[1] = receiver.get().getUniqueId().toString();
            receiver.get().sendMessage(
                    Component.text(sender.getUsername(), TextColor.color(85, 255, 255))
                            .append(Component.text(" added you as a friend!", TextColor.color(0, 170, 170)))
            );
            cooldowns.put(Arrays.toString(key), new Date());
            FriendsVelocityPlugin.getInstance().getServer().getScheduler()
                    .buildTask(FriendsVelocityPlugin.getInstance(), () -> {
                        cooldowns.remove(Arrays.toString(key));
                    })
                    .delay(10L, TimeUnit.MINUTES)
                    .schedule();
        }
    }

    /**
     * Tells the server the player is on to open a GUI for the player, containing the player's friends heads with the server they are playing on
     *
     * @param player the player
     */
    private void makeFriendGUI(Player player) {
        String playerUUID = player.getUniqueId().toString();
        ByteArrayDataOutput out = ByteStreams.newDataOutput();

        out.writeUTF("MakeGUI");
        out.writeUTF(playerUUID);

        Optional<Player> pOpt = FriendsVelocityPlugin.getInstance().getServer().getPlayer(UUID.fromString(playerUUID));
        if (pOpt.isEmpty()) {
            return;
        }

        Player p = pOpt.get();
        //Handled in com.grubnest.game.friends.paper.commands.friend.FriendMessageListener:onPluginMessageReceived()
        Optional<ServerConnection> server = p.getCurrentServer();
        server.ifPresent(serverConnection -> serverConnection.sendPluginMessage(this.identifier, out.toByteArray()));
    }

    /**
     * @param invocation the invocation context
     * @return list of suggestions, here: all the players connected on the proxy
     */
    @Override
    public List<String> suggest(Invocation invocation) {

        if (invocation.arguments().length == 1) {
            List<String> players = new ArrayList<>();
            for (Player p : FriendsVelocityPlugin.getInstance().getServer().getAllPlayers())
                players.add(p.getUsername());
            return players;
        }
        return SimpleCommand.super.suggest(invocation);
    }

    /**
     * Event handler: triggered when data are received on the channel "core:friendcommand" using Plugin Messaging
     *
     * @param event PluginMessageEvent
     */
    @Subscribe
    public void onPluginMessageEvent(PluginMessageEvent event) {
        // Received plugin message, check channel identifier matches
        if (!event.getIdentifier().equals(identifier)) return;

        // Since this message was meant for this listener set it to handled
        // We do this so the message doesn't get routed through.
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        //FriendsVelocityPlugin.getInstance().getLogger().info("Received a message");

        // Important! Check the origin of the plugin message
        // Depending on what the message does its critical that a player
        // is not able to spoof it
        if (event.getSource() instanceof Player) {
            return;
        }
        // Could also instead be
        if (!(event.getSource() instanceof ServerConnection)) {
            return;
        }

        ByteArrayDataInput in = event.dataAsDataStream();
        String subChannel = in.readUTF();

        switch (subChannel) {
            case "Join" -> {
                UUID playerUUID = UUID.fromString(in.readUTF());
                Optional<Player> optPlayer = FriendsVelocityPlugin.getInstance().getServer().getPlayer(playerUUID);
                if (optPlayer.isEmpty()) {
                    return;
                }
                Player p = optPlayer.get();

                UUID friendUUID = UUID.fromString(in.readUTF());
                Optional<Player> optFriend = FriendsVelocityPlugin.getInstance().getServer().getPlayer(friendUUID);

                if (optFriend.isEmpty()) {
                    p.sendMessage(Component.text("Error: your friend is offline.", TextColor.color(255, 85, 85)));
                    return;
                }

                Player friend = optFriend.get();
                Optional<ServerConnection> optServer = friend.getCurrentServer();

                if (optServer.isEmpty()) {
                    FriendsVelocityPlugin.getInstance().getLogger().info("Error: could not find server. (FriendCommand)");
                    return;
                }

                ServerConnection friendServer = optServer.get();
                p.createConnectionRequest(friendServer.getServer()).connect();
            }
            case "GetServersNames" -> {
                UUID playerUUID = UUID.fromString(in.readUTF());

                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("UpdateServersNames");
                out.writeUTF(playerUUID.toString());

                boolean valid = true;
                while (valid) {
                    try {
                        UUID friendUUID = UUID.fromString(in.readUTF());
                        Optional<Player> friend = FriendsVelocityPlugin.getInstance().getServer().getPlayer(friendUUID);
                        boolean mutual = FriendsAPI.isFriendAlready(friendUUID.toString(), playerUUID.toString());

                        String server;

                        if (!mutual) {
                            server = "Hidden";
                        } else if (friend.isEmpty()) {
                            server = "Offline";
                        } else {
                            Optional<ServerConnection> friendServerOpt = friend.get().getCurrentServer();
                            server = friendServerOpt.isEmpty() ? "Unknown server" : friendServerOpt.get().getServerInfo().getName();
                        }

                        out.writeUTF(server);
                    } catch (Exception e) {
                        valid = false;
                    }
                }

                Optional<Player> pOpt = FriendsVelocityPlugin.getInstance().getServer().getPlayer(playerUUID);
                if (pOpt.isEmpty()) {
                    return;
                }

                Player p = pOpt.get();

                Optional<ServerConnection> serverOpt = p.getCurrentServer();
                if (serverOpt.isEmpty()) {
                    return;
                }

                //Handled in com.grubnest.game.friends.paper.commands.friend.FriendMessageListener:onPluginMessageReceived()
                serverOpt.get().sendPluginMessage(identifier, out.toByteArray());
            }
            default ->
                    FriendsVelocityPlugin.getInstance().getLogger().info("Received an unknown subchannel in core:friendcommand");
        }

    }

    /**
     * @return the instance (singleton)
     */
    public static FriendCommand getInstance() {
        if (INSTANCE == null)
            INSTANCE = new FriendCommand();
        return INSTANCE;
    }
}