package io.github.giuliapais.adminclient;

import io.github.giuliapais.commons.MessagePrinter;
import io.github.giuliapais.commons.models.RobotInfo;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.jackson.JacksonFeature;

import java.util.List;

public class RestServiceManager {
    private String serverAddress = "localhost:9090";
    private String APIBase = "/api";
    private Client client;

    public RestServiceManager() {
        client = ClientBuilder
                .newBuilder()
                .register(JacksonFeature.class)
                .build();
    }

    public List<RobotInfo> getRobots() {
        String targetUri = "http://" + this.serverAddress + this.APIBase + "/robots";
        try {
            return client
                    .target(targetUri)
                    .request(MediaType.APPLICATION_JSON)
                    .get()
                    .readEntity(new GenericType<>() {
                    });
        } catch (ProcessingException e) {
            MessagePrinter.printMessage(
                    "Error while sending the request. The server might be offline, please try again later.",
                    MessagePrinter.ERROR_FORMAT,
                    true
            );
            return null;
        }
    }

    public Response getLastNMeasurements(int robotId, int n) {
        String targetUri = "http://" + this.serverAddress + this.APIBase + "/pollution/" + robotId;
        try {
            return client
                    .target(targetUri)
                    .queryParam("n", n)
                    .request(MediaType.APPLICATION_JSON)
                    .get();
        } catch (ProcessingException e) {
            MessagePrinter.printMessage(
                    "Error while sending the request. The server might be offline, please try again later.",
                    MessagePrinter.ERROR_FORMAT,
                    true
            );
            return null;
        }
    }

    public Response getRecordsBetween(long t1, long t2) {
        String targetUri = "http://" + this.serverAddress + this.APIBase + "/pollution";
        try {
            return client
                    .target(targetUri)
                    .queryParam("t1", t1)
                    .queryParam("t2", t2)
                    .request(MediaType.APPLICATION_JSON)
                    .get();
        } catch (ProcessingException e) {
            MessagePrinter.printMessage(
                    "Error while sending the request. The server might be offline, please try again later.",
                    MessagePrinter.ERROR_FORMAT,
                    true
            );
            return null;
        }
    }
}
