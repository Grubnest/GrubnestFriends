package com.grubnest.game.friends.paper.commands.friend;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.grubnest.game.core.databasehandler.utils.DataUtils;
import com.grubnest.game.friends.paper.FriendsBukkitPlugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
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

import java.util.*;

/**
 * The FriendGUI class opens a GUI to the player, showing their friends activity statuses
 *
 * @author NevaZyo
 * @version 1.0 at 06/01/2022
 */
public class FriendGUI implements Listener {

    /**
     * The owner's UUID
     */
    private final UUID playerUUID;

    /**
     * The size (in rows) the GUI should be to display the heads, keep in mind that one extra row will be added for the page-navigation items
     * This cannot be less than 1 or greater than 5
     */
    private int numberOfRows;

    /**
     * The GUI's inventory
     */
    private Inventory gui;

    /**
     * When exceeding the limit of heads per page, the current page will be used to store the index of the page that is currently displayed
     */
    private int currentPage;


    /**
     * The inside-list stores the UUIDs of the friends currently being displayed
     * The wrapping list stores all the pages created
     */
    private final List<List<UUID>> pages;


    /**
     * This list contains the names of the servers the player's currently displayed friends (in the GUI) are playing on
     * It is stored in the same order as their UUIDs in the page list
     */
    private List<String> currentPageServers;


    /**
     * The glass-pane item shown to the player, which does nothing
     */
    private ItemStack glass;

    /**
     * This item allows the player to display the previous page in the GUI. It's hidden if the player is viewing the first page.
     */
    private ItemStack previousPage;


    /**
     * This item allows the player to display the next page in the GUI. It's hidden if the player is viewing the last page.
     */
    private ItemStack nextPage;

    /**
     * Sets up a GUI displaying the player's friends, allowing him to join their server
     *
     * @param playerUUID   the UUID of the player you want to open the GUI for
     * @param friendsUUIDs a list containing the player's friends UUIDs
     */
    public FriendGUI(UUID playerUUID, List<UUID> friendsUUIDs) {

        this.playerUUID = playerUUID;
        int maxHeadsPerPage = 45;

        pages = new ArrayList<>();
        List<UUID> page = new ArrayList<>();

        int i = 0;
        for (UUID uuid : friendsUUIDs) {
            if (i == maxHeadsPerPage) {
                i = 0;
                pages.add(page);
                page = new ArrayList<>();
            }

            page.add(uuid);
            i++;
        }
        if (i != maxHeadsPerPage) {
            pages.add(page);
        }

        this.numberOfRows = (int) Math.ceil(((double) pages.get(0).size()) / 9);
        if (numberOfRows > 5) {
            numberOfRows = 5;
        }
        makeGUI();
    }

    /**
     * Sets the contents of the GUI, you may want to use requestPage() instead
     *
     * @param pageIndex the page's index
     */
    private void showPage(int pageIndex) {
        //Updating the stored index of the page currently being displayed to the player
        currentPage = pageIndex;

        final ItemStack[] contents = gui.getContents();

        //Showing the previousPage item if the page exists, otherwise showing glass
        contents[numberOfRows * 9 + 3] = pageIndex == 0 ? glass : previousPage;

        //Showing the nextPage item if the page exists, otherwise showing glass
        contents[numberOfRows * 9 + 5] = pageIndex == pages.size() - 1 ? glass : nextPage;

        //Getting the sign item
        final ItemMeta signMeta = contents[numberOfRows * 9 + 4].getItemMeta();

        //Replacing its name with the current page index
        Objects.requireNonNull(signMeta).setDisplayName("Page " + (currentPage + 1) + "/" + pages.size());
        contents[numberOfRows * 9 + 4].setItemMeta(signMeta);

        //Replacing the heads and the servers' status with the ones for the current page
        int slot = 0;
        for (UUID friendUUID : pages.get(pageIndex)) {
            ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
            SkullMeta meta = (SkullMeta) item.getItemMeta();

            Optional<String> nameOpt = DataUtils.getUsernameFromID(friendUUID);
            String name = nameOpt.orElse("NotFound");
            Objects.requireNonNull(meta).setDisplayName(name);

            meta.setOwningPlayer(Bukkit.getOfflinePlayer(friendUUID));
            String server = currentPageServers.get(slot);
            meta.setLore(Collections.singletonList(server));
            item.setItemMeta(meta);
            contents[slot] = item;
            slot++;
        }

        //Filling remaining empty slots with glass item
        for (int i = slot; i < (numberOfRows + 1) * 9; i++) {
            if (contents[i] == null || contents[i].getType() == Material.AIR || contents[i].getType() == Material.PLAYER_HEAD) {
                contents[i] = glass;
            }
        }

        //Replacing the inventory's contents with the new ones
        gui.setContents(contents);
    }

