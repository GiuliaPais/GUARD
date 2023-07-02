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
            assertEquals(i + 1, districts[i].getId());
        }
        assertArrayEquals(
                districts[0].getSpan_x(),
                new byte[]{0, 4}
        );
        assertArrayEquals(
                districts[0].getSpan_y(),
                new byte[]{0, 4}
        );
        assertArrayEquals(
                districts[1].getSpan_x(),
                new byte[]{0, 4}
        );
        assertArrayEquals(
                districts[1].getSpan_y(),
                new byte[]{5, 9}
        );
        assertArrayEquals(
                districts[2].getSpan_x(),
                new byte[]{5, 9}
        );
        assertArrayEquals(
                districts[2].getSpan_y(),
                new byte[]{5, 9}
        );
        assertArrayEquals(
                districts[3].getSpan_x(),
                new byte[]{5, 9}
        );
        assertArrayEquals(
                districts[3].getSpan_y(),
                new byte[]{0, 4}
        );
    }

}