package io.github.giuliapais.robotsnetwork.comm.p2p;

public class Peer {
    private int id;
    private String ipAddress;
    private int port;

    public Peer() {
    }

    public Peer(int id, String ipAddress, int port) {
        this.id = id;
        this.ipAddress = ipAddress;
        this.port = port;
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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof Peer)) {
            return false;
        }
        Peer peer = (Peer) obj;
        return peer.getId() == this.id & peer.getIpAddress().equals(this.ipAddress) & peer.getPort() == this.port;
    }
}
