package io.github.giuliapais.exceptions;

import io.github.giuliapais.api.models.IdPresentErrorMessage;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class IdPresentExceptionMapper implements ExceptionMapper<IdPresentException> {
    @Override
    public Response toResponse(IdPresentException exception) {
        IdPresentErrorMessage errorMessage = new IdPresentErrorMessage(
                "Robot id already present",
                Response.Status.CONFLICT.getStatusCode());
        return Response.status(Response.Status.CONFLICT)
                .entity(errorMessage)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
