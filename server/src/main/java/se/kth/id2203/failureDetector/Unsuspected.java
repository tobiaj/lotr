package se.kth.id2203.failureDetector;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

/**
 * Created by tobiaj on 2017-02-28.
 */
public class Unsuspected implements KompicsEvent, Serializable {

    public NetAddress getAddress() {
        return address;
    }

    private NetAddress address;

    public Unsuspected(NetAddress address) {
        this.address = address;

    }
}
