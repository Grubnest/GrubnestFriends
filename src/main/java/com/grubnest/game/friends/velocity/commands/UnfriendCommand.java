package com.grubnest.game.friends.velocity.commands;

import com.grubnest.game.core.databasehandler.utils.DataUtils;
import com.grubnest.game.friends.database.FriendDBManager;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/**
 * The UnfriendCommand class allows the player to remove another player from their friends list
 *
 * @author NevaZyo
 * @version 1.0 at 06/01/2022
 */
public class UnfriendCommand implements SimpleCommand {

    private static UnfriendCommand INSTANCE = null;

    /**
     * Singleton constructor
     */
    private UnfriendCommand() {}

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
        if (args.length > 1) {
            sender.sendMessage(Component.text("Too many arguments"));
            return;
        }

        if (args.length == 0) {
            sender.sendMessage(Component.text("Please provide a player to remove from your friends.", TextColor.color(255, 0, 0)));
            return;
        }

        Optional<UUID> toRemoveUUIDOpt;
        toRemoveUUIDOpt = DataUtils.getIDFromUsername(args[0]);
        if (toRemoveUUIDOpt.isEmpty()) {
            sender.sendMessage(Component.text("Couldn't find this player", TextColor.color(210, 184, 139)));
            return;
        }

        String[] key = new String[2];
        key[0] = sender.getUniqueId().toString();
        key[1] = toRemoveUUIDOpt.get().toString();

        try {
            if (!FriendDBManager.isFriendAlready(key[0], key[1])) {
                sender.sendMessage(Component.text("This player isn't in your friends list.", TextColor.color(255, 0, 0)));
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        try {
            FriendDBManager.removeFromFriendDB(key[0], key[1]);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        sender.sendMessage(Component.text("Removed from your friends list!", TextColor.color(100, 224, 114)));
    }

    /**
     * @return the class instance (singleton)
     */
    public static UnfriendCommand getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new UnfriendCommand();
        }
        return INSTANCE;
    }

}
