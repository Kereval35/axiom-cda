package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.bbr.Decor;
import net.ihe.gazelle.axiomcda.api.config.GenerationConfig;
import net.ihe.gazelle.axiomcda.api.fsh.FshBundle;
import net.ihe.gazelle.axiomcda.engine.technical.JaxbBbrLoader;
import net.ihe.gazelle.axiomcda.engine.util.ResourcePaths;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultTerminologyToFshGeneratorTest {
    @Test
    void generatesValueSetsFromTerminology() throws Exception {
        Path bbrPath = ResourcePaths.getResourcePath("head.xml");
        Decor decor = new JaxbBbrLoader().load(bbrPath);
        DefaultTerminologyToFshGenerator generator = new DefaultTerminologyToFshGenerator();

        FshBundle bundle = generator.generate(decor, GenerationConfig.defaults());

        assertFalse(bundle.files().isEmpty(), "Terminology bundle should not be empty");
        assertTrue(bundle.files().keySet().stream().anyMatch(path -> path.startsWith("ValueSets/")),
                "Expected at least one ValueSet FSH file");
        assertTrue(bundle.files().values().stream().anyMatch(content -> content.startsWith("ValueSet: ")),
                "Expected ValueSet definitions in output");
    }
}
