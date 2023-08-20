package io.github.giuliapais.robotsnetwork.comm.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.giuliapais.commons.models.MapPosition;
import io.github.giuliapais.commons.models.RobotPosUpdate;
import io.github.giuliapais.commons.MessagePrinter;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.jackson.JacksonFeature;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestServiceManager {
    private static volatile RestServiceManager instance;
    private final String API_ADDRESS = "/api/robots";
    private final String SERVER_ADDRESS;
    private final String targetUri;
    private Client client;

    private RestServiceManager(String serverAddress) {
        this.SERVER_ADDRESS = serverAddress;
        this.targetUri = "http://" + this.SERVER_ADDRESS + this.API_ADDRESS;
        client = ClientBuilder
                .newBuilder()
                .register(JacksonFeature.class)
                .build();
    }

    public static RestServiceManager getInstance(String serverAddress) {
        RestServiceManager result = instance;
        if (result == null) {
            synchronized (RestServiceManager.class) {
                result = instance;
                if (result == null) {
                    instance = result = new RestServiceManager(serverAddress);
                }
            }
        }
        return result;
    }

    public JsonNode registerToServer(int robotId, String selfAddress, int port) {
        MessagePrinter.printMessage("Sending registration request to the server...",
                MessagePrinter.INFO_FORMAT, true);
        HashMap<String, Object> payLoad = new HashMap<>();
        payLoad.put("id", robotId);
        payLoad.put("ipAddress", selfAddress);
        payLoad.put("port", port);
        try {
            Response serverResponse = client
                    .target(this.targetUri)
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

    public void deleteRobot(int robotId, boolean join) {
        Thread thread = new Thread(() -> {
            client.target(targetUri)
                    .path(Integer.toString(robotId))
                    .request(MediaType.APPLICATION_JSON)
                    .delete();
        });
        thread.start();
        if (join) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void updatePositions(HashMap<Integer, MapPosition> changes) {
        List<RobotPosUpdate> updates = new ArrayList<>();
        for (Map.Entry<Integer, MapPosition> entry : changes.entrySet()) {
            updates.add(new RobotPosUpdate(entry.getKey(), entry.getValue().getDistrict(),
                    entry.getValue().getX(), entry.getValue().getY()));
        }
        Thread thread = new Thread(() -> {
            client.target(targetUri)
                    .request(MediaType.APPLICATION_JSON)
                    .put(Entity.json(updates));
        });
        thread.start();
    }
}
