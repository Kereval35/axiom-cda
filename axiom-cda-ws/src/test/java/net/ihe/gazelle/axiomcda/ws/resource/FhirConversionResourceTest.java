package net.ihe.gazelle.axiomcda.ws.resource;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class FhirConversionResourceTest {

    @Test
    void generatesAndConvertsObservationProfileFromCrBio() throws Exception {
        String bbr = readFixture("observation/bio.xml");
        Map<String, Object> generationRequest = new HashMap<>();
        generationRequest.put("bbr", bbr);
        generationRequest.put("sushiRepo", false);
        generationRequest.put("emitIr", false);
        generationRequest.put("emitLogs", true);
        generationRequest.put("yamlConfig", null);

        JsonPath generation = given()
                .contentType(ContentType.JSON)
                .body(generationRequest)
                .when()
                .post("/api/generate")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();

        List<Map<String, Object>> profiles = generation.getList("profiles");
        List<Map<String, Object>> irTemplates = generation.getList("irTemplates");
        assertNotNull(profiles);
        assertNotNull(irTemplates);
        assertFalse(irTemplates.isEmpty());
        assertTrue(profiles.stream().filter(profile -> Boolean.TRUE.equals(profile.get("fhirTransformEligible"))).count() > 1);

        Map<String, Object> eligibleProfile = profiles.stream()
                .filter(profile -> Boolean.TRUE.equals(profile.get("fhirTransformEligible")))
                .filter(profile -> {
                    String templateId = String.valueOf(profile.get("templateId"));
                    return irTemplates.stream()
                            .filter(item -> templateId.equals(String.valueOf(item.get("id"))))
                            .map(item -> String.valueOf(item.get("displayName")))
                            .anyMatch(name -> name.contains("biologie") || name.contains("Resultat"));
                })
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected a laboratory Observation profile to be FHIR-eligible"));

        String templateId = String.valueOf(eligibleProfile.get("templateId"));
        Map<String, Object> template = irTemplates.stream()
                .filter(item -> templateId.equals(item.get("id")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected a matching IR template for eligible profile"));

        String structureMap = readFixture("observation/clean-observation-structuremap.json");
        Map<String, Object> conversionRequest = new HashMap<>();
        conversionRequest.put("sourceProfileName", eligibleProfile.get("name"));
        conversionRequest.put("template", template);
        conversionRequest.put("structureMap", structureMap);

        JsonPath conversion = given()
                .contentType(ContentType.JSON)
                .body(conversionRequest)
                .when()
                .post("/api/convert/fhir")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();

        List<Map<String, Object>> convertedProfiles = conversion.getList("profiles");
        assertNotNull(convertedProfiles);
        assertFalse(convertedProfiles.isEmpty());

        String content = String.valueOf(convertedProfiles.get(0).get("content"));
        assertTrue(content.contains("Parent: http://fhir.ehdsi.eu/laboratory/StructureDefinition/Observation-resultslab-lab-myhealtheu"));
        assertTrue(content.contains("* category.coding.code = #laboratory"));
        assertTrue(content.contains("* code"));
        assertTrue(content.contains("* status"));
    }

    @Test
    void requiresOfficialIgDependencyForExternalParentSushiCompilation() {
        Map<String, Object> request = new HashMap<>();
        request.put("profileName", "ExternalObservation");
        request.put("parent", "http://example.org/fhir/StructureDefinition/ExternalObservation");
        request.put("fshContent", """
                Profile: ExternalObservation
                Parent: http://example.org/fhir/StructureDefinition/ExternalObservation
                Id: external-observation
                * status 1..1
                """);

        given()
                .contentType(ContentType.JSON)
                .body(request)
                .when()
                .post("/api/convert/fhir/sushi")
                .then()
                .statusCode(400);
    }

    @Test
    void projectPlusRequiredIncludesDoesNotReturnOtherProfilesForBioProject() throws Exception {
        String bbr = readFixture("observation/bio.xml");
        Map<String, Object> generationRequest = new HashMap<>();
        generationRequest.put("bbr", bbr);
        generationRequest.put("sushiRepo", false);
        generationRequest.put("emitIr", false);
        generationRequest.put("emitLogs", true);
        generationRequest.put("yamlConfig", null);
        generationRequest.put("projectPlusRequiredIncludes", true);
        generationRequest.put("ownedRepositoryPrefixes", List.of());

        JsonPath generation = given()
                .contentType(ContentType.JSON)
                .body(generationRequest)
                .when()
                .post("/api/generate")
                .then()
                .statusCode(200)
                .extract()
                .jsonPath();

        List<Map<String, Object>> profiles = generation.getList("profiles");
        assertNotNull(profiles);
        assertFalse(profiles.isEmpty());
        assertEquals(profiles.size(), generation.getInt("report.profilesGenerated"));
        assertTrue(profiles.stream().noneMatch(profile -> "OTHER".equals(String.valueOf(profile.get("templateOrigin")))));
        assertTrue(profiles.stream().allMatch(profile -> "PROJECT".equals(String.valueOf(profile.get("templateOrigin")))));
        assertTrue(profiles.stream().allMatch(profile -> {
            String templateOrigin = String.valueOf(profile.get("templateOrigin"));
            String ownershipStatus = String.valueOf(profile.get("ownershipStatus"));
            String selectionReason = String.valueOf(profile.get("selectionReason"));
            if ("PROJECT".equals(templateOrigin)) {
                return "PROJECT".equals(ownershipStatus) && "DIRECT".equals(selectionReason);
            }
            return false;
        }));
    }

    private String readFixture(String path) throws Exception {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "Missing test fixture: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
