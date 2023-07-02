package io.github.giuliapais.commons;

public class GreenfieldMap {
    private final District[] districts;
    private final byte width = 10;
    private final byte height = 10;


    public GreenfieldMap() {
        districts = new District[4];
        for (int i = 1; i < 5; i++) {
            byte[] temp_x = new byte[2];
            byte[] temp_y = new byte[2];
            if (i <= 2) {
                temp_x[1] = (width / 2) - 1;
            } else {
                temp_x[0] = (byte) (width / 2);
                temp_x[1] = (byte) (width - 1);
            }
            if (i == 1 || i == 4) {
                temp_y[1] = (height / 2) - 1;
            } else {
                temp_y[0] = (byte) (height / 2);
                temp_y[1] = (byte) (height - 1);
            }
            districts[i - 1] = new District((byte) i, temp_x, temp_y);
        }
    }

    public District[] getDistricts() {
        return districts;
    }

    public District getDistrict(byte id) {
        return districts[id - 1];
    }

    public byte getWidth() {
        return width;
    }

    public byte getHeight() {
        return height;
    }

    public boolean inDistrict(byte x, byte y, byte district) {
        return districts[district - 1].inDistrict(x, y);
    }

    public boolean inDistrict(byte[] coords, byte district) {
        return districts[district - 1].inDistrict(coords);
    }
}
