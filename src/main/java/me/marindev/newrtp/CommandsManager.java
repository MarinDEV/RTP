package me.marindev.newrtp;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import io.papermc.lib.PaperLib;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class CommandsManager extends JavaPlugin implements CommandExecutor, TabCompleter {

    CommandsManager self = this;
    SafeSpot mainCache = null;
    SafeSpot rtpCache = null;
    SafeSpot northCache = null;
    SafeSpot southCache = null;
    SafeSpot eastCache = null;
    SafeSpot westCache = null;
    ArrayList<Player> delay = new ArrayList<>();
    Location spawnLocation;
    HashMap<Player,ArrayList<Player>> tpPlayerRequest = new HashMap<Player,ArrayList<Player>>();
    BukkitRunnable cacheRefresher;
    BukkitRunnable northRb;
    BukkitRunnable southRb;
    BukkitRunnable eastRb;
    BukkitRunnable westRb;
    BukkitRunnable randomRb;
    public WorldGuard wg;

    @Override
    public void onEnable() {
        wg = WorldGuard.getInstance();
        Bukkit.getPluginCommand("rtp").setExecutor(this);
        Bukkit.getPluginCommand("rtp").setTabCompleter(this);
        saveResource("config.yml",false);
        spawnLocation = getSpawnLocation();
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Checkpoint 1");
        cacheRefresher = new BukkitRunnable() {
            @Override
            public void run() {
                mainCache = new SafeSpot(spawnLocation, getConfig().getInt("spawn-radius"), getConfig().getInt("main-search-radius"));
                northRb = new BukkitRunnable() {
                    @Override
                    public void run() {
                        Location north = mainCache.findNewLocation("north",15,5);
                        if (north != null) {
                            northCache = new SafeSpot(north, 0, getConfig().getInt("side-search-radius"));
                            Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "North: " + north.getX() + ":" + north.getY() + ":" + north.getZ());
                            this.cancel();
                        }
                    }
                };
                southRb = new BukkitRunnable() {
                    @Override
                    public void run() {
                        Location south = mainCache.findNewLocation("south",15,5);
                        if(south != null){
                            southCache = new SafeSpot(south,0,getConfig().getInt("side-search-radius"));
                            Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "South: " + south.getX() + ":" + south.getY() + ":" + south.getZ());
                            this.cancel();
                        }
                    }
                };
                eastRb = new BukkitRunnable() {
                    @Override
                    public void run() {
                        Location east = mainCache.findNewLocation("east",15,5);
                        if(east != null){
                            eastCache = new SafeSpot(east,0,getConfig().getInt("side-search-radius"));
                            Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "East: " + east.getX() + ":" + east.getY() + ":" + east.getZ());
                            this.cancel();
                        }
                    }
                };
                westRb = new BukkitRunnable() {
                    @Override
                    public void run() {
                        Location west = mainCache.findNewLocation("west",15,5);
                        if(west != null){
                            westCache = new SafeSpot(west,0,getConfig().getInt("side-search-radius"));
                            Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "West: " + west.getX() + ":" + west.getY() + ":" + west.getZ());
                            this.cancel();
                        }
                    }
                };
                randomRb = new BukkitRunnable() {
                    @Override
                    public void run() {
                        Location random = mainCache.findNewLocation(15,5);
                        if(random != null){
                            rtpCache = new SafeSpot(random,0,getConfig().getInt("side-search-radius"));
                            Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Random: " + random.getX() + ":" + random.getY() + ":" + random.getZ());
                            this.cancel();
                        }
                    }
                };
                CompletableFuture.runAsync(northRb);
                CompletableFuture.runAsync(southRb);
                CompletableFuture.runAsync(eastRb);
                CompletableFuture.runAsync(westRb);
                CompletableFuture.runAsync(randomRb);
            }
        };
        cacheRefresher.runTaskTimer(this,20,1200*getConfig().getInt("cache-time"));
    }

    public void onDisable(){
        cacheRefresher.cancel();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String args[]){
        if(!command.getName().equalsIgnoreCase("rtp")) return false;
        if(!(sender instanceof Player)){
            if(args.length >= 1 && args[0].equalsIgnoreCase("reload")){
                reloadConfig();
                reloadCaches();
                sender.sendMessage(ChatColor.GREEN + "config.yml was reloaded!");
                return true;
            }
            sender.sendMessage(ChatColor.RED + "This command can only be used in game!");
            return false;
        }
        Player player = (Player) sender;
        if(args.length == 0){
            if(!isInConfigRegion(player)){
                player.sendMessage(getConfigMessage("command-not-allowed").replaceAll("<command>", "/rtp"));
                return false;
            }
            if(!player.hasPermission("rtp.tp") && !player.isOp()){
                String noPermission = getConfigMessage("no-permission-message");
                player.sendMessage(noPermission);
                return false;
            }
            if(delay.contains(player) && !player.hasPermission("rtp.delaybypass")){
                player.sendMessage(getConfigMessage("delay-message"));
                return false;
            }
            if(!delay.contains(player)){
                delay.add(player);
            }
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> delay.remove(player), 20 * this.getConfig().getInt("rtp-delay"));
            player.sendMessage(getConfigMessage("searching-message"));
            BukkitRunnable rb = new BukkitRunnable() {
                @Override
                public void run() {
                    Location newLocation = null;
                    try {
                        newLocation = rtpCache.findNewLocation();
                    }catch (NullPointerException e){

                    }
                    if (newLocation == null) {
                        player.sendMessage(getConfigMessage("no-free-spot-message"));
                        return;
                    }
                    //Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "New Location: " + newLocation.getX() + ":" + newLocation.getY() + ":" + newLocation.getZ());
                    player.sendMessage(getConfigMessage("teleporting-message").replaceAll("<time>", getConfig().getInt("teleport-delay") + ""));
                    teleport(player, newLocation);
                }
            };
            rb.runTaskAsynchronously(this);
            return true;
        }
        if(args[0].equalsIgnoreCase("north")){
            if(!isInConfigRegion(player)){
                player.sendMessage(getConfigMessage("command-not-allowed").replaceAll("<command>", "/rtp north"));
                return false;
            }
            if(!player.hasPermission("rtp.direction") && !player.isOp()){
                player.sendMessage(getConfigMessage("no-permission-message"));
                return false;
            }
            if(delay.contains(player) && !player.hasPermission("rtp.delaybypass")){
                player.sendMessage(getConfigMessage("delay-message"));
                return false;
            }
            if(!delay.contains(player)){
                delay.add(player);
            }
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> delay.remove(player), 20 * this.getConfig().getInt("rtp-delay"));
            player.sendMessage(getConfigMessage("searching-message"));
            BukkitRunnable rb = new BukkitRunnable() {
                @Override
                public void run() {
                    Location newLocation = null;
                    try {
                        newLocation = northCache.findNewLocation();
                    }catch (NullPointerException e){

                    }
                    if (newLocation == null) {
                        player.sendMessage(getConfigMessage("no-free-spot-message"));
                        return;
                    }
                    //Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "New Location: " + newLocation.getX() + ":" + newLocation.getY() + ":" + newLocation.getZ());
                    player.sendMessage(getConfigMessage("teleporting-message").replaceAll("<time>", getConfig().getInt("teleport-delay") + ""));
                    teleport(player, newLocation);
                }
            };
            rb.runTaskAsynchronously(this);
            return true;
        }else if(args[0].equalsIgnoreCase("south")){
            if(!isInConfigRegion(player)){
                player.sendMessage(getConfigMessage("command-not-allowed").replaceAll("<command>", "/rtp south"));
                return false;
            }
            if(!player.hasPermission("rtp.direction") && !player.isOp()){
                player.sendMessage(getConfigMessage("no-permission-message"));
                return false;
            }
            if(delay.contains(player) && !player.hasPermission("rtp.delaybypass")){
                player.sendMessage(getConfigMessage("delay-message"));
                return false;
            }
            if(!delay.contains(player)){
                delay.add(player);
            }
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> delay.remove(player), 20 * this.getConfig().getInt("rtp-delay"));
            player.sendMessage(getConfigMessage("searching-message"));
            BukkitRunnable rb = new BukkitRunnable() {
                @Override
                public void run() {
                    Location newLocation = null;
                    try {
                        newLocation = southCache.findNewLocation();
                    }catch (NullPointerException e){

                    }
                    if (newLocation == null) {
                        player.sendMessage(getConfigMessage("no-free-spot-message"));
                        return;
                    }
                    //Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "New Location: " + newLocation.getX() + ":" + newLocation.getY() + ":" + newLocation.getZ());
                    player.sendMessage(getConfigMessage("teleporting-message").replaceAll("<time>", getConfig().getInt("teleport-delay") + ""));
                    teleport(player, newLocation);
                }
            };
            rb.runTaskAsynchronously(this);
        }else if(args[0].equalsIgnoreCase("east")){
            if(!isInConfigRegion(player)){
                player.sendMessage(getConfigMessage("command-not-allowed").replaceAll("<command>", "/rtp east"));
                return false;
            }
            if(!player.hasPermission("rtp.direction") && !player.isOp()){
                player.sendMessage(getConfigMessage("no-permission-message"));
                return false;
            }
            if(delay.contains(player) && !player.hasPermission("rtp.delaybypass")){
                player.sendMessage(getConfigMessage("delay-message"));
                return false;
            }
            if(!delay.contains(player)){
                delay.add(player);
            }
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> delay.remove(player), 20 * this.getConfig().getInt("rtp-delay"));
            player.sendMessage(getConfigMessage("searching-message"));
            BukkitRunnable rb = new BukkitRunnable() {
                @Override
                public void run() {
                    Location newLocation = null;
                    try {
                        newLocation = eastCache.findNewLocation();
                    }catch (NullPointerException e){

                    }
                    if (newLocation == null) {
                        player.sendMessage(getConfigMessage("no-free-spot-message"));
                        return;
                    }
                    //Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "New Location: " + newLocation.getX() + ":" + newLocation.getY() + ":" + newLocation.getZ());
                    player.sendMessage(getConfigMessage("teleporting-message").replaceAll("<time>", getConfig().getInt("teleport-delay") + ""));
                    teleport(player, newLocation);
                }
            };
            rb.runTaskAsynchronously(this);
        }else if(args[0].equalsIgnoreCase("west")){
            if(!isInConfigRegion(player)){
                player.sendMessage(getConfigMessage("command-not-allowed").replaceAll("<command>", "/rtp west"));
                return false;
            }
            if(!player.hasPermission("rtp.direction") && !player.isOp()){
                player.sendMessage(getConfigMessage("no-permission-message"));
                return false;
            }
            if(delay.contains(player) && !player.hasPermission("rtp.delaybypass")){
                player.sendMessage(getConfigMessage("delay-message"));
                return false;
            }
            if(!delay.contains(player)){
                delay.add(player);
            }
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> delay.remove(player), 20 * this.getConfig().getInt("rtp-delay"));
            player.sendMessage(getConfigMessage("searching-message"));
            BukkitRunnable rb = new BukkitRunnable() {
                @Override
                public void run() {
                    Location newLocation = null;
                    try {
                        newLocation = westCache.findNewLocation();
                    }catch (NullPointerException e){

                    }
                    if (newLocation == null) {
                        player.sendMessage(getConfigMessage("no-free-spot-message"));
                        return;
                    }
                    //Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "New Location: " + newLocation.getX() + ":" + newLocation.getY() + ":" + newLocation.getZ());
                    player.sendMessage(getConfigMessage("teleporting-message").replaceAll("<time>", getConfig().getInt("teleport-delay") + ""));
                    teleport(player, newLocation);
                }
            };
            rb.runTaskAsynchronously(this);
        }else if(args[0].equalsIgnoreCase("random")){
            if(!isInConfigRegion(player)){
                player.sendMessage(getConfigMessage("command-not-allowed").replaceAll("<command>", "/rtp random"));
                return false;
            }
            if(!player.hasPermission("rtp.random") && !player.isOp()){
                player.sendMessage(getConfigMessage("no-permission-message"));
                return false;
            }
            if(delay.contains(player) && !player.hasPermission("rtp.delaybypass")){
                player.sendMessage(getConfigMessage("delay-message"));
                return false;
            }
            if(!delay.contains(player)){
                delay.add(player);
            }
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> delay.remove(player), 20 * this.getConfig().getInt("rtp-delay"));
            player.sendMessage(getConfigMessage("searching-message"));
            BukkitRunnable rb = new BukkitRunnable() {
                @Override
                public void run() {
                    Location newLocation = null;
                    try {
                        newLocation = mainCache.findNewLocation();
                    }catch (NullPointerException e){

                    }
                    if (newLocation == null) {
                        player.sendMessage(getConfigMessage("no-free-spot-message"));
                        return;
                    }
                    //Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "New Location: " + newLocation.getX() + ":" + newLocation.getY() + ":" + newLocation.getZ());
                    player.sendMessage(getConfigMessage("teleporting-message").replaceAll("<time>", getConfig().getInt("teleport-delay") + ""));
                    teleport(player, newLocation);
                }
            };
            rb.runTaskAsynchronously(this);
        }else if(args[0].equalsIgnoreCase("reload")){
            if(!player.hasPermission("rtp.reload") && !player.isOp()){
                player.sendMessage(getConfigMessage("no-permission-message"));
                return false;
            }
            reloadConfig();
            reloadCaches();
            player.sendMessage(getConfigMessage("config-reloaded-message"));
            return true;
        }else if(args[0].equalsIgnoreCase("accept")) {
            if (isInConfigRegion(player)) {
                player.sendMessage(getConfigMessage("command-not-allowed").replaceAll("<command>", "/rtp accept"));
                return false;
            }
            Player teleportingPlayer = null;
            if (args.length == 1) {
                ArrayList<Player> requests = new ArrayList<>();
                if (tpPlayerRequest.containsKey(player)) {
                    requests = tpPlayerRequest.get(player);
                }
                if (requests.size() == 0) {
                    player.sendMessage(getConfigMessage("no-requests-message"));
                    return false;
                } else if (requests.size() > 1) {
                    player.sendMessage(getConfigMessage("accept-usage-message"));
                    return false;
                }
                teleportingPlayer = requests.get(0);
            } else if (args.length > 1) {
                teleportingPlayer = Bukkit.getPlayer(args[1]);
                if (teleportingPlayer == null) {
                    player.sendMessage(getConfigMessage("player-not-found-message").replaceAll("<player>", args[1]));
                    return false;
                }
                if (!tpPlayerRequest.get(player).contains(teleportingPlayer)) {
                    player.sendMessage(getConfigMessage("player-not-requested-message").replaceAll("<player>", teleportingPlayer.getName()));
                    return false;
                }
            }
            tpPlayerRequest.get(player).remove(teleportingPlayer);
            player.sendMessage(getConfigMessage("accepted-message-target").replaceAll("<player>", teleportingPlayer.getName()));
            teleportingPlayer.sendMessage(getConfigMessage("accepted-message-teleporting")
                    .replaceAll("<player>", player.getName()));
            teleportingPlayer.sendMessage(getConfigMessage("searching-message"));
            SafeSpot newSpot = new SafeSpot(player.getLocation(), getConfig().getInt("player-prevented-range"), getConfig().getInt("player-search-range"));
            Player finalTeleportingPlayer = teleportingPlayer;
            BukkitRunnable rb = new BukkitRunnable() {
                @Override
                public void run() {
                    Location newLocation = null;
                    try {
                        newLocation = newSpot.findNewLocation();
                    }catch (NullPointerException e){

                    }
                    if (newLocation == null) {
                        finalTeleportingPlayer.sendMessage(getConfigMessage("no-free-spot-message"));
                        return;
                    }
                    //Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "New Location: " + newLocation.getX() + ":" + newLocation.getY() + ":" + newLocation.getZ());
                    finalTeleportingPlayer.sendMessage(getConfigMessage("teleporting-message").replaceAll("<time>", getConfig().getInt("teleport-delay") + ""));
                    teleport(finalTeleportingPlayer, newLocation);
                }
            };
            rb.runTaskAsynchronously(this);
//            Location location = newSpot.findNewLocation();
//            if(location == null) {
//                teleportingPlayer.sendMessage(getConfigMessage("no-free-spot-message"));
//                return false;
//            }
            return true;
        }else{
            if(!player.hasPermission("rtp.player") && !player.isOp()){
                player.sendMessage(getConfigMessage("no-permission-message"));
                return false;
            }
            if(!isInConfigRegion(player)){
                player.sendMessage(getConfigMessage("command-not-allowed").replaceAll("<command>", "/rtp [player]"));
                return false;
            }
            Player targetPlayer = Bukkit.getPlayer(args[0]);
            if(targetPlayer == null){
                player.sendMessage(getConfigMessage("player-not-found-message").replaceAll("<player>",args[0]));
                return false;
            }
            if(targetPlayer.equals(player)){
                player.sendMessage(getConfigMessage("cannot-tp-to-self-message"));
                return false;
            }
            if(!tpPlayerRequest.containsKey(targetPlayer)){
                tpPlayerRequest.put(targetPlayer,new ArrayList<>());
            }
            ArrayList<Player> requests = tpPlayerRequest.get(targetPlayer);
            if(requests.contains(player)){
                player.sendMessage(getConfigMessage("already-requested-message").replaceAll("<player>",targetPlayer.getName()));
                return false;
            }
            requests.add(player);
            Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable() {
                @Override
                public void run() {
                    requests.remove(player);
                }
            },20*30);
            targetPlayer.sendMessage(getConfigMessage("target-request-message").replaceAll("<player>",player.getName()));
            player.sendMessage(getConfigMessage("sender-request-message").replaceAll("<player>",targetPlayer.getName()));
            return true;
        }
