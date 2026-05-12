package net.ihe.gazelle.axiomcda.engine.business;

public class ObservationFshGeneratorFactory implements FshGeneratorFactory {

    @Override
    public boolean supports(FshGeneratorContext context) {
        return context != null
                && context.template() != null
                && "Observation".equals(context.template().rootCdaType());
    }

    @Override
    public IrToFhirFshGenerator create() {
        return new ObservationIrToFhirFshGenerator();
    }

    @Override
    public int priority() {
        return 200;
    }
}
