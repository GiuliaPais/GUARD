package io.github.giuliapais.robotsnetwork.core;

import io.github.giuliapais.utils.MessagePrinter;

import java.util.Random;

public class CleaningRobot extends Thread {
    private int robotId;
    private int port;
    private String serverAddress;
    private int district;
    private int x;
    private int y;
    private MessagePrinter messagePrinter;

    private final int clockSkew;
    private int logicalClock;

    private MockSensorComponent mockSensorComponent;


    private volatile boolean stop = false;

    public CleaningRobot() {
        Random random = new Random();
        this.clockSkew = random.nextInt(1000);
        this.logicalClock = 0;
        this.mockSensorComponent = new MockSensorComponent();
    }

    public CleaningRobot(int robotId, int port, String serverAddress, int district,
                         int x, int y, MessagePrinter messagePrinter) {
        this.robotId = robotId;
        this.port = port;
        this.serverAddress = serverAddress;
        this.district = district;
        this.x = x;
        this.y = y;
        this.messagePrinter = messagePrinter;
        Random random = new Random();
        this.clockSkew = random.nextInt(1000);
        this.logicalClock = 0;
        this.mockSensorComponent = new MockSensorComponent();
    }

    public int getRobotId() {
        return robotId;
    }

    public void setRobotId(int robotId) {
        this.robotId = robotId;
        mockSensorComponent.setRobotId(robotId);
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getServerAddress() {
        return serverAddress;
    }

    public void setServerAddress(String serverAddress) {
        this.serverAddress = serverAddress;
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

    public MessagePrinter getMessagePrinter() {
        return messagePrinter;
    }

    public void setMessagePrinter(MessagePrinter messagePrinter) {
        this.messagePrinter = messagePrinter;
        mockSensorComponent.setMessagePrinter(messagePrinter);
    }

    public void stopMeGently() {
        stop = true;
    }

    @Override
    public void run() {
        messagePrinter.printSensorInitMessage();
        mockSensorComponent.setUncaughtExceptionHandler((t, e) -> {
            messagePrinter.printMessage(
                    "Something went wrong :(" +
                            MessagePrinter.STRING_SEP +
                            e.getMessage() +
                            MessagePrinter.STRING_SEP +
                            e.getCause().getMessage(),
                    MessagePrinter.ERROR_FORMAT,
                    true
            );
            messagePrinter.printQuitMessage();
            this.interrupt();
        });
        mockSensorComponent.start();
        try {
            mockSensorComponent.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // TODO: here goes all the proper robot logic
    }

}
