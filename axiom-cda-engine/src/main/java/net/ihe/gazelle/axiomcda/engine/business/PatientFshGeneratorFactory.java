package net.ihe.gazelle.axiomcda.engine.business;

public class PatientFshGeneratorFactory implements FshGeneratorFactory {

    @Override
    public boolean supports(FshGeneratorContext context) {
        if (context == null || context.template() == null) {
            return false;
        }
        String targetType = SemanticMappingModelSupport.resolvePrimaryTargetType(context.mappingModel());
        if (!"Patient".equals(targetType)) {
            return false;
        }
        String rootCdaType = context.template().rootCdaType();
        return "RecordTarget".equals(rootCdaType) || "PatientRole".equals(rootCdaType) || "Patient".equals(rootCdaType);
    }

    @Override
    public IrToFhirFshGenerator create() {
        return new PatientIrToFhirFshGenerator();
    }

    @Override
    public int priority() {
        return 100;
    }
}
