package io.github.giuliapais.commons.models;

import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class MapPosition {
    private int district;
    private int x;
    private int y;

    public MapPosition() {
    }

    public MapPosition(int district, int x, int y) {
        this.district = district;
        this.x = x;
        this.y = y;
    }

    public MapPosition(MapPosition mapPosition) {
        this.district = mapPosition.getDistrict();
        this.x = mapPosition.getX();
        this.y = mapPosition.getY();
    }

    public int getDistrict() {
        return district;
    }

    public void setDistrict(int district) {
        this.district = district;
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
}
