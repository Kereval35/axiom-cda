package net.ihe.gazelle.axiomcda.engine.business;

public interface FshGeneratorFactory {

    boolean supports(FshGeneratorContext context);

    IrToFhirFshGenerator create();

    default int priority() {
        return 0;
    }
}
