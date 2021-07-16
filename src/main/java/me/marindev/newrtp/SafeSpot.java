package me.marindev.newrtp;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import io.papermc.lib.PaperLib;
import me.marindev.hosom.custommobs.Area;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.plugin.Plugin;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The SafeSpot object has some parameters and methods to find a random location using those parameters.
 */
public class SafeSpot {
    Plugin plugin = CommandsManager.getPlugin(CommandsManager.class);
    Location startingPoint;
    int preventedRange;
    int permittedRange;

    /**
     * @param start Starting point.
     * @param preventedRange Range in which the player should not spawn in. This
     *                       could be the size of spawn to make sure players spawn outside of spawn.
     * @param permittedRange Range of search.
     */
    public SafeSpot(Location start, int preventedRange,int permittedRange){
        startingPoint = start;
        this.preventedRange = preventedRange;
        this.permittedRange = permittedRange;
    }

    /**
     * Searches for a random location using the parameters.
     * @return The location found.
     */
    public Location findNewLocation(){
        int random = getRandomInteger(1,4);
        return findNewLocation(random,plugin.getConfig().getInt("tries"),1);
    }
    public Location findNewLocation(int tries,int timeout){
        int random = getRandomInteger(1,4);
        return findNewLocation(random,tries,timeout);
    }

    public Location findNewLocation(String direction,int tries,int timeout){
        int dirInt = intFromDirection(direction);
        return findNewLocation(dirInt,tries,timeout);
    }
    /**
     * Searches for a random location using the parameters as well as a direction.
     * The direction to int conversion is done using {@link #intFromDirection(String direction)}
     * @return The location found.
     */

