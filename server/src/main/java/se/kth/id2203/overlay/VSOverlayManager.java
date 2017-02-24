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

import com.larskroll.common.J6;
import java.util.Collection;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.bootstrapping.*;
import se.kth.id2203.heartbeat.HeartBeat;
import se.kth.id2203.heartbeat.HeartbeatPort;
import se.kth.id2203.heartbeat.HeartbeatResponse;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;
import sun.nio.ch.Net;

/**
 * The V(ery)S(imple)OverlayManager.
 * <p>
 * Keeps all nodes in a single partition in one replication group.
 * <p>
 * Note: This implementation does not fulfill the project task. You have to
 * support multiple partitions!
 * <p>
 * @author Lars Kroll <lkroll@kth.se>
 */
public class VSOverlayManager extends ComponentDefinition {

    final static Logger LOG = LoggerFactory.getLogger(VSOverlayManager.class);
    //******* Ports ******
    protected final Positive<Bootstrapping> boot = requires(Bootstrapping.class);
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);

    protected final Negative<HeartbeatPort> heartbeatSend = provides(HeartbeatPort.class);

    protected final Positive<HeartbeatPort> hearbeatReceive = requires(HeartbeatPort.class);

    //******* Fields ******
    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);

    //******* Changes ******
    private LookupTable lut = new LookupTable();
    private boolean leader = false;
    private UUID timeoutId;
    private NetAddress addressToleader;
    private boolean leaderAlive;


    //******* Handlers ******

    /*

    protected final Handler<Start> startHandler = new Handler<Start>() {

        @Override
        public void handle(Start event) {
            long timeout = (config().getValue("id2203.project.keepAlivePeriod", Long.class) * 2);
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(timeout, timeout);
            spt.setTimeoutEvent(new BSTimeout(spt));
            trigger(spt, timer);
            timeoutId = spt.getTimeoutEvent().getTimeoutId();
        }
    };

    protected final Handler<BSTimeout> timeoutHandler = new Handler<BSTimeout>() {

        @Override
        public void handle(BSTimeout e) {

            if(!leader) {
                    LOG.debug("Not leader, i am " + self + " sending heartbeat to leader " );
                    trigger(new HeartBeat(leaderAlive), hearbeatPort);
                }
            }
        };
        */


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

                    for (NetAddress address : lut.getNodes()){
                        if (address.equals(self)){
                            leader = true;
                            LOG.info("TO STRING IN OVERLAY: " + lut.toString() + " \n AND I AM:  "+ self +
                                    " \n I AM LEADER: " + leader);
                        }
                        else {
                            addressToleader = address;
                            LOG.info("TO STRING IN OVERLAY: " + lut.toString() + " \n AND I AM:  "+ self +
                                    " \n I Have leader status : " + leader  + " my leader is: " + addressToleader);
                        }
                        break;
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

        protected final ClassMatchedHandler<HeartBeat, Message> heartBeatHandler = new ClassMatchedHandler<HeartBeat, Message>() {
            @Override
            public void handle(HeartBeat heartBeat, Message message) {
                if (leader){
                    LOG.info("I am: " + self + " \n i am leader " +   leader + " got message from SUPERVISOR");
                    trigger(new Message(self, message.getSource(), new HeartbeatResponse(true, self)), net);
                }
            }
        };


        {
            subscribe(initialAssignmentHandler, boot);
            subscribe(bootHandler, boot);
            subscribe(connectHandler, net);
            subscribe(heartBeatHandler, net);
            //subscribe(startHandler, control);

        }
    }
