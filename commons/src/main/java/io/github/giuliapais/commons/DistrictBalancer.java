package io.github.giuliapais.commons;

import io.github.giuliapais.commons.models.MapPosition;

import java.util.*;
import java.util.stream.Collectors;


/**
 * Helper class that, based on the structure of the map and the current status,
 * monitors and balances the
 * robots in the grid.
 */
public class DistrictBalancer {
    /* ATTRIBUTES --------------------------------------------------------------------------------------------------- */
    private final GreenfieldMap greenfieldMap = new GreenfieldMap();

    // Maps each robot currently active in the grid to its district
    private final HashMap<Integer, Integer> robotRegister = new HashMap<>();
    // Maps each district to the number of robots (count) currently active in it
    private final HashMap<Integer, Integer> districtRegister;
    private final HashMap<Integer, MapPosition> robotPositions = new HashMap<>();
    private final Random random = new Random();

    /* CONSTRUCTORS ------------------------------------------------------------------------------------------------- */

    /**
     * Default constructor.
     * Initializes the district register with the number of districts in the map.
     */
    public DistrictBalancer() {
        int nDistricts = greenfieldMap.getDistricts().length;
        districtRegister = new HashMap<>(nDistricts);
        for (int i = 1; i < nDistricts + 1; i++) {
            districtRegister.put(i, 0);
        }
    }

    /* METHODS ------------------------------------------------------------------------------------------------------ */
    /* Private --------- */


    /* Public ---------- */

    /**
     * Returns the current district of the given robot.
     *
     * @param robotId the id of the robot
     * @return the district id or null if the robot is not present
     */
    public synchronized int getDistrict(int robotId) {
        return robotRegister.get(robotId);
    }

    /**
     * Returns the current map position of the given robot.
     *
     * @param robotId the id of the robot
     * @return the map position or null if the robot is not present
     * @see MapPosition
     */
    public synchronized MapPosition getRobotPosition(int robotId) {
        return robotPositions.get(robotId);
    }

    /**
     * Returns a random position in the bounds of the given district.
     *
     * @param districtId the id of the district
     * @return an array of two integers, representing the x and y coordinates of the position
     */
    public int[] getPosInDistrict(int districtId) {
        District district = greenfieldMap.getDistrict(districtId);
        int x = random.nextInt(district.span_x()[1] - district.span_x()[0] + 1) + district.span_x()[0];
        int y = random.nextInt(district.span_y()[1] - district.span_y()[0] + 1) + district.span_y()[0];
        return new int[]{x, y};
    }

    /**
     * Adds a robot to the grid, assigning it to the less crowded district. If the load in all districts is equal,
     * it follows the natural order of numbering (from 1 to n). It generates a random position in the chosen
     * district that can be accessed through a call to {@link #getRobotPosition(int)}.
     * <p>
     * NOTE: used on admin-server to add robots to the grid.
     *
     * @param robotId the id of the robot to add
     */
    public synchronized void addRobot(int robotId) {
        // Check if the robot is already registered
        if (robotRegister.containsKey(robotId)) {
            return;
        }
        // Get the less crowded district
        Optional<Integer> lessCrowded = districtRegister.entrySet()
                .stream()
                .min(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey);
        Integer chosenDistrict = lessCrowded.orElse(1);
        robotRegister.put(robotId, chosenDistrict);
        districtRegister.replace(chosenDistrict, districtRegister.get(chosenDistrict) + 1);
        int[] pos = getPosInDistrict(chosenDistrict);
        robotPositions.put(robotId, new MapPosition(chosenDistrict, pos[0], pos[1]));
    }

