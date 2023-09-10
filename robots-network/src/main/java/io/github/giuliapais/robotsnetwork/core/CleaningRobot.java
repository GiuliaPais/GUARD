package io.github.giuliapais.robotsnetwork.core;

import io.github.giuliapais.commons.DistrictBalancer;
import io.github.giuliapais.commons.models.MapPosition;
import io.github.giuliapais.robotsnetwork.comm.p2p.ActivePeers;
import io.github.giuliapais.robotsnetwork.comm.p2p.P2PServiceManager;
import io.github.giuliapais.robotsnetwork.comm.p2p.Peer;
import io.github.giuliapais.robotsnetwork.comm.rest.RestServiceManager;
import io.github.giuliapais.commons.MessagePrinter;

import java.util.List;
import java.util.Random;


/**
 * Represents an active cleaning robot.
 * <p>
 * The thread is in charge of initializing the sensor component (which runs independently as a new thread) and
 * launch a service manager for P2P services. While running, the thread will periodically simulate the need for repairs
 * (10% chance every 10 seconds) and trigger the mutual exclusion protocol. The robot will gracefully terminate
 * when the appropriate flag is set.
 *
 * @see MockSensorComponent
 * @see P2PServiceManager
 */
public class CleaningRobot extends Thread {
    /* ATTRIBUTES --------------------------------------------------------------------------------------------------- */
    private static final int REPAIR_CHANCE = 10; // 10%
    private static final int REPAIR_CHECK_INTERVAL = 10000; // 10 seconds
    private final int robotId;
    private final DistrictBalancer districtBalancer = new DistrictBalancer();
    private volatile boolean stop = false;
    private volatile boolean userRequestRepair = false;
    private final Random random = new Random();

    /* Independent thread that handles simulation of sensor data, connection to MQTT broker and publishing */
    private final MockSensorComponent mockSensorComponent;
    /* Utility that encapsulates access to gRPC services */
    private final P2PServiceManager p2pServiceManager;

    /* ---- Used for P2P communication */
    private final ActivePeers peers;


    /* CONSTRUCTORS ------------------------------------------------------------------------------------------------- */
    public CleaningRobot(int robotId, int port, MapPosition mapPosition,
                         List<Peer> peers, String selfIpAddress) {
        this.robotId = robotId;
        this.districtBalancer.addRobot(robotId, mapPosition);
        this.peers = ActivePeers.getInstance();
        this.peers.addPeers(peers);
        this.mockSensorComponent = new MockSensorComponent(robotId, mapPosition.getDistrict());
        this.p2pServiceManager = new P2PServiceManager(robotId, port, selfIpAddress, districtBalancer);
    }

    /* METHODS ------------------------------------------------------------------------------------------------------ */
    /* Private --------- */
    private void stopGently() {
        // Stop P2PServiceManager components
        p2pServiceManager.gracefulStop();
        mockSensorComponent.interrupt();
        // Send REST delete request to server
        RestServiceManager.getInstance(null).deleteRobot(robotId, true);
        // Interrupt this
        MessagePrinter.printMessage(
                "Leaving Greenfield... Bye!",
                MessagePrinter.INFO_FORMAT,
                true
        );
        Thread.currentThread().interrupt();
    }


    /* Public ---------- */
    public void stopMeGently() {
        stop = true;
    }

    public void requestRepair() {
        userRequestRepair = true;
    }

    @Override
    public void run() {
        mockSensorComponent.start();

        // Send introduction messages when joining the robots network
        p2pServiceManager.introduceMe();

        while (!stop) {
            try {
                Thread.sleep(REPAIR_CHECK_INTERVAL);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            // Check if the robot needs repairs
            if (this.userRequestRepair || random.nextInt(100) < REPAIR_CHANCE) {
                userRequestRepair = false;
                p2pServiceManager.requestRepair();
            }
        }

        // graceful exit
        stopGently();
    }

}
