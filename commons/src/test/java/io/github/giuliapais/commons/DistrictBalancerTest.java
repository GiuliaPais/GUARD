package io.github.giuliapais.commons;

import io.github.giuliapais.commons.models.MapPosition;
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
        districtBalancer.addRobot(1);
        MapPosition robotPosition = districtBalancer.getRobotPosition(1);
        assertEquals(1, robotPosition.getDistrict());
        Field robotRegisterField = districtBalancer.getClass().getDeclaredField("robotRegister");
        Field districtRegisterField = districtBalancer.getClass().getDeclaredField("districtRegister");
        robotRegisterField.setAccessible(true);
        districtRegisterField.setAccessible(true);
        HashMap<Integer, Integer> robotRegister =
                (HashMap<Integer, Integer>) robotRegisterField.get(districtBalancer);
        HashMap<Integer, Integer> districtRegister =
                (HashMap<Integer, Integer>) districtRegisterField.get(districtBalancer);
        assertEquals(1, robotRegister.size());
        assertTrue(robotRegister.containsKey(1) && robotRegister.get(1) != null && robotRegister.get(1) == 1);
        assertEquals(4, districtRegister.size());
        assertTrue(districtRegister.containsKey(1) && districtRegister.get(1) == 1);
        for (int i = 2; i <= 4; i++) {
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
        HashMap<Integer, Integer> robotRegister =
                (HashMap<Integer, Integer>) robotRegisterField.get(districtBalancer);
        HashMap<Integer, Integer> districtRegister =
                (HashMap<Integer, Integer>) districtRegisterField.get(districtBalancer);
        // Removing a robot that exists
        boolean removed = districtBalancer.removeRobot(1);
        assertTrue(removed);
        assertEquals(3, robotRegister.size());
        assertFalse(robotRegister.containsKey(1));
        assertEquals(4, districtRegister.size());
        assertTrue(districtRegister.containsKey(1) && districtRegister.get(1) == 0);
        assertNull(districtBalancer.getRobotPosition(1));
        // Removing a robot that doesn't exist
        removed = districtBalancer.removeRobot(10);
        assertFalse(removed);
        assertEquals(3, robotRegister.size());
        assertEquals(4, districtRegister.size());
    }

    @Test
    @DisplayName("Positions are updated correctly from list of changes")
    void updatePositions() throws NoSuchFieldException, IllegalAccessException {
        Field robotRegisterField = districtBalancer.getClass().getDeclaredField("robotRegister");
        Field districtRegisterField = districtBalancer.getClass().getDeclaredField("districtRegister");
        robotRegisterField.setAccessible(true);
        districtRegisterField.setAccessible(true);
        HashMap<Integer, Integer> robotRegister =
                (HashMap<Integer, Integer>) robotRegisterField.get(districtBalancer);
        HashMap<Integer, Integer> districtRegister =
                (HashMap<Integer, Integer>) districtRegisterField.get(districtBalancer);
        for (int i = 1; i <= 6; i++) {
            districtBalancer.addRobot(i);
        }
        MapPosition robot1 = districtBalancer.getRobotPosition(1);
        MapPosition robot4 = districtBalancer.getRobotPosition(4);
        assertEquals(1, robot1.getDistrict());
        assertEquals(4, robot4.getDistrict());
        assertEquals(1, robotRegister.get(1));
        assertEquals(4, robotRegister.get(4));
        assertEquals(2, districtRegister.get(1));
        assertEquals(1, districtRegister.get(4));
        HashMap<Integer, MapPosition> changes = new HashMap<>();
        changes.put(1, new MapPosition(4, 8, 1));
        changes.put(4, new MapPosition(1, 1, 1));
        districtBalancer.updatePositions(changes);
        assertEquals(4, districtBalancer.getRobotPosition(1).getDistrict());
        assertEquals(8, districtBalancer.getRobotPosition(1).getX());
        assertEquals(1, districtBalancer.getRobotPosition(1).getY());
        assertEquals(1, districtBalancer.getRobotPosition(4).getDistrict());
        assertEquals(1, districtBalancer.getRobotPosition(4).getX());
        assertEquals(1, districtBalancer.getRobotPosition(4).getY());
        assertEquals(1, robotRegister.get(4));
        assertEquals(4, robotRegister.get(1));
        assertEquals(2, districtRegister.get(1));
        assertEquals(1, districtRegister.get(4));
    }

