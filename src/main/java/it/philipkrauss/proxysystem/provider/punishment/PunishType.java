package it.philipkrauss.proxysystem.provider.punishment;

import java.util.Arrays;

public enum PunishType {

    BAN (1, "Ban"),
    MUTE (2, "Mute"),
    KICK (3, "Kick"),
    EXEMPTION (4, "Exemption"),
    UNPUNISH (5, "Unpunish");

    public static PunishType getTypeById(int id) {
        return Arrays.stream(values()).filter(punishType -> punishType.getId() == id).findFirst().orElse(null);
    }

    private final int id;
    private final String displayName;

    PunishType(int id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getId() {
        return id;
    }

}
