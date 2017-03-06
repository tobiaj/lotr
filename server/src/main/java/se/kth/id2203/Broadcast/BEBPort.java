package se.kth.id2203.Broadcast;

import se.sics.kompics.PortType;

/**
 * Created by habtu on 2017-02-27.
 */
public class BEBPort extends PortType {

    {
        indication(Broadcast.class);
        indication(GetBroadcast.class);
    }
}