//    @Test
//    @DisplayName("Moving a robot works properly")
//    void moveRobot() throws NoSuchFieldException, IllegalAccessException {
//        for (int i = 1; i <= 4; i++) {
//            districtBalancer.addRobot(i);
//        }
//        Field robotRegisterField = districtBalancer.getClass().getDeclaredField("robotRegister");
//        Field districtRegisterField = districtBalancer.getClass().getDeclaredField("districtRegister");
//        robotRegisterField.setAccessible(true);
//        districtRegisterField.setAccessible(true);
//        HashMap<Integer, Integer> robotRegister = (HashMap<Integer, Integer>) robotRegisterField.get(districtBalancer);
//        HashMap<Integer, Integer> districtRegister = (HashMap<Integer, Integer>) districtRegisterField.get(districtBalancer);
//        // Moving a robot that exists
//        int moved = districtBalancer.changeDistrict(1, 4);
//        assertEquals(0, moved);
//        assertEquals(4, robotRegister.size());
//        assertTrue(robotRegister.containsKey(1) && robotRegister.get(1) == 4);
//        assertEquals(4, districtRegister.size());
//        assertTrue(districtRegister.containsKey(1) && districtRegister.get(1) == 0);
//        assertTrue(districtRegister.containsKey(4) && districtRegister.get(4) == 2);
//        // Moving a robot to the same district
//        moved = districtBalancer.changeDistrict(1, 4);
//        assertEquals(2, moved);
//
//        // Moving a robot that doesn't exist
//        moved = districtBalancer.changeDistrict(10, 4);
//        assertEquals(1, moved);
//    }

    @Test
    @DisplayName("Repeating add operation on the same robot does not replicate")
    public void addSameRobot() {
        districtBalancer.addRobot(1);
        MapPosition robotPosition = districtBalancer.getRobotPosition(1);
        assertEquals(1, robotPosition.getDistrict());
        districtBalancer.addRobot(1);
        MapPosition robotPosition2 = districtBalancer.getRobotPosition(1);
        assertEquals(1, robotPosition2.getDistrict());
    }

    @Test
    @DisplayName("Getting a random position in a district")
    void getPosition() throws NoSuchFieldException, IllegalAccessException {
        int[] gridPosition = districtBalancer.getPosInDistrict(1);
        Field greenfieldMapField = districtBalancer.getClass().getDeclaredField("greenfieldMap");
        greenfieldMapField.setAccessible(true);
        GreenfieldMap greenfieldMap = (GreenfieldMap) greenfieldMapField.get(districtBalancer);
        assertTrue(greenfieldMap.inDistrict(gridPosition, 1));
        int[] gridPosition2 = districtBalancer.getPosInDistrict(1);
        assertFalse(gridPosition2[0] == gridPosition[0] & gridPosition2[1] == gridPosition[1]);
        assertTrue(greenfieldMap.inDistrict(gridPosition2, 1));
    }

    @Nested
    class LoadBalancing {
        @Test
        void whenAddingOnly() {
            for (int i = 1; i <= 8; i++) {
                districtBalancer.addRobot(i);
                MapPosition robotPosition = districtBalancer.getRobotPosition(i);
                if (i <= 4) {
                    assertEquals(i, robotPosition.getDistrict());
                } else {
                    assertEquals(i - 4, robotPosition.getDistrict());
                }
            }
        }

//        @Test
//        void whenMixedOP() {
//            // Adding 4, removing 1
//            for (int i = 1; i <= 4; i++) {
//                districtBalancer.addRobot(i);
//            }
//            districtBalancer.removeRobot(3); // less crowded is district 3
//            districtBalancer.addRobot(5);
//            MapPosition robotPosition = districtBalancer.getRobotPosition(5);
//            assertEquals(3, robotPosition.getDistrict());
//
//            // Now moving 2 to district 4
//            districtBalancer.changeDistrict(2, 4);
//            robotPosition = districtBalancer.getRobotPosition(6);
//            assertEquals(2, robotPosition.getDistrict());
//        }

        @Test
        void whenRobotsLessThanDistrictThenChangesNull() throws NoSuchFieldException, IllegalAccessException {
            Field robotRegisterField = districtBalancer.getClass().getDeclaredField("robotRegister");
            Field districtRegisterField = districtBalancer.getClass().getDeclaredField("districtRegister");
            robotRegisterField.setAccessible(true);
            districtRegisterField.setAccessible(true);

            // TEST SET
            HashMap<Integer, Integer> districts = new HashMap<>();
            HashMap<Integer, Integer> robots = new HashMap<>();
            districts.put(1, 1);
            districts.put(2, 1);
            districts.put(3, 1);
            districts.put(4, 0);
            robots.put(1, 1);
            robots.put(2, 2);
            robots.put(3, 3);
            robotRegisterField.set(districtBalancer, robots);
            districtRegisterField.set(districtBalancer, districts);

            HashMap<Integer, Integer> changes = districtBalancer.rebalance();
            assertNull(changes);
        }

        @Test
        void whenRobotsBalancedThenNull() throws NoSuchFieldException, IllegalAccessException {
            Field robotRegisterField = districtBalancer.getClass().getDeclaredField("robotRegister");
            Field districtRegisterField = districtBalancer.getClass().getDeclaredField("districtRegister");
            robotRegisterField.setAccessible(true);
            districtRegisterField.setAccessible(true);

            // TEST SET
            HashMap<Integer, Integer> districts = new HashMap<>();
            HashMap<Integer, Integer> robots = new HashMap<>();
            districts.put(1, 2);
            districts.put(2, 1);
            districts.put(3, 1);
            districts.put(4, 1);
            robots.put(1, 1);
            robots.put(2, 1);
            robots.put(3, 2);
            robots.put(4, 3);
            robots.put(5, 4);
            robotRegisterField.set(districtBalancer, robots);
            districtRegisterField.set(districtBalancer, districts);

            HashMap<Integer, Integer> changes = districtBalancer.rebalance();
            assertNull(changes);

            districts.clear();
            districts.put(1, 2);
            districts.put(2, 2);
            districts.put(3, 2);
            districts.put(4, 2);
            robots.clear();
            robots.put(1, 1);
            robots.put(2, 1);
            robots.put(3, 2);
            robots.put(4, 2);
            robots.put(5, 3);
            robots.put(6, 3);
            robots.put(7, 4);
            robots.put(8, 4);
            robotRegisterField.set(districtBalancer, robots);
            districtRegisterField.set(districtBalancer, districts);

            changes = districtBalancer.rebalance();
            assertNull(changes);
        }

        @Test
        void whenUnbalancedThenNotNull() throws NoSuchFieldException, IllegalAccessException {
            Field robotRegisterField = districtBalancer.getClass().getDeclaredField("robotRegister");
            Field districtRegisterField = districtBalancer.getClass().getDeclaredField("districtRegister");
            robotRegisterField.setAccessible(true);
            districtRegisterField.setAccessible(true);

            // TEST SET
            HashMap<Integer, Integer> districts = new HashMap<>();
            HashMap<Integer, Integer> robots = new HashMap<>();
            districts.put(1, 2);
            districts.put(2, 0);
            districts.put(3, 3);
            districts.put(4, 0);
            robots.put(1, 1);
            robots.put(2, 1);
            robots.put(3, 3);
            robots.put(4, 3);
            robots.put(5, 4);
            robotRegisterField.set(districtBalancer, robots);
            districtRegisterField.set(districtBalancer, districts);

            HashMap<Integer, Integer> changes = districtBalancer.rebalance();
            assertNotNull(changes);
            assertEquals(2, changes.size());
            assertEquals(districtRegisterField.get(districtBalancer), districts);
            assertEquals(robotRegisterField.get(districtBalancer), robots);

            districts.clear();
            districts.put(1, 1);
            districts.put(2, 3);
            districts.put(3, 1);
            districts.put(4, 1);
            robots.clear();
            robots.put(1, 1);
            robots.put(2, 2);
            robots.put(3, 2);
            robots.put(4, 2);
            robots.put(5, 3);
            robots.put(6, 4);
            robotRegisterField.set(districtBalancer, robots);
            districtRegisterField.set(districtBalancer, districts);

            changes = districtBalancer.rebalance();
            assertNotNull(changes);
            assertEquals(1, changes.size());
            assertFalse(changes.containsValue(2));
            assertEquals(districtRegisterField.get(districtBalancer), districts);
            assertEquals(robotRegisterField.get(districtBalancer), robots);

            districts.clear();
            districts.put(1, 1);
            districts.put(2, 3);
            districts.put(3, 1);
            districts.put(4, 2);
            robots.clear();
            robots.put(1, 1);
            robots.put(2, 2);
            robots.put(3, 2);
            robots.put(4, 2);
            robots.put(5, 3);
            robots.put(6, 4);
            robots.put(7, 4);
            robotRegisterField.set(districtBalancer, robots);
            districtRegisterField.set(districtBalancer, districts);

            changes = districtBalancer.rebalance();
            assertNotNull(changes);
            assertEquals(1, changes.size());
            assertFalse(changes.containsValue(2));
            assertFalse(changes.containsValue(4));
            assertEquals(districtRegisterField.get(districtBalancer), districts);
            assertEquals(robotRegisterField.get(districtBalancer), robots);
        }
    }
}