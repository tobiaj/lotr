package se.kth.id2203.Broadcast;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.KompicsEvent;

import java.text.CollationElementIterator;
import java.util.Collection;
import java.util.HashSet;

/**
 * Created by habtu on 2017-02-27.
 */
public class Broadcast implements KompicsEvent {


    int key;
    String value;

    Collection<NetAddress> nodes;

    public Broadcast(int key, String value, Collection<NetAddress> nodes){
        this.key = key;
        this.value = value;
        this.nodes = nodes;
    }
}
