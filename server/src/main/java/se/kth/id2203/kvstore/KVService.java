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
package se.kth.id2203.kvstore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.id2203.Broadcast.*;
import se.kth.id2203.kvstore.OpResponse.Code;
import se.kth.id2203.networking.Message;
import se.kth.id2203.networking.NetAddress;
import se.kth.id2203.overlay.BroadcastNodes;
import se.kth.id2203.overlay.Routing;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;

import java.util.HashMap;
import java.util.LinkedList;

/**
 *
 * @author Lars Kroll <lkroll@kth.se>
 */
public class KVService extends ComponentDefinition {

    final static Logger LOG = LoggerFactory.getLogger(KVService.class);
    //******* Ports ******
    protected final Positive<Network> net = requires(Network.class);
    protected final Positive<Routing> route = requires(Routing.class);

    protected final Negative<BEBPort> beb = provides(BEBPort.class);

    //******* Fields ******
    final NetAddress self = config().getValue("id2203.project.address", NetAddress.class);


    //****** Fields ******
    private HashMap<String, ValueSeq> database = new HashMap<>();
    private HashMap<String, MajorityAck> ackPutList = new HashMap<>();
    private HashMap<String, MajorityAck> ackGetList = new HashMap<>();
    private LinkedList<NetAddress> nodesToBroadcastTo = new LinkedList<>();
    private int sequenceNumber = 0;


    //******* Handlers ******
    protected final ClassMatchedHandler<Operation, Message> opHandler = new ClassMatchedHandler<Operation, Message>() {

        @Override
        public void handle(Operation content, Message context) {
            LOG.info("Got operation {}! Now implement me please :)", content.operation);
            String operation = content.operation;
            if (operation.equals("put")){
                doCommandPut(content, context);
            }
            else if (operation.equals("get")) {
                doCommandGet(content, context);

            }

            else if (operation.equals("swap")) {
                doCommandSwap(content, context);

            }
            else{
                trigger(new Message(self, context.getSource(), new OpResponse(content.id, Code.NOT_FOUND, "")), net);

            }

        }

    };

    private void doCommandSwap(Operation content, Message context) {
        String[] split = content.key.split(" ");
        String key = split[0];
        String value = split[1];
        String newValue = split[2];

        if (database.get(key) != null){
            ValueSeq valueSeq = database.get(key);
            String matchValue = valueSeq.getValue();

            if (matchValue.equals(value)) {
                valueSeq.setValue(newValue);
                MajorityAck majorityAck = new MajorityAck(nodesToBroadcastTo, 0, context.getSource(), content.id);
                int temp = valueSeq.getSequenceNumber();
                valueSeq.setSequenceNumber(temp + 1);
                database.put(key, valueSeq);
                ackPutList.put(key, majorityAck);

                trigger(new Broadcast(key, valueSeq, nodesToBroadcastTo), beb);

                //trigger(new Message(self, context.getSource(), new OpResponse(content.id, Code.OK, "")), net);
                LOG.info("DATABASE CONTAINS " + database.toString());
            }
            else{
                trigger(new Message(self, context.getSource(), new OpResponse(content.id, Code.NOT_FOUND, "")), net);

            }
        }
        else {
            trigger(new Message(self, context.getSource(), new OpResponse(content.id, Code.NOT_FOUND, "")), net);
        }

    }

    private void doCommandGet(Operation content, Message context) {
        LOG.info("GOT GET COMMAND FOR KEY " + content.key);

        MajorityAck majorityAck = new MajorityAck(nodesToBroadcastTo, 0, context.getSource(), content.id);
        ValueSeq valueSeq = database.get(content.key);
        ackGetList.put(content.key, majorityAck);

        trigger(new GetBroadcast(content.key, valueSeq), beb);

    }

    private void doCommandPut(Operation content, Message context) {
        String[] split = content.key.split(" ");
        String key = split[0];
        String value = split[1];
        LOG.info("ADDED TO THE DATABASE KEY: " + split[0] + " and the Value " +  split[1]);

        ValueSeq valueSeq;
        MajorityAck majorityAck;
        if (database.get(key)!=null){
            valueSeq = database.get(key);
            int tempNumber = valueSeq.getSequenceNumber() + 1;
            valueSeq.setSequenceNumber(tempNumber);
            valueSeq.setValue(value);

            majorityAck = new MajorityAck(nodesToBroadcastTo, 1, context.getSource(), content.id);
            ackPutList.put(key, majorityAck);

            LOG.info("DATABASE CONTAINS: " + database.toString() + " and nodes to broadcast to is " + nodesToBroadcastTo.toString());

        }
        else {
            sequenceNumber++;
            valueSeq = new ValueSeq(value, 0);
            majorityAck = new MajorityAck(nodesToBroadcastTo, 1, context.getSource(), content.id);
            database.put(key, valueSeq);
            ackPutList.put(key, majorityAck);

            LOG.info("DATABASE CONTAINS: " + database.toString() + " and nodes to broadcast to is " + nodesToBroadcastTo.toString());
        }

        trigger(new Broadcast(key, valueSeq, nodesToBroadcastTo), beb);

        //trigger(new Message(self, context.getSource(), new OpResponse(content.id, Code.OK, "")), net);

    }


    protected final Handler<BroadcastNodes> broadcastNodesHandler = new Handler<BroadcastNodes>() {
        @Override
        public void handle(BroadcastNodes broadcastNodes) {
            nodesToBroadcastTo = broadcastNodes.getAddresses();
            LOG.info("KVSERVICE GOT NODES IN GROUP " + nodesToBroadcastTo.toString());
        }
    };


