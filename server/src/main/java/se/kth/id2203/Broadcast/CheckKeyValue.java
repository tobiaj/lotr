package se.kth.id2203.Broadcast;

import se.kth.id2203.kvstore.ValueSeq;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

/**
 * Created by tobiaj on 2017-03-05.
 */
public class CheckKeyValue implements KompicsEvent, Serializable {

    public ValueSeq getValueSeq() {
        return valueSeq;
    }

    public ValueSeq valueSeq;

    public String getKey() {
        return key;
    }

    public String key;

    public CheckKeyValue(String key, ValueSeq valueSeq) {
        this.key = key;
        this.valueSeq = valueSeq;

    }
}
