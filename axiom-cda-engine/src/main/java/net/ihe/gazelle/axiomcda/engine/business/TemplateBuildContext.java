package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.bbr.TemplateDefinition;
import net.ihe.gazelle.axiomcda.api.cda.CdaStructureDefinition;
import net.ihe.gazelle.axiomcda.api.config.GenerationConfig;
import net.ihe.gazelle.axiomcda.api.ir.IRBindingStrength;
import net.ihe.gazelle.axiomcda.api.ir.IRDiagnostic;
import net.ihe.gazelle.axiomcda.api.ir.IRDiagnosticSeverity;
import net.ihe.gazelle.axiomcda.api.port.CdaModelRepository;

import java.util.List;
import java.util.Map;

final class TemplateBuildContext {
    private final TemplateDefinition template;
    private final String preferredLanguage;
    private final GenerationConfig config;
    private final CdaModelRepository cdaRepository;
    private final Map<String, TemplateDefinition> templateById;
    private final List<IRDiagnostic> diagnostics;
    private final BbrTerminologyCanonicalResolver canonicalResolver;
    private CdaStructureDefinition cdaDefinition;

    TemplateBuildContext(TemplateDefinition template,
                         String preferredLanguage,
                         GenerationConfig config,
                         CdaModelRepository cdaRepository,
                         Map<String, TemplateDefinition> templateById,
                         List<IRDiagnostic> diagnostics) {
        this(
                template,
                preferredLanguage,
                config,
                cdaRepository,
                templateById,
                diagnostics,
                BbrTerminologyCanonicalResolver.fromDecor(null)
        );
    }

    TemplateBuildContext(TemplateDefinition template,
                         String preferredLanguage,
                         GenerationConfig config,
                         CdaModelRepository cdaRepository,
                         Map<String, TemplateDefinition> templateById,
                         List<IRDiagnostic> diagnostics,
                         BbrTerminologyCanonicalResolver canonicalResolver) {
        this.template = template;
        this.preferredLanguage = preferredLanguage;
        this.config = config;
        this.cdaRepository = cdaRepository;
        this.templateById = templateById;
        this.diagnostics = diagnostics;
        this.canonicalResolver = canonicalResolver == null
                ? BbrTerminologyCanonicalResolver.fromDecor(null)
                : canonicalResolver;
    }

    TemplateDefinition template() {
        return template;
    }

    String preferredLanguage() {
        return preferredLanguage;
    }

    GenerationConfig config() {
        return config;
    }

    CdaModelRepository cdaRepository() {
        return cdaRepository;
    }

    Map<String, TemplateDefinition> templateById() {
        return templateById;
    }

    void setCdaDefinition(CdaStructureDefinition definition) {
        this.cdaDefinition = definition;
    }

    String normalizePath(String relativePath) {
        return CdaPathNormalizer.normalizePath(relativePath, cdaDefinition, cdaRepository);
    }

    String rootCdaType() {
        return cdaDefinition != null ? cdaDefinition.name() : null;
    }

    void addDiagnostic(IRDiagnosticSeverity severity, String path, String message) {
        diagnostics.add(new IRDiagnostic(severity, template.getId(), path, message));
    }

    String resolveValueSet(String oid) {
        return canonicalResolver.resolveValueSet(oid, config);
    }

    String resolveCodeSystem(String oid) {
        return canonicalResolver.resolveCodeSystem(oid, config);
    }

    IRBindingStrength defaultBindingStrength() {
        return config.valueSetPolicy().defaultStrength();
    }
}
