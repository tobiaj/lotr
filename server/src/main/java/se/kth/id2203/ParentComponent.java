package se.kth.id2203;

import com.google.common.base.Optional;
import se.kth.id2203.Broadcast.BEB;
import se.kth.id2203.Broadcast.BEBPort;
import se.kth.id2203.bootstrapping.BootstrapClient;
import se.kth.id2203.bootstrapping.BootstrapServer;
import se.kth.id2203.bootstrapping.Bootstrapping;
import se.kth.id2203.failureDetector.FailureDetector;
import se.kth.id2203.failureDetector.FailurePort;
import se.kth.id2203.kvstore.KVService;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.Routing;
import se.kth.id2203.supervisor.Supervisor;
import se.kth.id2203.supervisor.SupervisorPort;
import se.kth.id2203.overlay.VSOverlayManager;
import se.sics.kompics.Channel;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Init;
import se.sics.kompics.Positive;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

public class ParentComponent
        extends ComponentDefinition {

    //******* Ports ******
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);
    //******* Children ******
    protected final Component overlay = create(VSOverlayManager.class, Init.NONE);
    protected final Component kv = create(KVService.class, Init.NONE);
    protected final Component boot;

    //******* Changes ******
    protected final Component supervisor = create(Supervisor.class, Init.NONE);
    protected final Component failureDetector = create(FailureDetector.class, Init.NONE);
    protected final Component broadcast = create(BEB.class, Init.NONE);


    {

        Optional<NetAddress> serverO = config().readValue("id2203.project.bootstrap-address", NetAddress.class);
        if (serverO.isPresent()) { // start in client mode
            boot = create(BootstrapClient.class, Init.NONE);

        } else { // start in server mode
            boot = create(BootstrapServer.class, Init.NONE);
            connect(supervisor.getNegative(SupervisorPort.class), boot.getPositive(SupervisorPort.class), Channel.TWO_WAY);

        }
        connect(timer, boot.getNegative(Timer.class), Channel.TWO_WAY);
        connect(net, boot.getNegative(Network.class), Channel.TWO_WAY);
        // Overlay
        connect(boot.getPositive(Bootstrapping.class), overlay.getNegative(Bootstrapping.class), Channel.TWO_WAY);
        connect(net, overlay.getNegative(Network.class), Channel.TWO_WAY);
        // KV
        connect(net, kv.getNegative(Network.class), Channel.TWO_WAY);

        //Changes
        connect(net, supervisor.getNegative(Network.class), Channel.TWO_WAY);
        connect(net, failureDetector.getNegative(Network.class), Channel.TWO_WAY);
        connect(timer, failureDetector.getNegative(Timer.class), Channel.TWO_WAY);

        //Added BEB
        connect(net, broadcast.getNegative(Network.class), Channel.TWO_WAY);
        connect(kv.getPositive(BEBPort.class), broadcast.getNegative(BEBPort.class), Channel.TWO_WAY);

        connect(supervisor.getPositive(FailurePort.class), failureDetector.getNegative(FailurePort.class), Channel.TWO_WAY);
        connect(overlay.getPositive(Routing.class), kv.getNegative(Routing.class), Channel.TWO_WAY);
        //connect(overlay.getPositive(FailurePort.class), failureDetector.getNegative(FailurePort.class), Channel.TWO_WAY);

    }
}
