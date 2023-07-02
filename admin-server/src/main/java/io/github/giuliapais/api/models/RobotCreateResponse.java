package io.github.giuliapais.api.models;

import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.List;

@XmlRootElement
public class RobotCreateResponse {
    private MapPosition mapPosition;
    private List<Robot> activeRobots;
    private Robot identity;

    public RobotCreateResponse() {
    }

    public MapPosition getMapPosition() {
        return mapPosition;
    }

    public void setMapPosition(MapPosition mapPosition) {
        this.mapPosition = mapPosition;
    }

    public List<Robot> getActiveRobots() {
        return activeRobots;
    }

    public void setActiveRobots(List<Robot> activeRobots) {
        this.activeRobots = activeRobots;
    }

    public Robot getIdentity() {
        return identity;
    }

    public void setIdentity(Robot identity) {
        this.identity = identity;
    }
}
