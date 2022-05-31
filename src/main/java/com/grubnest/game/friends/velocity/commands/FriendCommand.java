package com.grubnest.game.friends.velocity.commands;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.grubnest.game.core.databasehandler.MySQL;
import com.grubnest.game.core.velocity.VelocityPlugin;
import com.grubnest.game.friends.database.FriendDBManager;
import com.grubnest.game.friends.database.PlayerDBManager;
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

import java.util.*;
import java.util.concurrent.TimeUnit;

public class FriendCommand implements SimpleCommand
{
    private static FriendCommand INSTANCE = null;
    private final HashMap<String, Date> cooldowns = new HashMap<>();
    private final ChannelIdentifier identifier;
    private final VelocityPlugin plugin = (VelocityPlugin) FriendsVelocityPlugin.getInstance().getServer().getPluginManager().getPlugin("velocitycore").get().getInstance().get();

    private final MySQL mySQL = plugin.getMySQL();

    /**
     * Singleton constructor
     */
    private FriendCommand()
    {
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
        if (!(source instanceof Player)) return;

        Player sender = (Player) source;

        String[] args = invocation.arguments();
        if (args.length > 1)
        {
            sender.sendMessage(Component.text("Too many arguments"));
            return;
        }

        if (args.length == 0)
        {
            makeFriendGUI(sender);
            return;
        }

        if (args[0].equalsIgnoreCase(sender.getUsername())) {
            sender.sendMessage(Component.text("Feeling lonely? Join our discord server!"));
            return;
        }

        UUID friendUUID = PlayerDBManager.getUUIDFromUsername(mySQL, args[0]);
        if (friendUUID == null)
        {
            sender.sendMessage(Component.text("Couldn't find this player", TextColor.color(210, 184, 139)));
            return;
        }

        String[] key = new String[2];
        key[0] = sender.getUniqueId().toString();
        key[1] = friendUUID.toString();

        if (FriendDBManager.isFriendAlready(mySQL, key[0], key[1]))
        {
            sender.sendMessage(Component.text("You've already marked this player as a friend.", TextColor.color(255, 0, 0)));
            return;
        }

        FriendDBManager.markAsFriend(mySQL, key[0], key[1]);
        sender.sendMessage(Component.text("Added to your friends list!", TextColor.color(100, 224, 114)));

        if (!cooldowns.containsKey(Arrays.toString(key)))
            sendFriendNotification(sender, FriendsVelocityPlugin.getInstance().getServer().getPlayer(friendUUID));
        else if ( (new Date().getTime() - cooldowns.get(Arrays.toString(key)).getTime() )/1000 >= 10)
            sendFriendNotification(sender, FriendsVelocityPlugin.getInstance().getServer().getPlayer(friendUUID));
    }

    /**
     * Sends a notification to the player added to the sender's friends list
     *
     * @param sender the player that typed the command
     * @param receiver the player added as a friend by the sender
     */
    private void sendFriendNotification(Player sender, Optional<Player> receiver)
    {
        if (receiver.isPresent())
        {
            String[] key = new String[2];
            key[0] = sender.getUniqueId().toString();
            key[1] = receiver.get().getUniqueId().toString();
            receiver.get().sendMessage(
                    Component.text(sender.getUsername(), TextColor.color(100, 224, 195))
                            .append(Component.text(" added you as a friend!", TextColor.color(100, 224, 114)))
            );
            cooldowns.put(Arrays.toString(key), new Date());
            FriendsVelocityPlugin.getInstance().getServer().getScheduler()
                    .buildTask(FriendsVelocityPlugin.getInstance(), () -> {
                        cooldowns.remove(Arrays.toString(key));
                        sender.sendMessage(Component.text("Removed key " + Arrays.toString(key) + " from cooldown"));
                    })
                    .delay(10L, TimeUnit.SECONDS)
                    .schedule();
        }
    }

    /**
     * Tries to get the player's friends names
     *
     * @param friendsUUIDs list containing the player's friends UUIDs
     * @return list containing the player's friends names
     */
    private List<String> getFriendsNames(List<UUID> friendsUUIDs)
    {
        List<String> friendsNames = new ArrayList<>();
        for (UUID uuid : friendsUUIDs)
            friendsNames.add(PlayerDBManager.getUsernameFromUUID(mySQL, uuid));
        return friendsNames;
    }

