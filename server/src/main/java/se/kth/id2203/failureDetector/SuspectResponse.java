package se.kth.id2203.failureDetector;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

/**
 * Created by tobiaj on 2017-02-28.
 */
public class SuspectResponse implements KompicsEvent, Serializable {

    public NetAddress getAddress() {
        return address;
    }

    private NetAddress address;

    public SuspectResponse (NetAddress address){
        this.address = address;

    }
}