    /**
     * Adds a robot to the grid, at the given position.
     * <p>
     * NOTE: this method should be used only by the robot processes once registration to the server is completed.
     *
     * @param robotId  the id of the robot to add
     * @param position the position of the robot
     */
    public synchronized void addRobot(int robotId, MapPosition position) {
        // Check if the robot is already registered, if so update the position
        if (robotRegister.containsKey(robotId)) {
            int oldDistrict = robotRegister.get(robotId);
            int newDistrict = position.getDistrict();
            if (oldDistrict != newDistrict) {
                robotPositions.replace(robotId, position);
                robotRegister.replace(robotId, position.getDistrict());
                districtRegister.replace(oldDistrict, districtRegister.get(oldDistrict) - 1);
                districtRegister.replace(newDistrict, districtRegister.get(newDistrict) + 1);
            }
            return;
        }
        robotPositions.put(robotId, position);
        robotRegister.put(robotId, position.getDistrict());
        districtRegister.replace(position.getDistrict(), districtRegister.get(position.getDistrict()) + 1);
    }

    /**
     * Removes a robot from the grid. Performs removal operation only.
     *
     * @param robotId the id of the robot to remove
     * @return true if the robot was registered, false otherwise
     */
    public synchronized boolean removeRobot(int robotId) {
        if (!robotRegister.containsKey(robotId)) {
            return false;
        }
        Integer district = robotRegister.get(robotId);
        robotRegister.remove(robotId);
        districtRegister.replace(district, districtRegister.get(district) - 1);
        robotPositions.remove(robotId);
        return true;
    }

    /**
     * Given a map of changes, updates the positions of the robots in the grid in a synchronized way -
     * all updates are performed as an atomic operation.
     * <p>
     * NOTE: used on admin-server to execute update from REST POST request.
     *
     * @param changes the map of changes
     */
    public synchronized void updatePositions(HashMap<Integer, MapPosition> changes) {
        for (Map.Entry<Integer, MapPosition> entry : changes.entrySet()) {
            updatePosition(entry.getKey(), entry.getValue());
        }
    }

    /**
     * Updates the position of a robot in the grid. If the robot is not registered, it does nothing.
     *
     * @param robotId     the id of the robot to update
     * @param mapPosition the new position of the robot
     * @return 0 if the operation was successful, 1 if the robot id was not registered, 2 if the robot was already
     * registered in the given district (no change)
     */
    public synchronized int updatePosition(int robotId, MapPosition mapPosition) {
        if (!robotRegister.containsKey(robotId)) {
            return 1;
        }
        robotPositions.replace(robotId, mapPosition);
        int oldDistrict = robotRegister.get(robotId);
        int newDistrict = mapPosition.getDistrict();
        if (oldDistrict != newDistrict) {
            robotRegister.replace(robotId, mapPosition.getDistrict());
            districtRegister.replace(oldDistrict, districtRegister.get(oldDistrict) - 1);
            districtRegister.replace(newDistrict, districtRegister.get(newDistrict) + 1);
            return 0;
        }
        return 2;
    }

