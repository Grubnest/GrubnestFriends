package com.grubnest.game.friends.velocity;

import com.google.inject.Inject;
import com.grubnest.game.core.velocity.VelocityPlugin;
import com.grubnest.game.friends.database.FriendDBManager;
import com.grubnest.game.friends.database.PlayerDBManager;
import com.grubnest.game.friends.velocity.commands.FriendCommand;
import com.grubnest.game.friends.velocity.commands.UnfriendCommand;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

@Plugin(id="grubnestfriends", name="Grubnest Friends Plugin", version="0.1.0-SNAPSHOT",
        url="https://grubnest.com", description="Grubnest Friends running on Velocity", authors={"NevaZyo"})
public class FriendsVelocityPlugin
{

    private final ProxyServer server;
    private final Logger logger;
    private static FriendsVelocityPlugin instance;

    @Inject
    public FriendsVelocityPlugin(ProxyServer server, Logger logger)
    {
        this.server = server;
        this.logger = logger;
        this.logger.info("GrubnestFriends is enabled on Velocity!");
        instance = this;
    }

    /**
     * Event handler: triggered when the proxy is initializing, listeners and commands are registered here
     *
     * @param e ProxyInitializeEvent
     */
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent e)
    {
        CommandManager commandManager = server.getCommandManager();
        commandManager.register("friend", FriendCommand.getInstance());
        commandManager.register("unfriend", UnfriendCommand.getInstance());

        createTables();
    }


    /**
     * Creates all needed database tables
     */
    private void createTables()
    {
        FriendDBManager.createTable(VelocityPlugin.getInstance().getMySQL());
        PlayerDBManager.createTable(VelocityPlugin.getInstance().getMySQL());
    }

    /**
     * Get the ProxyServer object
     *
     * @return ProxyServer object
     */
    public ProxyServer getServer()
    {
        return this.server;
    }

    /**
     * Get the Logger object
     *
     * @return Logger object
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Get Plugin Instance
     *
     * @return Plugin Instance
     */
    public static FriendsVelocityPlugin getInstance()
    {
        return instance;
    }
}
