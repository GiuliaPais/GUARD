package io.github.giuliapais.utils;

import com.diogonunes.jcolor.AnsiFormat;
import com.diogonunes.jcolor.Attribute;
import jakarta.json.JsonObject;

import static com.diogonunes.jcolor.Ansi.colorize;

public class MessagePrinter {
    public static final String STRING_SEP = System.getProperty("line.separator");
    public static final String CMD_PROMPT = "> ";
    public static final AnsiFormat ERROR_FORMAT = new AnsiFormat(Attribute.RED_TEXT());
    public static final AnsiFormat SUCCESS_FORMAT = new AnsiFormat(Attribute.GREEN_TEXT());
    public static final AnsiFormat INFO_FORMAT = new AnsiFormat(Attribute.CYAN_TEXT());

    private String getWelcomeMessage() {
        AnsiFormat format = new AnsiFormat(Attribute.BRIGHT_CYAN_TEXT(), Attribute.BOLD());
        return colorize("================================================================================" +
                        STRING_SEP +
                        "            || GUARD - Greenfield Urban Air Reconnaissance Drones ||            " +
                        STRING_SEP +
                        "                             Cleaning robot process                             " +
                        STRING_SEP +
                        "--------------------------------------------------------------------------------",
                format);
    }

    private String getIdErrorMessage() {
        return colorize("It seems either you didn't provide an ID or the ID you provided is not valid." +
                        STRING_SEP +
                        "Please insert a valid ID (>= 1) and try again. Insert \"q\" to quit.",
                ERROR_FORMAT);
    }

    private String getPortErrorMessage() {
        return colorize("It seems either you didn't provide a port or the port you provided is not valid." +
                        STRING_SEP +
                        "Please insert a valid port [1024 - 65535] and try again. Insert \"q\" to quit.",
                ERROR_FORMAT);
    }

    private String getServerAddressErrorMessage() {
        return colorize(
                "It seems either you didn't provide a server address" +
                        STRING_SEP +
                        "or the address you provided is not valid." +
                        STRING_SEP +
                        "Please insert a valid server address (e.g. localhost:9090) and try again." +
                        STRING_SEP +
                        "Insert \"q\" to quit.",
                ERROR_FORMAT);
    }

    private String getSummaryInitMessage(int id, int port, String serverAddress) {
        return colorize(
                "Robot initialized with the following parameters:" +
                        STRING_SEP +
                        "ID: " + id +
                        STRING_SEP +
                        "Port: " + port +
                        STRING_SEP +
                        "Server address: " + serverAddress +
                        STRING_SEP +
                        "--------------------------------------------------------------------------------",
                INFO_FORMAT);
    }

    private String getRegisterSuccessMessage(JsonObject response) {
        JsonObject mapPosition = response.getJsonObject("mapPosition");
        return colorize(
                "Registration successful!" +
                        STRING_SEP +
                        "Starting district: " + mapPosition.getInt("district") +
                        STRING_SEP +
                        "Starting coordinates: [" + mapPosition.getInt("x") + ", " +
                        mapPosition.getInt("y") + "]" +
                        STRING_SEP +
                        "List of registered robots: " + response.getJsonArray("activeRobots"),
                SUCCESS_FORMAT);
    }

    private String getRegisterFailureMessage(JsonObject response) {
        return colorize(
                "Registration failed!" +
                        STRING_SEP +
                        "Error message: " + response.getString("errorMessage") +
                        STRING_SEP +
                        "Status code: " + response.getInt("statusCode"),
                ERROR_FORMAT);
    }

    private String getQuitMessage() {
        AnsiFormat format = new AnsiFormat(Attribute.CYAN_TEXT());
        return colorize("Quitting... Goodbye!", format);
    }

    private String getSensorInitMessage() {
        return colorize(
                "Initializing and starting sensors...",
                INFO_FORMAT);
    }

    public void printWelcomeMessage() {
        String welcomeMessage = getWelcomeMessage();
        System.out.println(welcomeMessage);
    }

    public void printIdErrorMessage() {
        String idErrorMessage = getIdErrorMessage();
        System.out.println(idErrorMessage);
    }

    public void printQuitMessage() {
        String quitMessage = getQuitMessage();
        System.out.println(quitMessage);
    }

    public void printMessage(String message, AnsiFormat format, boolean newLine) {
        String formMessage = colorize(message, format);
        if (newLine) {
            System.out.println(formMessage);
        } else {
            System.out.print(formMessage);
        }
    }

    public void printPortErrorMessage() {
        String portErrorMessage = getPortErrorMessage();
        System.out.println(portErrorMessage);
    }

    public void printServerAddressErrorMessage() {
        String serverAddressErrorMessage = getServerAddressErrorMessage();
        System.out.println(serverAddressErrorMessage);
    }

    public void printSummaryInitMessage(int id, int port, String serverAddress) {
        String summaryInitMessage = getSummaryInitMessage(id, port, serverAddress);
        System.out.println(summaryInitMessage);
    }

    public void printRegisterSuccessMessage(JsonObject response) {
        String registerSuccessMessage = getRegisterSuccessMessage(response);
        System.out.println(registerSuccessMessage);
    }

    public void printRegisterFailureMessage(JsonObject response) {
        String registerFailureMessage = getRegisterFailureMessage(response);
        System.out.println(registerFailureMessage);
    }

    public void printParamErrorMessage(String which) {
        switch (which) {
            case "id" -> printIdErrorMessage();
            case "port" -> printPortErrorMessage();
            case "serverAddress" -> printServerAddressErrorMessage();
        }
    }

    public void printSensorInitMessage() {
        String sensorInitMessage = getSensorInitMessage();
        System.out.println(sensorInitMessage);
    }
}
