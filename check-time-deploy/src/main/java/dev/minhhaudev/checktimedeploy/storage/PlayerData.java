package dev.minhhaudev.checktimedeploy.storage;

import java.time.Instant;

public class PlayerData {
    public final String uuid;
    public final String name;
    public final Instant firstJoin;
    public final Instant lastJoin;
    public final Instant lastQuit;
    public final int joinCount;
    public final int quitCount;

    public PlayerData(String uuid, String name, Instant firstJoin, Instant lastJoin, Instant lastQuit, int joinCount, int quitCount) {
        this.uuid = uuid;
        this.name = name;
        this.firstJoin = firstJoin;
        this.lastJoin = lastJoin;
        this.lastQuit = lastQuit;
        this.joinCount = joinCount;
        this.quitCount = quitCount;
    }
}
