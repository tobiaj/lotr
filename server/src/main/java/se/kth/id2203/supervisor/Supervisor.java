package se.kth.id2203.supervisor;

import com.larskroll.common.J6;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.failureDetector.FailurePort;
import se.kth.id2203.failureDetector.StartFailureDetector;
import se.kth.id2203.failureDetector.UpdateLeadersInSupervisor;
import se.kth.id2203.kvstore.OpResponse;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.LookupTable;
import se.kth.id2203.overlay.RouteMsg;
import se.kth.id2203.overlay.Routing;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import sun.nio.ch.Net;

import java.util.*;

/**
 * Created by tobiaj on 2017-02-21.
 */
public class Supervisor extends ComponentDefinition{

    final static Logger LOG = LoggerFactory.getLogger(Supervisor.class);

    //******* Ports ******
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);

    protected final Positive<SupervisorPort> supervisor = requires(SupervisorPort.class);
    protected final Negative<FailurePort> failure = provides(FailurePort.class);

    //******* Fields ******
    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);

    private LookupTable lut;
    private HashMap<Integer, Range> keysToGroups = new HashMap<>();
    private HashSet<NetAddress> leaderNodes = new HashSet<>();
    private HashSet<NetAddress> passiveNodes = new HashSet<>();
    private int numberOfLeaders;

    private UUID timeoutId;

    //******* Handlers ******


    protected final ClassMatchedHandler<RouteMsg, Message> routeHandler = new ClassMatchedHandler<RouteMsg, Message>() {

        @Override
        public void handle(RouteMsg content, Message context) {
            LOG.info("I AM IN ROUTE HANDLER and key is: " + content.key);

            int partition = findCorrectPartition(content.key);

            Collection<NetAddress> nodes = lut.getNodes(partition);

            LOG.info("GOT PARTIION " + nodes.toString());


            NetAddress target = null;

            for (NetAddress address : nodes){
                if (leaderNodes.contains(address)){
                    target = address;
                }
            }
            LOG.info("TARGET IS " + target);
            LOG.info("Forwarding message for key {} with operation {} to {}", content.key, content.msg, target);
            trigger(new Message(context.getSource(), target, content.msg), net);

        }
    };

    protected final Handler<StartSupervisor> startSupervisorHandler = new Handler<StartSupervisor>() {
        @Override
        public void handle(StartSupervisor startSupervisor) {
            LOG.info("I AM SUPERVISOR");
            lut = startSupervisor.getLookupTable();
            LOG.info("I GOT LOOKUPTABLE " + lut.toString());
            numberOfLeaders = lut.getNumberOfGroups();
            createKeyRanges();

            getStartingLeaders();
        }
    };

    protected final ClassMatchedHandler<GetLeaders, Message> getLeadersResponse = new ClassMatchedHandler<GetLeaders, Message>() {
        @Override
        public void handle(GetLeaders getLeaders, Message message) {
            LOG.info("SUPERVISOR GOT LEADER: " + message.getSource());
            leaderNodes.add(message.getSource());
            if (leaderNodes.size() == numberOfLeaders) {
                startFailureDetector();
            }

        }
    };

    protected final Handler<UpdateLeadersInSupervisor> updateLeadersInSupervisorHandler = new Handler<UpdateLeadersInSupervisor>() {
        @Override
        public void handle(UpdateLeadersInSupervisor updateLeadersInSupervisor) {
            LOG.info("SUPERVISOR GOT NEW LEADERS " + leaderNodes.toString());
            leaderNodes = updateLeadersInSupervisor.getLeaders();
        }
    };

    private void getStartingLeaders() {
        for (NetAddress address : lut.getNodes()){
            trigger(new Message(self, address, new GetLeaders()), net);
        }
    }

    private void startFailureDetector() {
        for (NetAddress address : lut.getNodes()){
            if (!leaderNodes.contains(address)){
                passiveNodes.add(address);
            }
        }

        trigger(new StartFailureDetector(leaderNodes, passiveNodes, lut), failure);
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

    private int findCorrectPartition(String input) {

        String[] split = input.split(" ");
        int group;
        int hashKey;

        if (split.length > 1) {
            String key = split[0];
            String value = split[1];
            LOG.info("SPLIT : " + split[0]);
            LOG.info("SPLIT : " + split[1]);

            hashKey = key.hashCode();
            LOG.info("HASHKEY : " + hashKey);

            group = hashKey % lut.getNumberOfGroups();

            LOG.info("GROUP RESPONSIBLE IS " + group);

            return group;
        }

        else {

            hashKey = input.hashCode();
            group = hashKey % lut.getNumberOfGroups();


        }

        return group;

    }


    {
        subscribe(routeHandler, net);
        subscribe(startSupervisorHandler, supervisor);
        subscribe(getLeadersResponse, net);
        subscribe(updateLeadersInSupervisorHandler, failure);

    }

}
