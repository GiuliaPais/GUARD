package io.github.giuliapais.structures;

import io.github.giuliapais.api.models.Robot;
import io.github.giuliapais.exceptions.IdPresentException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RobotHashMapUnitTest {

    RobotHashMap robotHashMap;

    @BeforeEach
    void setUp() {
        robotHashMap = new RobotHashMap();
    }

    @Nested
    class PutTests {

        @Test
        @DisplayName("Should throw NullPointerException when null is passed")
        @Tag("single-thread")
        void putNull() {
            assertThrows(NullPointerException.class, () -> robotHashMap.put(null));
        }

        @Test
        @DisplayName("Should throw IdPresentException when id is already present - single thread")
        void putIsolation() {
            Robot robot1 = mock(Robot.class);
            Robot robot2 = mock(Robot.class);
            when(robot1.getId()).thenReturn(1);
            when(robot2.getId()).thenReturn(1);
            assertThrows(IdPresentException.class, () -> {
                robotHashMap.put(robot1);
                robotHashMap.put(robot2);
            });
        }

        @RepeatedTest(10)
        @DisplayName("Should throw IdPresentException when id is already present - multi thread")
        void putMultiThread() throws InterruptedException {
            ExecutorService service = Executors.newFixedThreadPool(10);
            CountDownLatch latch = new CountDownLatch(1);
            ConcurrentLinkedQueue<Boolean> results = new ConcurrentLinkedQueue<>();
            for (int i = 0; i < 10; i++) {
                service.submit(() -> {
                    try {
                        latch.await();
                        Robot robot = mock(Robot.class);
                        when(robot.getId()).thenReturn(1);
                        robotHashMap.put(robot);
                        results.add(true); // Add true to results if put operation succeeds
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt(); // Restore the interrupt status
                    } catch (IdPresentException e) {
                        results.add(false); // Add false to results if put operation fails
                    }
                });
            }
            latch.countDown();
            service.shutdown();
            assertTrue(service.awaitTermination(1, TimeUnit.MINUTES),
                    "Timeout was reached before all tasks finished.");
            long successfulPuts = results.stream().filter(x -> x).count();
            long failedPuts = results.size() - successfulPuts;
            assertEquals(1, successfulPuts);
            assertEquals(9, failedPuts);
        }
    }

}