package se.kth.id2203.heartbeat;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;
import sun.nio.ch.Net;

import java.io.Serializable;

/**
 * Created by tobiaj on 2017-02-23.
 */
public class HeartbeatResponse implements KompicsEvent, Serializable{

    public boolean isAlive() {
        return alive;
    }

    public NetAddress getSender() {
        return sender;
    }

    boolean alive;
    NetAddress sender;

    public HeartbeatResponse(boolean alive, NetAddress sender){
        this.alive = alive;
        this.sender = sender;
    }
}
