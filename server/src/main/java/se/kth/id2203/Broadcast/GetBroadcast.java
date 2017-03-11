package se.kth.id2203.Broadcast;

import se.kth.id2203.kvstore.ValueSeq;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.supervisor.GetLeaders;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;
import java.util.Collection;

/**
 * Created by tobiaj on 2017-03-05.
 */
public class GetBroadcast implements KompicsEvent, Serializable{

    public String getKey() {
        return key;
    }

    public ValueSeq getValueSeq() {
        return valueSeq;
    }

    public String key;
    public ValueSeq valueSeq;

    Collection<NetAddress> nodes;

    public GetBroadcast(String key, ValueSeq valueSeq, Collection<NetAddress> nodes){
        this.key = key;
        this.valueSeq = valueSeq;
        this.nodes = nodes;

    }
}
