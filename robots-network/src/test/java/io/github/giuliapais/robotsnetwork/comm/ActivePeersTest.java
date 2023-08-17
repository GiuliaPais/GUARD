package io.github.giuliapais.robotsnetwork.comm;

import io.github.giuliapais.robotsnetwork.comm.p2p.ActivePeers;
import io.github.giuliapais.robotsnetwork.comm.p2p.Peer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static org.junit.jupiter.api.Assertions.*;

class ActivePeersTest {

    ActivePeers activePeers;

    class MockListener implements PropertyChangeListener {

        boolean changed = false;

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            this.changed = true;
        }
    }

    @BeforeEach
    void setup() {
        activePeers = ActivePeers.getInstance();
    }

    @Test
    void testChangeRecivedWhenAdded() {
        MockListener listener = new MockListener();
        activePeers.addPropertyChangeListener(listener);
        activePeers.addPeer(new Peer(1, "localhost", 9991));
        assertTrue(listener.changed);
    }

    @Test
    void testNoChangeWhenAddingSamePeer() {
        activePeers.addPeer(new Peer(1, "localhost", 9991));
        MockListener listener = new MockListener();
        activePeers.addPropertyChangeListener(listener);
        activePeers.addPeer(new Peer(1, "localhost", 9991));
        assertFalse(listener.changed);
    }
}