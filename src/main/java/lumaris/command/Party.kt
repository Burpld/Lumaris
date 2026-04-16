package lumaris.command

import lumaris.GameManager
import lumaris.Global
import lumaris.Main
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scheduler.BukkitTask
import java.util.*


class Party(private val plugin: Main) : Listener, CommandExecutor, TabCompleter {
    // Key = specific player. Value = party leader.
    private val partyMap = hashMapOf<UUID, UUID>()
    private val inviteMap = hashMapOf<UUID, MutableSet<UUID>>()
    private val queueList = mutableListOf<UUID>()

    private var countdownTask: BukkitTask? = null

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<String>
    ): List<String>? {
        if (args.size == 1) {
            return listOf("new", "add", "accept", "queue", "dequeue", "leave", "list", "kick", "promote");
        }

        return null;
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<String>
    ): Boolean {
        if (sender !is Player) return true

        if (args.isEmpty()) {
            sender.sendMessage("§e/party <new | add | accept | queue | dequeue | leave | list | kick | promote>")
            return true
        }

        val sub = args[0].lowercase()

        when (sub) {
            "new" -> {
                // if the player already appears in the map of people in a party
                if (partyMap.containsKey(sender.uniqueId)) {
                    sender.sendMessage("§cAlready in a party!")
                    return true
                }
                partyMap[sender.uniqueId] = sender.uniqueId
                sender.sendMessage("§aParty created!")
            }

            "add", "invite" -> {
                if (args.size < 2) return true // if the user did not add a username at the end
                val target = Bukkit.getPlayer(args[1])
                if (target == null) {
                    sender.sendMessage("§cPlayer not found.")
                    return true
                }
                if (!isPlayerPartyLeader(sender)) {
                    sender.sendMessage("§cOnly leaders can invite.")
                    return true
                }

                if (target == sender) {
                    sender.sendMessage("§cYou are already in the party silly!")
                    return true
                }

                // Add the player to a map of people being invited
                inviteMap.getOrPut(target.uniqueId) { hashSetOf() }.add(sender.uniqueId)
                target.sendMessage("§c--------------------------------\n§aYou were invited to ${sender.name}'s party! /party accept\n§c--------------------------------")
                sender.sendMessage("§aInvitation sent.")
            }

            "join", "accept" -> {
                if (args.size == 1) {
                    sender.sendMessage("§cUsage: /party accept <playername>")
                    return true
                }

                val targetName = args[1]
                val targetLeader = Bukkit.getPlayer(targetName)

                if (targetLeader == null) {
                    sender.sendMessage("§cPlayer not found.")
                    return true
                }

                val playerUUID: UUID = sender.uniqueId
                val leaderUUID = targetLeader.uniqueId


                // 1. Get the set of all people who invited this player
                val invites: MutableSet<UUID>? = inviteMap[playerUUID]


                // 2. Check if the specific target is in that set
                if (invites != null && invites.contains(leaderUUID)) {
                    // Success! Remove this specific invite

                    invites.remove(leaderUUID)
                    if (invites.isEmpty()) inviteMap.remove(playerUUID)

                    // Add to party
                    partyMap[playerUUID] = leaderUUID

                    sender.sendMessage("§aJoined ${targetLeader.name}'s party!")
                    targetLeader.sendMessage("§a${sender.name} joined!")
                }
                else {
                    sender.sendMessage("§cYou don't have a pending invite from ${targetLeader.name}.")
                }
            }

            "queue", "q" -> {
                if (!partyMap.containsKey(sender.uniqueId)) {
                    addToQueue(sender)
                    checkQueue()
                    return true
                }
                if (!isPlayerPartyLeader(sender)) {
                    sender.sendMessage("§cOnly your party leader can join the queue!")
                    return true
                }
                for (online in Bukkit.getOnlinePlayers()) {
                    isPlayerPartyLeader(online)
                    if (partyMap[online.uniqueId] != null && sender.uniqueId == partyMap[online.uniqueId]) {
                        addToQueue(online)
                    }
                }
                checkQueue()
            }

            "dequeue", "dq" -> {
                leaveQueue(sender)
            }

            "leave" -> {
                leaveParty(sender)
            }

            "list" -> {
                var message = "§eParty Members:"

                val userID = partyMap[sender.uniqueId]

                for (online in Bukkit.getOnlinePlayers()) {
                    // Check if the online player belongs to the current leader (player)
                    val leaderID = partyMap[online.uniqueId]

                    // If the online player is real, and the sender is real, and they both share the same party leader
                    if (leaderID != null && userID != null && userID == leaderID) {
                        message += "\n" + online.name
                    }
                }

                if (userID == null) {
                    sender.sendMessage("§cYou are not in a party!")
                }
                else {
                    sender.sendMessage(message)
                }
            }

            "partyleader" -> {
                if (partyMap[sender.uniqueId] == null) return true

                val leader = Bukkit.getPlayer(partyMap[sender.uniqueId]!!) ?: return true

                sender.sendMessage("§aYour party leader is: §l${leader.name}")
            }

            "kick" -> {
                if (args.size == 1) {
                    sender.sendMessage("§cUsage: /party kick <playername>")
                    return true
                }

                val target = Bukkit.getPlayer(args[1])
                if (target == null) {
                    sender.sendMessage("§cPlayer not found.")
                    return true
                }

                if (!isPlayerPartyLeader(sender)) {
                    sender.sendMessage("§cOnly leaders can kick members.")
                    return true
                }

                if (partyMap[target.uniqueId] != sender.uniqueId || target == sender) {
                    sender.sendMessage("§cPlayer is not a valid target!")
                    return true
                }

                partyMap.remove(target.uniqueId)
                leaveQueue(target)
                target.sendMessage("§c§l(!) §cYou were kicked from the party.")
                sender.sendMessage("§aKicked ${target.name} from the party.")
            }

            "promote" -> {
                if (args.size == 1) {
                    sender.sendMessage("§cUsage: /party promote <playername>")
                    return true
                }

                val target = Bukkit.getPlayer(args[1])
                if (target == null) {
                    sender.sendMessage("§cPlayer not found.")
                    return true
                }

                if (!isPlayerPartyLeader(sender)) {
                    sender.sendMessage("§cOnly leaders can promote members.")
                    return true
                }

                if (partyMap[target.uniqueId] != sender.uniqueId || target == sender) {
                    sender.sendMessage("§cPlayer is not a valid target!")
                    return true
                }

                val oldLeaderID = sender.uniqueId
                val newLeaderID = target.uniqueId

                for (entry in partyMap.entries) {
                    if (entry.value == oldLeaderID) {
                        entry.setValue(newLeaderID)
                    }
                }

                sender.sendMessage("§aYou promoted ${target.name} to party leader.")
                target.sendMessage("§aYou have been promoted to party leader!")
            }

            "luigipump" -> {
                sender.sendMessage("§l§vPump on brother.")
            }
        }

        return true
    }

    // ------------------- MATCHMAKING LOGIC -------------------
    private fun addToQueue(player: Player) {
        if (!queueList.contains(player.uniqueId)) {
            queueList.add(player.uniqueId)
            player.sendMessage("§aYou are now in the queue!")
            giveLeaveItem(player)
            startQueueTimer(player)
        }
    }

    fun checkQueue() {
        val count = queueList.size
        if (count >= Global.MAX_PLAYERS_QUEUED) {
            startGame()
            return
        }
        if (count >= Global.MIN_PLAYERS_QUEUED && countdownTask == null) {
            countdownTask = object : BukkitRunnable() {
                var timer = 30
                override fun run() {
                    if (queueList.size < Global.MIN_PLAYERS_QUEUED) {
                        broadcastQueue("§cNot enough players. Countdown cancelled.")
                        this.cancel()
                        countdownTask = null
                        return
                    }
                    if (timer <= 0) {
                        startGame()
                        this.cancel()
                        return
                    }
                    if (timer % 10 == 0 || timer <= 5) {
                        broadcastQueue("§eStarting in §6$timer§e seconds...")
                    }
                    timer--
                }
            }.runTaskTimer(plugin, 0L, 20L)
        }
    }

    fun startGame() {
        if (countdownTask != null) {
            countdownTask!!.cancel()
            countdownTask = null
        }
        broadcastQueue("§6§lGAME STARTING!")
        for (u in queueList) {
            val p = Bukkit.getPlayer(u) ?: return

            p.inventory.remove(Material.BARRIER)
            p.inventory.setItem(8, null)
            p.sendActionBar(Component.text(""))
        }

        val game = GameManager(queueList, partyMap)

        game.teamGenerator.assignTeams()

        for (queuedPlayer in queueList) {
            game.teamGenerator.messageTeam(Bukkit.getPlayer(queuedPlayer))
        }

        GameManager.runningGames.add(game)

        queueList.clear()
    }

    // ------------------- LISTENERS -------------------
    @EventHandler
    @Suppress("unused")
    fun onMannequinClick(event: PlayerInteractAtEntityEvent) {
        if (event.rightClicked is ArmorStand && event.rightClicked.scoreboardTags.contains("battlebox")) {
            event.isCancelled = true
            val player = event.getPlayer()

            if (partyMap.containsKey(player.uniqueId) && player.uniqueId != partyMap[player.uniqueId]) {
                player.sendMessage("§cOnly your party leader can join the queue!")
            }
            else {
                player.performCommand("party queue")
            }
        }
    }

    @EventHandler
    @Suppress("unused")
    fun onBarrierClick(event: PlayerInteractEvent) {
        val player = event.getPlayer()
        val item = event.item

        if (item != null && item.type == Material.BARRIER) {
            event.isCancelled = true // Stop placement

            if (event.action == Action.RIGHT_CLICK_AIR || event.action == Action.RIGHT_CLICK_BLOCK) {
                if (queueList.contains(player.uniqueId)) {
                    leaveQueue(player)
                }
            }
        }
    }

    @EventHandler
    @Suppress("unused")
    fun onItemDrop(event: PlayerDropItemEvent) {
        if (event.itemDrop.itemStack.type == Material.BARRIER) {
            event.isCancelled = true;
        }
    }

    @EventHandler
    @Suppress("unused")
    fun onQuit(event: PlayerQuitEvent) {
        leaveParty(event.player)
        leaveQueue(event.player)
        inviteMap.remove(event.player.uniqueId)
    }

    // ------------------- HELPERS -------------------
    private fun leaveQueue(player: Player) {
        // Wipe client-side visuals immediately
        clearQueueItems(player)

        player.sendMessage("§c§l(!) §cYou have left the queue.")

        queueList.remove(player.uniqueId)

        if (partyMap[player.uniqueId] == null) {
            return
        }

        // If the value equals the key. I.e, the player is a party leader.
        if (!isPlayerPartyLeader(player)) {
            return
        }

        queueList.removeIf { i: UUID ->
            val playerInQueue = Bukkit.getPlayer(partyMap[i]!!) ?: false

            if (playerInQueue == player) {
                val toKick = Bukkit.getPlayer(i)
                if (toKick != null) {
                    toKick.sendMessage("§c§l(!) §cParty leader left the queue.")
                    clearQueueItems(toKick)
                    return@removeIf true // This tells Java to remove "i" from queueList
                }
            }
            false
        }
    }

    @Suppress("unused")
    private fun processJoin(player: Player, leaderUUID: UUID) {
        val leader = Bukkit.getPlayer(leaderUUID)

        if (leader == null) {
            player.sendMessage("§cThat leader is no longer online.")
            return
        }

        // Optional: Party size check
        val size = partyMap.values.stream().filter { l: UUID? -> l == leaderUUID }.count()
        if (size >= 2) { // 2 others + leader = 3
            player.sendMessage("§cParty is full!")
            return
        }

        partyMap[player.uniqueId] = leaderUUID
        player.sendMessage("§aJoined " + leader.name + "'s party!")
        leader.sendMessage("§a" + player.name + " joined your party!")
    }

    private fun clearQueueItems(player: Player) {
        player.sendActionBar(Component.text(""))
        player.inventory.setItem(8, null)
        player.inventory.remove(Material.BARRIER)
    }

    private fun disbandParty(player: Player?) {
        if (player == null || partyMap[player.uniqueId] == null) {
            return
        }

        // If the value equals the key. I.e, the player is a party leader.
        if (!isPlayerPartyLeader(player)) {
            player.sendMessage("§cYou have to be the party leader to do that.")
            return
        }

        val it: MutableIterator<MutableMap.MutableEntry<UUID, UUID>> = partyMap.entries.iterator()

        while (it.hasNext()) {
            val entry = it.next()
            val member = Bukkit.getPlayer(entry.key)
            val leader = Bukkit.getPlayer(entry.value)

            if (member == null || leader == null) continue

            // Check if the leader of this entry is the player who left/disbanded
            if (leader == player) {
                member.sendMessage("§c§l(!) §cParty has been disbanded.")

                // 1. Remove from the queue list
                queueList.remove(member.uniqueId)

                // 2. Clear items for the member
                clearQueueItems(member)

                // 3. Safely remove the current entry from the partyMap
                it.remove()
            }
        }
    }

    private fun leaveParty(player: Player) {
        if (partyMap[player.uniqueId] == null) {
            return
        }

        if (isPlayerPartyLeader(player)) {
            disbandParty(player)
        }
        else {
            player.sendMessage("§c§l(!) §cYou left the party.")
            partyMap.remove(player.uniqueId)
        }
    }

    private fun isPlayerPartyLeader(player: Player): Boolean {
        return partyMap[player.uniqueId] == player.uniqueId
    }

    private fun broadcastQueue(msg: String) {
        for (u in queueList) {
            val p = Bukkit.getPlayer(u) ?: continue

            p.sendMessage(msg)
        }
    }

    private fun startQueueTimer(player: Player) {
        object : BukkitRunnable() {
            var seconds = 0
            override fun run() {
                // If they left the queue, clear and stop task
                if (!queueList.contains(player.uniqueId)) {
                    player.sendActionBar(Component.text(""))
                    cancel()
                    return
                }
                player.sendActionBar(Component.text("§aQueued! §7(" + seconds++ + "s) §e[Right-Click to Leave]"))
            }
        }.runTaskTimer(plugin, 0L, 20L)
    }

    private fun giveLeaveItem(player: Player) {
        val barrier = ItemStack(Material.BARRIER)
        val meta = barrier.itemMeta
        if (meta != null) {
            meta.displayName(Component.text("§c§lLeave Queue §7(Right-Click)"))
            barrier.setItemMeta(meta)
        }
        player.inventory.setItem(8, barrier)
    }

    // ------------------- ACCESSORS -------------------
    fun getPartyMapValue(player: UUID): UUID? {
        return partyMap[player]
    }
}