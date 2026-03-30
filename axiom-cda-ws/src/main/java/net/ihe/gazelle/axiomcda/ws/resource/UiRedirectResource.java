package net.ihe.gazelle.axiomcda.ws.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.net.URI;

@Path("/axiom-cda")
public class UiRedirectResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response redirectToUiRoot() {
        return Response.seeOther(URI.create("/axiom-cda/")).build();
    }
}
