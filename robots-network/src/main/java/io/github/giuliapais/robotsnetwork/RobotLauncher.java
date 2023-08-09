package io.github.giuliapais.robotsnetwork;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.giuliapais.robotsnetwork.core.CleaningRobot;
import io.github.giuliapais.robotsnetwork.comm.Peer;
import io.github.giuliapais.utils.InputValidator;
import io.github.giuliapais.utils.MessagePrinter;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.jackson.JacksonFeature;
import picocli.CommandLine;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;


@CommandLine.Command(name = "robot-launcher", description = "Initializes a new robot process",
        mixinStandardHelpOptions = true)
public class RobotLauncher implements Runnable {
    private final String API_ADDRESS = "/api/robots";

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

    private String selfIpAddress;

    private static Client client;
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

    private JsonNode registerToServer() {
        MessagePrinter.printMessage("Sending registration request to the server...",
                MessagePrinter.INFO_FORMAT, true);
        client = ClientBuilder
                .newBuilder()
                .register(JacksonFeature.class)
                .build();
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            this.selfIpAddress = inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            MessagePrinter.printMessage("Could not retrieve local IP address. " +
                            "Please check your network connection and try again.",
                    MessagePrinter.ERROR_FORMAT, true);
        }
        HashMap<String, Object> payLoad = new HashMap<>();
        payLoad.put("id", this.id);
        payLoad.put("ipAddress", this.selfIpAddress);
        payLoad.put("port", this.port);
        try {
            Response serverResponse = client
                    .target("http://" + this.serverAddress + this.API_ADDRESS)
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.json(payLoad));
            String entity = serverResponse.readEntity(String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonObject = objectMapper.readTree(entity);
            if (serverResponse.getStatus() == 200) {
                MessagePrinter.printRegisterSuccessMessage(jsonObject);
                return jsonObject;
            } else {
                MessagePrinter.printRegisterFailureMessage(jsonObject);
                return null;
            }
        } catch (ProcessingException e) {
            if (e.getCause() instanceof ConnectException) {
                MessagePrinter.printMessage("Could not establish a connection to the server :(",
                        MessagePrinter.ERROR_FORMAT,
                        true);
            } else {
                MessagePrinter.printMessage("Something went wrong in processing the request :(",
                        MessagePrinter.ERROR_FORMAT, true);
            }
            return null;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
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
        JsonNode serverResponse = registerToServer();
        if (serverResponse == null) {
            System.exit(0);
        }
        int assignedDistrict = serverResponse.get("mapPosition").get("district").asInt();
        int assignedX = serverResponse.get("mapPosition").get("x").asInt();
        int assignedY = serverResponse.get("mapPosition").get("y").asInt();
        JsonNode activeRobots = serverResponse.get("activeRobots");
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            List<Peer> activePeers = objectMapper.readerFor(new TypeReference<List<Peer>>() {
            }).readValue(activeRobots);
            cleaningRobot = new CleaningRobot(
                    this.id, this.port,
                    this.serverAddress,
                    assignedDistrict, assignedX, assignedY,
                    activePeers,
                    this.selfIpAddress,
                    client,
                    "http://" + this.serverAddress + this.API_ADDRESS);
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
                    MessagePrinter.printQuitMessage();
                    if (cleaningRobot.isAlive()) {
                        cleaningRobot.stopMeGently();
                    }
                    System.exit(0);
                }
                cleaningRobot.requestRepair();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
//        Thread.setDefaultUncaughtExceptionHandler(
//                (t, e) -> {
//                    MessagePrinter.printMessage(">>> Something went wrong!" +
//                                    MessagePrinter.STRING_SEP +
//                                    e.getMessage() +
//                                    MessagePrinter.STRING_SEP +
//                                    e.getCause().getMessage(),
//                            MessagePrinter.ERROR_FORMAT, true);
//                    MessagePrinter.printQuitMessage();
//                    if (cleaningRobot.isAlive()) {
//                        cleaningRobot.stopMeGently();
//                    }
//                    if (client != null) {
//                        client.close();
//                    }
//                    System.exit(0);
//                }
//        );
        RobotLauncher robotLauncher = new RobotLauncher();
        int exitCode = new CommandLine(robotLauncher).execute(args);
        System.exit(exitCode);
    }


}
