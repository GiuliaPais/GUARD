package io.github.giuliapais.commons;

/**
 * Represents the map of the Greenfield city.
 * The map has 4 districts and spans 10x10 blocks.
 */
public class GreenfieldMap {
    private final District[] districts;
    private final byte width = 10;
    private final byte height = 10;


    /**
     * Creates a new GreenfieldMap object.
     */
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

    /**
     * Returns an array of {@link District} objects representing the districts in the map.
     *
     * @return the array of districts in the map
     */
    public District[] getDistricts() {
        return districts;
    }

    /**
     * Returns the district with the given id.
     *
     * @param id the id of the district
     * @return the {@link District} with the given id
     */
    public District getDistrict(int id) {
        return districts[id - 1];
    }

    /**
     * Given a set of coordinates and a district id,
     * returns true if the coordinates are in the district, false otherwise.
     *
     * @param coords   x and y coordinates as int array
     * @param district the id of the district (1-4)
     * @return true if the coordinates are in the district, false otherwise
     */
    public boolean inDistrict(int[] coords, int district) {
        return districts[district - 1].inDistrict(coords);
    }
}
