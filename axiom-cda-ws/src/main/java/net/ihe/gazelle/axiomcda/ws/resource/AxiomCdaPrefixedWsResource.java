package net.ihe.gazelle.axiomcda.ws.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import net.ihe.gazelle.axiomcda.ws.dto.GenerationRequest;
import net.ihe.gazelle.axiomcda.ws.dto.GenerationResult;
import net.ihe.gazelle.axiomcda.ws.service.FshGenerationService;

@Path("/axiom-cda/api/generate")
public class AxiomCdaPrefixedWsResource {

    @Inject
    FshGenerationService generationService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public GenerationResult generate(GenerationRequest request) throws Exception {
        return generationService.generateFsh(request);
    }
}
