package io.github.giuliapais.commons.models;

import io.github.giuliapais.commons.MessagePrinter;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RobotInfo {
    private int id;
    private String ipAddress;
    private int port;
    private MapPosition mapPosition;

    public RobotInfo() {
    }

    public RobotInfo(int id, String ipAddress, int port, MapPosition mapPosition) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.port = port;
        this.mapPosition = mapPosition;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public MapPosition getMapPosition() {
        return mapPosition;
    }

    public void setMapPosition(MapPosition mapPosition) {
        this.mapPosition = mapPosition;
    }

    @Override
    public String toString() {
        return "{" +
                MessagePrinter.STRING_SEP +
                "   Robot id = " + id + "," +
                MessagePrinter.STRING_SEP +
                "   Robot IP Address = " + ipAddress + ',' +
                MessagePrinter.STRING_SEP +
                "   Port = " + port + "," +
                MessagePrinter.STRING_SEP +
                "   Assigned district = " + mapPosition.getDistrict() + "," +
                MessagePrinter.STRING_SEP +
                "   Position = (" + mapPosition.getX() + ", " + mapPosition.getY() + ")" +
                MessagePrinter.STRING_SEP +
                '}';
    }
}
