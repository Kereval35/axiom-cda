package net.ihe.gazelle.axiomcda.api.port;

import net.ihe.gazelle.axiomcda.api.bbr.Decor;
import net.ihe.gazelle.axiomcda.api.config.GenerationConfig;
import net.ihe.gazelle.axiomcda.api.ir.IrTransformResult;

public interface BbrToIrTransformer {
    IrTransformResult transform(Decor decor, GenerationConfig config, CdaModelRepository cdaRepository);
}
