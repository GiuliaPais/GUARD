package io.github.giuliapais.robotsnetwork;

import io.github.giuliapais.robotsnetwork.core.CleaningRobot;
import io.github.giuliapais.utils.MessagePrinter;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.jackson.JacksonFeature;
import picocli.CommandLine;
import io.github.giuliapais.utils.InputValidator;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
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

    private final MessagePrinter messagePrinter = new MessagePrinter();
    private Client client;
    private final CleaningRobot cleaningRobot = new CleaningRobot();

    private static Scanner scanner;

    private void checkInput(String which) throws NoSuchFieldException, IllegalAccessException {
        Field field = RobotLauncher.class.getDeclaredField(which);
        field.setAccessible(true);
        InputValidator.ValidationResult validationResult = InputValidator.validate(which, field.get(this));
        if (validationResult.code() != 0) {
            messagePrinter.printParamErrorMessage(which);
            messagePrinter.printMessage(MessagePrinter.CMD_PROMPT, MessagePrinter.ERROR_FORMAT, false);
            boolean valid = false;
            InputValidator.ValidationResult newValidationResult;
            while (!valid) {
                String input = scanner.nextLine();
                if (input.equals("q")) {
                    messagePrinter.printQuitMessage();
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
                    messagePrinter.printMessage(newValidationResult.message(),
                            MessagePrinter.ERROR_FORMAT, true);
                    messagePrinter.printMessage(MessagePrinter.CMD_PROMPT, MessagePrinter.ERROR_FORMAT, false);
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

    private JsonObject registerToServer() {
        messagePrinter.printMessage("Sending registration request to the server...",
                MessagePrinter.INFO_FORMAT, true);
        client = ClientBuilder
                .newBuilder()
                .register(JacksonFeature.class)
                .build();
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            this.selfIpAddress = inetAddress.getHostAddress();
        } catch (UnknownHostException e) {
            messagePrinter.printMessage("Could not retrieve local IP address. " +
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
            JsonReader jsonReader = Json.createReader(new StringReader(entity));
            JsonObject jsonObject = jsonReader.readObject();
            jsonReader.close();
            if (serverResponse.getStatus() == 200) {
                messagePrinter.printRegisterSuccessMessage(jsonObject);
                return jsonObject;
            } else {
                messagePrinter.printRegisterFailureMessage(jsonObject);
                return null;
            }
        } catch (ProcessingException e) {
            if (e.getCause() instanceof ConnectException) {
                messagePrinter.printMessage("Could not establish a connection to the server :(",
                        MessagePrinter.ERROR_FORMAT,
                        true);
            } else {
                messagePrinter.printMessage("Something went wrong in processing the request :(",
                        MessagePrinter.ERROR_FORMAT, true);
            }
            return null;
        }
    }

    @Override
    public void run() {
        scanner = new Scanner(System.in);
        messagePrinter.printWelcomeMessage();
        try {
            checkAndFixInputs();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        JsonObject serverResponse = registerToServer();
        if (serverResponse == null) {
            System.exit(0);
        }
        int assignedDistrict = serverResponse.getJsonObject("mapPosition").getInt("district");
        int assignedX = serverResponse.getJsonObject("mapPosition").getInt("x");
        int assignedY = serverResponse.getJsonObject("mapPosition").getInt("y");
        cleaningRobot.setRobotId(this.id);
        cleaningRobot.setPort(this.port);
        cleaningRobot.setServerAddress(this.serverAddress);
        cleaningRobot.setDistrict(assignedDistrict);
        cleaningRobot.setX(assignedX);
        cleaningRobot.setY(assignedY);
        cleaningRobot.setMessagePrinter(this.messagePrinter);
        cleaningRobot.start();
        while (true) {
            String input = scanner.nextLine();
            if (input.equals("q")) {
                messagePrinter.printQuitMessage();
                System.exit(0);
            }
        }
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new RobotLauncher()).execute(args));
    }


}
