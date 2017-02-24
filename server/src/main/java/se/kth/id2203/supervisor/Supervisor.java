package se.kth.id2203.supervisor;

import com.google.common.collect.TreeMultimap;
import com.google.common.primitives.UnsignedInteger;
import com.larskroll.common.J6;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.bootstrapping.BSTimeout;
import se.kth.id2203.heartbeat.HeartBeat;
import se.kth.id2203.heartbeat.HeartbeatPort;
import se.kth.id2203.heartbeat.HeartbeatResponse;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.LookupTable;
import se.kth.id2203.overlay.RouteMsg;
import se.kth.id2203.overlay.Routing;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;
import sun.nio.ch.Net;

import java.util.*;

/**
 * Created by tobiaj on 2017-02-21.
 */
public class Supervisor extends ComponentDefinition{

    final static Logger LOG = LoggerFactory.getLogger(Supervisor.class);

    //******* Ports ******
    protected final Negative<Routing> route = provides(Routing.class);
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);

    protected final Negative<HeartbeatPort> heartbeatPort = provides(HeartbeatPort.class);
    protected final Positive<HeartbeatPort> heartbeatReceive = requires(HeartbeatPort.class);

    protected final Positive<SupervisorPort> supervisor = requires(SupervisorPort.class);

    //******* Fields ******
    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);

    private LookupTable lut;
    private HashMap<Integer, Range> keysToGroups = new HashMap<>();
    private HashSet<NetAddress> overlayLeaders = new HashSet<>();

    private UUID timeoutId;

    //******* Handlers ******

    protected final Handler<BSTimeout> timeoutHandler = new Handler<BSTimeout>() {

        @Override
        public void handle(BSTimeout e) {
            if (overlayLeaders.isEmpty()){
                for (NetAddress address : lut.getNodes()){
                    LOG.info("I AM SUPERVISOR I GOT TIMEOUT SEDING TO OVERLAYS, TARGET IS " + address);
                    trigger(new Message(self, address, new HeartBeat(address)), net);
                }
            }
            else{

                for (NetAddress address : overlayLeaders){
                    LOG.info("I AM SUPERVISOR I GOT TIMEOUT SEDING TO OVERLAYS, TARGET IS " + address);
                    trigger(new Message(self, address, new HeartBeat(address)), net);
                }

            }
        }
    };

    protected final ClassMatchedHandler<RouteMsg, Message> routeHandler = new ClassMatchedHandler<RouteMsg, Message>() {

        @Override
        public void handle(RouteMsg content, Message context) {
            LOG.info("I AM IN ROUTE HANDLER and key is: " + content.key);
            Collection<NetAddress> partition = lut.lookup(content.key);

            LOG.info("GOT PARTIION " + partition.toString());

            /*
             NetAddress target = J6.randomElement(partition);
            LOG.info("Forwarding message for key {} to {}", content.key, target);
            trigger(new Message(context.getSource(), target, content.msg), net);*/
        }
    };
    protected final Handler<RouteMsg> localRouteHandler = new Handler<RouteMsg>() {

        @Override
        public void handle(RouteMsg event) {
            Collection<NetAddress> partition = lut.lookup(event.key);
            NetAddress target = J6.randomElement(partition);
            LOG.info("Routing message for key {} to {}", event.key, target);
            trigger(new Message(self, target, event.msg), net);
        }
    };


    protected final Handler<StartSupervisor> startSupervisorHandler = new Handler<StartSupervisor>() {
        @Override
        public void handle(StartSupervisor startSupervisor) {
            LOG.info("I AM SUPERVISOR");
            lut = startSupervisor.getLookupTable();
            LOG.info("I GOT LOOKUPTABLE " + lut.toString());
            createKeyRanges();

            startHeartbeat();

        }
    };

    private void startHeartbeat() {
        long timeout = (config().getValue("id2203.project.keepAlivePeriod", Long.class) * 2);
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(timeout, timeout);
        spt.setTimeoutEvent(new BSTimeout(spt));
        trigger(spt, timer);
        timeoutId = spt.getTimeoutEvent().getTimeoutId();
    }


    protected final ClassMatchedHandler<HeartbeatResponse, Message> heartBeatHandler = new ClassMatchedHandler<HeartbeatResponse, Message>() {
        @Override
        public void handle(HeartbeatResponse heartbeatResponse, Message message) {
            NetAddress sender = heartbeatResponse.getSender();
            LOG.info("GOT HEARTBEAT FROM " + sender);
            overlayLeaders.add(sender);
        }
    };

    {
        subscribe(routeHandler, net);
        subscribe(localRouteHandler, route);
        subscribe(startSupervisorHandler, supervisor);
        subscribe(timeoutHandler, timer);
        subscribe(heartBeatHandler, net);

    }


    private void createKeyRanges(){

        int temp = lut.getNumberOfGroups();
        LOG.info("TEMP NUMBER IS " + temp);
        int maxRange = 100000;
        int divided = maxRange / temp;

        int min = -1;
        int max = divided;

        for (int i = 0; i < temp ; i++) {
            Range range = new Range(min + 1, max);
            keysToGroups.put(i, range);

            min+= divided;
            max += divided;
        }

        for (Range range : keysToGroups.values()){
            LOG.info(" Range min is " + range.getMin() + " and max is " + range.getMax());
        }


    }

}
