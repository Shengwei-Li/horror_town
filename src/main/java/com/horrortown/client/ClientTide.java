package com.horrortown.client;

/** Whether the server says a tide night is on in this world (tints the fog, raises the Watcher). */
public final class ClientTide {
    private static boolean active;

    private ClientTide() {
    }

    public static void set(boolean value) {
        active = value;
    }

    public static boolean active() {
        return active;
    }
}
