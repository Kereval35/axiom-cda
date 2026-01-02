package net.ihe.gazelle.axiomcda.engine.technical;

import net.ihe.gazelle.axiomcda.api.bbr.Decor;
import net.ihe.gazelle.axiomcda.api.port.BbrLoader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.nio.file.Files;
import java.nio.file.Path;

public class JaxbBbrLoader implements BbrLoader {
    @Override
    public Decor load(Path path) throws Exception {
        if (path == null) {
            throw new IllegalArgumentException("path must be set");
        }
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("BBR file does not exist: " + path);
        }
        JAXBContext context = JAXBContext.newInstance(Decor.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        return (Decor) unmarshaller.unmarshal(path.toFile());
    }
}
