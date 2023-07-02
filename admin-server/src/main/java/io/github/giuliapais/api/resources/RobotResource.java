package io.github.giuliapais.api.resources;

import io.github.giuliapais.api.models.Robot;
import io.github.giuliapais.api.services.RobotService;
import io.github.giuliapais.exceptions.IdPresentException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

@Path("robots")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RobotResource {
    RobotService robotService = RobotService.getInstance();

    @GET
    public List<Robot> getAll() {
        return robotService.getAllRobots();
    }

    @POST
    public Robot register(Robot robot) throws IdPresentException {
        return robotService.addRobot(robot);
    }

    @DELETE
    @Path("{id}")
    public Response delete(@PathParam("id") int id) {
        boolean removed = robotService.removeRobot(id);
        Response response;
        if (removed) {
            response = Response.ok().build();
        } else {
            response = Response.status(Response.Status.NOT_FOUND).build();
        }
        return response;
    }

    @PUT
    @Path("{id}")
    public Response update(@PathParam("id") int id, Robot robot) {
        Robot updated = robotService.updateRobot(id, robot);
        if (updated == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response
                    .ok(updated)
                    .build();
        }
    }
}
