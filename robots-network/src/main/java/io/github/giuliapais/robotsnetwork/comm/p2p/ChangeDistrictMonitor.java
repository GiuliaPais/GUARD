package io.github.giuliapais.robotsnetwork.comm.p2p;

public class ChangeDistrictMonitor {
    private static volatile ChangeDistrictMonitor instance;
    private int newDistrict = 0;

    private ChangeDistrictMonitor() {
    }

    public static ChangeDistrictMonitor getInstance() {
        if (instance == null) {
            synchronized (ChangeDistrictMonitor.class) {
                if (instance == null) {
                    instance = new ChangeDistrictMonitor();
                }
            }
        }
        return instance;
    }

    public synchronized void districtChanged(int newDistrict) {
        this.newDistrict = newDistrict;
        notifyAll();
    }

    public synchronized int monitorChanges() throws InterruptedException {
        while (newDistrict == 0) {
            wait();
        }
        int result = newDistrict;
        newDistrict = 0;
        return result;
    }
}
