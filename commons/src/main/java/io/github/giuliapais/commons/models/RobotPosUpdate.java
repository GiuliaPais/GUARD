package io.github.giuliapais.commons.models;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class RobotPosUpdate {
    private int robotId;
    private MapPosition mapPosition;


    public RobotPosUpdate() {
    }

    public RobotPosUpdate(int robotId, int district, int x, int y) {
        this.robotId = robotId;
        this.mapPosition = new MapPosition(district, x, y);
    }

    public int getRobotId() {
        return robotId;
    }

    public void setRobotId(int robotId) {
        this.robotId = robotId;
    }

    public MapPosition getMapPosition() {
        return mapPosition;
    }

    public void setMapPosition(MapPosition mapPosition) {
        this.mapPosition = mapPosition;
    }
}
