package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.config.GenerationConfig;
import net.ihe.gazelle.axiomcda.api.fsh.FshBundle;
import net.ihe.gazelle.axiomcda.api.ir.*;
import net.ihe.gazelle.axiomcda.api.port.CdaModelRepository;
import net.ihe.gazelle.axiomcda.engine.technical.JsonCdaModelRepository;
import net.ihe.gazelle.axiomcda.engine.util.ResourcePaths;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultIrToFshGeneratorTest {
    @Test
    void generatesOnlyConstraintForInclude() throws Exception {
        IRTemplate recordTarget = new IRTemplate(
                "T1",
                "RecordTarget",
                "RecordTarget",
                "RecordTarget",
                "RecordTarget",
                List.of(new IRElementConstraint(
                        "patientRole",
                        new IRCardinality(1, "1"),
                        null,
                        null,
                        null,
                        List.of(),
                        null
                )),
                List.of(new IRTemplateInclude("patientRole", "T2", new IRCardinality(1, "1"))),
                List.of()
        );

        IRTemplate patientRole = new IRTemplate(
                "T2",
                "PatientRole",
                "PatientRole",
                "PatientRole",
                "PatientRole",
                List.of(),
                List.of(),
                List.of()
        );

        CdaModelRepository cdaRepository = new JsonCdaModelRepository(ResourcePaths.getResourcePath("package"));
        DefaultIrToFshGenerator generator = new DefaultIrToFshGenerator();
        FshBundle bundle = generator.generate(List.of(recordTarget, patientRole), GenerationConfig.defaults(), cdaRepository);

        String fsh = bundle.files().get(DefaultIrToFshGenerator.RESOURCES_DIR + "/RecordTarget.fsh");
        assertTrue(fsh.contains("* patientRole only PatientRole"));
    }
}
