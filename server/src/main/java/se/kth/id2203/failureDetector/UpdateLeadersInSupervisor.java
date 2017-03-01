package se.kth.id2203.failureDetector;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.HashSet;

/**
 * Created by tobiaj on 2017-02-28.
 */
public class UpdateLeadersInSupervisor implements KompicsEvent, Serializable {

    public HashSet<NetAddress> getLeaders() {
        return leaders;
    }

    private HashSet<NetAddress> leaders;

    public UpdateLeadersInSupervisor(HashSet<NetAddress> leaders){
        this.leaders = leaders;

    }
}
