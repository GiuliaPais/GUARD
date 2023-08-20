package io.github.giuliapais.api.services;

import io.github.giuliapais.commons.models.MapPosition;
import io.github.giuliapais.api.models.Robot;
import io.github.giuliapais.api.models.RobotCreateResponse;
import io.github.giuliapais.commons.models.RobotInfo;
import io.github.giuliapais.commons.models.RobotPosUpdate;
import io.github.giuliapais.exceptions.IdPresentException;
import io.github.giuliapais.commons.DistrictBalancer;
import io.github.giuliapais.structures.RobotHashMap;
import io.github.giuliapais.commons.MessagePrinter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Singleton class, offers the actual services for robot management
 */
public class RobotService {

    private static volatile RobotService instance = null;
    private final RobotHashMap robots = new RobotHashMap();
    private final DistrictBalancer districtBalancer = new DistrictBalancer();

    private RobotService() {
    }

    public static RobotService getInstance() {
        RobotService result = instance;
        if (result == null) {
            synchronized (RobotService.class) {
                result = instance;
                if (result == null) {
                    instance = result = new RobotService();
                }
            }
        }
        return result;
    }

    /**
     * When receiving a POST request, the function tries to add the robot to the list of active robots.
     * If insertion fails (robot id already present) an exception is raised and an appropriate response is
     * sent automatically by the server.
     * If insertion succeeds, the robot is added to the grid in the less crowded district and a random position in
     * the chosen district is included in the response.
     *
     * @param robot The robot to be added
     * @return A response containing the robot's position and the list of active robots
     * @throws IdPresentException If the robot id is already present in the grid
     */
    public RobotCreateResponse addRobot(Robot robot) throws IdPresentException {
        List<Robot> activeRobots = null;
        synchronized (this) {
            activeRobots = List.copyOf(robots.getMap().values());
            robots.put(robot); // Raises exception if robot already present
        }
        districtBalancer.addRobot(robot.getId());
        MapPosition mapPosition = districtBalancer.getRobotPosition(robot.getId());
        RobotCreateResponse response = new RobotCreateResponse();
        response.setMapPosition(mapPosition);
        response.setActiveRobots(activeRobots);
        response.setIdentity(robot);
        return response;
    }

    /**
     * Removes a robot from the active robots list and from the grid.
     *
     * @param id The robot id to remove
     * @return True if the robot was removed from either the active robots or the grid, false otherwise
     */
    public boolean removeRobot(int id) {
        boolean mapRemoved = robots.remove(id);
        boolean districtRemoved = districtBalancer.removeRobot(id);
        return mapRemoved || districtRemoved;
    }

    public List<RobotInfo> getAllRobots() {
        // Gets robots ids, ipAddress and port
        HashMap<Integer, Robot> map = robots.getMap();
        // Gets robot positions
        HashMap<Integer, MapPosition> positions = districtBalancer.getRobotPositions();
        // Creates a list
        List<RobotInfo> robotsInfo = new ArrayList<>();
        for (Integer key : map.keySet()) {
            Robot robot = map.get(key);
            MapPosition position = positions.get(key);
            robotsInfo.add(new RobotInfo(robot.getId(), robot.getIpAddress(), robot.getPort(), position));
        }
        return robotsInfo;
    }

    public void updateRobotPositions(List<RobotPosUpdate> changes) {
        HashMap<Integer, MapPosition> changesMap = changes.stream()
                .collect(HashMap::new, (m, v) -> m.put(v.getRobotId(), v.getMapPosition()), HashMap::putAll);
        districtBalancer.updatePositions(changesMap);
    }

    public void printGridStatus() {
        HashMap<Integer, List<Integer>> grid = districtBalancer.getSnapshot();
        MessagePrinter.printMessage("Grid status:", MessagePrinter.INFO_FORMAT, true);
        for (Map.Entry<Integer, List<Integer>> entry : grid.entrySet()) {
            MessagePrinter.printMessage("District " + entry.getKey() + " -> " + entry.getValue(),
                    MessagePrinter.INFO_FORMAT, true);
        }
    }

}
