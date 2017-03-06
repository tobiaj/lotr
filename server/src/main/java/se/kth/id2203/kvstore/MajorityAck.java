package se.kth.id2203.kvstore;

import se.kth.id2203.networking.NetAddress;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.UUID;

/**
 * Created by tobiaj on 2017-03-05.
 */
public class MajorityAck implements Serializable {

    public LinkedList<NetAddress> getBroadCastednodes() {
        return broadCastednodes;
    }

    public void setBroadCastednodes(LinkedList<NetAddress> broadCastednodes) {
        this.broadCastednodes = broadCastednodes;
    }

    public int getNumberofNodes() {
        return numberofNodes;
    }

    public void setNumberofNodes(int numberofNodes) {
        this.numberofNodes = numberofNodes;
    }

    public LinkedList<NetAddress> broadCastednodes;
    public int numberofNodes;

    public NetAddress getClient() {
        return client;
    }

    public NetAddress client;

    public UUID getId() {
        return id;
    }

    public UUID id;


    public MajorityAck(LinkedList<NetAddress> broadCastednodes, int numberofNodes, NetAddress client, UUID id){
        this.broadCastednodes = broadCastednodes;
        this.numberofNodes = numberofNodes;
        this.client = client;
        this.id = id;
    }

}