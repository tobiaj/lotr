package se.kth.id2203.failureDetector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.bootstrapping.BSTimeout;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.LookupTable;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;

import java.util.HashSet;
import java.util.UUID;

/**
 * Created by tobiaj on 2017-02-24.
 */
public class FailureDetector extends ComponentDefinition {

    final static Logger LOG = LoggerFactory.getLogger(FailureDetector.class);

    //******* Ports ******
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);

    protected final Positive<FailurePort> failure = requires(FailurePort.class);

    //******* Fields ******
    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);

    private HashSet<NetAddress> nodes;
    private HashSet<NetAddress> active = new HashSet<>();
    private HashSet<NetAddress> suspects = new HashSet<>();

    private UUID timeoutId;


    //****Handlers****//
    protected final Handler<BSTimeout> timeoutHandler = new Handler<BSTimeout>() {

        @Override
        public void handle(BSTimeout e) {
            if (active.isEmpty()){
                LOG.info("Failure detector sending out check FIRST TIME MY NODES ARE " + nodes.toString());
                for (NetAddress address : nodes){
                    trigger(new Message(self, address, new FailureCheck(address)), net);
                }
            }
            else{
                LOG.info("Failure detector sending out check TO ONLY ACTIVE LEADERS");
                for (NetAddress address : active){
                    trigger(new Message(self, address, new FailureCheck(address)), net);
                }

            }
        }
    };

    protected final Handler<StartFailureDetector> startFailureDetectorHandler = new Handler<StartFailureDetector>() {
        @Override
        public void handle(StartFailureDetector startFailureDetector) {
            nodes = startFailureDetector.getNodes();
            LOG.info("I am FailureDetector and i got LUT: \n " + nodes.toString());

            startHeartbeat();

        }
    };

    protected final ClassMatchedHandler<FailureCheckResponse, Message> failureCheckResponseHandler = new ClassMatchedHandler<FailureCheckResponse, Message>() {
        @Override
        public void handle(FailureCheckResponse failureCheckResponse, Message message) {
            NetAddress sender = failureCheckResponse.getSender();
            LOG.info("GOT HEARTBEAT FROM " + sender);
            active.add(sender);
        }
    };

    //*****Functions*****//

    private void startHeartbeat() {
        long timeout = (config().getValue("id2203.project.keepAlivePeriod", Long.class) * 6);
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(timeout, timeout);
        spt.setTimeoutEvent(new BSTimeout(spt));
        trigger(spt, timer);
        timeoutId = spt.getTimeoutEvent().getTimeoutId();

    }

    {
        subscribe(timeoutHandler, timer);
        subscribe(startFailureDetectorHandler, failure);
        subscribe(failureCheckResponseHandler, net);

    }

}
