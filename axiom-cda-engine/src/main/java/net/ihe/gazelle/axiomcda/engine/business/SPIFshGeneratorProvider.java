package net.ihe.gazelle.axiomcda.engine.business;

import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;

public class SPIFshGeneratorProvider {

    private final List<FshGeneratorFactory> factories;

    public SPIFshGeneratorProvider() {
        this(ServiceLoader.load(FshGeneratorFactory.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .sorted(Comparator.comparingInt(FshGeneratorFactory::priority).reversed())
                .toList());
    }

    SPIFshGeneratorProvider(List<FshGeneratorFactory> factories) {
        this.factories = List.copyOf(factories);
    }

    public FshGeneratorFactory resolveFactory(FshGeneratorContext context) {
        return factories.stream()
                .filter(factory -> factory.supports(context))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No FHIR FSH generator factory supports template '"
                        + safeRootType(context) + "'."));
    }

    private String safeRootType(FshGeneratorContext context) {
        if (context == null || context.template() == null) {
            return "unknown";
        }
        return context.template().rootCdaType();
    }
}
