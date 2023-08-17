package io.github.giuliapais.adminserver;

import io.github.giuliapais.utils.MessagePrinter;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

public class AdminServerMain {
    private static final MessagePrinter messagePrinter = new MessagePrinter();
    private static PollutionMonitor pollutionMonitor;

    private static HttpServer launchHttpServer() throws IOException {
        ResourceConfig config = new ResourceConfig()
                .packages("io.github.giuliapais.api",
                        "io.github.giuliapais.exceptions")
                .register(JacksonFeature.class);
        URI uri = URI.create("http://localhost:9090/api");
        HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(uri, config);
        httpServer.start();
        messagePrinter.printHTTPServerInitMessage(uri);
        return (httpServer);
    }

    public static void main(String[] args) {
        messagePrinter.printServerWelcomeMessage();
        BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(System.in));

        /* HTTP server initialization phase */
        HttpServer httpServer = null;
        try {
            httpServer = launchHttpServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        /* MQTT client initialization phase */
        try {
            pollutionMonitor = new PollutionMonitor();
        } catch (MqttException e) {
            MessagePrinter.printMessage(
                    "Could not connect to MQTT broker, shutting down...",
                    MessagePrinter.ERROR_FORMAT,
                    true
            );
            httpServer.shutdownNow();
            System.exit(1);
        }

        while (true) {
            try {
                System.out.print(">:");
                String line = reader.readLine();
                if (line != null && line.equals("quit")) {
                    messagePrinter.printStopServerMessage();
                    httpServer.shutdownNow();
                    pollutionMonitor.disconnect();
                    break;
                }
            } catch (IOException | MqttException e) {
                throw new RuntimeException(e);
            }
        }
        System.exit(0);
    }
}
