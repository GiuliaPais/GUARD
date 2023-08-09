package io.github.giuliapais.commons;

import java.util.*;


/**
 * Helper class that, based on the structure of the map and the current status, monitors and balances the
 * robots in th grid.
 */
public class DistrictBalancer {

    private final GreenfieldMap greenfieldMap = new GreenfieldMap();

    // Maps each robot currently active in the grid to its district
    private final HashMap<Integer, Integer> robotRegister = new HashMap<>();
    // Maps each district to the number of robots (count) currently active in it
    private final HashMap<Integer, Integer> districtRegister;
    private final Random random = new Random();

    public DistrictBalancer() {
        int nDistricts = greenfieldMap.getDistricts().length;
        districtRegister = new HashMap<>(nDistricts);
        for (int i = 1; i < nDistricts + 1; i++) {
            districtRegister.put(i, 0);
        }
    }

    public synchronized int getDistrict(int robotId) {
        return robotRegister.get(robotId);
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
     * it follows the natural order of numbering (from 1 to n).
     *
     * @param robotId the id of the robot to add
     * @return the district id the robot has been assigned to
     */
    public synchronized int addRobot(int robotId) {
        // Check if the robot is already registered
        if (robotRegister.containsKey(robotId)) {
            return robotRegister.get(robotId);
        }
        // Get the less crowded district
        Optional<Integer> lessCrowded = districtRegister.entrySet()
                .stream()
                .min(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey);
        Integer chosenDistrict = lessCrowded.orElse(1);
        robotRegister.put(robotId, chosenDistrict);
        districtRegister.replace(chosenDistrict, districtRegister.get(chosenDistrict) + 1);
        return chosenDistrict;
    }

    public synchronized void addRobot(int robotId, int districtId) {
        // Check if the robot is already registered
        if (robotRegister.containsKey(robotId)) {
            return;
        }
        robotRegister.put(robotId, districtId);
        districtRegister.replace(districtId, districtRegister.get(districtId) + 1);
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
        return true;
    }

    /**
     * Changes the assigned district for a robot.
     *
     * @param robotId     the id of the robot to move
     * @param newDistrict the id of the district to move the robot to
     * @return 0 if the operation was successful, 1 if the robot id was not registered,
     * 2 if the robot was already in the given district (no changes)
     */
    public synchronized int changeDistrict(int robotId, int newDistrict) {
        if (!robotRegister.containsKey(robotId)) {
            return 1;
        }
        if (Objects.equals(robotRegister.get(robotId), newDistrict)) {
            return 2;
        }
        Integer oldDistrict = robotRegister.get(robotId);
        robotRegister.replace(robotId, newDistrict);
        districtRegister.replace(oldDistrict, districtRegister.get(oldDistrict) - 1);
        districtRegister.replace(newDistrict, districtRegister.get(newDistrict) + 1);
        return 0;
    }

    /**
     * Changes the assigned district for a set of robots.
     *
     * @param changes a map of robot ids to new district ids
     * @return an array of integers, each representing the result of the operation for the corresponding robot id
     * (see {@link #changeDistrict(int, int)})
     */
    public synchronized int[] changeDistrict(HashMap<Integer, Integer> changes) {
        int[] results = new int[changes.size()];
        int i = 0;
        for (Map.Entry<Integer, Integer> entry : changes.entrySet()) {
            int code = changeDistrict(entry.getKey(), entry.getValue());
            results[i] = code;
            i++;
        }
        return results;
    }

    /**
     * Upserts a set of robots to the grid. If a robot is already registered, it changes its district
     * (if needed), otherwise it adds it to the grid.
     *
     * @param robotsToDistricts a map of robot ids to district ids
     */
    public synchronized void upsert(HashMap<Integer, Integer> robotsToDistricts) {
        for (Map.Entry<Integer, Integer> entry : robotsToDistricts.entrySet()) {
            int code = changeDistrict(entry.getKey(), entry.getValue());
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
}
