package net.ihe.gazelle.axiomcda.ws.resource;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import net.ihe.gazelle.axiomcda.ws.dto.FhirBuiltInMappingPreset;
import net.ihe.gazelle.axiomcda.ws.dto.FhirConversionRequest;
import net.ihe.gazelle.axiomcda.ws.dto.FhirConversionResult;
import net.ihe.gazelle.axiomcda.ws.dto.FhirPackagePreset;
import net.ihe.gazelle.axiomcda.ws.dto.SushiCompileRequest;
import net.ihe.gazelle.axiomcda.ws.dto.SushiCompileResult;
import net.ihe.gazelle.axiomcda.ws.service.FhirBuiltInMappingPresetService;
import net.ihe.gazelle.axiomcda.ws.service.FhirConversionService;
import net.ihe.gazelle.axiomcda.ws.service.FhirPackagePresetService;

import java.util.List;

@Path("/axiom-cda/api/convert/fhir")
public class FhirConversionPrefixedResource {

    @Inject
    FhirConversionService conversionService;

    @Inject
    FhirPackagePresetService packagePresetService;

    @Inject
    FhirBuiltInMappingPresetService builtInMappingPresetService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public FhirConversionResult convert(FhirConversionRequest request) throws Exception {
        return conversionService.convertObservation(request);
    }

    @POST
    @Path("/sushi")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public SushiCompileResult compileWithSushi(SushiCompileRequest request) throws Exception {
        return conversionService.compileWithSushi(request);
    }

    @GET
    @Path("/package-presets")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FhirPackagePreset> getPackagePresets() {
        return packagePresetService.getPresets();
    }

    @GET
    @Path("/mapping-presets")
    @Produces(MediaType.APPLICATION_JSON)
    public List<FhirBuiltInMappingPreset> getMappingPresets() {
        return builtInMappingPresetService.getObservationPresets();
    }
}
