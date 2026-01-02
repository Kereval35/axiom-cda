package net.ihe.gazelle.axiomcda.engine.technical;

import net.ihe.gazelle.axiomcda.api.fsh.FshBundle;
import net.ihe.gazelle.axiomcda.api.port.FshWriter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class DefaultFshWriter implements FshWriter {
    @Override
    public void write(Path outputDir, FshBundle bundle) throws Exception {
        if (outputDir == null) {
            throw new IllegalArgumentException("outputDir must be set");
        }
        if (bundle == null) {
            throw new IllegalArgumentException("bundle must be set");
        }
        for (var entry : bundle.files().entrySet()) {
            Path target = outputDir.resolve(entry.getKey());
            Files.createDirectories(target.getParent());
            Files.writeString(target, entry.getValue(), StandardCharsets.UTF_8);
        }
    }
}
