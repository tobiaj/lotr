package se.kth.id2203.failureDetector;

import com.google.common.collect.ImmutableSet;
import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.util.HashSet;

/**
 * Created by tobiaj on 2017-02-24.
 */
public class StartFailureDetector implements KompicsEvent{

    public HashSet<NetAddress> getNodes() {
        return nodes;
    }

    private HashSet<NetAddress> nodes;

    public StartFailureDetector(HashSet<NetAddress> nodes){
        this.nodes = nodes;
    }
}
