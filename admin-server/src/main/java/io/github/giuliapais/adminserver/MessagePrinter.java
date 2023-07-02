package io.github.giuliapais.adminserver;

import com.diogonunes.jcolor.AnsiFormat;
import com.diogonunes.jcolor.Attribute;

import java.net.URI;

import static com.diogonunes.jcolor.Ansi.colorize;

class MessagePrinter {
    private final String STRING_SEP = System.getProperty("line.separator");

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

//    private String getCommandHelpMessage() {
//
//    }

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
}
