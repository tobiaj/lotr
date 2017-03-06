package se.kth.id2203.Broadcast;

import se.kth.id2203.kvstore.ValueSeq;
import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.Collection;

/**
 * Created by habtu on 2017-02-27.
 */
public class Broadcast implements KompicsEvent, Serializable {

    public String getKey() {
        return key;
    }

    String key;

    public ValueSeq getValueSeq() {
        return valueSeq;
    }

    ValueSeq valueSeq;

    Collection<NetAddress> nodes;

    public Broadcast(String key, ValueSeq valueSeq, Collection<NetAddress> nodes){
        this.key = key;
        this.valueSeq = valueSeq;
        this.nodes = nodes;
    }
}
