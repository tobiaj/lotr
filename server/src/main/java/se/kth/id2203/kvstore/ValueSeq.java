package se.kth.id2203.kvstore;

import java.io.Serializable;

/**
 * Created by tobiaj on 2017-03-05.
 */
public class ValueSeq implements Serializable{

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public String value;
    public int sequenceNumber;

    public ValueSeq(String value, int sequenceNumber) {
        this.value = value;
        this.sequenceNumber = sequenceNumber;

    }
}
