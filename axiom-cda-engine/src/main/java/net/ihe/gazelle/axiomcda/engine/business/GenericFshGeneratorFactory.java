package net.ihe.gazelle.axiomcda.engine.business;

public class GenericFshGeneratorFactory implements FshGeneratorFactory {

    @Override
    public boolean supports(FshGeneratorContext context) {
        return context != null && context.template() != null;
    }

    @Override
    public IrToFhirFshGenerator create() {
        return new GenericIrToFhirFshGenerator();
    }

    @Override
    public int priority() {
        return -100;
    }
}
