package io.github.giuliapais.api.resources;

import io.github.giuliapais.api.services.PollutionDataService;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("pollution")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class PollutionDataResource {
    PollutionDataService pollutionDataService = PollutionDataService.getInstance();

    @GET
    @Path("{id}")
    public Response getLastNAverage(
            @PathParam("id") int id,
            @QueryParam("n") int n
    ) {
        if (n <= 0) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        double average = pollutionDataService.getAverage(id, n);
        Response response;
        if (average != -1) {
            response = Response.ok(average).build();
        } else {
            response = Response.status(Response.Status.NOT_FOUND).build();
        }
        return response;
    }

    @GET
    public Response getAverageBetween(
            @QueryParam("t1") long t1,
            @QueryParam("t2") long t2
    ) {
        double average;
        if (t1 < t2) {
            average = pollutionDataService.getAverageBetweenTimestamps(t1, t2);
        } else {
            average = pollutionDataService.getAverageBetweenTimestamps(t2, t1);
        }
        return Response.ok(average).build();
    }

}
