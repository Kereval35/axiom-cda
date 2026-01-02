package net.ihe.gazelle.axiomcda.api.port;

import net.ihe.gazelle.axiomcda.api.report.GenerationReport;

public interface GenerationReportWriter {
    void write(GenerationReport report);
}
