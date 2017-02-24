package se.kth.id2203.supervisor;

import se.sics.kompics.PortType;

/**
 * Created by tobiaj on 2017-02-21.
 */
public class SupervisorPort extends PortType {

    {
        indication(StartSupervisor.class);
    }
}
