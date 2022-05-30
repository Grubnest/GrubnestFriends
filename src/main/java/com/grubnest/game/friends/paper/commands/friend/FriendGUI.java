package com.grubnest.game.friends.paper.commands.friend;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.grubnest.game.core.GrubnestCorePlugin;
import com.grubnest.game.friends.database.PlayerDBManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class FriendGUI implements Listener
{
    private final UUID playerUUID;
    private int numberOfRows;
    private Inventory gui;
    private int currentPage;
    private final List<List<UUID>> pages;
    private List<String> currentPageServers;
    private ItemStack glass;
    private ItemStack previousPage;
    private ItemStack nextPage;

    /**
     * Sets up a GUI displaying the player's friends, allowing him to join their server
     *
     * @param playerUUID the UUID of the player you want to open the GUI for
     * @param friendsUUIDs a list containing the player's friends UUIDs
     */
    public FriendGUI(UUID playerUUID, List<UUID> friendsUUIDs)
    {

        this.playerUUID = playerUUID;
        int maxHeadsPerPage = 45;

        pages = new ArrayList<>();
        List<UUID> page = new ArrayList<>();

        int i = 0;
        for (UUID uuid : friendsUUIDs)
        {
            if (i == maxHeadsPerPage)
            {
                i = 0;
                pages.add(page);
                page = new ArrayList<>();
            }

            page.add(uuid);
            i++;
        }
        if (i != maxHeadsPerPage) pages.add(page);

        this.numberOfRows = (int) Math.ceil( ((double)pages.get(0).size()) /9);
        if (numberOfRows > 5) numberOfRows = 5;
        makeGUI();
    }

    /**
     * Sets the contents of the GUI, you may want to use requestPage() instead
     *
     * @param pageIndex the page's index
     */
    private void showPage(int pageIndex)
    {
        //Bukkit.broadcastMessage("Showing page " + pageIndex);
        currentPage = pageIndex;

        final ItemStack[] contents = gui.getContents();
        contents[numberOfRows*9 + 3] = pageIndex==0 ? glass : previousPage;
        contents[numberOfRows*9 + 5] = pageIndex==pages.size()-1 ? glass : nextPage;
        ItemMeta signMeta = contents[numberOfRows*9 + 4].getItemMeta();
        signMeta.setDisplayName("Page " + (currentPage+1) + "/" + pages.size());
        contents[numberOfRows*9 + 4].setItemMeta(signMeta);

        int slot = 0;
        for (UUID friendUUID : pages.get(pageIndex))
        {
            ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            assert meta != null;
            meta.setDisplayName(PlayerDBManager.getUsernameFromUUID(GrubnestCorePlugin.getInstance().getMySQL(), friendUUID));
            //meta.setDisplayName(friendUUID.toString());
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(friendUUID));
            String server = currentPageServers.get(slot);
            meta.setLore(Collections.singletonList(server));
            item.setItemMeta(meta);
            contents[slot] = item;
            slot++;
        }

        for (int i = slot; i<(numberOfRows+1)*9; i++)
        {
            if (contents[i] == null || contents[i].getType() == Material.AIR || contents[i].getType() == Material.PLAYER_HEAD)
                contents[i] = glass;
        }

        gui.setContents(contents);
    }

    /**
     * Sends a request to the proxy to get the names of the servers the player's friends (shown in the current GUI page) are playing on
     *
     * @param uuids the friends UUIDs
     */
    private void requestServers(List<UUID> uuids)
    {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetServersNames");
        out.writeUTF(playerUUID.toString());
        for (UUID uuid : uuids)
            out.writeUTF(uuid.toString());
        GrubnestCorePlugin.getInstance().getServer().sendPluginMessage(GrubnestCorePlugin.getInstance(), "core:friendcommand", out.toByteArray());
        //Bukkit.broadcastMessage("Request servers for UUIDs " + uuids);
    }

    /**
     * Basic setter
     *
     * @param servers new list
     */
    public void setCurrentPageServers(List<String> servers)
    {
        this.currentPageServers = servers;
        //Bukkit.broadcastMessage("Setting current page servers list");

        showPage(currentPage);
    }

    /**
     * Setting up the GUI's items
     */
    private void makeGUI()
    {
        Player p = Bukkit.getPlayer(playerUUID);
        if (p==null) return;

        gui = Bukkit.createInventory(null, (numberOfRows+1)*9, "Your friends");
        p.openInventory(gui);
        GrubnestCorePlugin.getInstance().getServer().getPluginManager().registerEvents(this, GrubnestCorePlugin.getInstance());

        this.glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        meta.setDisplayName(" ");
        glass.setItemMeta(meta);

        this.previousPage = new ItemStack(Material.PAPER);
        meta = previousPage.getItemMeta();
        meta.setDisplayName("Previous page");
        previousPage.setItemMeta(meta);

        this.nextPage = new ItemStack(Material.PAPER);
        meta = nextPage.getItemMeta();
        meta.setDisplayName("Next page");
        nextPage.setItemMeta(meta);

        final ItemStack sign = new ItemStack(Material.OAK_SIGN);

        ItemStack[] contents = gui.getContents();
        contents[numberOfRows*9 + 4] = sign;

        gui.setContents(contents);

        requestPage(0);
    }

    /**
     * Tries to display the wanted friends page
     *
     * @param pageIndex the index of the page
     */
    private void requestPage(int pageIndex)
    {
        currentPage = pageIndex;
        //Bukkit.broadcastMessage("Requesting page at index " + pageIndex);
        requestServers(pages.get(pageIndex));
    }

    /**
     * GUI's management
     *
     * @param e InventoryClickEvent
     */
    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) {
        if (!e.getInventory().equals(gui)) return;
        e.setCancelled(true);

        final ItemStack clickedItem = e.getCurrentItem();

        // verify current item is not null
        if (clickedItem == null || clickedItem.getType().isAir()) return;

        final Player p = (Player) e.getWhoClicked();

        if (clickedItem.getType() == Material.PAPER)
        {
            if (clickedItem.getItemMeta().getDisplayName().toLowerCase().contains("previous"))
                requestPage(currentPage - 1);
            else
                requestPage(currentPage + 1);
            return;
        }

        if (clickedItem.getType() == Material.PLAYER_HEAD)
        {
            if (clickedItem.getItemMeta().getLore().get(0).toLowerCase().contains("hidden"))
            {
                p.sendMessage("Error: this player has not added you to their friends list.");
                return;
            }
            sendConnectionRequest(p, PlayerDBManager.getUUIDFromUsername(GrubnestCorePlugin.getInstance().getMySQL(), clickedItem.getItemMeta().getDisplayName()));
        }
    }

    /**
     * Sends a request to the proxy to connect the player to their friend
     *
     * @param p the player to connect
     * @param friendUUID their friend
     */
    private void sendConnectionRequest(Player p, UUID friendUUID)
    {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF("Join");
        output.writeUTF(p.getUniqueId().toString());
        output.writeUTF(friendUUID.toString());
        p.sendPluginMessage(GrubnestCorePlugin.getInstance(), "core:friendcommand", output.toByteArray());
    }

    /**
     * Cancel the click
     *
     * @param e InventoryDragEvent
     */
    @EventHandler
    public void onInventoryClick(final InventoryDragEvent e) {
        if (e.getInventory().equals(gui)) {
            e.setCancelled(true);
        }
    }

    /**
     * Close the GUI properly and unregistering the listeners
     *
     * @param e InventoryCloseEvent
     */
    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent e)
    {
        if (!e.getInventory().equals(gui)) return;
        FriendMessageListener.getInstance().getOpenedGUIs().remove(this.playerUUID);
        HandlerList.unregisterAll(this);
    }

}
