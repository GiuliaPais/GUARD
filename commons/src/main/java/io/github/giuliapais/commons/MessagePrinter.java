package io.github.giuliapais.commons;

import com.diogonunes.jcolor.Ansi;
import com.diogonunes.jcolor.AnsiFormat;
import com.diogonunes.jcolor.Attribute;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URI;

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

    private static String getRobotWelcomeMessage() {
        AnsiFormat format = new AnsiFormat(Attribute.BRIGHT_CYAN_TEXT(), Attribute.BOLD());
        return Ansi.colorize("================================================================================" +
                        STRING_SEP +
                        "            || GUARD - Greenfield Urban Air Reconnaissance Drones ||            " +
                        STRING_SEP +
                        "                             Cleaning robot process                             " +
                        STRING_SEP +
                        "--------------------------------------------------------------------------------",
                format);
    }

    private static String getServerWelcomeMessage() {
        AnsiFormat format = new AnsiFormat(Attribute.BRIGHT_CYAN_TEXT(), Attribute.BOLD());
        return Ansi.colorize("================================================================================" +
                        STRING_SEP +
                        "            || GUARD - Greenfield Urban Air Reconnaissance Drones ||            " +
                        STRING_SEP +
                        "                         Administrator server interface                         " +
                        STRING_SEP +
                        "--------------------------------------------------------------------------------",
                format);
    }

    private static String getClientWelcomeMessage() {
        AnsiFormat format = new AnsiFormat(Attribute.BRIGHT_CYAN_TEXT(), Attribute.BOLD());
        return Ansi.colorize("================================================================================" +
                        STRING_SEP +
                        "            || GUARD - Greenfield Urban Air Reconnaissance Drones ||            " +
                        STRING_SEP +
                        "                         Administrator client interface                         " +
                        STRING_SEP +
                        "--------------------------------------------------------------------------------",
                format);
    }

    private static String getHTTPServerInitMessage(URI serverURI) {
        AnsiFormat format = new AnsiFormat(Attribute.TEXT_COLOR(209));
        return Ansi.colorize("HTTP server started. API requests can be sent to " +
                        serverURI.toString() + "." +
                        STRING_SEP +
                        "To stop the server, use the command 'quit'.",
                format);
    }

    private static String getStopServerMessage() {
        AnsiFormat format = new AnsiFormat(Attribute.YELLOW_TEXT());
        return Ansi.colorize("Stopping server...",
                format);
    }


    private static String getIdErrorMessage() {
        return Ansi.colorize("It seems either you didn't provide an ID or the ID you provided is not valid." +
                        STRING_SEP +
                        "Please insert a valid ID (>= 1) and try again. Insert \"q\" to quit.",
                ERROR_FORMAT);
    }

    private static String getPortErrorMessage() {
        return Ansi.colorize("It seems either you didn't provide a port or the port you provided is not valid." +
                        STRING_SEP +
                        "Please insert a valid port [1024 - 65535] and try again. Insert \"q\" to quit.",
                ERROR_FORMAT);
    }

    private static String getServerAddressErrorMessage() {
        return Ansi.colorize(
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
        return Ansi.colorize(
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
        return Ansi.colorize(
                "Registration failed!" +
                        STRING_SEP +
                        "Error message: " + response.get("errorMessage") +
                        STRING_SEP +
                        "Status code: " + response.get("statusCode"),
                ERROR_FORMAT);
    }

    private static String getQuitMessage() {
        AnsiFormat format = new AnsiFormat(Attribute.CYAN_TEXT());
        return Ansi.colorize("Quitting... Goodbye!", format);
    }

    private static String getAvailableCommands() {
        return Ansi.colorize(
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

    private static String getClientAvailableCommands() {
        return Ansi.colorize(
                "Available commands: " +
                        STRING_SEP +
                        "--> \"quit\" to exit" +
                        STRING_SEP +
                        "--> \"help\" to show this message" +
                        STRING_SEP +
                        "--> \"stats\" to print the menu for computing statistics",
                ACCENT_FORMAT_2
        );
    }

    private static String getStatRobotCmd() {
        return Ansi.colorize("   1. " + "\"robots\"", Attribute.BOLD(), Attribute.MAGENTA_TEXT()) +
                Ansi.colorize(
                        "       Prints the list of robots currently active in Greenfield and " +
                                "their position on the map.",
                        ACCENT_FORMAT);

    }

    private static String getStatRobotLastNCmd() {
        return Ansi.colorize("   2. " + "\"last-n\"", Attribute.BOLD(), Attribute.MAGENTA_TEXT()) +
                Ansi.colorize(
                        "       Given a robot id, it returns and prints the average value of the last N " +
                                "pollution measurements received from that robot.",
                        ACCENT_FORMAT);

    }

    private static String getStatBetweenCmd() {
        return Ansi.colorize("   3. " + "\"between\"", Attribute.BOLD(), Attribute.MAGENTA_TEXT()) +
                Ansi.colorize(
                        "      Given two timestamps t1 and t2, it returns the average pollution value " +
                                "for all robots and all districts in the time interval [t1, t2].",
                        ACCENT_FORMAT);

    }

    private static String getStatBackCmd() {
        return Ansi.colorize("   0. " + "\"back\"", Attribute.BOLD(), Attribute.MAGENTA_TEXT()) +
                Ansi.colorize(
                        "         Go back to previous menu.",
                        ACCENT_FORMAT);

    }

    public static void printWelcomeMessage() {
        String welcomeMessage = getRobotWelcomeMessage();
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
        String formMessage = Ansi.colorize(message, format);
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

    public static void printAvailableCommands() {
        String availableCommands = getAvailableCommands();
        System.out.println(availableCommands);
    }

    public static void printServerWelcomeMessage() {
        String serverMessage = getServerWelcomeMessage();
        System.out.println(serverMessage);
    }

    public static void printHTTPServerInitMessage(URI serverURI) {
        String serverMessage = getHTTPServerInitMessage(serverURI);
        System.out.println(serverMessage);
    }

    public static void printStopServerMessage() {
        String serverMessage = getStopServerMessage();
        System.out.println(serverMessage);
    }

    public static void printClientWelcomeMessage() {
        String clientMessage = getClientWelcomeMessage();
        System.out.println(clientMessage);
    }

    public static void printClientAvailableCommands() {
        String availableCommands = getClientAvailableCommands();
        System.out.println(availableCommands);
        System.out.print(Ansi.colorize(CMD_PROMPT, ACCENT_FORMAT_2));
    }

    public static void printStatsCommands() {
        System.out.println(
                Ansi.colorize(
                        "Available statistics - type the number or the name of the command to execute it: ",
                        ACCENT_FORMAT
                )
        );
        System.out.println(getStatRobotCmd());
        System.out.println(getStatRobotLastNCmd());
        System.out.println(getStatBetweenCmd());
        System.out.println(getStatBackCmd());
        System.out.print(Ansi.colorize(CMD_PROMPT, ACCENT_FORMAT));
    }
}
