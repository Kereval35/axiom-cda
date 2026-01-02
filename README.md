# axiom-cda

Java 21 CLI to generate FSH-CDA profiles from ART-DECOR BBR exports.

## Build

```
mvn -q -pl axiom-cda-cli -am package
```

## Run

```
java -cp axiom-cda-cli/target/classes:axiom-cda-engine/target/classes:axiom-cda-api/target/classes \
  net.ihe.gazelle.axiomcda.cli.AxiomCdaCli generate \
  --bbr axiom-cda-engine/src/main/resources/head.xml \
  --out /tmp/axiom-cda-out
```

To prefix profile names or ids:

```
java ... AxiomCdaCli generate --bbr head.xml --out /tmp/out --profile-prefix MY --id-prefix my-
```

To use a different CDA package directory:

```
java ... AxiomCdaCli generate --bbr head.xml --out /tmp/out --cda-package /path/to/package
```

## Config

Pass a YAML config file to override naming, ValueSet mappings, or template selection.

```
java ... AxiomCdaCli generate --bbr head.xml --out /tmp/out --config config.yaml
```
