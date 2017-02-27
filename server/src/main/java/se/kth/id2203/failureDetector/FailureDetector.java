package se.kth.id2203.failureDetector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.bootstrapping.BSTimeout;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
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

    private HashSet<NetAddress> leadernodes;
    private HashSet<NetAddress> passivenodes;
    private HashSet<NetAddress> leaderActive = new HashSet<>();
    private HashSet<NetAddress> passiveActive = new HashSet<>();
    private HashSet<NetAddress> leaderSuspects = new HashSet<>();
    private HashSet<NetAddress> passiveSuspects = new HashSet<>();

    private long delay = 2000;

    private UUID timeoutId;


    //****Handlers****//
    protected final Handler<BSTimeout> timeoutHandler = new Handler<BSTimeout>() {

        @Override
        public void handle(BSTimeout e) {

            checkLeaders();

            checkPassive();

            //startHeartbeat(delay);

        }
    };

    protected final Handler<StartFailureDetector> startFailureDetectorHandler = new Handler<StartFailureDetector>() {
        @Override
        public void handle(StartFailureDetector startFailureDetector) {
            leadernodes = startFailureDetector.getLeaderNodes();
            passivenodes = startFailureDetector.getPassiveNodes();
            startHeartbeat(0);

        }
    };

    protected final ClassMatchedHandler<LeaderFailureCheckResponse, Message> leaderFailureCheckResponseHandler = new ClassMatchedHandler<LeaderFailureCheckResponse, Message>() {
        @Override
        public void handle(LeaderFailureCheckResponse leaderFailureCheckResponse, Message message) {
            NetAddress sender = leaderFailureCheckResponse.getSender();
            LOG.info("IN LEADER, GOT HEARTBEAT FROM " + sender);
            leaderSuspects.remove(sender);
            leaderActive.add(sender);
        }
    };

    protected final ClassMatchedHandler<PassiveFailureCheckResponse, Message> passiveFailureCheckResponseHandler = new ClassMatchedHandler<PassiveFailureCheckResponse, Message>() {
        @Override
        public void handle(PassiveFailureCheckResponse passiveFailureCheckResponse, Message message) {
            NetAddress sender = passiveFailureCheckResponse.getSender();
            LOG.info("IN PASSIVE, GOT HEARTBEAT FROM " + sender);
            passiveSuspects.remove(sender);
            passiveActive.add(sender);
        }
    };


//*****Functions*****//

    private void startHeartbeat(long delay) {
        long timeout = (config().getValue("id2203.project.keepAlivePeriod", Long.class) * 12) + delay;
        LOG.info("TIMEOUT IN FAILUREDETECTOR IS " + timeout + " and delay is " + delay);
        SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(timeout, timeout);
        spt.setTimeoutEvent(new BSTimeout(spt));
        trigger(spt, timer);
        timeoutId = spt.getTimeoutEvent().getTimeoutId();

    }

    private void checkLeaders() {

        if (leaderSuspects.isEmpty()){

            if(leaderActive.isEmpty()){
                LOG.info("FIRST TIME LEADER");
                for(NetAddress address : leadernodes){
                    leaderSuspects.add(address);
                    trigger(new Message(self,address,new LeaderFailureCheck(address)),net);
                }
            }else{
                LOG.info("ACTIVE LEADERS FAILURE CHECK");
                for(NetAddress address : leaderActive){
                    leaderSuspects.add(address);
                    trigger(new Message(self,address,new LeaderFailureCheck(address)),net);
                }

            }
        }
        else{
            LOG.info("LEADER SUSPECTS " + leaderSuspects.toString());
            announceFailure();
        }

    }

    private void checkPassive() {

        if (passiveSuspects.isEmpty()){

            if(passiveActive.isEmpty()){
                LOG.info("FIRST TIME PASSIVE");
                for(NetAddress address : passivenodes){
                    passiveSuspects.add(address);
                    trigger(new Message(self,address,new PassiveFailureCheck(address)),net);
                }
            }else{
                LOG.info("PASSIVE NODES FAILURE CHECK ");
                for(NetAddress address : passiveActive){
                    passiveSuspects.add(address);
                    trigger(new Message(self,address,new PassiveFailureCheck(address)),net);
                }

            }
        }
        else{
            LOG.info("PASSIVE SUSPECTS " + passiveSuspects.toString());
        }

    }


    private void announceFailure() {
        LOG.info("FAAAAAAAAAAAAAAAAAAAAAAAAAAAil!!!!!!!!!!!!!!!! ");

    }

    {
        subscribe(timeoutHandler, timer);
        subscribe(startFailureDetectorHandler, failure);
        subscribe(leaderFailureCheckResponseHandler, net);
        subscribe(passiveFailureCheckResponseHandler, net);

    }

}
