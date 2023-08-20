package io.github.giuliapais.api.resources;

import io.github.giuliapais.api.models.Robot;
import io.github.giuliapais.api.models.RobotCreateResponse;
import io.github.giuliapais.api.services.RobotService;
import io.github.giuliapais.commons.MessagePrinter;
import io.github.giuliapais.commons.models.RobotInfo;
import io.github.giuliapais.commons.models.RobotPosUpdate;
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
    public List<RobotInfo> getAll() {
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
            MessagePrinter.printMessage("Delete request: robot " + id + " removed from the grid",
                    MessagePrinter.WARNING_FORMAT, true);
            robotService.printGridStatus();
            response = Response.ok().build();
        } else {
            response = Response.status(Response.Status.NOT_FOUND).build();
        }
        return response;
    }

    @PUT
    public Response updateRobots(List<RobotPosUpdate> changes) {
        robotService.updateRobotPositions(changes);
        MessagePrinter.printMessage(
                "Update request: robot positions updated",
                MessagePrinter.WARNING_FORMAT,
                true
        );
        robotService.printGridStatus();
        return Response.ok().build();
    }

}
