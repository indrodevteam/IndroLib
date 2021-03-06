package io.github.indroDevTeam.indroLib.objects.ranks;

import io.github.indroDevTeam.indroLib.datamanager.SQLUtils;
import io.github.indroDevTeam.indroLib.objects.warps.Warp;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RankUtils {

    /**
     * @param sqlUtils connection to database
     * @apiNote this must be run to create table in database
     */
    public static void createRankTable(SQLUtils sqlUtils) {
        sqlUtils.createTable("rankPresets",
                "rankId",
                "display VARCHAR(100)",
                "lBrace VARCHAR(100)",
                "rBrace VARCHAR(100)",
                "primaryColour VARCHAR(100)",
                "secondaryColour VARCHAR(100)",
                "nextRankId VARCHAR(100)",
                "nextAdvancement VARCHAR(100)",
                "level INT"
        );
    }

    /**
     * @param rankId unique id for rank being deleted
     */
    public static void deleteRank(String rankId, SQLUtils sqlUtils) {
        sqlUtils.deleteRow("rankId", rankId, "rankPresets");
    }

    /**
     * @param rankId What is unique id of the rank you want to get
     * @return Rank object
     * @apiNote this method streamlines getting a rank object be doing all the information from the database for you
     */
    public static Rank getRank(String rankId, SQLUtils sqlUtils) {
        return new Rank(
                rankId,
                (String) sqlUtils.getData("display", "rankID", rankId, "rankPresets"),
                (String) sqlUtils.getData("lBrace", "rankID", rankId, "rankPresets"),
                (String) sqlUtils.getData("rBrace", "rankID", rankId, "rankPresets"),
                readColour(
                        (String) sqlUtils.getData("primaryColour", "rankID", rankId, "rankPresets")
                ),
                readColour(
                        (String) sqlUtils.getData("secondaryColour", "rankID", rankId, "rankPresets")
                ),
                (String) sqlUtils.getData("nextRankId", "rankID", rankId, "rankPresets"),
                getAdvancement(
                        (String) sqlUtils.getData("nextAdvancement", "rankID", rankId, "rankPresets")
                ),
                (Integer) sqlUtils.getData("level", "rankID", rankId, "rankPresets")
        );
    }

    /**
     * @param player Target player
     */
    public static Rank getRank(Player player, SQLUtils sqlUtils) {
        return getRank(
                (String) sqlUtils.getData("rank", "UUID", player.getUniqueId().toString(), "players"),
                sqlUtils
        );
    }

    /**
     * @param player Target player
     * @param rank   Rank object being set
     */
    public static void setPlayerRank(Player player, Rank rank, SQLUtils sqlUtils) {
        sqlUtils.setData(rank.getId(), "UUID", player.getUniqueId().toString(), "rank", "players");
        RankEvent rankEvent = new RankEvent(player, rank);
        Bukkit.getPluginManager().callEvent(rankEvent);
    }

    /**
     * @param player Target player
     * @param rankId id of rank being set
     */
    public static void setPlayerRank(Player player, String rankId, SQLUtils sqlUtils) {
        sqlUtils.setData(rankId, "UUID", player.getUniqueId().toString(), "rank", "players");
        RankEvent rankEvent = new RankEvent(player, getRank(rankId, sqlUtils));
        Bukkit.getPluginManager().callEvent(rankEvent);
    }

    public static void setPlayerNameColour(Player player, String colour, SQLUtils sqlUtils) {
        sqlUtils.setData(colour, "UUID", player.getUniqueId().toString(), "nameColour", "players");
    }

    /**
     * @param player Target player
     * @apiNote This method updates the players rank and nameColour thus must be called after setting either
     */
    public static void loadPlayerRank(Player player, SQLUtils sqlUtils) {
        String rankId = (String) sqlUtils.getData(
                "rank", "UUID", player.getUniqueId().toString(), "players"
        );
        Rank rank = getRank(rankId, sqlUtils);

        //get colours and names
        ChatColor n = readColour((String) sqlUtils.getData(
                "nameColour", "UUID", player.getUniqueId().toString(), "players"
        ));
        ChatColor p = rank.getPrimary();
        ChatColor s = rank.getSecondary();
        String lb = rank.getlBrace();
        if (lb == null) lb = "[";
        String rb = rank.getrBrace();
        if (rb == null) lb = "]";
        String d = rank.getDisplay();
        String name = player.getName();

        String finalName = s + lb + p + d + s + rb + n + " " + name + ChatColor.WHITE + "";
        player.setPlayerListName(finalName);
        player.setDisplayName(finalName);
    }

    /**
     * @param target   Target player
     * @param sqlUtils Instance of the sqlUtils
     * @return Returns the rank object of the next rank
     */
    public static Rank getNextRank(Player target, SQLUtils sqlUtils) {
        Rank currentRank = getRank(target, sqlUtils);
        return getRank(getRank(currentRank.getId(), sqlUtils).getNextRankId(), sqlUtils);
    }

    /**
     * @param rank rank object being checked for
     * @param sqlUtils sql connection
     * @return true if it exists and false otherwise
     */
    public static boolean rankExist(Rank rank, SQLUtils sqlUtils) {
        return sqlUtils.rowExists("rankID", rank.getId(), "rankPresets");
    }

    /**
     * @param rankId rank unique id being checked for
     * @param sqlUtils sql connection
     * @return true if it exists and false otherwise
     */
    public static boolean rankExist(String rankId, SQLUtils sqlUtils) {
        return sqlUtils.rowExists("rankID", rankId, "rankPresets");
    }

    public static List<Rank> getRanks(SQLUtils sqlUtils) {
        List<Rank> rankList = new ArrayList<>();
        List<Object> rankIds = sqlUtils.getColumn("rankID", "rankPresets");
        int number = sqlUtils.countRows("rankPresets");
        for (int i = 0; i < number; i++)
            rankList.add(getRank((String) rankIds.get(i), sqlUtils));
        return rankList;
    }

    /**
     * @param color Converts string of a ChatColour name to a ChatColour
     */
    public static ChatColor readColour(String color) {
        if (color == null) {
            return ChatColor.WHITE;
        }
        switch (color.toLowerCase()) {
            case "dark_red":
                return ChatColor.DARK_RED;
            case "red":
                return ChatColor.RED;
            case "gold":
                return ChatColor.GOLD;
            case "yellow":
                return ChatColor.YELLOW;
            case "dark_green":
                return ChatColor.DARK_GREEN;
            case "green":
                return ChatColor.GREEN;
            case "aqua":
                return ChatColor.AQUA;
            case "dark_aqua":
                return ChatColor.DARK_AQUA;
            case "dark_blue":
                return ChatColor.DARK_BLUE;
            case "blue":
                return ChatColor.BLUE;
            case "light_purple":
                return ChatColor.LIGHT_PURPLE;
            case "dark_purple":
                return ChatColor.DARK_PURPLE;
            case "white":
                return ChatColor.WHITE;
            case "gray":
                return ChatColor.GRAY;
            case "dark_gray":
                return ChatColor.DARK_GRAY;
            case "black":
                return ChatColor.BLACK;
            default:
                Bukkit.getLogger().warning("'" + color + "' is an invalid colour! try reloading the plugin.");
                Bukkit.getLogger().warning("Defaulting to white!");
                return ChatColor.WHITE;
        }
    }

    /**
     * @param player target player
     * @param name   name of advancement
     * @return true if the target has the advancement and false if they don't
     */
    public static boolean hasAdvancement(Player player, String name) {
        // name should be something like minecraft:husbandry/break_diamond_hoe
        Advancement a = getAdvancement(name);
        if (a == null) {
            // advancement does not exists.
            return false;
        }
        AdvancementProgress progress = player.getAdvancementProgress(a);
        // getting the progress of this advancement.
        return progress.isDone();
        //returns true or false.
    }

    /**
     * @param name String name of advancement must be in 'minecraft:story/mine_diamond' format
     * @return advancement if it exists and null if it doesn't
     */
    public static Advancement getAdvancement(String name) {
        Iterator<Advancement> it = Bukkit.getServer().advancementIterator();
        // gets all 'registered' advancements on the server.
        while (it.hasNext()) {
            // loops through these.
            Advancement a = it.next();
            if (a.getKey().toString().equalsIgnoreCase(name)) {
                //checks if one of these has the same name as the one you asked for. If so, this is the one it will return.
                return a;
            }
        }
        return null;
    }
}
