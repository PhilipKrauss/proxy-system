package it.philipkrauss.proxysystem.provider.permission;

import com.google.common.collect.Maps;

import java.util.Map;

public class PermissionProvider {

    public static PermissionProvider create() {
        return new PermissionProvider();
    }

    private final Map<String, String> prefix = Maps.newHashMap();
    private final Map<String, String> color = Maps.newHashMap();

    private PermissionProvider() {
        // teamleitung
        this.prefix.put("Admin", "§4Admin §8● §4");
        this.color.put("Admin", "§4");
        this.prefix.put("SrModerator", "§9SrMod §8● §9");
        this.color.put("SrModerator", "§9");
        this.prefix.put("Developer", "§bDev §8● §b");
        this.color.put("Developer", "§b");
        this.prefix.put("SrBuilder", "§eSrBuild §8● §e");
        this.color.put("SrBuilder", "§e");
        // team
        this.prefix.put("Moderator", "§9Mod §8● §9");
        this.color.put("Moderator", "§9");
        this.prefix.put("Supporter", "§3Support §8● §3");
        this.color.put("Supporter", "§3");
        this.prefix.put("Builder", "§eBuild §8● §e");
        this.color.put("Builder", "§e");
        // players
        this.prefix.put("VIP", "§5VIP §8● §5");
        this.color.put("VIP", "§5");
        this.prefix.put("Superior", "§2Superior §8● §2");
        this.color.put("Superior", "§2");
        this.prefix.put("Prime", "§6Prime §8● §6");
        this.color.put("Prime", "§6");
        // default
        this.prefix.put("Spieler", "§7");
        this.color.put("Spieler", "§7");
    }

    public String getPrefix(String group) {
        return this.prefix.getOrDefault(group, "§7");
    }

    public String getColor(String group) {
        return this.color.getOrDefault(group, "§7");
    }

}
