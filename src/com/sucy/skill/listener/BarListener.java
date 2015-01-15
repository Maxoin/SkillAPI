package com.sucy.skill.listener;

import com.sucy.skill.SkillAPI;
import com.sucy.skill.api.event.PlayerClassChangeEvent;
import com.sucy.skill.api.event.PlayerSkillDowngradeEvent;
import com.sucy.skill.api.event.PlayerSkillUnlockEvent;
import com.sucy.skill.api.event.PlayerSkillUpgradeEvent;
import com.sucy.skill.api.player.PlayerData;
import com.sucy.skill.api.player.PlayerSkillBar;
import com.sucy.skill.api.skills.Skill;
import com.sucy.skill.api.skills.SkillShot;
import com.sucy.skill.api.skills.TargetSkill;
import com.sucy.skill.dynamic.DynamicSkill;
import com.sucy.skill.tree.SkillTree;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.*;

/**
 * Handles interactions with skill bars. This shouldn't be
 * use by other plugins as it is handled by the API.
 */
public class BarListener implements Listener
{
    private final SkillAPI plugin;

    /**
     * Initializes a new BarListener. Do not use this constructor as
     * the API handles it already.
     *
     * @param plugin plugin
     */
    public BarListener(SkillAPI plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Sets up skill bars on joining
     *
     * @param event event details
     */
    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        PlayerData data = SkillAPI.getPlayerData(event.getPlayer());
        if (data.hasClass()) {
            data.getSkillBar().setup(event.getPlayer());
        }
    }

    /**
     * Clears skill bars upon quitting the game
     *
     * @param event event details
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        PlayerData data = SkillAPI.getPlayerData(event.getPlayer());
        if (data.hasClass()) {
            data.getSkillBar().clear(event.getPlayer());
        }
    }

    /**
     * Manages setting up and clearing the skill bar when a player changes professions
     *
     * @param event event details
     */
    @EventHandler
    public void onProfess(PlayerClassChangeEvent event) {

        Player p = event.getPlayerData().getPlayer();

        // Professing as a first class sets up the skill bar
        if (event.getPreviousClass() == null && event.getNewClass() != null) {
            PlayerSkillBar bar = event.getPlayerData().getSkillBar();
            if (!bar.isSetup()) bar.setup(p);
        }

        // Resetting your class clears the skill bar
        else if (event.getPreviousClass() != null && event.getNewClass() == null) {
            PlayerSkillBar bar = event.getPlayerData().getSkillBar();
            bar.reset();
            bar.clear(p);
            bar.update(p);
        }
    }

    /**
     * Adds unlocked skills to the skill bar if applicable
     *
     * @param event event details
     */
    @EventHandler
    public void onUnlock(PlayerSkillUnlockEvent event) {
        if (event.getUnlockedSkill().getData() instanceof DynamicSkill && !((DynamicSkill) event.getUnlockedSkill().getData()).canCast()) return;
        if (!(event.getUnlockedSkill().getData() instanceof TargetSkill) && !(event.getUnlockedSkill().getData() instanceof SkillShot)) return;
        SkillAPI.getPlayerData(event.getPlayerData().getPlayer()).getSkillBar().unlock(event.getUnlockedSkill());
    }

