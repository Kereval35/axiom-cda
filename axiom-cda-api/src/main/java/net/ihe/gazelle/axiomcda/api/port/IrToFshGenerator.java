package net.ihe.gazelle.axiomcda.api.port;

import net.ihe.gazelle.axiomcda.api.config.GenerationConfig;
import net.ihe.gazelle.axiomcda.api.fsh.FshBundle;
import net.ihe.gazelle.axiomcda.api.ir.IRTemplate;

import java.util.List;

public interface IrToFshGenerator {
    FshBundle generate(List<IRTemplate> templates, GenerationConfig config, CdaModelRepository cdaRepository);
}
