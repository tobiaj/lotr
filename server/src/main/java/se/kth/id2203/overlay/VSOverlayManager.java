/*
 * The MIT License
 *
 * Copyright 2017 Lars Kroll <lkroll@kth.se>.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package se.kth.id2203.overlay;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.Broadcast.BEBPort;
import se.kth.id2203.Broadcast.Broadcast;
import se.kth.id2203.bootstrapping.*;
import se.kth.id2203.failureDetector.*;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.supervisor.GetLeaders;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;

/**
 * The V(ery)S(imple)OverlayManager.
 * <p>
 * Keeps all nodes in a single partition in one replication group.
 * <p>
 * Note: This implementation does not fulfill the project task. You have to
 * support multiple partitions!
 * <p>
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class VSOverlayManager extends ComponentDefinition {

    final static Logger LOG = LoggerFactory.getLogger(VSOverlayManager.class);
    //******* Ports ******
    protected final Positive<Bootstrapping> boot = requires(Bootstrapping.class);
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);

    protected final Negative<FailurePort> failure = provides(FailurePort.class);
    protected final Negative<BEBPort> beb = provides(BEBPort.class);

    //******* Fields ******
    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);

    //******* Changes ******
    private LookupTable lut = new LookupTable();
    private boolean leader = false;
    private UUID timeoutId;
    private NetAddress addressToleader;
    private NetAddress supervisor;
    private HashSet<NetAddress> failureAddressToLeader = new HashSet<>();
    private boolean leaderAlive;
    private LinkedList<NetAddress> nodesInGroup = new LinkedList<>();
    private HashSet<NetAddress> inactive = new HashSet<>();


    //******* Handlers ******


    protected final Handler<GetInitialAssignments> initialAssignmentHandler = new Handler<GetInitialAssignments>() {

        @Override
        public void handle(GetInitialAssignments event) {
            LOG.info("Generating LookupTable...");
            lut = lut.generate(event.nodes, 0);
            LOG.debug("Generated assignments:\n{}", lut);
            trigger(new InitialAssignments(lut), boot);
        }
    };

    protected final Handler<Booted> bootHandler = new Handler<Booted>() {

        @Override
        public void handle(Booted event) {
            if (event.assignment instanceof LookupTable) {
                LOG.info("Got NodeAssignment, overlay ready.");
                lut = (LookupTable) event.assignment;

                for (NetAddress address : lut.getNodes()) {
                    nodesInGroup.add(address);

                }

                LOG.info("NODES IN THE LINKED LIST IS NOW " + nodesInGroup.toString());

                if (nodesInGroup.getFirst().equals(self)){
                    leader = true;
                    LOG.info("TO STRING IN OVERLAY: " + lut.toString() + " \n AND I AM:  "+ self +
                            " \n I AM LEADER: " + leader);

                }
                else {
                    addressToleader = nodesInGroup.getFirst();
                    failureAddressToLeader.add(addressToleader);
                    LOG.info("TO STRING IN OVERLAY: " + lut.toString() + " \n AND I AM:  "+ self +
                            " \n I Have leader status : " + leader  + " my leader is: " + addressToleader);

                }

            } else {
                LOG.error("Got invalid NodeAssignment type. Expected: LookupTable; Got: {}", event.assignment.getClass());
            }
        }
    };

    protected final ClassMatchedHandler<Connect, Message> connectHandler = new ClassMatchedHandler<Connect, Message>() {

        @Override
        public void handle(Connect content, Message context) {
            if (lut != null) {
                LOG.debug("Accepting connection request from {}", context.getSource());
                int size = lut.getNodes().size();
                trigger(new Message(self, context.getSource(), content.ack(size)), net);
            } else {
                LOG.info("Rejecting connection request from {}, as system is not ready, yet.", context.getSource());
            }
        }
    };

    protected final ClassMatchedHandler<LeaderFailureCheck, Message> leaderFailureCheck = new ClassMatchedHandler<LeaderFailureCheck, Message>() {
        @Override
        public void handle(LeaderFailureCheck leaderFailureCheck, Message message) {
            //To be safe
            if (leader){
                LOG.info("LEADER GOT FAILURE CHECK FROM: " + message.getSource());
                trigger(new Message(self, message.getSource(), new LeaderFailureCheckResponse(true, self)), net);
                trigger(new Broadcast(new Random().nextInt(9), "simon", lut.getNodes()), beb);

            }
        }
    };

    protected final ClassMatchedHandler<PassiveFailureCheck, Message> passiveFailureCheck = new ClassMatchedHandler<PassiveFailureCheck, Message>() {
        @Override
        public void handle(PassiveFailureCheck passiveFailureCheck, Message message) {
            LOG.info("PASSIVE GOT FAILURE CHECK FROM: " + message.getSource());

                trigger(new Message(self, message.getSource(), new PassiveFailureCheckResponse(true, self)), net);

        }
    };


    protected final ClassMatchedHandler<GetLeaders, Message> getLeaders = new ClassMatchedHandler<GetLeaders, Message>() {
        @Override
        public void handle(GetLeaders getLeaders, Message message) {
            supervisor = message.getSource();
            if (leader) {
                trigger(new Message(self, supervisor, new GetLeaders()), net);
            }
        }
    };

    protected final ClassMatchedHandler<Suspected, Message> suspectedMessage = new ClassMatchedHandler<Suspected, Message>() {
        @Override
        public void handle(Suspected suspected, Message message) {
            NetAddress address = suspected.getAddress();
            LOG.info("OVERLAY GOT SUSPECT " + address);
            if (nodesInGroup.contains(address)){

                nodesInGroup.remove(address);
                inactive.add(address);

                addressToleader = nodesInGroup.getFirst();

                if (addressToleader.equals(self)) {
                    if (!leader) {
                        leader = true;
                        trigger(new Message(self, message.getSource(), new SuspectResponse(self)), net);
                    }

                }

                LOG.info("NODES IN GROUP IS " + nodesInGroup.toString());
                LOG.info("NODES IN INACTIVE " + inactive.toString());

            }
        }
    };

    protected final ClassMatchedHandler<Unsuspected, Message> unsuspectedMessage = new ClassMatchedHandler<Unsuspected, Message>() {
        @Override
        public void handle(Unsuspected unsuspected, Message message) {
            NetAddress address = unsuspected.getAddress();
            LOG.info("OVERLAY " + address + " CAME BACK TO LIVE ADDED TO THE LIST AGAIN");

            if (inactive.contains(address)){
                inactive.remove(address);
                nodesInGroup.addLast(address);
            }
        }
    };

    {
        subscribe(initialAssignmentHandler, boot);
        subscribe(bootHandler, boot);
        subscribe(connectHandler, net);
        subscribe(leaderFailureCheck, net);
        subscribe(passiveFailureCheck, net);
        subscribe(getLeaders, net);
        subscribe(suspectedMessage, net);
        subscribe(unsuspectedMessage, net);

    }
}
