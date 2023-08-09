package io.github.giuliapais.commons;

public class GreenfieldMap {
    private final District[] districts;
    private final byte width = 10;
    private final byte height = 10;


    public GreenfieldMap() {
        districts = new District[4];
        for (int i = 1; i < 5; i++) {
            int[] temp_x = new int[2];
            int[] temp_y = new int[2];
            if (i <= 2) {
                temp_x[1] = (width / 2) - 1;
            } else {
                temp_x[0] = (width / 2);
                temp_x[1] = (width - 1);
            }
            if (i == 1 || i == 4) {
                temp_y[1] = (height / 2) - 1;
            } else {
                temp_y[0] = (height / 2);
                temp_y[1] = (height - 1);
            }
            districts[i - 1] = new District(i, temp_x, temp_y);
        }
    }

    public District[] getDistricts() {
        return districts;
    }

    public District getDistrict(int id) {
        return districts[id - 1];
    }

    public byte getWidth() {
        return width;
    }

    public byte getHeight() {
        return height;
    }

    public boolean inDistrict(int x, int y, int district) {
        return districts[district - 1].inDistrict(x, y);
    }

    public boolean inDistrict(int[] coords, int district) {
        return districts[district - 1].inDistrict(coords);
    }
}
