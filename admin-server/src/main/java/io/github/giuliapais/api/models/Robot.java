package io.github.giuliapais.api.models;

import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.Arrays;
import java.util.Objects;

@XmlRootElement
public class Robot {
    private int id;
    private String ipAddress;
    private short port;

    public Robot() {
    }

    public Robot(int id, String ipAddress, short port) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.port = port;
    }

    public Robot(Robot robot) {
        this.id = robot.getId();
        this.ipAddress = robot.getIpAddress();
        this.port = robot.getPort();
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public short getPort() {
        return port;
    }

    public void setPort(short port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Robot robot = (Robot) o;
        return id == robot.id && port == robot.port &&
                Objects.equals(ipAddress, robot.ipAddress);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, ipAddress, port);
        result = 31 * result;
        return result;
    }
}
