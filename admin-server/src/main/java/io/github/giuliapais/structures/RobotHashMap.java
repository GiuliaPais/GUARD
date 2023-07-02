package io.github.giuliapais.structures;

import io.github.giuliapais.api.models.Robot;
import io.github.giuliapais.exceptions.IdPresentException;

import java.util.HashMap;

/**
 * A thread-safe HashMap that stores Robots.
 *
 * @see Robot
 */
public class RobotHashMap {
    private final HashMap<Integer, Robot> internalMap = new HashMap<>();

    public RobotHashMap() {
    }

    /**
     * Returns a deep copy of the internal HashMap.
     */
    public HashMap<Integer, Robot> getMap() {
        HashMap<Integer, Robot> copy = new HashMap<>();
        synchronized (this) {
            internalMap.forEach((key, value) -> copy.put(key, new Robot(value)));
        }
        return copy;
    }

    /**
     * Adds a robot to the map only if another robot with the same id is not present.
     * <p>
     * Implementation of this method is thread-safe and checks for null values and
     * id presence.
     *
     * @param value a {@link Robot} object to be added to the map
     * @throws NullPointerException if value is null
     * @throws IdPresentException   if a robot with the same id is already present
     */
    public void put(Robot value) throws IdPresentException {
        if (value == null) {
            throw new NullPointerException();
        }
        synchronized (this) {
            if (internalMap.containsKey(value.getId())) {
                throw new IdPresentException("Robot with id " + value.getId() + " already present");
            }
            internalMap.put(value.getId(), value);
        }
    }

    public synchronized boolean remove(int key) {
        return internalMap.remove(key) != null;
    }

    public synchronized Robot update(int key, Robot newValue,
                                     DistrictBalancer districtBalancer) {
        if (!internalMap.containsKey(key)) {
            return null;
        }
        Robot old = internalMap.get(key);
        if (old.equals(newValue)) {
            return newValue;
        }
        Robot toReturn = new Robot();
        toReturn.setId(key);
        if (newValue.getIpAddress() != null &&
                !newValue.getIpAddress().equals(old.getIpAddress())) {
            toReturn.setIpAddress(newValue.getIpAddress());
        } else {
            toReturn.setIpAddress(old.getIpAddress());
        }
        if (newValue.getPort() != 0 &&
                newValue.getPort() != old.getPort()) {
            toReturn.setPort(newValue.getPort());
        } else {
            toReturn.setPort(old.getPort());
        }
        if (newValue.getDistrict() != 0 &&
                newValue.getDistrict() != old.getDistrict()) {
            toReturn.setDistrict(newValue.getDistrict());
            toReturn.setPosition(
                    districtBalancer.getPosInDistrict(newValue.getDistrict()));
            districtBalancer.changeDistrict(toReturn.getId(),
                    toReturn.getDistrict());
        } else {
            toReturn.setDistrict(old.getDistrict());
            toReturn.setPosition(old.getPosition());
        }
        internalMap.replace(key, toReturn);
        return toReturn;
    }
}
