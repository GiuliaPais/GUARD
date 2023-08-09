package io.github.giuliapais.robotsnetwork.comm;

import java.util.PriorityQueue;

public class CrashEventMonitor {
    private static CrashEventMonitor instanceOutgoing;
    private static CrashEventMonitor instanceIncoming;
    private PriorityQueue<EventWrapper> crashEvents = new PriorityQueue<>();

    private static class EventWrapper implements Comparable<EventWrapper> {
        CrashEvent event;

        public EventWrapper(CrashEvent event) {
            this.event = event;
        }

        @Override
        public int compareTo(EventWrapper o) {
            // Compare crashedRobotId
            int crashedRobotIdComparison = Integer.compare(event.getCrashedRobotId(), o.getEvent().getCrashedRobotId());
            if (crashedRobotIdComparison != 0) {
                return crashedRobotIdComparison;
            }
            // Compare timestamp
            int timestampComparison = Long.compare(event.getTimestamp(), o.getEvent().getTimestamp());
            if (timestampComparison != 0) {
                return timestampComparison;
            }
            // Compare robotId
            int robotIdComparison = Integer.compare(event.getRobotId(), o.getEvent().getRobotId());
            if (robotIdComparison != 0) {
                return robotIdComparison;
            }
            return 0;
        }

        public CrashEvent getEvent() {
            return event;
        }
    }

    private CrashEventMonitor() {
    }

    public enum CrashEventMonitorType {
        INCOMING,
        OUTGOING
    }

    public static CrashEventMonitor getInstance(CrashEventMonitorType type) {
        if (type == CrashEventMonitorType.INCOMING) {
            CrashEventMonitor result = instanceIncoming;
            if (result == null) {
                synchronized (CrashEventMonitor.class) {
                    result = instanceIncoming;
                    if (result == null) {
                        instanceIncoming = result = new CrashEventMonitor();
                    }
                }
            }
            return result;
        } else {
            CrashEventMonitor result = instanceOutgoing;
            if (result == null) {
                synchronized (CrashEventMonitor.class) {
                    result = instanceOutgoing;
                    if (result == null) {
                        instanceOutgoing = result = new CrashEventMonitor();
                    }
                }
            }
            return result;
        }
    }

    public synchronized void addCrashEvent(CrashEvent crashEvent) {
        crashEvents.add(new EventWrapper(crashEvent));
        notifyAll();
    }

    public synchronized CrashEvent getCrashEvent() throws InterruptedException {
        while (crashEvents.isEmpty()) {
            wait();
        }
        return crashEvents.poll().getEvent();
    }

}
