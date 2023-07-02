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
     *
     * @return a deep copy of the internal HashMap<Integer, Robot>
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

    public synchronized boolean update(int key, Robot newValue) {
        if (!internalMap.containsKey(key)) {
            return false;
        }
        Robot old = internalMap.get(key);
        if (old.equals(newValue)) {
            return false;
        }
        if (newValue.getIpAddress() != null &&
                !newValue.getIpAddress().equals(old.getIpAddress())) {
            old.setIpAddress(newValue.getIpAddress());
        } else {
            old.setIpAddress(old.getIpAddress());
        }
        if (newValue.getPort() != 0 &&
                newValue.getPort() != old.getPort()) {
            old.setPort(newValue.getPort());
        } else {
            old.setPort(old.getPort());
        }
        internalMap.replace(key, old);
        return true;
    }
}