    protected final ClassMatchedHandler<Broadcast, Message> broadcastReceiver = new ClassMatchedHandler<Broadcast, Message>() {
        @Override
        public void handle(Broadcast broadcast, Message message) {
            LOG.info("Key: " + broadcast.getKey() + " Value: " + broadcast.getValueSeq().getValue()
                    + "\n Received from: " + message.getSource());
            ValueSeq valueSeq = broadcast.getValueSeq();
            database.put(broadcast.getKey(), valueSeq);
            LOG.info("DATABASE CONTAINS: "+ database.toString());
            trigger(new Message(self, message.getSource(), new AckMessage(broadcast.getKey())), net);


        }
    };

    protected final ClassMatchedHandler<GetBroadcast, Message> getBroadcastHandler = new ClassMatchedHandler<GetBroadcast, Message>() {
        @Override
        public void handle(GetBroadcast getBroadcast, Message message) {
            ValueSeq valueSeq = database.get(getBroadcast.key);
            ValueSeq currentValueSeq = getBroadcast.getValueSeq();

            if (currentValueSeq.getSequenceNumber() > valueSeq.getSequenceNumber()){
                valueSeq.setSequenceNumber(currentValueSeq.getSequenceNumber());
                valueSeq.setValue(currentValueSeq.getValue());
            }

            trigger(new Message(self, message.getSource(), new CheckKeyValue(getBroadcast.key, valueSeq)), net);

        }
    };

    protected final ClassMatchedHandler<CheckKeyValue, Message> checkKeyValueMessageHandler = new ClassMatchedHandler<CheckKeyValue, Message>() {
        @Override
        public void handle(CheckKeyValue checkKeyValue, Message message) {

            ValueSeq valueSeq = checkKeyValue.getValueSeq();
            String key = checkKeyValue.getKey();
            ValueSeq currentValueSeq = database.get(key);

            if(ackGetList.get(key) != null){
                MajorityAck tempMap = ackGetList.get(key);

                LinkedList<NetAddress> tempList = tempMap.getBroadCastednodes();
                int nodes = tempMap.getNumberofNodes() + 1;
                //Safety check
                if (tempList.contains(message.getSource())) {

                    //If broadcast nodes only is one
                    if (nodes == tempList.size()) {

                        if (valueSeq.getSequenceNumber() > currentValueSeq.getSequenceNumber()){
                            currentValueSeq.setSequenceNumber(valueSeq.getSequenceNumber());
                            currentValueSeq.setValue(valueSeq.getValue());
                        }

                        ackGetList.remove(key);
                        trigger(new Message(self, tempMap.getClient(), new OpResponse(tempMap.getId(), Code.OK, valueSeq.getValue())), net);

                    } else if (nodes >= (tempList.size() / 2)) {

                        if (valueSeq.getSequenceNumber() > currentValueSeq.getSequenceNumber()){
                            currentValueSeq.setSequenceNumber(valueSeq.getSequenceNumber());
                            currentValueSeq.setValue(valueSeq.getValue());
                        }

                        ackGetList.remove(key);
                        trigger(new Message(self, tempMap.getClient(), new OpResponse(tempMap.getId(), Code.OK, valueSeq.getValue())), net);

                    }
                    else{
                        if (valueSeq.getSequenceNumber() > currentValueSeq.getSequenceNumber()){
                            currentValueSeq.setSequenceNumber(valueSeq.getSequenceNumber());
                            currentValueSeq.setValue(valueSeq.getValue());
                        }

                        LOG.info("Still need more ack to reach majority!");
                        tempMap.setNumberofNodes(nodes);
                    }
                }

            }
            else{
                LOG.info("Majority all ready fulfilled or key does not exist!");
            }
        }
    };

    protected final ClassMatchedHandler<AckMessage, Message> ackReceiverHandler = new ClassMatchedHandler<AckMessage, Message>() {
        @Override
        public void handle(AckMessage ackMessage, Message message) {
            String key = ackMessage.getKey();
            LOG.info("GOT ACK MESSAGE FOR KEY " + key);

            if (ackPutList.get(key) != null) {
                MajorityAck tempMap = ackPutList.get(key);
                LinkedList<NetAddress> tempList = tempMap.getBroadCastednodes();
                int nodes = tempMap.getNumberofNodes() + 1;
                //Safety check
                if (tempList.contains(message.getSource())) {

                    //If broadcast nodes only is one
                    if (nodes == tempList.size()) {
                        ackPutList.remove(key);
                        trigger(new Message(self, tempMap.getClient(), new OpResponse(tempMap.getId(), Code.OK, "")), net);

                    } else if (nodes >= (tempList.size() / 2)) {
                        ackPutList.remove(key);
                        trigger(new Message(self, tempMap.getClient(), new OpResponse(tempMap.getId(), Code.OK, "")), net);

                    }
                    else{
                        LOG.info("Still need more ack to reach majority!");
                        tempMap.setNumberofNodes(nodes);
                    }
                }

            }
            else{
                LOG.info("Majority all ready fulfilled or key does not exist!");
            }
        }
    };

    {
        subscribe(opHandler, net);
        subscribe(broadcastNodesHandler, route);
        subscribe(broadcastReceiver, net);
        subscribe(ackReceiverHandler, net);
        subscribe(getBroadcastHandler, net);
        subscribe(checkKeyValueMessageHandler, net);

    }

}
