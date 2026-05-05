package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.bbr.TemplateDefinition;
import net.ihe.gazelle.axiomcda.api.config.GenerationConfig;
import net.ihe.gazelle.axiomcda.api.ir.IRDiagnostic;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TemplateBuildContextTest {
    @Test
    void fallsBackToOidCanonicalWithoutWarning() {
        TemplateDefinition template = new TemplateDefinition();
        template.setId("1.2.3");
        var diagnostics = new ArrayList<IRDiagnostic>();
        TemplateBuildContext context = new TemplateBuildContext(template,
                "en-US",
                GenerationConfig.defaults(),
                null,
                Map.of(),
                diagnostics);

        assertEquals("urn:oid:2.16.840.1.113883.1.11.78", context.resolveValueSet("2.16.840.1.113883.1.11.78"));
        assertTrue(diagnostics.isEmpty());
    }
}
