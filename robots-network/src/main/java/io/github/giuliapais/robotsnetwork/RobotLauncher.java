package io.github.giuliapais.robotsnetwork;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.giuliapais.commons.models.MapPosition;
import io.github.giuliapais.robotsnetwork.comm.p2p.Peer;
import io.github.giuliapais.robotsnetwork.comm.rest.RestServiceManager;
import io.github.giuliapais.robotsnetwork.core.CleaningRobot;
import io.github.giuliapais.utils.InputValidator;
import io.github.giuliapais.commons.MessagePrinter;
import picocli.CommandLine;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Scanner;


@CommandLine.Command(name = "robot-launcher", description = "Initializes a new robot process",
        mixinStandardHelpOptions = true)
public class RobotLauncher implements Runnable {

    @CommandLine.Option(names = {"-i", "--id"},
            description = "Robot ID - must be an integer number unique in the network")
    private int id;

    @CommandLine.Option(names = {"-p", "--port"},
            description = "Robot port - port number on which the robot will listen for requests " +
                    "from other robots. NOTE: if all robots are on local network, " +
                    "ensure that the port is not already in use.")
    private int port;

    @CommandLine.Option(names = {"-s", "--server-address"},
            description = "Server address - address of the server to which the robot will " +
                    "register itself. Must include also the port number (e.g. localhost:8080). " +
                    "Defaults to ${DEFAULT-VALUE}")
    private String serverAddress = "localhost:9090";

    @CommandLine.Option(names = {"-h", "--help"}, usageHelp = true,
            description = "Display this help message")
    private boolean helpRequested = false;

    private static CleaningRobot cleaningRobot;

    private static Scanner scanner;

    private void checkInput(String which) throws NoSuchFieldException, IllegalAccessException {
        Field field = RobotLauncher.class.getDeclaredField(which);
        field.setAccessible(true);
        InputValidator.ValidationResult validationResult = InputValidator.validate(which, field.get(this));
        if (validationResult.code() != 0) {
            MessagePrinter.printParamErrorMessage(which);
            MessagePrinter.printMessage(MessagePrinter.CMD_PROMPT, MessagePrinter.ERROR_FORMAT, false);
            boolean valid = false;
            InputValidator.ValidationResult newValidationResult;
            while (!valid) {
                String input = scanner.nextLine();
                if (input.equals("q")) {
                    MessagePrinter.printQuitMessage();
                    System.exit(0);
                }
                newValidationResult = InputValidator.validate(which, input);
                if (newValidationResult.code() == 0) {
                    if (field.getType().isInstance(newValidationResult.convertedValue())) {
                        field.set(this, newValidationResult.convertedValue());
                    } else {
                        field.set(this, newValidationResult.convertedValue());
                    }
                    valid = true;
                } else {
                    MessagePrinter.printMessage(newValidationResult.message(),
                            MessagePrinter.ERROR_FORMAT, true);
                    MessagePrinter.printMessage(MessagePrinter.CMD_PROMPT, MessagePrinter.ERROR_FORMAT, false);
                }
            }
        }
    }

    private void checkAndFixInputs() throws NoSuchFieldException, IllegalAccessException {
        // Check if the ID was set and/or is valid
        checkInput("id");
        // Check if the port was set and/or is valid
        checkInput("port");
        // Check if the server address was set and/or is valid
        checkInput("serverAddress");
    }

    @Override
    public void run() {
        scanner = new Scanner(System.in);
        MessagePrinter.printWelcomeMessage();
        try {
            checkAndFixInputs();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        String selfIpAddress = null;
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            selfIpAddress = inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            MessagePrinter.printMessage("Could not retrieve local IP address. " +
                            "Please check your network connection and try again.",
                    MessagePrinter.ERROR_FORMAT, true);
        }
        JsonNode serverResponse = RestServiceManager.getInstance(this.serverAddress)
                .registerToServer(this.id, selfIpAddress, this.port);
        if (serverResponse == null) {
            System.exit(0);
        }
        MapPosition mapPosition = new MapPosition(
                serverResponse.get("mapPosition").get("district").asInt(),
                serverResponse.get("mapPosition").get("x").asInt(),
                serverResponse.get("mapPosition").get("y").asInt());
        JsonNode activeRobots = serverResponse.get("activeRobots");
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            List<Peer> activePeers = objectMapper.readerFor(new TypeReference<List<Peer>>() {
            }).readValue(activeRobots);
            cleaningRobot = new CleaningRobot(
                    this.id, this.port,
                    mapPosition,
                    activePeers,
                    selfIpAddress);
            cleaningRobot.start();
            MessagePrinter.printAvailableCommands();
            MessagePrinter.printMessage(MessagePrinter.CMD_PROMPT, MessagePrinter.ACCENT_FORMAT_2, false);
            while (true) {
                String input = scanner.nextLine();
                if (input.equals("help") || (!input.equals("quit") & !input.equals("fix"))) {
                    MessagePrinter.printAvailableCommands();
                    MessagePrinter.printMessage(MessagePrinter.CMD_PROMPT,
                            MessagePrinter.ACCENT_FORMAT_2, false);
                    continue;
                }
                if (input.equals("quit")) {
                    if (cleaningRobot.isAlive()) {
                        cleaningRobot.stopMeGently();
                        cleaningRobot.join();
                    }
                    System.exit(0);
                }
                cleaningRobot.requestRepair();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(
                (t, e) -> {
                    MessagePrinter.printMessage(">>> Something went wrong!" +
                                    MessagePrinter.STRING_SEP +
                                    e.getMessage() +
                                    MessagePrinter.STRING_SEP +
                                    e.getCause().getMessage(),
                            MessagePrinter.ERROR_FORMAT, true);
                    MessagePrinter.printQuitMessage();
                    if (cleaningRobot.isAlive()) {
                        cleaningRobot.stopMeGently();
                        try {
                            cleaningRobot.join();
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                        }
                    }
                    System.exit(0);
                }
        );
        RobotLauncher robotLauncher = new RobotLauncher();
        int exitCode = new CommandLine(robotLauncher).execute(args);
        System.exit(exitCode);
    }


}
