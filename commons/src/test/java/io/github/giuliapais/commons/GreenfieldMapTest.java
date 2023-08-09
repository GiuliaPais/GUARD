package io.github.giuliapais.commons;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GreenfieldMapTest {

    @Test
    void correctDistricts() {
        GreenfieldMap map = new GreenfieldMap();
        District[] districts = map.getDistricts();
        assertEquals(4, districts.length);
        for (int i = 0; i < 4; i++) {
            assertEquals(i + 1, districts[i].id());
        }
        assertArrayEquals(
                districts[0].span_x(),
                new int[]{0, 4}
        );
        assertArrayEquals(
                districts[0].span_y(),
                new int[]{0, 4}
        );
        assertArrayEquals(
                districts[1].span_x(),
                new int[]{0, 4}
        );
        assertArrayEquals(
                districts[1].span_y(),
                new int[]{5, 9}
        );
        assertArrayEquals(
                districts[2].span_x(),
                new int[]{5, 9}
        );
        assertArrayEquals(
                districts[2].span_y(),
                new int[]{5, 9}
        );
        assertArrayEquals(
                districts[3].span_x(),
                new int[]{5, 9}
        );
        assertArrayEquals(
                districts[3].span_y(),
                new int[]{0, 4}
        );
    }

}