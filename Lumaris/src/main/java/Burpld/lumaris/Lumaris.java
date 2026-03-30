package Burpld.lumaris;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class Lumaris extends JavaPlugin {

    // Spawn coordinates
    private final String WORLD_NAME = "world";
    private final double X = -97.5;
    private final double Y = 178;
    private final double Z = -224.5;
    private final float YAW = -180;
    private final float PITCH = 1;

    @Override
    public void onEnable() {
        getLogger().info("Lumaris /spawn plugin enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Lumaris /spawn plugin disabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cOnly players can use this command.");
            return true;
        }

        if (command.getName().equalsIgnoreCase("spawn")) {

            World world = Bukkit.getWorld(WORLD_NAME);

            if (world == null) {
                player.sendMessage("§cSpawn world not found!");
                return true;
            }

            Location spawnLocation = new Location(world, X, Y, Z, YAW, PITCH);
            player.teleport(spawnLocation);
            player.sendMessage("§aTeleported to spawn!");
            return true;
        }

        return false;
    }
}