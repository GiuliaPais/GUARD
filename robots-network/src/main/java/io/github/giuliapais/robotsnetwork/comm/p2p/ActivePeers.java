package io.github.giuliapais.robotsnetwork.comm.p2p;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;


/**
 * Singleton class that keeps track of the peers that are currently active in the network.
 * Uses PropertyChangeSupport to notify changes in the underlying structure.
 */
public class ActivePeers {
    private static volatile ActivePeers instance = null;
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private final HashMap<Integer, Peer> peers;

    private ActivePeers() {
        this.peers = new HashMap<>();
    }

    public static ActivePeers getInstance() {
        ActivePeers result = instance;
        if (result == null) {
            synchronized (ActivePeers.class) {
                result = instance;
                if (result == null) {
                    instance = result = new ActivePeers();
                }
            }
        }
        return result;
    }

    public synchronized void addPeer(Peer peer) {
        Peer oldValue = this.peers.get(peer.getId());
        this.peers.put(peer.getId(), peer);
        support.fireIndexedPropertyChange("peers", peer.getId(), oldValue, peer);
    }

    public synchronized void addPeers(List<Peer> peers) {
        for (Peer peer : peers) {
            this.peers.put(peer.getId(), peer);
        }
    }

    public synchronized boolean removePeer(int peerId) {
        Peer oldValue = this.peers.get(peerId);
        this.peers.remove(peerId);
        support.fireIndexedPropertyChange("peers", peerId, oldValue, peers.get(peerId));
        return oldValue != null & peers.get(peerId) == null;
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    public synchronized boolean isEmpty() {
        return this.peers.isEmpty();
    }

    public synchronized List<Peer> getPeers() {
        return new ArrayList<>(peers.values());
    }

    public synchronized List<Integer> getIds() {
        return new ArrayList<>(peers.keySet());
    }
}
