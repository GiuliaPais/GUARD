package io.github.giuliapais.commons;

public record District(int id, int[] span_x, int[] span_y) {

    public boolean inDistrict(int x, int y) {
        return x >= span_x[0] && x <= span_x[1] && y >= span_y[0] && y <= span_y[1];
    }

    public boolean inDistrict(int[] coords) {
        return inDistrict(coords[0], coords[1]);
    }
}
