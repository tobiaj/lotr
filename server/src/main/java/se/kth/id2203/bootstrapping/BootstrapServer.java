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
package se.kth.id2203.bootstrapping;

import com.google.common.collect.ImmutableSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.LookupTable;
import se.kth.id2203.supervisor.StartSupervisor;
import se.kth.id2203.supervisor.SupervisorPort;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Negative;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.CancelPeriodicTimeout;
import se.sics.kompics.timer.SchedulePeriodicTimeout;
import se.sics.kompics.timer.Timer;
import sun.nio.ch.Net;

public class BootstrapServer extends ComponentDefinition {

    final static Logger LOG = LoggerFactory.getLogger(BootstrapServer.class);
    //******* Ports ******
    protected final Negative<Bootstrapping> boot = provides(Bootstrapping.class);

    protected final Negative<SupervisorPort> supervisorPortNegative = provides(SupervisorPort.class);

    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Timer> timer = requires(Timer.class);
    //******* Fields ******
    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);
    final int bootThreshold = config().getValue("id2203.project.bootThreshold", Integer.class);
    private State state = State.COLLECTING;
    private UUID timeoutId;
    private Set<NetAddress> active = new HashSet<>();
    private Set<NetAddress> ready = new HashSet<>();
    private Set<NetAddress> watingNodes = new HashSet<>();

    private HashMap<Integer, Set<NetAddress>> groups = new HashMap<>();
    private LookupTable lut = new LookupTable();
    private int groupCounter = 0;
    private int counter = 0;
    private boolean done = false;

    private NodeAssignment initialAssignment = null;

    //******* Handlers ******
    protected final Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start e) {
            LOG.info("Starting bootstrap server on {}, waiting for {} nodes...", self, bootThreshold);
            long timeout = (config().getValue("id2203.project.keepAlivePeriod", Long.class) * 2);
            SchedulePeriodicTimeout spt = new SchedulePeriodicTimeout(timeout, timeout);
            spt.setTimeoutEvent(new BSTimeout(spt));
            trigger(spt, timer);
            timeoutId = spt.getTimeoutEvent().getTimeoutId();
            //active.add(self);
        }
    };
    protected final Handler<BSTimeout> timeoutHandler = new Handler<BSTimeout>() {
        @Override
        public void handle(BSTimeout e) {
            if (state == State.COLLECTING) {
                LOG.info("{} hosts in active set.", active.size());
                if (groupCounter == 3 && !done) {


                    lut = lut.generate(ImmutableSet.copyOf(groups.get(0)), 0);

                    lut.addToExisting(ImmutableSet.copyOf(groups.get(1)), 1, lut);

                    lut.addToExisting(ImmutableSet.copyOf(groups.get(2)), 2, lut);


                    bootUp();

                }
            } else if (state == State.SEEDING) {
                LOG.info("{} hosts in ready set.", ready.size());
                if (ready.size() >= 9) {
                    ready = new HashSet<>();
                    done = true;
                    LOG.info("Finished seeding. Bootstrapping complete.");
                    LOG.info("TRIGGER THE SUPERVISOR");
                    state = State.COLLECTING;

                    trigger(new StartSupervisor(lut), supervisorPortNegative);


                }
            } else if (state == State.DONE) {
                //suicide();
            }
        }
    };
    protected final ClassMatchedHandler<InitialAssignments, Message> assignmentHandler = new ClassMatchedHandler<InitialAssignments, Message>() {
        @Override
        public void handle(InitialAssignments initialAssignments, Message message) {
            LOG.info("Seeding assignments... WITH COUNTER " + counter);
            initialAssignment = initialAssignments.assignment;
            LOG.debug("InitalAssignment in handler: " + initialAssignment);

            NetAddress address = message.getSource();
            ready.add(address);
            trigger(new Message(self, address, new Boot(initialAssignment)), net);

            /*
            if (groups.get(counter) != null) {
                Set<NetAddress> temp = groups.get(counter);
                for (NetAddress address : temp) {
                    trigger(new Message(self, address, new Boot(initialAssignment)), net);
                }
                ++counter;
                if (counter == 2) {
                    LOG.info("TRIGGER THE SUPERVISOR");
                    state = State.COLLECTING;

                    trigger(new StartSupervisor(lut), supervisorPortNegative);

                }
            }
            */



            //initialAssignment = null;
            //active = new HashSet<>();
            //ready.add(self);

    }

    };
    protected final ClassMatchedHandler<CheckIn, Message> checkinHandler = new ClassMatchedHandler<CheckIn, Message>() {

        @Override
        public void handle(CheckIn content, Message context) {
            LOG.debug("SOURCE: " + context.getSource());

            if (!watingNodes.contains(context.getSource())) {
                if (groups.size() > 0) {

                    for (Set<NetAddress> set : groups.values()) {
                        if (!set.contains(context.getSource())) {
                            active.add(context.getSource());
                            watingNodes.add(context.getSource());

                        }
                    }
                } else {

                    active.add(context.getSource());
                    watingNodes.add(context.getSource());

                }
                LOG.info("ADDED TO THE ACTIVE LIST NOW IS " + active.toString());

                if (active.size() == 3) {
                    groups.put(groupCounter, active);
                    groupCounter++;
                    active = new HashSet<>();
                }
            }

        }
    };
    protected final ClassMatchedHandler<Ready, Message> readyHandler = new ClassMatchedHandler<Ready, Message>() {
        @Override
        public void handle(Ready content, Message context) {
            ready.add(context.getSource());
        }
    };

    {
        subscribe(startHandler, control);
        subscribe(timeoutHandler, timer);
        subscribe(assignmentHandler, net);
        subscribe(checkinHandler, net);
        subscribe(readyHandler, net);
    }

    @Override
    public void tearDown() {
        trigger(new CancelPeriodicTimeout(timeoutId), timer);
    }

    private void bootUp() {
        LOG.info("Threshold reached. Generating assignments...");
        state = State.SEEDING;

        for (Set<NetAddress> set : groups.values()){

            for (NetAddress address : set){

            trigger(new Message(self, address, new GetInitialAssignments(ImmutableSet.copyOf(set), 0)), net);

            }
        }

    }

    static enum State {

        COLLECTING,
        SEEDING,
        DONE;
    }
}
