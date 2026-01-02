package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.bbr.Decor;
import net.ihe.gazelle.axiomcda.api.config.GenerationConfig;
import net.ihe.gazelle.axiomcda.api.ir.IRTemplate;
import net.ihe.gazelle.axiomcda.api.port.BbrToIrTransformer;
import net.ihe.gazelle.axiomcda.api.port.CdaModelRepository;
import net.ihe.gazelle.axiomcda.engine.technical.JaxbBbrLoader;
import net.ihe.gazelle.axiomcda.engine.technical.JsonCdaModelRepository;
import net.ihe.gazelle.axiomcda.engine.util.ResourcePaths;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DefaultBbrToIrTransformerTest {
    @Test
    void extractsRecordTargetPatientRoleCardinality() throws Exception {
        Path bbrPath = ResourcePaths.getResourcePath("head.xml");
        Decor decor = new JaxbBbrLoader().load(bbrPath);
        CdaModelRepository cdaRepository = new JsonCdaModelRepository(ResourcePaths.getResourcePath("package"));

        BbrToIrTransformer transformer = new DefaultBbrToIrTransformer();
        var result = transformer.transform(decor, GenerationConfig.defaults(), cdaRepository);

        Optional<IRTemplate> recordTarget = result.templates().stream()
                .filter(template -> "RecordTarget".equals(template.rootCdaType()))
                .findFirst();

        assertTrue(recordTarget.isPresent());

        var patientRole = recordTarget.get().elements().stream()
                .filter(element -> "patientRole".equals(element.path()))
                .findFirst();

        assertTrue(patientRole.isPresent());
        assertNotNull(patientRole.get().cardinality());
        assertEquals(1, patientRole.get().cardinality().min());
        assertEquals("1", patientRole.get().cardinality().max());
    }
}
