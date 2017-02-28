package se.kth.id2203.failureDetector;

import com.google.common.collect.ImmutableSet;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.LookupTable;
import se.sics.kompics.KompicsEvent;

import java.util.HashSet;

/**
 * Created by tobiaj on 2017-02-24.
 */
public class StartFailureDetector implements KompicsEvent{


    public HashSet<NetAddress> getLeaderNodes() {
        return leaderNodes;
    }

    public HashSet<NetAddress> getPassiveNodes() {
        return passiveNodes;
    }

    private HashSet<NetAddress> leaderNodes;
    private HashSet<NetAddress> passiveNodes;

    public LookupTable getLut() {
        return lut;
    }

    private LookupTable lut;

    public StartFailureDetector(HashSet<NetAddress> leaderNodes, HashSet<NetAddress> passiveNodes, LookupTable lut){

        this.leaderNodes = leaderNodes;
        this.passiveNodes = passiveNodes;
        this.lut = lut;

    }
}
