package io.github.giuliapais.structures;

import io.github.giuliapais.commons.District;
import io.github.giuliapais.commons.GreenfieldMap;

import java.util.*;

public class DistrictBalancer {
    private final GreenfieldMap greenfieldMap = new GreenfieldMap();

    private final HashMap<Integer, Byte> robotRegister = new HashMap<>();
    private final HashMap<Byte, Integer> districtRegister;
    private final Random random = new Random();

    public DistrictBalancer() {
        int nDistricts = greenfieldMap.getDistricts().length;
        districtRegister = new HashMap<>(nDistricts);
        for (byte i = 1; i < nDistricts + 1; i++) {
            districtRegister.put(i, 0);
        }
    }

    public byte[] getPosInDistrict(byte districtId) {
        District district = greenfieldMap.getDistrict(districtId);
        int x = random.nextInt(district.getSpan_x()[1] - district.getSpan_x()[0] + 1) + district.getSpan_x()[0];
        int y = random.nextInt(district.getSpan_y()[1] - district.getSpan_y()[0] + 1) + district.getSpan_y()[0];
        return new byte[]{(byte) x, (byte) y};
    }

    public synchronized byte addRobot(int robotId) {
        // Check if the robot is already registered
        if (robotRegister.containsKey(robotId)) {
            return robotRegister.get(robotId);
        }
        // Get the less crowded district
        Optional<Byte> lessCrowded = districtRegister.entrySet()
                .stream()
                .min(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey);
        byte chosenDistrict = lessCrowded.orElse((byte) 1);
        robotRegister.put(robotId, chosenDistrict);
        districtRegister.replace(chosenDistrict, districtRegister.get(chosenDistrict) + 1);
        return chosenDistrict;
    }

    public synchronized boolean removeRobot(int robotId) {
        if (!robotRegister.containsKey(robotId)) {
            return false;
        }
        byte district = robotRegister.get(robotId);
        robotRegister.remove(robotId);
        districtRegister.replace(district, districtRegister.get(district) - 1);
        return true;
    }

    public synchronized int changeDistrict(int robotId, byte newDistrict) {
        if (!robotRegister.containsKey(robotId)) {
            return 1;
        }
        if (robotRegister.get(robotId) == newDistrict) {
            return 2;
        }
        byte oldDistrict = robotRegister.get(robotId);
        robotRegister.replace(robotId, newDistrict);
        districtRegister.replace(oldDistrict, districtRegister.get(oldDistrict) - 1);
        districtRegister.replace(newDistrict, districtRegister.get(newDistrict) + 1);
        return 0;
    }
}
