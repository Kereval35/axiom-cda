package net.ihe.gazelle.axiomcda.engine.business;

import net.ihe.gazelle.axiomcda.api.bbr.Decor;
import net.ihe.gazelle.axiomcda.api.bbr.Rules;
import net.ihe.gazelle.axiomcda.api.bbr.TemplateDefinition;
import net.ihe.gazelle.axiomcda.api.config.GenerationConfig;
import net.ihe.gazelle.axiomcda.api.ir.IRDiagnostic;
import net.ihe.gazelle.axiomcda.api.ir.IRTemplate;
import net.ihe.gazelle.axiomcda.api.ir.IRTemplateOrigin;
import net.ihe.gazelle.axiomcda.api.ir.IrTransformResult;
import net.ihe.gazelle.axiomcda.api.port.BbrToIrTransformer;
import net.ihe.gazelle.axiomcda.api.port.CdaModelRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DefaultBbrToIrTransformer implements BbrToIrTransformer {
    private final BbrTemplateSelectionService selectionService;
    private final BbrTemplateBuildService buildService;

    public DefaultBbrToIrTransformer() {
        this(new BbrTemplateSelectionService(), new BbrTemplateBuildService());
    }

    DefaultBbrToIrTransformer(BbrTemplateSelectionService selectionService, BbrTemplateBuildService buildService) {
        this.selectionService = selectionService;
        this.buildService = buildService;
    }

    @Override
    public IrTransformResult transform(Decor decor, GenerationConfig config, CdaModelRepository cdaRepository) {
        if (decor == null) {
            throw new IllegalArgumentException("decor must be set");
        }
        List<IRDiagnostic> diagnostics = new ArrayList<>();
        List<IRTemplate> templates = new ArrayList<>();

        Rules rules = decor.getRules();
        if (rules == null) {
            return new IrTransformResult(List.of(), List.of(), 0);
        }

        Map<String, TemplateDefinition> latestById = new HashMap<>();
        selectionService.selectLatestById(rules).forEach(latestById::put);

        String preferredLanguage = decor.getProject() != null ? decor.getProject().getDefaultLanguage() : null;
        OwnershipContext ownershipContext = selectionService.ownershipContextFromDecor(decor);
        Set<String> selectedIds = selectionService.selectTemplateIds(latestById, config.templateSelection());
        Set<String> projectOwnedIds = selectionService.findProjectOwnedIds(selectedIds, latestById, ownershipContext, config.templateSelection());
        Set<String> expandedIds = config.templateSelection().projectPlusRequiredIncludes()
                ? selectionService.expandIncludes(projectOwnedIds, latestById)
                : selectionService.expandIncludes(selectedIds, latestById);
        Map<String, IRTemplateOrigin> originByTemplateId = selectionService.classifyOrigins(
                projectOwnedIds,
                expandedIds,
                latestById,
                ownershipContext,
                config.templateSelection()
        );

        int templatesConsidered = 0;
        for (String templateId : expandedIds) {
            TemplateDefinition template = latestById.get(templateId);
            if (template == null) {
                continue;
            }
            templatesConsidered++;
            IRTemplate irTemplate = buildService.buildTemplate(
                    template,
                    preferredLanguage,
                    config,
                    cdaRepository,
                    latestById,
                    diagnostics,
                    originByTemplateId.getOrDefault(templateId, IRTemplateOrigin.OTHER)
            );
            if (irTemplate != null) {
                templates.add(irTemplate);
            }
        }

        templates.sort(Comparator.comparing(IRTemplate::rootCdaType).thenComparing(IRTemplate::id));
        return new IrTransformResult(templates, diagnostics, templatesConsidered);
    }
}
