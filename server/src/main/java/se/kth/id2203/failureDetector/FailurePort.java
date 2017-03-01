package se.kth.id2203.failureDetector;

import se.sics.kompics.PortType;

/**
 * Created by tobiaj on 2017-02-23.
 */
public class FailurePort extends PortType {

    {
        indication(LeaderFailureCheck.class);
        indication(PassiveFailureCheck.class);
        indication(StartFailureDetector.class);
        request(UpdateLeadersInSupervisor.class);
    }
}
