package se.kth.id2203.heartbeat;

import se.sics.kompics.PortType;
import se.sics.kompics.sl.Port;

/**
 * Created by tobiaj on 2017-02-23.
 */
public class HeartbeatPort extends PortType {

    {
        indication(HeartBeat.class);
        indication(HeartbeatResponse.class);
    }
}