    /**
     * Updates the skill bar when a skill is upgraded
     *
     * @param event event details
     */
    @EventHandler
    public void onUpgrade(PlayerSkillUpgradeEvent event) {
        final Player player = event.getPlayerData().getPlayer();
        Bukkit.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
            @Override
            public void run() {
                SkillAPI.getPlayerData(player).getSkillBar().update(player);
            }
        }, 0);
    }

    /**
     * Updates a player's skill bar when downgrading a skill to level 0
     *
     * @param event event details
     */
    @EventHandler
    public void onDowngrade(PlayerSkillDowngradeEvent event) {
        SkillAPI.getPlayerData(event.getPlayerData().getPlayer()).getSkillBar().update(event.getPlayerData().getPlayer());
    }

    /**
     * Clears the skill bar on death
     *
     * @param event event details
     */
    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        PlayerData data = SkillAPI.getPlayerData(event.getEntity());
        if (data.hasClass()) {
            data.getSkillBar().clear(event);
        }
    }

    /**
     * Sets the skill bar back up on respawn
     *
     * @param event event details
     */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        PlayerData data = SkillAPI.getPlayerData(event.getPlayer());
        if (data.hasClass()) {
            data.getSkillBar().setup(event.getPlayer());
            data.getSkillBar().update(event.getPlayer());
        }
    }

    /**
     * Event for assigning skills to the skill bar
     *
     * @param event event details
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onAssign(InventoryClickEvent event) {

        // Players without a class aren't effected
        PlayerData data = SkillAPI.getPlayerData((Player)event.getWhoClicked());
        if (!data.hasClass()) {
            return;
        }

        // Disabled skill bars aren't affected either
        final PlayerSkillBar skillBar = data.getSkillBar();
        if (!skillBar.isEnabled()) return;

        // Prevent moving skill icons
        if (event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD) {
            if (!skillBar.isWeaponSlot(event.getHotbarButton())) {
                event.setCancelled(true);
            }
        }
        else if (event.getSlotType() == InventoryType.SlotType.QUICKBAR) {
            int slot = event.getSlot();
            if (slot < 9 && slot >= 0) {
                if (!skillBar.isWeaponSlot(slot)) {
                    event.setCancelled(true);
                }
                if (event.getClick() == ClickType.RIGHT) {
                    if (!skillBar.isWeaponSlot(slot) || (skillBar.isWeaponSlot(slot) && (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR))) {
                        event.setCancelled(true);
                        skillBar.toggleSlot(slot);
                    }
                }
            }
        }

        // Make sure it's the right type of click action
        if (event.getAction() != InventoryAction.HOTBAR_MOVE_AND_READD && event.getAction() != InventoryAction.HOTBAR_SWAP) return;

        // Must be a skill tree
        if (event.getInventory().getHolder() instanceof SkillTree) {
            SkillTree tree = (SkillTree)event.getInventory().getHolder();

            // Must be hovering over a skill
            if (tree.isSkill(event.getWhoClicked(), event.getRawSlot())) {
                Skill skill = tree.getSkill(event.getRawSlot());

                // Must be an active skill
                if ((skill instanceof DynamicSkill && ((DynamicSkill)skill).canCast())
                        || (!(skill instanceof DynamicSkill) && (skill instanceof TargetSkill || skill instanceof SkillShot))) {

                    // Assign the skill if the player has it
                    if (data.hasSkill(skill.getName())) {
                        skillBar.assign(data.getSkill(skill.getName()), event.getHotbarButton());
                    }
                }
            }
        }
    }

    /**
     * Applies skill bar actions when pressing the number keys
     *
     * @param event event details
     */
    @EventHandler
    public void onCast(PlayerItemHeldEvent event) {
        PlayerData data = SkillAPI.getPlayerData(event.getPlayer());
        if (!data.hasClass()) return;

        PlayerSkillBar bar = data.getSkillBar();
        if (!bar.isWeaponSlot(event.getNewSlot()) && bar.isEnabled()) {
            event.setCancelled(true);
            bar.apply(event.getNewSlot());
        }
    }

    /**
     * Clears or sets up the skill bar upon changing from or to creative mode
     *
     * @param event event details
     */
    @EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChangeMode(PlayerGameModeChangeEvent event) {

        // Clear on entering creative mode
        final PlayerData data = SkillAPI.getPlayerData(event.getPlayer());
        if (event.getNewGameMode() == GameMode.CREATIVE && data.hasClass()) {
            data.getSkillBar().clear(event.getPlayer());
        }

        // Setup on leaving creative mode
        else if (event.getPlayer().getGameMode() == GameMode.CREATIVE && data.hasClass()) {
            final Player player = event.getPlayer();
            plugin.getServer().getScheduler().runTaskLater(plugin, new Runnable() {
                @Override
                public void run() {
                    SkillAPI.getPlayerData(player).getSkillBar().setup(player);
                }
            }, 0);
        }
    }
}
