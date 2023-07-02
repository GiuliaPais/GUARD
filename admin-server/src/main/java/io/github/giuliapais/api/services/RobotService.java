package io.github.giuliapais.api.services;

import io.github.giuliapais.api.models.Robot;
import io.github.giuliapais.exceptions.IdPresentException;
import io.github.giuliapais.structures.DistrictBalancer;
import io.github.giuliapais.structures.RobotHashMap;

import java.util.List;

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

    public Robot addRobot(Robot robot) throws IdPresentException {
        robots.put(robot);
        byte assignedDistrict = districtBalancer.addRobot(robot.getId());
        byte[] pos = districtBalancer.getPosInDistrict(assignedDistrict);
        robot.setDistrict(assignedDistrict);
        robot.setPosition(pos);
        return robot;
    }

    public boolean removeRobot(int id) {
        boolean mapRemoved = robots.remove(id);
        boolean districtRemoved = districtBalancer.removeRobot(id);
        return mapRemoved || districtRemoved;
    }

    public List<Robot> getAllRobots() {
        return List.copyOf(robots.getMap().values());
    }

    public Robot updateRobot(int id, Robot robot) {
        return robots.update(id, robot, districtBalancer);
    }
}
