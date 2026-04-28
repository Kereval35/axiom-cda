package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.bbr.Decor;
import net.ihe.gazelle.axiomcda.api.bbr.IncludeDefinition;
import net.ihe.gazelle.axiomcda.api.bbr.RuleDefinition;
import net.ihe.gazelle.axiomcda.api.bbr.TemplateDefinition;
import net.ihe.gazelle.axiomcda.api.config.GenerationConfig;
import net.ihe.gazelle.axiomcda.api.config.TemplateSelection;
import net.ihe.gazelle.axiomcda.api.ir.IRTemplate;
import net.ihe.gazelle.axiomcda.api.ir.IRTemplateOrigin;
import net.ihe.gazelle.axiomcda.api.port.BbrToIrTransformer;
import net.ihe.gazelle.axiomcda.api.port.CdaModelRepository;
import net.ihe.gazelle.axiomcda.engine.technical.JaxbBbrLoader;
import net.ihe.gazelle.axiomcda.engine.technical.JsonCdaModelRepository;
import net.ihe.gazelle.axiomcda.engine.util.ResourcePaths;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
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

    @Test
    void marksNonProjectTemplatesAddedByIncludeAsRequiredIncludes() throws Exception {
        Path bbrPath = ResourcePaths.getResourcePath("head.xml");
        Decor decor = new JaxbBbrLoader().load(bbrPath);
        CdaModelRepository cdaRepository = new JsonCdaModelRepository(ResourcePaths.getResourcePath("package"));

        TemplateDefinition rootTemplate = findTemplate(decor, "1.2.250.1.213.1.1.1.1");
        TemplateDefinition recordTarget = findTemplate(decor, "1.2.250.1.213.1.1.1.1.10.10");

        TemplateDefinition externalClone = new TemplateDefinition();
        externalClone.setId("9.9.9.9");
        externalClone.setName(recordTarget.getName());
        externalClone.setDisplayName(recordTarget.getDisplayName());
        externalClone.setStatusCode(recordTarget.getStatusCode());
        externalClone.setEffectiveDate(recordTarget.getEffectiveDate());
        externalClone.setIdent("IHE-PCC-");
        externalClone.getDesc().addAll(recordTarget.getDesc());
        externalClone.getClassification().addAll(recordTarget.getClassification());
        externalClone.getRelationship().addAll(recordTarget.getRelationship());
        externalClone.getAttributeOrChoiceOrElement().addAll(recordTarget.getAttributeOrChoiceOrElement());
        decor.getRules().getTemplateAssociationOrTemplate().add(externalClone);

        RuleDefinition clinicalDocumentRule = rootTemplate.getAttributeOrChoiceOrElement().stream()
                .filter(RuleDefinition.class::isInstance)
                .map(RuleDefinition.class::cast)
                .findFirst()
                .orElseThrow();

        clinicalDocumentRule.getLetOrAssertOrReport().stream()
                .filter(IncludeDefinition.class::isInstance)
                .map(IncludeDefinition.class::cast)
                .filter(include -> "1.2.250.1.213.1.1.1.1.10.10".equals(include.getRef()))
                .findFirst()
                .orElseThrow()
                .setRef("9.9.9.9");

        GenerationConfig config = new GenerationConfig(
                GenerationConfig.defaults().naming(),
                GenerationConfig.defaults().nullFlavorPolicy(),
                GenerationConfig.defaults().valueSetPolicy(),
                new TemplateSelection(List.of(), List.of("1.2.250.1.213.1.1.1.1"), true),
                GenerationConfig.defaults().emitInvariants(),
                GenerationConfig.defaults().emitIrSnapshot()
        );

        BbrToIrTransformer transformer = new DefaultBbrToIrTransformer();
        var result = transformer.transform(decor, config, cdaRepository);

        IRTemplate externalTemplate = result.templates().stream()
                .filter(template -> "9.9.9.9".equals(template.id()))
                .findFirst()
                .orElseThrow();

        assertEquals(IRTemplateOrigin.REQUIRED_INCLUDE, externalTemplate.origin());
        assertTrue(result.templates().stream().anyMatch(template -> template.origin() == IRTemplateOrigin.PROJECT));
        assertTrue(result.templates().stream().noneMatch(template -> template.origin() == IRTemplateOrigin.OTHER));
    }

    @Test
    void keepsProjectTemplatesForCompiledBioProjectMode() throws Exception {
        Path bbrPath = Path.of("..", "axiom-cda-ws", "src", "test", "resources", "observation", "bio.xml").normalize();
        Decor decor = new JaxbBbrLoader().load(bbrPath);
        CdaModelRepository cdaRepository = new JsonCdaModelRepository(ResourcePaths.getResourcePath("package"));

        GenerationConfig config = new GenerationConfig(
                GenerationConfig.defaults().naming(),
                GenerationConfig.defaults().nullFlavorPolicy(),
                GenerationConfig.defaults().valueSetPolicy(),
                new TemplateSelection(List.of(), List.of(), true),
                GenerationConfig.defaults().emitInvariants(),
                GenerationConfig.defaults().emitIrSnapshot()
        );

        BbrToIrTransformer transformer = new DefaultBbrToIrTransformer();
        var result = transformer.transform(decor, config, cdaRepository);

        assertFalse(result.templates().isEmpty());
        assertTrue(result.templates().stream().anyMatch(template -> "1.2.250.1.213.1.1.1.55".equals(template.id())));
        assertTrue(result.templates().stream().anyMatch(template -> "1.2.250.1.213.1.1.3.48".equals(template.id())));
        assertTrue(result.templates().stream().anyMatch(template -> template.origin() == IRTemplateOrigin.PROJECT));
        assertTrue(result.templates().stream().anyMatch(template -> template.origin() == IRTemplateOrigin.REQUIRED_INCLUDE));
        assertTrue(result.templates().stream().noneMatch(template -> template.origin() == IRTemplateOrigin.OTHER));
    }

    @Test
    void reclassifiesSelectedButNotOwnedTemplatesAsRequiredIncludesInProjectMode() throws Exception {
        Path bbrPath = ResourcePaths.getResourcePath("head.xml");
        Decor decor = new JaxbBbrLoader().load(bbrPath);
        CdaModelRepository cdaRepository = new JsonCdaModelRepository(ResourcePaths.getResourcePath("package"));

        TemplateDefinition rootTemplate = findTemplate(decor, "1.2.250.1.213.1.1.1.1");
        TemplateDefinition recordTarget = findTemplate(decor, "1.2.250.1.213.1.1.1.1.10.10");

        TemplateDefinition externalClone = new TemplateDefinition();
        externalClone.setId("9.9.9.9");
        externalClone.setName(recordTarget.getName());
        externalClone.setDisplayName(recordTarget.getDisplayName());
        externalClone.setStatusCode(recordTarget.getStatusCode());
        externalClone.setEffectiveDate(recordTarget.getEffectiveDate());
        externalClone.setIdent("IHE-PCC-");
        externalClone.getDesc().addAll(recordTarget.getDesc());
        externalClone.getClassification().addAll(recordTarget.getClassification());
        externalClone.getRelationship().addAll(recordTarget.getRelationship());
        externalClone.getAttributeOrChoiceOrElement().addAll(recordTarget.getAttributeOrChoiceOrElement());
        decor.getRules().getTemplateAssociationOrTemplate().add(externalClone);

        RuleDefinition clinicalDocumentRule = rootTemplate.getAttributeOrChoiceOrElement().stream()
                .filter(RuleDefinition.class::isInstance)
                .map(RuleDefinition.class::cast)
                .findFirst()
                .orElseThrow();

        clinicalDocumentRule.getLetOrAssertOrReport().stream()
                .filter(IncludeDefinition.class::isInstance)
                .map(IncludeDefinition.class::cast)
                .filter(include -> "1.2.250.1.213.1.1.1.1.10.10".equals(include.getRef()))
                .findFirst()
                .orElseThrow()
                .setRef("9.9.9.9");

        GenerationConfig config = new GenerationConfig(
                GenerationConfig.defaults().naming(),
                GenerationConfig.defaults().nullFlavorPolicy(),
                GenerationConfig.defaults().valueSetPolicy(),
                new TemplateSelection(List.of(), List.of("1.2.250.1.213.1.1.1.1", "9.9.9.9"), true),
                GenerationConfig.defaults().emitInvariants(),
                GenerationConfig.defaults().emitIrSnapshot()
        );

        BbrToIrTransformer transformer = new DefaultBbrToIrTransformer();
        var result = transformer.transform(decor, config, cdaRepository);

        IRTemplate externalTemplate = result.templates().stream()
                .filter(template -> "9.9.9.9".equals(template.id()))
                .findFirst()
                .orElseThrow();

        assertEquals(IRTemplateOrigin.REQUIRED_INCLUDE, externalTemplate.origin());
    }

    private TemplateDefinition findTemplate(Decor decor, String id) {
        return decor.getRules().getTemplateAssociationOrTemplate().stream()
                .filter(TemplateDefinition.class::isInstance)
                .map(TemplateDefinition.class::cast)
                .filter(template -> id.equals(template.getId()))
                .findFirst()
                .orElseThrow();
    }
}
