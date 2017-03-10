package se.kth.id2203.simulation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.kvstore.OpResponse;
import se.kth.id2203.kvstore.Operation;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.RouteMsg;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Created by tobiaj on 2017-03-08.
 */
public class ScenarioClient2 extends ComponentDefinition{

    final static Logger LOG = LoggerFactory.getLogger(ScenarioClient.class);
    //******* Ports ******
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);
    //******* Fields ******
    private final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    private final NetAddress server = config().getValue("id2203.project.bootstrap-address", NetAddress.class);
    private final SimulationResultMap res3 = SimulationResultSingleton.getInstance();
    private final Map<UUID, String> pending = new TreeMap<>();
    //******* Handlers ******
    protected final Handler<Start> startHandler = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            int messages = res3.get("messages", Integer.class);
            for (int i = 0; i < messages; i++) {
                i = i * 500;
                Operation op = new Operation("test" + i, "get");
                RouteMsg rm = new RouteMsg(op.key, op); // don't know which partition is responsible, so ask the bootstrap server to forward it
                trigger(new Message(self, server, rm), net);
                pending.put(op.id, op.key);
                LOG.info("Sending {}", op);
                res3.put(op.key, "SENT");
            }
        }
    };
    protected final ClassMatchedHandler<OpResponse, Message> responseHandler = new ClassMatchedHandler<OpResponse, Message>() {

        @Override
        public void handle(OpResponse content, Message context) {
            LOG.debug("SELF {} Got OpResponse: {} with value {}", self, content, content.value);
            String key = pending.remove(content.id);
            if (key != null) {
                res3.put(key, content.status.toString());
            } else {
                LOG.warn("ID {} was not pending! Ignoring response.", content.id);
            }
        }
    };

    {
        subscribe(startHandler, control);
        subscribe(responseHandler, net);
    }
}
