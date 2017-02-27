package se.kth.id2203.Broadcast;

import se.sics.kompics.KompicsEvent;

/**
 * Created by habtu on 2017-02-27.
 */
public class Broadcast implements KompicsEvent {


    int key;
    String value;

    public Broadcast(int key, String value){
        this.key = key;
        this.value = value;
    }
}
