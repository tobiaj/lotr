package se.kth.id2203.failureDetector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.bootstrapping.BSTimeout;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.LookupTable;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;
import sun.nio.ch.Net;

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

    private LookupTable lut;

    private HashSet<NetAddress> leaderActive = new HashSet<>();
    private HashSet<NetAddress> passiveActive = new HashSet<>();
    private HashSet<NetAddress> leaderSuspects = new HashSet<>();
    private HashSet<NetAddress> passiveSuspects = new HashSet<>();

    private long timeout;
    private long delay = 2000;

    private UUID timeoutId;


    //****Handlers****//
    protected final Handler<BSTimeout> timeoutHandler = new Handler<BSTimeout>() {

        @Override
        public void handle(BSTimeout e) {

            boolean suspected = false;

            if (!leaderSuspects.isEmpty()) {
                suspected = true;
                announceLeaderFailure();
            }

            if (!passiveSuspects.isEmpty()){
                suspected = true;
                announcePassiveFailure();
            }

            checkLeaders();
            checkPassive();

            if (suspected){
                restartHeartBeat(delay);
            }
        }

    };

    protected final Handler<StartFailureDetector> startFailureDetectorHandler = new Handler<StartFailureDetector>() {
        @Override
        public void handle(StartFailureDetector startFailureDetector) {
            leaderActive = startFailureDetector.getLeaderNodes();
            passiveActive = startFailureDetector.getPassiveNodes();
            lut = startFailureDetector.getLut();
            startHeartbeat(0);

        }
    };

    protected final ClassMatchedHandler<LeaderFailureCheckResponse, Message> leaderFailureCheckResponseHandler = new ClassMatchedHandler<LeaderFailureCheckResponse, Message>() {
        @Override
        public void handle(LeaderFailureCheckResponse leaderFailureCheckResponse, Message message) {
            NetAddress sender = leaderFailureCheckResponse.getSender();
            LOG.info("IN LEADER, GOT HEARTBEAT FROM " + sender);
            //check if sender is in suspect

            if (!leaderActive.contains(sender)){
                leaderActive.add(sender);
                for (NetAddress address : leaderActive){
                    trigger(new Message(self, address, new Unsuspected(sender)), net);
                }
            }

            leaderSuspects.remove(sender);
            leaderActive.add(sender);
        }
    };

    protected final ClassMatchedHandler<PassiveFailureCheckResponse, Message> passiveFailureCheckResponseHandler = new ClassMatchedHandler<PassiveFailureCheckResponse, Message>() {
        @Override
        public void handle(PassiveFailureCheckResponse passiveFailureCheckResponse, Message message) {
            NetAddress sender = passiveFailureCheckResponse.getSender();
            LOG.info("IN PASSIVE, GOT HEARTBEAT FROM " + sender);
            //check if sender is in suspect

            if (leaderActive.contains((sender))){
                passiveSuspects.remove(sender);
                passiveActive.remove(sender);

            }
            else if (!passiveActive.contains(sender)){
                passiveActive.add(sender);
                for (NetAddress address : passiveActive){
                    trigger(new Message(self, address, new Unsuspected(sender)), net);
                }

            }
            else{
                passiveSuspects.remove(sender);
                passiveActive.add(sender);
            }


        }
    };

    protected final ClassMatchedHandler<SuspectResponse, Message> suspectResponse = new ClassMatchedHandler<SuspectResponse, Message>() {
        @Override
        public void handle(SuspectResponse suspectResponse, Message message) {
            LOG.info("GOT NEW LEADER FROM A GROUP, LEADER IS " + suspectResponse.getAddress());
            leaderActive.add(suspectResponse.getAddress());

            if (passiveActive.contains(suspectResponse.getAddress())){
                passiveActive.remove(suspectResponse.getAddress());
            }
            trigger(new UpdateLeadersInSupervisor(leaderActive), failure);
        }
    };


//*****Functions*****//

    private void startHeartbeat(long delay) {
        timeout = (config().getValue("id2203.project.keepAlivePeriod", Long.class) * 1) + delay;
        LOG.info("TIMEOUT IN FAILUREDETECTOR IS " + timeout + " and delay is " + delay);
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(timeout, timeout);
        spt.setTimeoutEvent(new BSTimeout(spt));
        trigger(spt, timer);
        timeoutId = spt.getTimeoutEvent().getTimeoutId();

    }

    private void restartHeartBeat(long delay) {
        trigger(new CancelPeriodicTimeout(timeoutId), timer);

        timeout = timeout + delay;
        LOG.info("RESET TIMEOUT IS NOW " + timeout + " and delay is " + delay);
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(timeout, timeout);
        spt.setTimeoutEvent(new BSTimeout(spt));
        trigger(spt, timer);
        timeoutId = spt.getTimeoutEvent().getTimeoutId();


    }

    private void checkLeaders() {

        LOG.info("ACTIVE LEADERS FAILURE CHECK");
        for(NetAddress address : leaderActive){
            leaderSuspects.add(address);
            trigger(new Message(self, address, new LeaderFailureCheck(address)), net);
        }

    }

    private void checkPassive() {

        LOG.info("PASSIVE NODES FAILURE CHECK ");
        for(NetAddress address : passiveActive){
            passiveSuspects.add(address);
            trigger(new Message(self,address,new PassiveFailureCheck(address)), net);

        }

    }

    private void announcePassiveFailure() {
        LOG.info("FAAAAAAAAAAAAAAAAAAAAAAAAAAAil!!!!!!!!!!!!!!!! PASSIVE");
        HashSet<NetAddress> temp;

        if (!passiveSuspects.isEmpty()){
            temp = passiveSuspects;
            for (NetAddress suspectAddress : temp) {
                LOG.info("SUSPECT PASSIVE ADDRESS IS " + suspectAddress);
                passiveActive.remove(suspectAddress);
                for (NetAddress address : lut.getNodes()) {
                    trigger(new Message(self, address, new Suspected(suspectAddress)), net);
                }
            }
        }

    }

    private void announceLeaderFailure() {
        LOG.info("FAAAAAAAAAAAAAAAAAAAAAAAAAAAil!!!!!!!!!!!!!!!! LEADER");
        HashSet<NetAddress> temp;

        if (!leaderSuspects.isEmpty()){
            temp = leaderSuspects;
            for (NetAddress suspectAddress : temp) {
                LOG.info("SUSPECT LEADER ADDRESS IS " + suspectAddress);
                leaderActive.remove(suspectAddress);
                for (NetAddress address : lut.getNodes()) {
                    trigger(new Message(self, address, new Suspected(suspectAddress)), net);
                }
            }
        }
    }

    {
        subscribe(timeoutHandler, timer);
        subscribe(startFailureDetectorHandler, failure);
        subscribe(leaderFailureCheckResponseHandler, net);
        subscribe(passiveFailureCheckResponseHandler, net);
        subscribe(suspectResponse, net);
    }

}
