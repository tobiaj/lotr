package se.kth.id2203.failureDetector;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

/**
 * Created by tobiaj on 2017-02-23.
 */
public class LeaderFailureCheckResponse implements KompicsEvent, Serializable{

    public boolean isAlive() {
        return alive;
    }

    public NetAddress getSender() {
        return sender;
    }

    boolean alive;
    NetAddress sender;

    public LeaderFailureCheckResponse(boolean alive, NetAddress sender){
        this.alive = alive;
        this.sender = sender;
    }
}
