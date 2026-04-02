package lumaris;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.EulerAngle;

import java.util.*;

@SuppressWarnings({"FieldCanBeLocal", "unused"})
public final class Main extends JavaPlugin implements Listener {
    // ------------------- CONFIG / SPAWN -------------------
    private final String WORLD_NAME = "world";
    private final double X = -97.5, Y = 178, Z = -224.5;
    private final float YAW = -180, PITCH = 1;

    // ------------------- STATE MANAGEMENT -------------------
    // Key = specific player. Value = party leader.
    private final HashMap<UUID, UUID> partyMap = new HashMap<>();
    private final HashMap<UUID, UUID> inviteMap = new HashMap<>();
    private final List<UUID> queueList = new ArrayList<>();

    private BukkitTask countdownTask = null;

    // --- TESTING LIMITS (Set to 1 and 2 for your solo/alt testing) ---
    private final int MIN_PLAYERS = 1;
    private final int MAX_PLAYERS = 6;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("Lumaris 1.21.11 Systems Enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) return true;

        // --- HUB COMMAND ---
        if (command.getName().equalsIgnoreCase("hub")) {
            World world = Bukkit.getWorld(WORLD_NAME);
            if (world != null) {
                player.teleport(new Location(world, X, Y, Z, YAW, PITCH));
                player.sendMessage("§aTeleported to Hub!");
            }
            return true;
        }

        // --- SPAWN NPC COMMAND ---
        if (command.getName().equalsIgnoreCase("spawnnpc")) {
            if (!player.isOp()) return true;
            spawnMannequin(player.getLocation());
            player.sendMessage("§aBattle Box Mannequin spawned!");
            return true;
        }

        // --- PARTY COMMAND ---
        if (command.getName().equalsIgnoreCase("party") || command.getName().equalsIgnoreCase("p")) {
            if (args.length == 0) {
                player.sendMessage("§e/party <new | add | accept/join | queue/q | leavequeue/dq | leave | list>");
                return true;
            }

            // The specific argument after /party which specifies what function to run
            String sub = args[0].toLowerCase();
            switch (sub) {
                case "new":
                    // if the player already appears in the map of people in a party
                    if (partyMap.containsKey(player.getUniqueId())) {
                        player.sendMessage("§cAlready in a party!");
                        return true;
                    }
                    partyMap.put(player.getUniqueId(), player.getUniqueId());
                    player.sendMessage("§aParty created!");
                    break;

                case "add":
                    if (args.length < 2) return true; // if the user did not add a username at the end
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        player.sendMessage("§cPlayer not found.");
                        return true;
                    }
                    if (!isPlayerPartyLeader(player)) {
                        player.sendMessage("§cOnly leaders can invite.");
                        return true;
                    }
                    // Add the player to a map of people being invited
                    inviteMap.put(target.getUniqueId(), player.getUniqueId());
                    target.sendMessage("§c--------------------------------\n§aYou were invited to " + player.getName() + "'s party! /party accept\n§c--------------------------------");
                    player.sendMessage("§aInvitation sent.");
                    break;

                case "join", "accept":
                    // If they accept, remove them from the pending invite map
                    Player leader = Bukkit.getPlayer(inviteMap.remove(player.getUniqueId()));
                    if (leader == null) {
                        player.sendMessage("§cNo pending invites.");
                        return true;
                    }
                    // looking through the map (specifically through the values column), if more than
                    // 2 people are already part of the party, say that the party is full and do not
                    // add them to the party
//                    long size = partyMap.values().stream().filter(l -> l.equals(leader.getUniqueId())).count();
//                    if (size >= 3) {
//                        player.sendMessage("§cParty is full (3/3)!");
//                        return true;
//                    }
                    // register the newly added player into the list of players
                    partyMap.put(player.getUniqueId(), leader.getUniqueId());
                    player.sendMessage("§aJoined " + leader.getName() + "'s party!");
                    leader.sendMessage("§c--------------------------------\n§a" + player.getName() + " joined!\n§c--------------------------------");
                    break;

                case "q", "queue":
                    if (!partyMap.containsKey(player.getUniqueId())) {
                        addToQueue(player);
                        checkQueue();
                        return true;
                    }
                    if (!isPlayerPartyLeader(player)) {
                        player.sendMessage("§cOnly your party leader can join the queue!");
                        return true;
                    }
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        if (partyMap.get(online.getUniqueId()) != null && player.getUniqueId().equals(partyMap.get(online.getUniqueId()))) {
                            addToQueue(online);
                        }
                    }
                    checkQueue();
                    break;

                case "dq", "leavequeue": {
                    leaveQueue(player);
                    break;
                }

                case "leave": {
                    leaveParty(player);
                    break;
                }

                case "list": {
                    String message = "§eParty Members:";
                    for (Player online : Bukkit.getOnlinePlayers()) {
                        // Check if the online player belongs to the current leader (player)
                        UUID leaderID = partyMap.get(online.getUniqueId());

                        if (leaderID != null && leaderID.equals(player.getUniqueId())) {
                            message += "\n" + online.getName();
                        }
                    }

                    if (partyMap.get(player.getUniqueId()) == null) {
                        player.sendMessage("§cYou are not in a party!");
                    }
                    else {
                        player.sendMessage(message);
                    }

                    break;
                }

                case "partyleader": {
                    if (Bukkit.getPlayer(partyMap.get(player.getUniqueId())) == null) return true;

                    player.sendMessage("Your party leader is: " + Objects.requireNonNull(Bukkit.getPlayer(partyMap.get(player.getUniqueId()))).getName());

                    break;
                }

                case "luigi": {
                    player.sendMessage("§o§b§lTest Message");
                    break;
                }
            }
            return true;
        }

        if (command.getName().equalsIgnoreCase("pc")) {
            if (args.length == 0) {
                return true;
            }

            String message = "";
            for (String i : args) {
                if (i == null) continue;

                message += i + " ";
            }

            for (Player online : Bukkit.getOnlinePlayers()) {
                if (partyMap.get(online.getUniqueId()) != null && player.getUniqueId().equals(partyMap.get(online.getUniqueId()))) {
                    online.sendMessage("§d<Party>§r " + player.getName() + ": " + message);
                }
            }

            return true;
        }

        return false;
    }

    // ------------------- MATCHMAKING LOGIC -------------------

    private void addToQueue(Player player) {
        if (!queueList.contains(player.getUniqueId())) {
            queueList.add(player.getUniqueId());
            player.sendMessage("§aYou are now in the queue!");
            giveLeaveItem(player);
            startQueueTimer(player);
        }
    }

    public void checkQueue() {
        int count = queueList.size();
        if (count >= MAX_PLAYERS) {
            startGame();
            return;
        }
        if (count >= MIN_PLAYERS && countdownTask == null) {
            countdownTask = new BukkitRunnable() {
                int timer = 30;
                @Override
                public void run() {
                    if (queueList.size() < MIN_PLAYERS) {
                        broadcastQueue("§cNot enough players. Countdown cancelled.");
                        this.cancel();
                        countdownTask = null;
                        return;
                    }
                    if (timer <= 0) {
                        startGame();
                        this.cancel();
                        return;
                    }
                    if (timer % 10 == 0 || timer <= 5) {
                        broadcastQueue("§eStarting in §6" + timer + "§e seconds...");
                    }
                    timer--;
                }
            }.runTaskTimer(this, 0L, 20L);
        }
    }

    public void startGame() {
        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        broadcastQueue("§6§lGAME STARTING!");
        for (UUID u : queueList) {
            Player p = Bukkit.getPlayer(u);

            if (p == null) return;

            p.getInventory().remove(Material.BARRIER);
            p.getInventory().setItem(8, null);
            p.sendActionBar(Component.text(""));
        }
        queueList.clear();
    }

    // ------------------- LISTENERS -------------------

    @EventHandler
    public void onMannequinClick(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked() instanceof ArmorStand npc && npc.getScoreboardTags().contains("battlebox")) {
            event.setCancelled(true);
            Player player = event.getPlayer();

            if (partyMap.containsKey(player.getUniqueId()) && !player.getUniqueId().equals(partyMap.get(player.getUniqueId()))) {
                player.sendMessage("§cOnly your party leader can join the queue!");
            } else {
                player.performCommand("party queue");
            }
        }
    }

    @EventHandler
    public void onBarrierClick(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item != null && item.getType() == Material.BARRIER) {
            event.setCancelled(true); // Stop placement

            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (queueList.contains(player.getUniqueId())) {
                    leaveQueue(player);
                }
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        disbandParty(event.getPlayer());
    }

    // ------------------- HELPERS -------------------
    private void leaveQueue(Player player) {
        // Wipe client-side visuals immediately
        clearQueueItems(player);

        player.sendMessage("§c§l(!) §cYou have left the queue.");

        queueList.remove(player.getUniqueId());

        if (partyMap.get(player.getUniqueId()) == null) {
            return;
        }

        // If the value equals the key. I.e, the player is a party leader.
        if (!isPlayerPartyLeader(player)) {
            return;
        }

        queueList.removeIf(i -> {
            if (i == null) return false;

            Player playerInQueue = Bukkit.getPlayer(partyMap.get(i));
            if (playerInQueue == null) return false;

            if (playerInQueue.equals(player)) {
                Player toKick = Bukkit.getPlayer(i);
                if (toKick != null)  {
                    toKick.sendMessage("§c§l(!) §cParty leader left the queue.");
                    clearQueueItems(toKick);
                    return true; // This tells Java to remove "i" from queueList
                }
            }
            return false;
        });
    }

    private void clearQueueItems(Player player) {
        player.sendActionBar(Component.text(""));
        player.getInventory().setItem(8, null);
        player.getInventory().remove(Material.BARRIER);
    }

    private void disbandParty(Player player) {
        if (player == null || partyMap.get(player.getUniqueId()) == null) {
            return;
        }

        // If the value equals the key. I.e, the player is a party leader.
        if (!isPlayerPartyLeader(player)) {
            player.sendMessage("§cYou have to be the party leader to do that.");
            return;
        }

        Iterator<Map.Entry<UUID, UUID>> it = partyMap.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<UUID, UUID> entry = it.next();
            Player member = Bukkit.getPlayer(entry.getKey());
            Player leader = Bukkit.getPlayer(entry.getValue());

            if (member == null || leader == null) continue;

            // Check if the leader of this entry is the player who left/disbanded
            if (leader.equals(player)) {
                member.sendMessage("§c§l(!) §cParty has been disbanded.");

                // 1. Remove from the queue list
                queueList.remove(member.getUniqueId());

                // 2. Clear items for the member
                clearQueueItems(member);

                // 3. Safely remove the current entry from the partyMap
                it.remove();
            }
        }
    }

    private void leaveParty(Player player) {
        if (partyMap.get(player.getUniqueId()) == null) {
            return;
        }

        if (isPlayerPartyLeader(player)) {
            disbandParty(player);
        }
        else {
            player.sendMessage("§c§l(!) §cYou left the party.");
            partyMap.remove(player.getUniqueId());
        }
    }

    private boolean isPlayerPartyLeader(Player player) {
        return partyMap.get(player.getUniqueId()).equals(player.getUniqueId());
    }


    private void broadcastQueue(String msg) {
        for (UUID u : queueList) {
            Player p = Bukkit.getPlayer(u);

            if (p == null) continue;

            p.sendMessage(msg);
        }
    }

    private void startQueueTimer(Player player) {
        new BukkitRunnable() {
            int seconds = 0;
            @Override
            public void run() {
                // If they left the queue, clear and stop task
                if (!queueList.contains(player.getUniqueId())) {
                    player.sendActionBar(Component.text(""));
                    this.cancel();
                    return;
                }
                player.sendActionBar(Component.text("§aQueued! §7(" + seconds++ + "s) §e[Right-Click to Leave]"));
            }
        }.runTaskTimer(this, 0L, 20L);
    }

    private void spawnMannequin(Location loc) {
        ArmorStand npc = loc.getWorld().spawn(loc, ArmorStand.class);
        npc.setGravity(false);
        npc.setInvulnerable(true);
        npc.setArms(true);
        npc.setBasePlate(false);
        npc.addScoreboardTag("battlebox");
        npc.setCustomNameVisible(true);
        npc.customName(Component.text("§6§lBATTLE BOX §7[Click to Play]"));

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta headMeta = (SkullMeta) head.getItemMeta();
        if (headMeta != null) {
            headMeta.setOwningPlayer(Bukkit.getOfflinePlayer("Burpld"));
            head.setItemMeta(headMeta);
        }

        npc.getEquipment().setHelmet(head);
        npc.getEquipment().setChestplate(new ItemStack(Material.NETHERITE_CHESTPLATE));
        npc.getEquipment().setLeggings(new ItemStack(Material.NETHERITE_LEGGINGS));
        npc.getEquipment().setBoots(new ItemStack(Material.NETHERITE_BOOTS));

        // 1.21.11 Official Spear
        npc.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_SPEAR));
        npc.setRightArmPose(new EulerAngle(Math.toRadians(-90), 0, 0));
    }

    private void giveLeaveItem(Player player) {
        ItemStack barrier = new ItemStack(Material.BARRIER);
        ItemMeta meta = barrier.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("§c§lLeave Queue §7(Right-Click)"));
            barrier.setItemMeta(meta);
        }
        player.getInventory().setItem(8, barrier);
    }
}