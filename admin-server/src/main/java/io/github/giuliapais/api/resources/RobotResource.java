package io.github.giuliapais.api.resources;

import io.github.giuliapais.api.models.MapPosition;
import io.github.giuliapais.api.models.Robot;
import io.github.giuliapais.api.models.RobotCreateResponse;
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
    public RobotCreateResponse register(Robot robot) throws IdPresentException {
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
        boolean updated = robotService.updateRobot(id, robot);
        if (!updated) {
            return Response.status(Response.Status.NOT_MODIFIED).build();
        } else {
            return Response
                    .ok()
                    .build();
        }
    }

    @PUT
    @Path("{id}/position")
    public Response updatePosition(@PathParam("id") int id, MapPosition newPos) {
        int status = robotService.updatePosition(id, newPos);
        if (status == 1) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else if (status == 2) {
            return Response.status(Response.Status.NOT_MODIFIED).build();
        } else {
            return Response
                    .ok()
                    .entity(newPos)
                    .build();
        }
    }
}