    public Location findNewLocation(int direction, int maxtries,int timeout){
        int tries = 0;
        boolean proceed = false;
        Location location = null;
        while(!proceed) {
            int z;
            int x;
            switch (direction) {
                case 1://North
                    z = getRandomInteger(startingPoint.getBlockZ() - permittedRange, startingPoint.getBlockZ() - preventedRange);
                    x = getRandomInteger(startingPoint.getBlockX() - permittedRange, startingPoint.getBlockX() + permittedRange);
                    Location tempLoc = new Location(startingPoint.getWorld(),x,0,z);
                    if(plugin.getServer().getPluginManager().getPlugin("CustomMobs")!=null){
                        for(Area area : me.marindev.hosom.custommobs.Main.getPlugin(me.marindev.hosom.custommobs.Main.class).getUtil().getAllAreas()){
                            if(area.containsLocation(tempLoc)){
                                Bukkit.getConsoleSender().sendMessage(area.getConfigName() + " - " + "Area contains location " + tempLoc);
                                tempLoc = null;
                                break;
                            }else{
                                Bukkit.getConsoleSender().sendMessage(area.getConfigName() + " - "  + tempLoc);
                            }
                        }
                    }
                    if(tempLoc == null) break;
                    try {
                        location = getSafeY(new Location(startingPoint.getWorld(), x + 0.5, 150, z + 0.5)).get(timeout, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (TimeoutException e) {
                        //e.printStackTrace();
                    }
                    break;
                case 2://South
                    z = getRandomInteger(startingPoint.getBlockZ() + preventedRange, startingPoint.getBlockZ() + permittedRange);
                    x = getRandomInteger(startingPoint.getBlockX() - permittedRange, startingPoint.getBlockX() + permittedRange);
                    tempLoc = new Location(startingPoint.getWorld(),x,0,z);
                    if(plugin.getServer().getPluginManager().getPlugin("CustomMobs")!=null){
                        for(Area area : me.marindev.hosom.custommobs.Main.getPlugin(me.marindev.hosom.custommobs.Main.class).getUtil().getAllAreas()){
                            if(area.containsLocation(tempLoc)){
                                Bukkit.getConsoleSender().sendMessage(area.getConfigName() + " - " + "Area contains location " + tempLoc);
                                tempLoc = null;
                                break;
                            }else{
                                Bukkit.getConsoleSender().sendMessage(area.getConfigName() + " - "  + tempLoc);
                            }
                        }
                    }
                    if(tempLoc == null) break;
                    try {
                        location = getSafeY(new Location(startingPoint.getWorld(), x + 0.5, 150, z + 0.5)).get(timeout, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (TimeoutException e) {
                        //e.printStackTrace();
                    }
                    break;
                case 3://West
                    z = getRandomInteger(startingPoint.getBlockZ() - permittedRange, startingPoint.getBlockZ() + permittedRange);
                    x = getRandomInteger(startingPoint.getBlockX() - permittedRange, startingPoint.getBlockX() - preventedRange);
                    tempLoc = new Location(startingPoint.getWorld(),x,0,z);
                    if(plugin.getServer().getPluginManager().getPlugin("CustomMobs")!=null){
                        for(Area area : me.marindev.hosom.custommobs.Main.getPlugin(me.marindev.hosom.custommobs.Main.class).getUtil().getAllAreas()){
                            if(area.containsLocation(tempLoc)){
                                Bukkit.getConsoleSender().sendMessage(area.getConfigName() + " - " + "Area contains location " + tempLoc);
                                tempLoc = null;
                                break;
                            }else{
                                Bukkit.getConsoleSender().sendMessage(area.getConfigName() + " - "  + tempLoc);
                            }
                        }
                    }
                    if(tempLoc == null) break;
                    try {
                        location = getSafeY(new Location(startingPoint.getWorld(), x + 0.5, 150, z + 0.5)).get(timeout, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (TimeoutException e) {
                        //e.printStackTrace();
                    }
                    break;
                default:
                    z = getRandomInteger(startingPoint.getBlockZ() - permittedRange, startingPoint.getBlockZ() + permittedRange);
                    x = getRandomInteger(startingPoint.getBlockX() + preventedRange, startingPoint.getBlockX() + permittedRange);
                    tempLoc = new Location(startingPoint.getWorld(),x,0,z);
                    if(plugin.getServer().getPluginManager().getPlugin("CustomMobs")!=null){
                        for(Area area : me.marindev.hosom.custommobs.Main.getPlugin(me.marindev.hosom.custommobs.Main.class).getUtil().getAllAreas()){
                            if(area.containsLocation(tempLoc)){
                                Bukkit.getConsoleSender().sendMessage(area.getConfigName() + " - " + "Area contains location " + tempLoc);
                                tempLoc = null;
                                break;
                            }else{
                                Bukkit.getConsoleSender().sendMessage(area.getConfigName() + " - "  + tempLoc);
                            }
                        }
                    }
                    if(tempLoc == null) break;
                    try {
                        location = getSafeY(new Location(startingPoint.getWorld(), x + 0.5, 150, z + 0.5)).get(timeout, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (TimeoutException e) {
                        //e.printStackTrace();
                    }
                    break;
            }
            tries++;
            if (location != null || tries >= maxtries) {
                proceed = true;
            }
            if(location != null){
                if(!location.getWorld().getWorldBorder().isInside(location)){
                    location = null;
                }
            }
        }
        if(location != null) {
            location.setY(location.getY()+2);
        }
        return location;
    }

    /**
     * Converts a direction to an integer from 0-3
     * @param direction
     */
    private int intFromDirection(String direction){
        if(direction.equalsIgnoreCase("north")){
            return 1;
        }else if(direction.equalsIgnoreCase("south")){
            return 2;
        }else if(direction.equalsIgnoreCase("west")){
            return 3;
        }else{
            return 4;
        }
    }

    /**
     * Gives a random integer from min to max (inclusive)
     */
    private int getRandomInteger(int min, int max){
        int minimum = min;
        int maximum = max;

        //Considers negative values of x and y.
        if(min>max){
            minimum = max;
            maximum = min;
        }
        Random r = new Random();
        return r.nextInt(maximum - minimum) + minimum;
    }

    /**
     * Sets a safe y-value for the specified Location.
     * @param location
     * @return true if a safe y-value was found.
     */
    private CompletableFuture<Location> getSafeY(Location location){
        CompletableFuture<Location> futureLocation = new CompletableFuture<>();
        PaperLib.getChunkAtAsync(location).whenComplete(((chunk, throwable) -> {
            final int x = location.getBlockX();
            final int z = location.getBlockZ();
            for(int y=60;y<=100;y++){
                Bukkit.getConsoleSender().sendMessage("Checkpoint 1A");
                ApplicableRegionSet set = WorldGuard.getInstance().getPlatform().getRegionContainer().get(new BukkitWorld(location.getWorld())).getApplicableRegions(BlockVector3.at(location.getX(),y,location.getZ()));
                Bukkit.getConsoleSender().sendMessage("Checkpoint 1B");
                if(set.size() != 0){
                    for(ProtectedRegion rg : set){
                        Bukkit.getConsoleSender().sendMessage(rg.getId());
                        if(rg.getFlag(Flags.PASSTHROUGH) != StateFlag.State.ALLOW) {
                            futureLocation.complete(null);
                            break;
                        }
                    }
                }else{
                    Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "Checkpoint B");
                }
                Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "Checkpoint 4");
                Block currentBlock = location.getWorld().getBlockAt(x, y, z);
                Block upOne = location.getWorld().getBlockAt(x, y + 1, z);
                Block downOne = location.getWorld().getBlockAt(x, y - 1, z);
                if(currentBlock.getType() == Material.AIR){
                    Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "Checkpoint 4.5");
                    if(upOne.getType() != Material.AIR){
                        Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "Checkpoint 5: Null");
                        futureLocation.complete(null);
                        break;
                    }
                    if(downOne.getType() != Material.WATER && downOne.getType() != Material.AIR
                            && downOne.getType() != Material.LAVA) {
                        location.setY(y);
                        futureLocation.complete(location);
                        Bukkit.getConsoleSender().sendMessage(ChatColor.DARK_AQUA + "Checkpoint 6: Complete");
                        break;
                    }
                }
            }
        }));
        if(futureLocation.isCompletedExceptionally()){
            futureLocation.complete(null);
        }
        if(futureLocation.isCancelled()){
            futureLocation.complete(null);
        }
        return futureLocation;
    }
}