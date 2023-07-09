package io.github.giuliapais.api.resources;

import io.github.giuliapais.api.models.Robot;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;

class RobotResourceIntegrationTest extends JerseyTest {
    @Override
    protected Application configure() {
        ResourceConfig config = new ResourceConfig()
                .packages("io.github.giuliapais.api",
                        "io.github.giuliapais.exceptions")
                .register(JacksonFeature.class);
        return (config);
    }

    @Test
    public void givenValidRobot_whenRegister_thenStatus200() {
        Robot validRobot = new Robot(142874, "localhost", (short) 9999);
        Response response = target("/robots")
                .request()
                .post(Entity.entity(validRobot, MediaType.APPLICATION_JSON));
        assertEquals(200, response.getStatus());
        String entity = response.readEntity(String.class);
        assertThat(entity, containsString("\"activeRobots\":"));
        assertThat(entity, containsString("\"identity\""));
        assertThat(entity, containsString("\"id\":142874"));
        assertThat(entity, containsString("\"ipAddress\":\"localhost\""));
        assertThat(entity, containsString("\"port\":9999"));
        assertThat(entity, containsString("\"mapPosition\""));
        assertThat(entity, containsString("\"district\""));
        assertThat(entity, containsString("\"x\""));
        assertThat(entity, containsString("\"y\""));
    }

    @Test
    public void givenInvalidRobot_whenRegister_thenStatus400() {
        Robot validRobot = new Robot(1, "localhost", (short) 9991);
        Response response = target("/robots")
                .request()
                .post(Entity.entity(validRobot, MediaType.APPLICATION_JSON));
        assertEquals(200, response.getStatus());
        Robot invalidRobot = new Robot(1, "localhost", (short) 9992);
        response = target("/robots")
                .request()
                .post(Entity.entity(invalidRobot, MediaType.APPLICATION_JSON));
        assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
        String entity = response.readEntity(String.class);
        assertThat(entity, containsString("\"errorMessage\":\"Robot id already present\""));
    }
}