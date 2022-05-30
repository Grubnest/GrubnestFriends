package com.grubnest.game.friends.velocity.commands;

import com.grubnest.game.core.databasehandler.MySQL;
import com.grubnest.game.core.velocity.VelocityPlugin;
import com.grubnest.game.friends.database.FriendDBManager;
import com.grubnest.game.friends.database.PlayerDBManager;
import com.grubnest.game.friends.velocity.FriendsVelocityPlugin;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UnfriendCommand implements SimpleCommand
{

    private static UnfriendCommand INSTANCE = null;
    private final MySQL mySQL = VelocityPlugin.getInstance().getMySQL();

    /**
     * Singleton constructor
     */
    private UnfriendCommand()
    {}

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
            sender.sendMessage(Component.text("Please provide a player to remove from your friends.", TextColor.color(255, 0, 0)));
            return;
        }

        UUID toRemoveUUID = PlayerDBManager.getUUIDFromUsername(mySQL, args[0]);
        if (toRemoveUUID == null)
        {
            sender.sendMessage(Component.text("Couldn't find this player", TextColor.color(210, 184, 139)));
            return;
        }

        String[] key = new String[2];
        key[0] = sender.getUniqueId().toString();
        key[1] = toRemoveUUID.toString();

        if (!FriendDBManager.isFriendAlready(mySQL, key[0], key[1]))
        {
            sender.sendMessage(Component.text("This player isn't in your friends list.", TextColor.color(255, 0, 0)));
            return;
        }

        FriendDBManager.removeFromFriendDB(mySQL, key[0], key[1]);
        sender.sendMessage(Component.text("Removed from your friends list!", TextColor.color(100, 224, 114)));
    }

    /**
     * @param invocation the invocation context
     * @return list of suggestions, here: all the players connected on the proxy
     */
    @Override
    public List<String> suggest(Invocation invocation)
    {
        if (invocation.arguments().length == 1)
        {
            List<String> names = new ArrayList<>();
            for (Player p : FriendsVelocityPlugin.getInstance().getServer().getAllPlayers())
                names.add(p.getUsername());
            return names;
            //return new ArrayList<>(getFriendsNames(getFriendsUUIDs(sender.getUniqueId().toString())));
        }
        return SimpleCommand.super.suggest(invocation);
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

    public static UnfriendCommand getInstance()
    {
        if (INSTANCE == null)
            INSTANCE = new UnfriendCommand();
        return INSTANCE;
    }

}
