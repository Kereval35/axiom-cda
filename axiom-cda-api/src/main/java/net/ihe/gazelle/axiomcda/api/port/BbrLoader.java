package net.ihe.gazelle.axiomcda.api.port;

import net.ihe.gazelle.axiomcda.api.bbr.Decor;

import java.nio.file.Path;

public interface BbrLoader {
    Decor load(Path path) throws Exception;
}
