package se.kth.id2203.Broadcast;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

import java.util.Collection;

/**
 * Created by habtu on 2017-02-27.
 */
public class BEB extends ComponentDefinition {


    final static Logger LOG = LoggerFactory.getLogger(BEB.class);

    //******* Ports ******
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);


    protected final Positive<BEBPort> leader = requires(BEBPort.class);

    //******* Fields ******
    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);

    /*Active nodes that will receive the broadcast from the leader*/
    private Collection<NetAddress> nodes;

    protected final Handler<Broadcast> broadcastInitiation = new Handler<Broadcast>()

    {
        @Override
        public void handle(Broadcast broadcast) {
            LOG.info("Key: " + broadcast.key + " Value: " + broadcast.value);
            nodes = broadcast.nodes;
            initiateBroadcast(broadcast);
        }
    };

    private void initiateBroadcast(Broadcast broadcast) {

        for (NetAddress node : nodes)
            if (!node.equals(self)) {
                trigger(new Message(self, node, new Broadcast(broadcast.key, broadcast.value, null)), net);
            }
    }

    {
        subscribe(broadcastInitiation, leader);
    }
}
