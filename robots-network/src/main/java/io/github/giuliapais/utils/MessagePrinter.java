package io.github.giuliapais.utils;

import com.diogonunes.jcolor.AnsiFormat;
import com.diogonunes.jcolor.Attribute;
import com.fasterxml.jackson.databind.JsonNode;

import static com.diogonunes.jcolor.Ansi.colorize;

public class MessagePrinter {
    public static final String STRING_SEP = System.getProperty("line.separator");
    public static final String CMD_PROMPT = "> ";
    public static final AnsiFormat ERROR_FORMAT = new AnsiFormat(Attribute.RED_TEXT());
    public static final AnsiFormat SUCCESS_FORMAT = new AnsiFormat(Attribute.GREEN_TEXT());
    public static final AnsiFormat INFO_FORMAT = new AnsiFormat(Attribute.CYAN_TEXT());
    public static final AnsiFormat WARNING_FORMAT = new AnsiFormat(Attribute.YELLOW_TEXT());
    public static final AnsiFormat ACCENT_FORMAT = new AnsiFormat(Attribute.MAGENTA_TEXT());
    public static final AnsiFormat ACCENT_FORMAT_2 = new AnsiFormat(Attribute.TEXT_COLOR(245, 144, 66),
            Attribute.BOLD());

    private static String getWelcomeMessage() {
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

    private static String getIdErrorMessage() {
        return colorize("It seems either you didn't provide an ID or the ID you provided is not valid." +
                        STRING_SEP +
                        "Please insert a valid ID (>= 1) and try again. Insert \"q\" to quit.",
                ERROR_FORMAT);
    }

    private static String getPortErrorMessage() {
        return colorize("It seems either you didn't provide a port or the port you provided is not valid." +
                        STRING_SEP +
                        "Please insert a valid port [1024 - 65535] and try again. Insert \"q\" to quit.",
                ERROR_FORMAT);
    }

    private static String getServerAddressErrorMessage() {
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

    private static String getRegisterSuccessMessage(JsonNode response) {
        JsonNode mapPosition = response.get("mapPosition");
        return colorize(
                "Registration successful!" +
                        STRING_SEP +
                        "Starting district: " + mapPosition.get("district") +
                        STRING_SEP +
                        "Starting coordinates: [" + mapPosition.get("x") + ", " +
                        mapPosition.get("y") + "]" +
                        STRING_SEP +
                        "List of registered robots: " + response.get("activeRobots").toPrettyString(),
                SUCCESS_FORMAT);
    }

    private static String getRegisterFailureMessage(JsonNode response) {
        return colorize(
                "Registration failed!" +
                        STRING_SEP +
                        "Error message: " + response.get("errorMessage") +
                        STRING_SEP +
                        "Status code: " + response.get("statusCode"),
                ERROR_FORMAT);
    }

    private static String getQuitMessage() {
        AnsiFormat format = new AnsiFormat(Attribute.CYAN_TEXT());
        return colorize("Quitting... Goodbye!", format);
    }

    private static String getSensorInitMessage() {
        return colorize(
                "Initializing and starting sensors...",
                INFO_FORMAT);
    }

    private static String getAvailableCommands() {
        return colorize(
                "Available commands: " +
                        STRING_SEP +
                        "--> \"quit\" to exit" +
                        STRING_SEP +
                        "--> \"fix\" to send the robot for repairs" +
                        STRING_SEP +
                        "--> \"help\" to show this message",
                ACCENT_FORMAT_2
        );
    }

    public static void printWelcomeMessage() {
        String welcomeMessage = getWelcomeMessage();
        System.out.println(welcomeMessage);
    }

    public static void printIdErrorMessage() {
        String idErrorMessage = getIdErrorMessage();
        System.out.println(idErrorMessage);
    }

    public static void printQuitMessage() {
        String quitMessage = getQuitMessage();
        System.out.println(quitMessage);
    }

    public static void printMessage(String message, AnsiFormat format, boolean newLine) {
        String formMessage = colorize(message, format);
        if (newLine) {
            System.out.println(formMessage);
        } else {
            System.out.print(formMessage);
        }
    }

    public static void printPortErrorMessage() {
        String portErrorMessage = getPortErrorMessage();
        System.out.println(portErrorMessage);
    }

    public static void printServerAddressErrorMessage() {
        String serverAddressErrorMessage = getServerAddressErrorMessage();
        System.out.println(serverAddressErrorMessage);
    }

    public static void printRegisterSuccessMessage(JsonNode response) {
        String registerSuccessMessage = getRegisterSuccessMessage(response);
        System.out.println(registerSuccessMessage);
    }

    public static void printRegisterFailureMessage(JsonNode response) {
        String registerFailureMessage = getRegisterFailureMessage(response);
        System.out.println(registerFailureMessage);
    }

    public static void printParamErrorMessage(String which) {
        switch (which) {
            case "id" -> printIdErrorMessage();
            case "port" -> printPortErrorMessage();
            case "serverAddress" -> printServerAddressErrorMessage();
        }
    }

    public static void printSensorInitMessage() {
        String sensorInitMessage = getSensorInitMessage();
        System.out.println(sensorInitMessage);
    }

    public static void printAvailableCommands() {
        String availableCommands = getAvailableCommands();
        System.out.println(availableCommands);
    }
}
