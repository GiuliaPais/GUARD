package io.github.giuliapais.api.services;

import io.github.giuliapais.api.models.MapPosition;
import io.github.giuliapais.api.models.Robot;
import io.github.giuliapais.api.models.RobotCreateResponse;
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

    public RobotCreateResponse addRobot(Robot robot) throws IdPresentException {
        List<Robot> activeRobots = null;
        synchronized (this) {
            activeRobots = List.copyOf(robots.getMap().values());
            robots.put(robot);
        }
        byte assignedDistrict = districtBalancer.addRobot(robot.getId());
        byte[] pos = districtBalancer.getPosInDistrict(assignedDistrict);
        RobotCreateResponse response = new RobotCreateResponse();
        MapPosition mapPosition = new MapPosition();
        mapPosition.setDistrict(assignedDistrict);
        mapPosition.setX(pos[0]);
        mapPosition.setY(pos[1]);
        response.setMapPosition(mapPosition);
        response.setActiveRobots(activeRobots);
        response.setIdentity(robot);
        return response;
    }

    public boolean removeRobot(int id) {
        boolean mapRemoved = robots.remove(id);
        boolean districtRemoved = districtBalancer.removeRobot(id);
        return mapRemoved || districtRemoved;
    }

    public List<Robot> getAllRobots() {
        return List.copyOf(robots.getMap().values());
    }

    public boolean updateRobot(int id, Robot robot) {
        return robots.update(id, robot);
    }

    public int updatePosition(int id, MapPosition newPos) {
        int changed = districtBalancer.changeDistrict(id, (byte) newPos.getDistrict());
        if (changed == 0) {
            byte[] pos = districtBalancer.getPosInDistrict((byte) newPos.getDistrict());
            newPos.setX(pos[0]);
            newPos.setY(pos[1]);
        }
        return changed;
    }

}
