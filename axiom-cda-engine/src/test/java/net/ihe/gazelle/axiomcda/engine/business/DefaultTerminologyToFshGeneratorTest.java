package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.bbr.Decor;
import net.ihe.gazelle.axiomcda.api.bbr.ItemStatusCodeLifeCycle;
import net.ihe.gazelle.axiomcda.api.bbr.Terminology;
import net.ihe.gazelle.axiomcda.api.bbr.ValueSet;
import net.ihe.gazelle.axiomcda.api.bbr.ValueSetConcept;
import net.ihe.gazelle.axiomcda.api.bbr.ValueSetConceptList;
import net.ihe.gazelle.axiomcda.api.bbr.VocabType;
import net.ihe.gazelle.axiomcda.api.config.GenerationConfig;
import net.ihe.gazelle.axiomcda.api.fsh.FshBundle;
import net.ihe.gazelle.axiomcda.engine.technical.JaxbBbrLoader;
import net.ihe.gazelle.axiomcda.engine.util.ResourcePaths;
import org.junit.jupiter.api.Test;

import javax.xml.namespace.QName;
import java.math.BigInteger;
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

    @Test
    void usesSourceCodeSystemCanonicalUriForValueSetConceptSystems() {
        Decor decor = new Decor();
        decor.setLanguage("en-US");
        Terminology terminology = new Terminology();
        decor.setTerminology(terminology);

        ValueSet valueSet = new ValueSet();
        valueSet.setId("1.2.3.4.5");
        valueSet.setName("AdministrativeGender");
        valueSet.setDisplayName("Administrative Gender");
        valueSet.setStatusCode(ItemStatusCodeLifeCycle.FINAL);

        ValueSet.SourceCodeSystem sourceCodeSystem = new ValueSet.SourceCodeSystem();
        sourceCodeSystem.getOtherAttributes().put(new QName("id"), "2.16.840.1.113883.5.1");
        sourceCodeSystem.getOtherAttributes().put(
                new QName("canonicalUri"),
                "http://terminology.hl7.org/CodeSystem/v3-AdministrativeGender"
        );
        valueSet.getSourceCodeSystem().add(sourceCodeSystem);

        ValueSetConcept concept = new ValueSetConcept();
        concept.setCode("F");
        concept.setCodeSystem("2.16.840.1.113883.5.1");
        concept.setDisplayName("Female");
        concept.setLevel(BigInteger.ZERO);
        concept.setType(VocabType.L);
        ValueSetConceptList conceptList = new ValueSetConceptList();
        conceptList.getConceptOrInclude().add(concept);
        valueSet.setConceptList(conceptList);

        terminology.getValueSet().add(valueSet);

        FshBundle bundle = new DefaultTerminologyToFshGenerator().generate(decor, GenerationConfig.defaults());

        String fsh = bundle.files().values().iterator().next();
        assertTrue(fsh.contains("* ^url = \"http://terminology.hl7.org/CodeSystem/v3-AdministrativeGender\""));
        assertTrue(fsh.contains("* include http://terminology.hl7.org/CodeSystem/v3-AdministrativeGender#F \"Female\""));
        assertFalse(fsh.contains("urn:oid:2.16.840.1.113883.5.1#F"));
    }
}
