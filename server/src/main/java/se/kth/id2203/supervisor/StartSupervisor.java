package se.kth.id2203.supervisor;

import se.kth.id2203.overlay.LookupTable;
import se.sics.kompics.KompicsEvent;

/**
 * Created by tobiaj on 2017-02-21.
 */
public class StartSupervisor implements KompicsEvent {

    public LookupTable getLookupTable() {
        return lookupTable;
    }

    private LookupTable lookupTable;

    public StartSupervisor(LookupTable lookupTable){
        this.lookupTable = lookupTable;
    }
}
