package io.github.giuliapais.utils;

import com.diogonunes.jcolor.AnsiFormat;
import com.diogonunes.jcolor.Attribute;

import java.net.URI;

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

    private String getServerWelcomeMessage() {
        AnsiFormat format = new AnsiFormat(Attribute.BRIGHT_CYAN_TEXT(), Attribute.BOLD());
        return colorize("================================================================================" +
                        STRING_SEP +
                        "            || GUARD - Greenfield Urban Air Reconnaissance Drones ||            " +
                        STRING_SEP +
                        "                         Administrator server interface                         " +
                        STRING_SEP +
                        "--------------------------------------------------------------------------------",
                format);
    }

    private String getHTTPServerInitMessage(URI serverURI) {
        AnsiFormat format = new AnsiFormat(Attribute.TEXT_COLOR(209));
        return colorize("HTTP server started. API requests can be sent to " +
                        serverURI.toString() + "." +
                        STRING_SEP +
                        "To stop the server, use the command 'quit'.",
                format);
    }

    private String getStopServerMessage() {
        AnsiFormat format = new AnsiFormat(Attribute.YELLOW_TEXT());
        return colorize("Stopping server...",
                format);
    }

    public void printServerWelcomeMessage() {
        String serverMessage = getServerWelcomeMessage();
        System.out.println(serverMessage);
    }

    public void printHTTPServerInitMessage(URI serverURI) {
        String serverMessage = getHTTPServerInitMessage(serverURI);
        System.out.println(serverMessage);
    }

    public void printStopServerMessage() {
        String serverMessage = getStopServerMessage();
        System.out.println(serverMessage);
    }

    public static void printMessage(String message, AnsiFormat format, boolean newLine) {
        String formMessage = colorize(message, format);
        if (newLine) {
            System.out.println(formMessage);
        } else {
            System.out.print(formMessage);
        }
    }
}
