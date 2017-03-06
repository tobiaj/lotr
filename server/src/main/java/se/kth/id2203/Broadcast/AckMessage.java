package se.kth.id2203.Broadcast;

import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

/**
 * Created by tobiaj on 2017-03-05.
 */
public class AckMessage implements KompicsEvent, Serializable {

    public String getKey() {
        return key;
    }

    public String key;

    public AckMessage(String key){
        this.key = key;
    }


}
