package io.github.giuliapais.api.models;

import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.Arrays;
import java.util.Objects;

@XmlRootElement
public class Robot {
    private int id;
    private String ipAddress;
    private short port;
    private byte district;
    private byte[] position;

    public Robot() {
    }

    public Robot(int id, String ipAddress, short port, byte district, byte[] position) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.port = port;
        this.district = district;
        this.position = position;
    }

    public Robot(Robot robot) {
        this.id = robot.getId();
        this.ipAddress = robot.getIpAddress();
        this.port = robot.getPort();
        this.district = robot.getDistrict();
        this.position = robot.getPosition();
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

    public byte getDistrict() {
        return district;
    }

    public void setDistrict(byte district) {
        this.district = district;
    }

    public byte[] getPosition() {
        return position;
    }

    public void setPosition(byte[] position) {
        this.position = position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Robot robot = (Robot) o;
        return id == robot.id && port == robot.port && district == robot.district &&
                Objects.equals(ipAddress, robot.ipAddress) && Arrays.equals(position, robot.position);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, ipAddress, port, district);
        result = 31 * result + Arrays.hashCode(position);
        return result;
    }
}
