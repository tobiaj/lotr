package se.kth.id2203.failureDetector;

import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.KompicsEvent;

import java.io.Serializable;

/**
 * Created by tobiaj on 2017-02-27.
 */
public class PassiveFailureCheck implements KompicsEvent, Serializable{

    public NetAddress getTarget() {
        return target;
    }

    private NetAddress target;

    public PassiveFailureCheck(NetAddress target){
        this.target = target;

    }
}
