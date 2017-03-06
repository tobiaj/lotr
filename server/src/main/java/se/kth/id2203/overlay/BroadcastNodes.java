package se.kth.id2203.overlay;

import se.kth.id2203.Broadcast.Broadcast;
import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.util.LinkedList;

/**
 * Created by tobiaj on 2017-03-02.
 */
public class BroadcastNodes implements KompicsEvent {

    public LinkedList<NetAddress> getAddresses() {
        return addresses;
    }

    private LinkedList<NetAddress> addresses;

    public BroadcastNodes(LinkedList<NetAddress> addresses){
        this.addresses = addresses;
    }
}