    /**
     * Sends a request to the proxy to get the names of the servers the player's friends (shown in the current GUI page) are playing on
     *
     * @param uuids the friends UUIDs
     */
    private void requestServers(List<UUID> uuids) {

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("GetServersNames");
        out.writeUTF(playerUUID.toString());
        for (UUID uuid : uuids) {
            out.writeUTF(uuid.toString());
        }

        //Handled in com.grubnest.game.friends.velocity.commands.FriendCommand:onPluginMessageReceived()
        FriendsBukkitPlugin.getInstance().getServer().sendPluginMessage(FriendsBukkitPlugin.getInstance(), "core:friendcommand", out.toByteArray());
    }

    /**
     * Sets the names of the servers of the players currently displayed in the GUI
     *
     * @param servers the new list containing the friends servers names
     */
    public void setCurrentPageServers(List<String> servers) {
        this.currentPageServers = servers;

        showPage(currentPage);
    }

    /**
     * Setting up the GUI's items
     */
    private void makeGUI() {
        Player p = Bukkit.getPlayer(playerUUID);
        if (p == null) {
            return;
        }

        gui = Bukkit.createInventory(null, (numberOfRows + 1) * 9, "Your friends");
        p.openInventory(gui);
        FriendsBukkitPlugin.getInstance().getServer().getPluginManager().registerEvents(this, FriendsBukkitPlugin.getInstance());

        this.glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta meta = glass.getItemMeta();
        Objects.requireNonNull(meta).setDisplayName(" ");
        glass.setItemMeta(meta);

        this.previousPage = new ItemStack(Material.PAPER);
        meta = previousPage.getItemMeta();
        Objects.requireNonNull(meta).setDisplayName("Previous page");
        previousPage.setItemMeta(meta);

        this.nextPage = new ItemStack(Material.PAPER);
        meta = nextPage.getItemMeta();
        Objects.requireNonNull(meta).setDisplayName("Next page");
        nextPage.setItemMeta(meta);

        final ItemStack sign = new ItemStack(Material.OAK_SIGN);

        ItemStack[] contents = gui.getContents();
        contents[numberOfRows * 9 + 4] = sign;

        gui.setContents(contents);

        requestPage(0);
    }

    /**
     * Tries to display the wanted friends page
     *
     * @param pageIndex the index of the page
     */
    private void requestPage(int pageIndex) {
        currentPage = pageIndex;
        requestServers(pages.get(pageIndex));
    }

    /**
     * GUI's management
     *
     * @param e InventoryClickEvent
     */
    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) {
        if (!e.getInventory().equals(gui)) {
            return;
        }
        e.setCancelled(true);

        final ItemStack clickedItem = e.getCurrentItem();

        if (clickedItem == null || clickedItem.getType().isAir()) {
            return;
        }

        final Player p = (Player) e.getWhoClicked();

        if (clickedItem.getType() == Material.PAPER) {
            if (Objects.requireNonNull(clickedItem.getItemMeta()).getDisplayName().toLowerCase().contains("previous"))
                requestPage(currentPage - 1);
            else
                requestPage(currentPage + 1);
            return;
        }

        if (clickedItem.getType() == Material.PLAYER_HEAD) {
            if (Objects.requireNonNull(Objects.requireNonNull(clickedItem.getItemMeta()).getLore()).get(0).toLowerCase().contains("hidden")) {
                p.sendMessage(ChatColor.RED + "This player has not added you to their friends list.");
                p.sendMessage(ChatColor.RED + "If you want to be able to join your friend's server, they have to mark you as a friend too.");
                return;
            }

            Optional<UUID> friendUUID = DataUtils.getIDFromUsername(clickedItem.getItemMeta().getDisplayName());
            friendUUID.ifPresent(uuid -> sendConnectionRequest(p, uuid));
        }
    }

    /**
     * Sends a request to the proxy to connect the player to their friend
     *
     * @param p          the player to connect
     * @param friendUUID their friend
     */
    private void sendConnectionRequest(Player p, UUID friendUUID) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF("Join");
        output.writeUTF(p.getUniqueId().toString());
        output.writeUTF(friendUUID.toString());

        //Handled in com.grubnest.game.friends.velocity.commands.FriendCommand:onPluginMessageReceived()
        p.sendPluginMessage(FriendsBukkitPlugin.getInstance(), "core:friendcommand", output.toByteArray());
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
     * Close the GUI properly and unregister the listeners
     *
     * @param e InventoryCloseEvent
     */
    @EventHandler
    public void onInventoryClose(final InventoryCloseEvent e) {
        if (!e.getInventory().equals(gui)) {
            return;
        }
        FriendMessageListener.getInstance().getOpenedGUIs().remove(this.playerUUID);
        HandlerList.unregisterAll(this);
    }

}
