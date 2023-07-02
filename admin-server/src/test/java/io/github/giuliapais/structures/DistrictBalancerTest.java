package io.github.giuliapais.structures;

import io.github.giuliapais.commons.GreenfieldMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class DistrictBalancerTest {

    DistrictBalancer districtBalancer;

    @BeforeEach
    void setUp() {
        districtBalancer = new DistrictBalancer();
    }

    @Test
    @DisplayName("Adding a robot works properly")
    void addRobot() throws NoSuchFieldException, IllegalAccessException {
        byte chosenDistrict = districtBalancer.addRobot(1);
        assertEquals(1, chosenDistrict);
        Field robotRegisterField = districtBalancer.getClass().getDeclaredField("robotRegister");
        Field districtRegisterField = districtBalancer.getClass().getDeclaredField("districtRegister");
        robotRegisterField.setAccessible(true);
        districtRegisterField.setAccessible(true);
        HashMap<Integer, Byte> robotRegister = (HashMap<Integer, Byte>) robotRegisterField.get(districtBalancer);
        HashMap<Byte, Integer> districtRegister = (HashMap<Byte, Integer>) districtRegisterField.get(districtBalancer);
        assertEquals(1, robotRegister.size());
        assertTrue(robotRegister.containsKey(1) && robotRegister.get(1) != null && robotRegister.get(1) == 1);
        assertEquals(4, districtRegister.size());
        assertTrue(districtRegister.containsKey((byte) 1) && districtRegister.get((byte) 1) == 1);
        for (byte i = 2; i <= 4; i++) {
            assertTrue(districtRegister.containsKey(i) && districtRegister.get(i) == 0);
        }
    }

    @Test
    @DisplayName("Removing a robot works properly")
    void removeRobot() throws NoSuchFieldException, IllegalAccessException {
        for (int i = 1; i <= 4; i++) {
            districtBalancer.addRobot(i);
        }
        Field robotRegisterField = districtBalancer.getClass().getDeclaredField("robotRegister");
        Field districtRegisterField = districtBalancer.getClass().getDeclaredField("districtRegister");
        robotRegisterField.setAccessible(true);
        districtRegisterField.setAccessible(true);
        HashMap<Integer, Byte> robotRegister = (HashMap<Integer, Byte>) robotRegisterField.get(districtBalancer);
        HashMap<Byte, Integer> districtRegister = (HashMap<Byte, Integer>) districtRegisterField.get(districtBalancer);
        // Removing a robot that exists
        boolean removed = districtBalancer.removeRobot(1);
        assertTrue(removed);
        assertEquals(3, robotRegister.size());
        assertFalse(robotRegister.containsKey(1));
        assertEquals(4, districtRegister.size());
        assertTrue(districtRegister.containsKey((byte) 1) && districtRegister.get((byte) 1) == 0);
        // Removing a robot that doesn't exist
        removed = districtBalancer.removeRobot(10);
        assertFalse(removed);
        assertEquals(3, robotRegister.size());
        assertEquals(4, districtRegister.size());
    }

    @Test
    @DisplayName("Moving a robot works properly")
    void moveRobot() throws NoSuchFieldException, IllegalAccessException {
        for (int i = 1; i <= 4; i++) {
            districtBalancer.addRobot(i);
        }
        Field robotRegisterField = districtBalancer.getClass().getDeclaredField("robotRegister");
        Field districtRegisterField = districtBalancer.getClass().getDeclaredField("districtRegister");
        robotRegisterField.setAccessible(true);
        districtRegisterField.setAccessible(true);
        HashMap<Integer, Byte> robotRegister = (HashMap<Integer, Byte>) robotRegisterField.get(districtBalancer);
        HashMap<Byte, Integer> districtRegister = (HashMap<Byte, Integer>) districtRegisterField.get(districtBalancer);
        // Moving a robot that exists
        int moved = districtBalancer.changeDistrict(1, (byte) 4);
        assertEquals(0, moved);
        assertEquals(4, robotRegister.size());
        assertTrue(robotRegister.containsKey(1) && robotRegister.get(1) == 4);
        assertEquals(4, districtRegister.size());
        assertTrue(districtRegister.containsKey((byte) 1) && districtRegister.get((byte) 1) == 0);
        assertTrue(districtRegister.containsKey((byte) 4) && districtRegister.get((byte) 4) == 2);
        // Moving a robot to the same district
        moved = districtBalancer.changeDistrict(1, (byte) 4);
        assertEquals(2, moved);

        // Moving a robot that doesn't exist
        moved = districtBalancer.changeDistrict(10, (byte) 4);
        assertEquals(1, moved);
    }

    @Test
    @DisplayName("Repeating add operation on the same robot does not replicate")
    public void addSameRobot() {
        byte chosenDistrict = districtBalancer.addRobot(1);
        assertEquals(1, chosenDistrict);
        byte chosenDistrict2 = districtBalancer.addRobot(1);
        assertEquals(1, chosenDistrict2);
    }

    @Test
    @DisplayName("Getting a random position in a district")
    void getPosition() throws NoSuchFieldException, IllegalAccessException {
        byte[] gridPosition = districtBalancer.getPosInDistrict((byte) 1);
        Field greenfieldMapField = districtBalancer.getClass().getDeclaredField("greenfieldMap");
        greenfieldMapField.setAccessible(true);
        GreenfieldMap greenfieldMap = (GreenfieldMap) greenfieldMapField.get(districtBalancer);
        assertTrue(greenfieldMap.inDistrict(gridPosition, (byte) 1));
        byte[] gridPosition2 = districtBalancer.getPosInDistrict((byte) 1);
        assertFalse(gridPosition2[0] == gridPosition[0] & gridPosition2[1] == gridPosition[1]);
        assertTrue(greenfieldMap.inDistrict(gridPosition2, (byte) 1));
    }

    @Nested
    class LoadBalancing {
        @Test
        void whenAddingOnly() {
            for (int i = 1; i <= 8; i++) {
                byte chosenDistrict = districtBalancer.addRobot(i);
                if (i <= 4) {
                    assertEquals(i, chosenDistrict);
                } else {
                    assertEquals(i - 4, chosenDistrict);
                }
            }
        }

        @Test
        void whenMixedOP() {
            // Adding 4, removing 1
            for (int i = 1; i <= 4; i++) {
                districtBalancer.addRobot(i);
            }
            districtBalancer.removeRobot(3); // less crowded is district 3
            byte chosenDistrict = districtBalancer.addRobot(5);
            assertEquals(3, chosenDistrict);

            // Now moving 2 to district 4
            districtBalancer.changeDistrict(2, (byte) 4);
            chosenDistrict = districtBalancer.addRobot(6);
            assertEquals(2, chosenDistrict);
        }
    }
}