    /**
     * Tells the server the player is on to open a GUI for the player, containing the player's friends heads with the server they are playing on
     *
     * @param player the player
     */
    private void makeFriendGUI(Player player)
    {
        String playerUUID = player.getUniqueId().toString();
        ByteArrayDataOutput out = ByteStreams.newDataOutput();

        out.writeUTF("MakeGUI");
        out.writeUTF(playerUUID);

        Player p = FriendsVelocityPlugin.getInstance().getServer().getPlayer(UUID.fromString(playerUUID)).get();
        p.getCurrentServer().get().sendPluginMessage(this.identifier, out.toByteArray());
    }

    /**
     * @param invocation the invocation context
     * @return list of suggestions, here: all the players connected on the proxy
     */
    @Override
    public List<String> suggest(Invocation invocation) {

        if (invocation.arguments().length == 1)
        {
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
    public void onPluginMessageEvent(PluginMessageEvent event)
    {
        // Received plugin message, check channel identifier matches
        if(!event.getIdentifier().equals(identifier)) return;

        // Since this message was meant for this listener set it to handled
        // We do this so the message doesn't get routed through.
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        //FriendsVelocityPlugin.getInstance().getLogger().info("Received a message");

        // Important! Check the origin of the plugin message
        // Depending on what the message does its critical that a player
        // is not able to spoof it
        if(event.getSource() instanceof Player)
        {
            return;
        }
        // Could also instead be
        if(event.getSource() instanceof ServerConnection)
        {
            ByteArrayDataInput in = event.dataAsDataStream();

            String subChannel = in.readUTF();
            if (subChannel.equals("Join"))
            {
                UUID playerUUID = UUID.fromString(in.readUTF());
                Optional<Player> optPlayer = FriendsVelocityPlugin.getInstance().getServer().getPlayer(playerUUID);
                if (optPlayer.isEmpty()) return;
                Player p = optPlayer.get();

                UUID friendUUID = UUID.fromString(in.readUTF());
                Optional<Player> optFriend = FriendsVelocityPlugin.getInstance().getServer().getPlayer(friendUUID);
                if (optFriend.isPresent())
                {
                    Player friend = optFriend.get();
                    Optional<ServerConnection> optServer = friend.getCurrentServer();
                    if (optServer.isPresent())
                    {
                        ServerConnection friendServer = optServer.get();
                        p.createConnectionRequest(friendServer.getServer()).connect();
                    }
                    else
                    {
                        FriendsVelocityPlugin.getInstance().getLogger().info("Error: could not find server. (FriendCommand)");
                    }
                }
                else
                {
                    p.sendMessage(Component.text("Error: your friend is offline."));
                }
            }
            else if (subChannel.equals("GetServersNames"))
            {
                //FriendsVelocityPlugin.getInstance().getLogger().info("Getting servers names");
                UUID playerUUID = UUID.fromString(in.readUTF());

                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("UpdateServersNames");
                out.writeUTF(playerUUID.toString());

                boolean valid = true;
                while (valid)
                {
                    try {
                        UUID friendUUID = UUID.fromString(in.readUTF());
                        Optional<Player> friend = FriendsVelocityPlugin.getInstance().getServer().getPlayer(friendUUID);

                        boolean mutual = FriendDBManager.isFriendAlready(mySQL, friendUUID.toString(), playerUUID.toString());
                        String server =
                                mutual ?
                                    friend.isPresent() ? friend.get().getCurrentServer().get().getServerInfo().getName() : "Offline"
                                : "Hidden";
                        out.writeUTF(server);
                    }catch(Exception e)
                    {
                        valid = false;
                    }
                }
                Player p = FriendsVelocityPlugin.getInstance().getServer().getPlayer(playerUUID).get();
                p.getCurrentServer().get().sendPluginMessage(identifier, out.toByteArray());
                //FriendsVelocityPlugin.getInstance().getLogger().info("Sent servers names " + Base64.getEncoder().encodeToString(out.toByteArray()));
            }
            else
            {
                FriendsVelocityPlugin.getInstance().getLogger().info("Received an unknown subchannel in core:friendcommand");
            }
        }
    }

    public static FriendCommand getInstance()
    {
        if (INSTANCE == null)
            INSTANCE = new FriendCommand();
        return INSTANCE;
    }
}
