package com.horrortown.entity;

/** What a townsfolk does for a living; decides their clothes and what they trade. */
public enum TownRole {
    FISHER("fisher"),
    SALTER("salter"),
    NET_MENDER("net_mender"),
    PORTER("porter"),
    /** 庙祝, the temple keeper */
    KEEPER("keeper");

    private final String name;

    TownRole(String name) {
        this.name = name;
    }

    public String getSerializedName() {
        return this.name;
    }

    public static TownRole byId(int id) {
        TownRole[] roles = values();
        return roles[Math.floorMod(id, roles.length)];
    }
}