//        if(!delay.contains(player)){
//            delay.add(player);
//        }
//        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> delay.remove(player), 20 * this.getConfig().getInt("rtp-delay"));
        return true;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args){
        if (command.getName().equalsIgnoreCase("rtp")){
            ArrayList<String> options = new ArrayList<>();
            if(args.length == 1){
                if(args[0].equalsIgnoreCase("")){
                    if(sender.hasPermission("rtp.direction") || sender.isOp()){
                        options.add("north");
                        options.add("south");
                        options.add("east");
                        options.add("west");
                    }
                    if(sender.hasPermission("rtp.random") || sender.isOp()){
                        options.add("random");
                    }
                    options.add("accept");
                    for(Player player : Bukkit.getOnlinePlayers()){
                        if(((Player)sender ).equals(player)) continue;
                        options.add(player.getName());
                    }
                    return options;
                }else{
                    if(!args[0].equalsIgnoreCase("north")){
                        options.add("<direction> <a>");
                    }
                    return options;
                }
            }
        }
        return null;
    }

    /**
     * Teleports player to a location with the delay specified in config.yml
     */
    public void teleport(Player p, Location location) {
        BukkitRunnable rb = new BukkitRunnable() {
            @Override
            public void run() {
                PaperLib.teleportAsync(p,location);
            }
        };
        rb.runTaskLater(this, 20 * this.getConfig().getInt("teleport-delay"));
    }

    /**
     * Gets the spawn location from config.yml
     */
    private Location getSpawnLocation(){
        String[] locString = this.getConfig().getString("spawn").split(":");
        World world = Bukkit.getWorld(locString[0]);
        double x = Double.parseDouble(locString[1]);
        double y = Double.parseDouble(locString[2]);
        double z = Double.parseDouble(locString[3]);
        Location location = new Location(world,x,y,z);
        return location;
    }

    /**
     * Gets a message from config and translates its color codes.
     */
    public String getConfigMessage(String path){
        if(getConfig().getString(path) == null){
            return path;
        }
        String plainString = getConfig().getString(path);
        return ChatColor.translateAlternateColorCodes('&',plainString);
    }

    public boolean isInConfigRegion(Player player){
        ArrayList<String> spawnAreas = (ArrayList) getConfig().getStringList("spawn-point-1");
        Location first = stringToLocation(spawnAreas.get(0));
        Location second = stringToLocation(spawnAreas.get(1));
        Location location = player.getLocation();
        if((location.getX()<first.getX() && location.getX()>second.getX() && location.getY()<first.getY() && location.getY()>second.getY() && location.getZ()<first.getZ() && location.getZ()>second.getZ())
                || location.getX()<second.getX() && location.getX()>first.getX() && location.getY()<second.getY() && location.getY()>first.getY() && location.getZ()<second.getZ() && location.getZ()>first.getZ()) {
            return true;
        }
        spawnAreas = (ArrayList) getConfig().getStringList("spawn-point-2");
        first = stringToLocation(spawnAreas.get(0));
        second = stringToLocation(spawnAreas.get(1));
        location = player.getLocation();
        if((location.getX()<first.getX() && location.getX()>second.getX() && location.getY()<first.getY() && location.getY()>second.getY() && location.getZ()<first.getZ() && location.getZ()>second.getZ())
                || location.getX()<second.getX() && location.getX()>first.getX() && location.getY()<second.getY() && location.getY()>first.getY() && location.getZ()<second.getZ() && location.getZ()>first.getZ()) {
            return true;
        }
//        RegionContainer container = wg.getPlatform().getRegionContainer();
//        World world = location.getWorld();
//        BukkitWorld world2 = new BukkitWorld(world);
//        container.get(world2);
//        RegionManager regions = container.get(new BukkitWorld(location.getWorld()));
//        ApplicableRegionSet regionsSet = regions.getApplicableRegions(BlockVector3.at(location.getX(),location.getY(),location.getZ()));
//        for (Iterator<ProtectedRegion> it = regionsSet.iterator(); it.hasNext(); ) {
//            ProtectedRegion region = it.next();
//            if(spawnAreas.contains(region.getId().toLowerCase())){
//                return true;
//            }
//        }
        return false;
    }

    public Location stringToLocation(String string){
        String[] split = string.split(":");
        World world = Bukkit.getWorld(split[0]);
        double x = Double.parseDouble(split[1]);
        double y = Double.parseDouble(split[2]);
        double z = Double.parseDouble(split[3]);
        Location location = new Location(world,x,y,z);
        return location;
    }

    public void reloadCaches(){

        mainCache = new SafeSpot(spawnLocation, getConfig().getInt("spawn-radius"), getConfig().getInt("main-search-radius"));
        northRb = new BukkitRunnable() {
            @Override
            public void run() {
                Location north = mainCache.findNewLocation("north",15,5);
                if (north != null) {
                    northCache = new SafeSpot(north, 0, getConfig().getInt("side-search-radius"));
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "North: " + north.getX() + ":" + north.getY() + ":" + north.getZ());
                    this.cancel();
                }
            }
        };
        southRb = new BukkitRunnable() {
            @Override
            public void run() {
                Location south = mainCache.findNewLocation("south",15,5);
                if(south != null){
                    southCache = new SafeSpot(south,0,getConfig().getInt("side-search-radius"));
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "South: " + south.getX() + ":" + south.getY() + ":" + south.getZ());
                    this.cancel();
                }
            }
        };
        eastRb = new BukkitRunnable() {
            @Override
            public void run() {
                Location east = mainCache.findNewLocation("east",15,5);
                if(east != null){
                    eastCache = new SafeSpot(east,0,getConfig().getInt("side-search-radius"));
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "East: " + east.getX() + ":" + east.getY() + ":" + east.getZ());
                    this.cancel();
                }
            }
        };
        westRb = new BukkitRunnable() {
            @Override
            public void run() {
                Location west = mainCache.findNewLocation("west",15,5);
                if(west != null){
                    westCache = new SafeSpot(west,0,getConfig().getInt("side-search-radius"));
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "West: " + west.getX() + ":" + west.getY() + ":" + west.getZ());
                    this.cancel();
                }
            }
        };
        randomRb = new BukkitRunnable() {
            @Override
            public void run() {
                Location random = mainCache.findNewLocation(15,5);
                if(random != null){
                    rtpCache = new SafeSpot(random,0,getConfig().getInt("side-search-radius"));
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Random: " + random.getX() + ":" + random.getY() + ":" + random.getZ());
                    this.cancel();
                }
            }
        };
        CompletableFuture.runAsync(northRb);
        CompletableFuture.runAsync(southRb);
        CompletableFuture.runAsync(eastRb);
        CompletableFuture.runAsync(westRb);
        CompletableFuture.runAsync(randomRb);
    }

}