package net.ihe.gazelle.axiomcda.engine.technical;

import net.ihe.gazelle.axiomcda.api.port.GenerationReportWriter;
import net.ihe.gazelle.axiomcda.api.report.GenerationReport;

public class ConsoleGenerationReportWriter implements GenerationReportWriter {
    @Override
    public void write(GenerationReport report) {
        if (report == null) {
            return;
        }
        System.out.println("Templates processed: " + report.templatesProcessed());
        System.out.println("Profiles generated: " + report.profilesGenerated());
        System.out.println("Invariants generated: " + report.invariantsGenerated());
        System.out.println("Unmapped elements: " + report.unmappedElements());
        System.out.println("Unresolved ValueSets: " + report.unresolvedValueSets());
        if (!report.warnings().isEmpty()) {
            System.out.println("Warnings:");
            for (String warning : report.warnings()) {
                System.out.println(" - " + warning);
            }
        }
    }
}
