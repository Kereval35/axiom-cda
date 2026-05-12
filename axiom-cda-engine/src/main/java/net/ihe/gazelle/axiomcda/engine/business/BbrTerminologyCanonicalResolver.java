package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.bbr.Decor;
import net.ihe.gazelle.axiomcda.api.bbr.Terminology;
import net.ihe.gazelle.axiomcda.api.bbr.ValueSet;
import net.ihe.gazelle.axiomcda.api.config.GenerationConfig;

import javax.xml.namespace.QName;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

final class BbrTerminologyCanonicalResolver {
    private static final String ID_ATTRIBUTE = "id";
    private static final String CANONICAL_URI_ATTRIBUTE = "canonicalUri";

    private final Map<String, String> sourceCodeSystemCanonicalByOid;
    private final Map<String, String> valueSetSourceCanonicalByOid;

    private BbrTerminologyCanonicalResolver(Map<String, String> sourceCodeSystemCanonicalByOid,
                                           Map<String, String> valueSetSourceCanonicalByOid) {
        this.sourceCodeSystemCanonicalByOid = sourceCodeSystemCanonicalByOid;
        this.valueSetSourceCanonicalByOid = valueSetSourceCanonicalByOid;
    }

    static BbrTerminologyCanonicalResolver fromDecor(Decor decor) {
        Map<String, String> codeSystemMappings = new LinkedHashMap<>();
        Map<String, String> valueSetMappings = new LinkedHashMap<>();
        if (decor == null) {
            return new BbrTerminologyCanonicalResolver(codeSystemMappings, valueSetMappings);
        }
        Terminology terminology = decor.getTerminology();
        if (terminology == null) {
            return new BbrTerminologyCanonicalResolver(codeSystemMappings, valueSetMappings);
        }
        for (ValueSet valueSet : terminology.getValueSet()) {
            collectSourceCodeSystems(valueSet, codeSystemMappings, valueSetMappings);
        }
        return new BbrTerminologyCanonicalResolver(codeSystemMappings, valueSetMappings);
    }

    String resolveCodeSystem(String oid, GenerationConfig config) {
        if (oid == null || oid.isBlank()) {
            return null;
        }
        if (oid.startsWith("http") || oid.startsWith("urn:")) {
            return oid;
        }
        String sourceCanonical = sourceCodeSystemCanonicalByOid.get(oid);
        if (sourceCanonical != null && !sourceCanonical.isBlank()) {
            return sourceCanonical;
        }
        String configuredCanonical = config != null ? config.valueSetPolicy().oidToCanonical().get(oid) : null;
        if (configuredCanonical != null && !configuredCanonical.isBlank()) {
            return configuredCanonical;
        }
        return "urn:oid:" + oid;
    }

    String resolveValueSet(String oid, GenerationConfig config) {
        if (oid == null || oid.isBlank()) {
            return null;
        }
        if (oid.startsWith("http") || oid.startsWith("urn:")) {
            return oid;
        }
        String mapped = config != null ? config.valueSetPolicy().oidToCanonical().get(oid) : null;
        if (mapped != null && !mapped.isBlank()) {
            return mapped;
        }
        String sourceCanonical = valueSetSourceCanonicalByOid.get(oid);
        if (sourceCanonical != null && !sourceCanonical.isBlank()) {
            return sourceCanonical;
        }
        if (config == null || config.valueSetPolicy().useOidAsCanonical()) {
            return "urn:oid:" + oid;
        }
        return null;
    }

    private static void collectSourceCodeSystems(ValueSet valueSet,
                                                 Map<String, String> codeSystemMappings,
                                                 Map<String, String> valueSetMappings) {
        if (valueSet == null) {
            return;
        }
        Set<String> sourceCanonicals = new LinkedHashSet<>();
        for (ValueSet.SourceCodeSystem sourceCodeSystem : valueSet.getSourceCodeSystem()) {
            Map<QName, String> attributes = sourceCodeSystem.getOtherAttributes();
            String id = attribute(attributes, ID_ATTRIBUTE);
            String canonicalUri = attribute(attributes, CANONICAL_URI_ATTRIBUTE);
            if (id != null && !id.isBlank() && canonicalUri != null && !canonicalUri.isBlank()) {
                codeSystemMappings.putIfAbsent(id, canonicalUri);
                sourceCanonicals.add(canonicalUri);
            }
        }
        if (sourceCanonicals.size() == 1) {
            String sourceCanonical = sourceCanonicals.iterator().next();
            putIfPresent(valueSetMappings, valueSet.getId(), sourceCanonical);
            putIfPresent(valueSetMappings, valueSet.getRef(), sourceCanonical);
        }
    }

    private static void putIfPresent(Map<String, String> mappings, String oid, String canonicalUri) {
        if (oid != null && !oid.isBlank() && canonicalUri != null && !canonicalUri.isBlank()) {
            mappings.putIfAbsent(oid, canonicalUri);
        }
    }

    private static String attribute(Map<QName, String> attributes, String localName) {
        if (attributes == null || attributes.isEmpty()) {
            return null;
        }
        for (Map.Entry<QName, String> entry : attributes.entrySet()) {
            QName name = entry.getKey();
            if (name != null && localName.equals(name.getLocalPart())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
