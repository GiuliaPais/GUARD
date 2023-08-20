package io.github.giuliapais.adminclient;

import io.github.giuliapais.commons.MessagePrinter;
import io.github.giuliapais.commons.models.RobotInfo;
import jakarta.ws.rs.core.Response;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class AdminClient {

    private final static HashMap<Integer, String> availableStatsCommands = new HashMap<>();

    static {
        availableStatsCommands.put(1, "robots");
        availableStatsCommands.put(2, "last-n");
        availableStatsCommands.put(3, "between");
        availableStatsCommands.put(0, "back");
    }

    private static boolean isValidStatCommand(String command) {
        String cleanCmd = command.trim().toLowerCase();
        // Check if can be converted into number
        Integer cmdNumber = null;
        try {
            cmdNumber = Integer.parseInt(cleanCmd);
            return availableStatsCommands.containsKey(cmdNumber);
        } catch (NumberFormatException e) {
            return availableStatsCommands.containsValue(cleanCmd);
        }
    }

    private static int getN(Scanner scanner, String message) {
        MessagePrinter.printMessage(message,
                MessagePrinter.ACCENT_FORMAT, false);
        String nRecords = scanner.nextLine();
        try {
            int n = Integer.parseInt(nRecords);
            if (n <= 0) {
                MessagePrinter.printMessage("Invalid number, please try again",
                        MessagePrinter.ERROR_FORMAT, true);
                return getN(scanner, message);
            }
            return n;
        } catch (NumberFormatException e) {
            MessagePrinter.printMessage("Invalid number, please try again",
                    MessagePrinter.ERROR_FORMAT, true);
            return getN(scanner, message);
        }
    }

    private static long getTimestamp(Scanner scanner, String message) {
        MessagePrinter.printMessage(message + ": ",
                MessagePrinter.ACCENT_FORMAT, false);
        String timestamp = scanner.nextLine();
        try {
            long ts = Long.parseLong(timestamp);
            if (ts < 0) {
                MessagePrinter.printMessage("Invalid timestamp, please try again",
                        MessagePrinter.ERROR_FORMAT, true);
                return getTimestamp(scanner, message);
            }
            return ts;
        } catch (NumberFormatException e) {
            MessagePrinter.printMessage("Invalid timestamp, please try again",
                    MessagePrinter.ERROR_FORMAT, true);
            return getTimestamp(scanner, message);
        }
    }

    public static void main(String[] args) {
        MessagePrinter.printClientWelcomeMessage();
        // Init rest client
        RestServiceManager restServiceManager = new RestServiceManager();

        Scanner scanner = new Scanner(System.in);
        MessagePrinter.printClientAvailableCommands();
        boolean exit = false;
        while (!exit) {
            String input = scanner.nextLine();
            if (input.equals("quit")) {
                exit = true;
                continue;
            }
            if (!input.equals("stats")) {
                MessagePrinter.printClientAvailableCommands();
                continue;
            }
            MessagePrinter.printStatsCommands();
            String subCommand;
            while (true) {
                subCommand = scanner.nextLine();
                if (subCommand.equals("back") || subCommand.equals("0")) {
                    MessagePrinter.printClientAvailableCommands();
                    break;
                }
                while (!isValidStatCommand(subCommand)) {
                    MessagePrinter.printMessage(
                            "Invalid command, please provide either the number or the command name without quotes",
                            MessagePrinter.ERROR_FORMAT, true
                    );
                    subCommand = scanner.nextLine();
                }
                if (subCommand.equals("robots") || subCommand.equals("1")) {
                    List<RobotInfo> robots = restServiceManager.getRobots();
                    if (robots != null) {
                        MessagePrinter.printMessage("Here is the list of active robots:",
                                MessagePrinter.INFO_FORMAT, true);
                        System.out.println(robots);
                        MessagePrinter.printMessage(MessagePrinter.CMD_PROMPT, MessagePrinter.ACCENT_FORMAT,
                                false);
                    }
                } else if (subCommand.equals("last-n") || subCommand.equals("2")) {
                    int n = getN(scanner, "Type a value for N (>= 1): ");
                    int robotId = getN(scanner, "Type a value for the robot ID (>= 1): ");
                    Response response = restServiceManager.getLastNMeasurements(robotId, n);
                    if (response.getStatus() == 404) {
                        MessagePrinter.printMessage(
                                "The robot with id " + robotId + " is not present in the system",
                                MessagePrinter.ERROR_FORMAT, true
                        );
                        MessagePrinter.printMessage(MessagePrinter.CMD_PROMPT, MessagePrinter.ACCENT_FORMAT,
                                false);
                    } else {
                        Double average = response.readEntity(Double.class);
                        MessagePrinter.printMessage(
                                "The average pollution level for the last " + n + " records for robot " +
                                        robotId + " is: " +
                                        new DecimalFormat("####.##").format(average),
                                MessagePrinter.INFO_FORMAT, true
                        );
                        MessagePrinter.printMessage(MessagePrinter.CMD_PROMPT, MessagePrinter.ACCENT_FORMAT,
                                false);
                    }
                } else if (subCommand.equals("between") || subCommand.equals("3")) {
                    MessagePrinter.printMessage(
                            "Please provide the timestamps as numbers " +
                                    "(long format - milliseconds between the ts and midnight Jan 1, 1970). " +
                                    "The current timestamp is: " + System.currentTimeMillis(),
                            MessagePrinter.ACCENT_FORMAT,
                            true
                    );
                    long t1 = getTimestamp(scanner, "t1");
                    long t2 = getTimestamp(scanner, "t2");
                    Response response = restServiceManager.getRecordsBetween(t1, t2);
                    MessagePrinter.printMessage(
                            "The average pollution level between " + t1 + " and " + t2 + " is: " +
                                    new DecimalFormat("####.##").format(response.readEntity(Double.class)),
                            MessagePrinter.INFO_FORMAT, true
                    );
                    MessagePrinter.printMessage(MessagePrinter.CMD_PROMPT, MessagePrinter.ACCENT_FORMAT,
                            false);
                }
            }
        }
    }
}
