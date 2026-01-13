package net.ihe.gazelle.axiomcda.engine.technical;

import net.ihe.gazelle.axiomcda.api.port.GenerationReportWriter;
import net.ihe.gazelle.axiomcda.api.report.GenerationReport;
import picocli.CommandLine.Help.Ansi;

import java.util.ArrayList;
import java.util.List;

public class ConsoleGenerationReportWriter implements GenerationReportWriter {
    @Override
    public void write(GenerationReport report) {
        if (report == null) {
            return;
        }
        Ansi ansi = Ansi.AUTO;
        System.out.println(ansi.string("@|bold,cyan axiom-cda generate report|@"));
        System.out.println();

        List<Row> rows = new ArrayList<>();
        rows.add(new Row("Templates considered", String.valueOf(report.templatesConsidered())));
        rows.add(new Row("Templates generated", String.valueOf(report.templatesGenerated())));
        rows.add(new Row("Templates skipped", String.valueOf(report.templatesSkipped())));
        rows.add(new Row("Templates OK", String.valueOf(report.templatesOk())));
        rows.add(new Row("Profiles generated", String.valueOf(report.profilesGenerated())));
        rows.add(new Row("Invariants generated", String.valueOf(report.invariantsGenerated())));
        rows.add(new Row("Unmapped elements", String.valueOf(report.unmappedElements())));
        rows.add(new Row("Unresolved ValueSets", String.valueOf(report.unresolvedValueSets())));
        rows.add(new Row("Warnings", String.valueOf(report.warnings().size())));
        rows.add(new Row("Errors", String.valueOf(report.errors().size())));

        printTable(rows);

        System.out.println();
        if (!report.errors().isEmpty()) {
            System.out.println(ansi.string("@|bold,red Errors|@"));
            for (String error : report.errors()) {
                System.out.println(" - " + colorizeWarning(ansi, error));
            }
        } else {
            System.out.println(ansi.string("@|bold,green No errors|@"));
        }

        System.out.println();
        if (!report.warnings().isEmpty()) {
            System.out.println(ansi.string("@|bold,yellow Warnings|@"));
            for (String warning : report.warnings()) {
                System.out.println(" - " + colorizeWarning(ansi, warning));
            }
        } else {
            System.out.println(ansi.string("@|bold,green No warnings|@"));
        }
    }

    private static void printTable(List<Row> rows) {
        int labelWidth = "Metric".length();
        int valueWidth = "Value".length();
        for (Row row : rows) {
            labelWidth = Math.max(labelWidth, row.label().length());
            valueWidth = Math.max(valueWidth, row.value().length());
        }

        String top = "+" + "-".repeat(labelWidth + 2) + "+" + "-".repeat(valueWidth + 2) + "+";
        String header = "| " + padRight("Metric", labelWidth) + " | " + padRight("Value", valueWidth) + " |";

        System.out.println(top);
        System.out.println(header);
        System.out.println(top);
        for (Row row : rows) {
            System.out.println("| " + padRight(row.label(), labelWidth) + " | " + padRight(row.value(), valueWidth) + " |");
        }
        System.out.println(top);
    }

    private static String padRight(String value, int width) {
        if (value == null) {
            value = "";
        }
        if (value.length() >= width) {
            return value;
        }
        return value + " ".repeat(width - value.length());
    }

    private static String colorizeWarning(Ansi ansi, String warning) {
        if (warning == null) {
            return "";
        }
        String lower = warning.toLowerCase();
        if (lower.startsWith("error:")) {
            return ansi.string("@|red " + warning + "|@");
        }
        if (lower.startsWith("warning:")) {
            return ansi.string("@|yellow " + warning + "|@");
        }
        return warning;
    }

    private record Row(String label, String value) {
    }
}
