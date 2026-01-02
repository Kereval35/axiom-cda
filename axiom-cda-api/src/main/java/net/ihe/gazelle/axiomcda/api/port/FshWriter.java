package net.ihe.gazelle.axiomcda.api.port;

import net.ihe.gazelle.axiomcda.api.fsh.FshBundle;

import java.nio.file.Path;

public interface FshWriter {
    void write(Path outputDir, FshBundle bundle) throws Exception;
}
