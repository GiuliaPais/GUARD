package io.github.giuliapais.robotsnetwork.core;

import io.github.giuliapais.commons.DistrictBalancer;
import io.github.giuliapais.robotsnetwork.comm.ActivePeers;
import io.github.giuliapais.robotsnetwork.comm.P2PServiceManager;
import io.github.giuliapais.robotsnetwork.comm.Peer;
import jakarta.ws.rs.client.Client;

import java.net.URI;
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
    private final DistrictBalancer districtBalancer = new DistrictBalancer();
    private final int robotId;
    private final int port;
    private final String serverAddress;
    private int district;
    private int x;
    private int y;
    private volatile boolean stop = false;
    private volatile boolean userRequestRepair = false;
    private final Random random = new Random();

    /* Independent thread that handles simulation of sensor data, connection to MQTT broker and publishing */
    private final MockSensorComponent mockSensorComponent;
    /* Utility that encapsulates access to gRPC services */
    private final P2PServiceManager p2pServiceManager;

    /* ---- Used for P2P communication */
    private final ActivePeers peers;
    private String selfIpAddress;


    /* CONSTRUCTORS ------------------------------------------------------------------------------------------------- */
    public CleaningRobot(int robotId, int port, String serverAddress, int district,
                         int x, int y, List<Peer> peers, String selfIpAddress, Client restClient, String uriApi) {
        this.robotId = robotId;
        this.port = port;
        this.serverAddress = serverAddress;
        this.district = district;
        this.x = x;
        this.y = y;
        this.districtBalancer.addRobot(robotId, district);
        this.peers = ActivePeers.getInstance();
        this.peers.addPeers(peers);
        this.selfIpAddress = selfIpAddress;
        this.mockSensorComponent = new MockSensorComponent();
        this.p2pServiceManager = new P2PServiceManager(robotId, port, selfIpAddress, districtBalancer, restClient,
                uriApi);
    }

    /* METHODS ------------------------------------------------------------------------------------------------------ */
    /* Private --------- */


    /* Public ---------- */
    /* -- Getters and setters */
    public int getRobotId() {
        return robotId;
    }

    public int getPort() {
        return port;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public int getDistrict() {
        return district;
    }

    public void setDistrict(int district) {
        this.district = district;
        mockSensorComponent.setDistrict(district);
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }


    public String getSelfIpAddress() {
        return selfIpAddress;
    }


    /* -- Other public methods */
    public void stopMeGently() {
        stop = true;
    }

    public void requestRepair() {
        userRequestRepair = true;
    }

//    private void stopAll() {
//        if (mockSensorComponent != null && mockSensorComponent.isAlive()) {
//            mockSensorComponent.stopMeGently();
//        }
//        if (grpcServer != null && !grpcServer.isShutdown()) {
//            grpcServer.shutdown();
//        }
//    }


    @Override
    public void run() {
        // TODO: reactivate this
//        MessagePrinter.printSensorInitMessage();

//        mockSensorComponent.setUncaughtExceptionHandler((t, e) -> {
//            messagePrinter.printMessage(
//                    "Something went wrong :(" +
//                            MessagePrinter.STRING_SEP +
//                            e.getMessage() +
//                            MessagePrinter.STRING_SEP +
//                            e.getCause().getMessage(),
//                    MessagePrinter.ERROR_FORMAT,
//                    true
//            );
//            messagePrinter.printQuitMessage();
//            this.interrupt();
//        });
//        mockSensorComponent.start();
//        try {
//            mockSensorComponent.join();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }

        // Send introduction messages when joining the robots network
        p2pServiceManager.introduceMe();

        // TODO: here goes all the proper robot logic
        // - Simulate repair event every 10 seconds -> triggers mutual exclusion
        //   repair can also be triggered by command line "fix"
        // - Implement graceful exit

        while (!stop) {
            // Check if the robot needs repairs
            if (this.userRequestRepair || random.nextInt(100) < REPAIR_CHANCE) {
                userRequestRepair = false;
                p2pServiceManager.requestRepair();
            }
            try {
                Thread.sleep(REPAIR_CHECK_INTERVAL);
            } catch (InterruptedException e) {
                throw new RuntimeException("Interrupted while waiting for repair timeout", e);
            }
        }

        // graceful exit
    }

}
