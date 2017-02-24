package se.kth.id2203.failureDetector;

import se.sics.kompics.PortType;

/**
 * Created by tobiaj on 2017-02-23.
 */
public class FailurePort extends PortType {

    {
        indication(FailureCheck.class);
        indication(StartFailureDetector.class);
        //indication(HeartbeatResponse.class);
    }
}
