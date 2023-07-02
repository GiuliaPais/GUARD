package io.github.giuliapais.commons;

public class District {
    private final byte id;
    private final byte[] span_x;
    private final byte[] span_y;

    public District(byte id, byte[] span_x, byte[] span_y) {
        this.id = id;
        this.span_x = span_x;
        this.span_y = span_y;
    }

    public byte getId() {
        return id;
    }

    public byte[] getSpan_x() {
        return span_x;
    }

    public byte[] getSpan_y() {
        return span_y;
    }

    public boolean inDistrict(byte x, byte y) {
        return x >= span_x[0] && x <= span_x[1] && y >= span_y[0] && y <= span_y[1];
    }

    public boolean inDistrict(byte[] coords) {
        return inDistrict(coords[0], coords[1]);
    }
}