    /**
     * Upserts a set of robots to the grid. If a robot is already registered, it changes its position
     * (if needed), otherwise it adds it to the grid.
     *
     * @param robotsToPositions a map of robot ids to map positions
     */
    public synchronized void upsert(HashMap<Integer, MapPosition> robotsToPositions) {
        for (Map.Entry<Integer, MapPosition> entry : robotsToPositions.entrySet()) {
            int code = updatePosition(entry.getKey(), entry.getValue());
            if (code == 1) {
                addRobot(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Evaluates, by scanning the grid, if there is need for rebalancing. If so, it returns a map of robot ids to
     * new district ids, otherwise it returns null.
     * NOTE: this method does NOT change the current status of the grid! Call changeDistrict method to apply changes.
     *
     * @return a map of robot ids to new district ids or null
     */
    public synchronized HashMap<Integer, Integer> rebalance() {
        int totRobots = robotRegister.size();
        int nDistricts = greenfieldMap.getDistricts().length;
        // Minimum number of robots per district
        int minLoad = totRobots / nDistricts;
        // Local copy
        HashMap<Integer, Integer> districts = new HashMap<>(this.districtRegister);
        // Partition districts by load
        HashMap<String, ArrayList<Integer>> districtsByLoad = new HashMap<>();
        districtsByLoad.put("empty", new ArrayList<>());
        districtsByLoad.put("min", new ArrayList<>());
        districtsByLoad.put("max", new ArrayList<>());
        districtsByLoad.put("over", new ArrayList<>());
        for (Integer districtNum : districts.keySet()) {
            int load = districts.get(districtNum);
            if (load == 0) {
                districtsByLoad.get("empty").add(districtNum);
            } else if (load == minLoad) {
                districtsByLoad.get("min").add(districtNum);
            } else if (load == minLoad + 1) {
                districtsByLoad.get("max").add(districtNum);
            } else if (load > minLoad + 1) {
                districtsByLoad.get("over").add(districtNum);
            }
        }

        if (districtsByLoad.get("empty").size() + districtsByLoad.get("max").size() == nDistricts ||
                districtsByLoad.get("min").size() + districtsByLoad.get("max").size() == nDistricts) {
            // No need to rebalance
            return null;
        }
        HashMap<Integer, Integer> changes = new HashMap<>();
        while (true) {
            int districtFromNum = 0;
            String districtFromType = null;
            int districtTo = 0;
            String districtToType = null;
            int robotToMove = 0;

            if (!districtsByLoad.get("over").isEmpty()) {
                districtFromType = "over";
                districtFromNum = districtsByLoad.get("over").get(0);
            } else if (!districtsByLoad.get("max").isEmpty()) {
                districtFromType = "max";
                districtFromNum = districtsByLoad.get("max").get(0);
            }

            if (!districtsByLoad.get("empty").isEmpty()) {
                districtToType = "empty";
                districtTo = districtsByLoad.get("empty").get(0);
            } else if (!districtsByLoad.get("min").isEmpty()) {
                districtToType = "min";
                districtTo = districtsByLoad.get("min").get(0);
            }

            for (Map.Entry<Integer, Integer> entry : robotRegister.entrySet()) {
                // Find the first robot in the originating district
                if (entry.getValue() == districtFromNum) {
                    robotToMove = entry.getKey();
                    break;
                }
            }
            changes.put(robotToMove, districtTo);

            districts.replace(districtFromNum, districts.get(districtFromNum) - 1); // Remove robot from the old district
            districts.replace(districtTo, districts.get(districtTo) + 1); // Add robot to the new district

            if (districts.get(districtFromNum) == minLoad + 1) {
                // Check again the size of the originating district and update
                districtsByLoad.get(districtFromType).remove(0);
                districtsByLoad.get("max").add(districtFromNum);
            } else if (districts.get(districtFromNum) == minLoad) {
                districtsByLoad.get(districtFromType).remove(0);
                districtsByLoad.get("min").add(districtFromNum);
            }
            districtsByLoad.get(districtToType).remove(0);
            if (districts.get(districtTo) == minLoad) {
                districtsByLoad.get("min").add(districtTo);
            } else if (districts.get(districtTo) == minLoad + 1) {
                districtsByLoad.get("max").add(districtTo);
            }

            if (!districtsByLoad.get("empty").isEmpty() || !districtsByLoad.get("over").isEmpty()) {
                continue;
            }
            if (districtsByLoad.get("min").size() + districtsByLoad.get("max").size() == nDistricts) {
                break;
            }
        }

        return changes;
    }

    /**
     * Produces a snapshot of the current grid status that is useful to print.
     *
     * @return A map of district ids to lists of robot ids
     */
    public synchronized HashMap<Integer, List<Integer>> getSnapshot() {
        HashMap<Integer, List<Integer>> gridStatus = (HashMap<Integer, List<Integer>>) robotRegister.entrySet().stream()
                .collect(Collectors.groupingBy(Map.Entry::getValue,
                        Collectors.mapping(Map.Entry::getKey, Collectors.toList())));
        for (District district : greenfieldMap.getDistricts()) {
            if (!gridStatus.containsKey(district.id())) {
                gridStatus.put(district.id(), new ArrayList<>());
            }
        }
        return gridStatus;
    }

    /**
     * Returns a deep copy of the current robot positions map.
     *
     * @return A copy of the current robot positions map
     */
    public synchronized HashMap<Integer, MapPosition> getRobotPositions() {
        HashMap<Integer, MapPosition> copy = new HashMap<>();
        robotPositions.forEach((key, value) -> copy.put(key, new MapPosition(value)));
        return copy;
    }
}
