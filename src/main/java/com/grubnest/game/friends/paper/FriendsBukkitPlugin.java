package com.grubnest.game.friends.paper;

import com.grubnest.game.friends.paper.commands.friend.FriendMessageListener;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class FriendsBukkitPlugin extends JavaPlugin
{
    private static FriendsBukkitPlugin instance;

    /**
     * Runs when plugin is enabled
     */
    @Override
    public void onEnable()
    {
        instance = this;

        //Register Plugin Messaging channels
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "core:friendcommand");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "core:friendcommand", FriendMessageListener.getInstance());

        getServer().getConsoleSender().sendMessage(ChatColor.AQUA + "GrubnestFriends is enabled");
    }

    /**
     * Runs when plugin is disabled
     */
    @Override
    public void onDisable()
    {
        //Unregister channels on disable
        this.getServer().getMessenger().unregisterOutgoingPluginChannel(this);
        this.getServer().getMessenger().unregisterIncomingPluginChannel(this);
    }

    public static FriendsBukkitPlugin getInstance()
    {
        return instance;
    }
}
