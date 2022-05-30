package com.grubnest.game.friends.paper;

import com.grubnest.game.friends.paper.commands.friend.FriendMessageListener;
import org.bukkit.plugin.java.JavaPlugin;

public class FriendsBukkitPlugin extends JavaPlugin
{
    /**
     * Runs when plugin is enabled
     */
    @Override
    public void onEnable()
    {
        //Register Plugin Messaging channels
        this.getServer().getMessenger().registerOutgoingPluginChannel(this, "core:friendcommand");
        this.getServer().getMessenger().registerIncomingPluginChannel(this, "core:friendcommand", FriendMessageListener.getInstance());

        getServer().getLogger().info("GrubnestFriends is enabled on Bukkit!");
        loadConfig();
    }

    /**
     * Loads the config and enables copying defaults
     */
    public void loadConfig() {
        getConfig().options().copyDefaults(true);
        saveConfig();
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
}
