# axiom-cda

Java 21 CLI to generate FSH-CDA profiles and terminology from ART-DECOR BBR exports.

## What this project does

This tool turns an ART-DECOR BBR (Building Block Repository) export into:
- FSH profiles that specialize HL7 CDA logical models from the `hl7.cda.uv.core` package
- FSH ValueSets and CodeSystems extracted from the BBR `terminology` section
- A SUSHI-ready repository with `sushi-config.yaml` and `input/fsh`

### End-to-end pipeline

1) **BBR (ART-DECOR) input**
   - We load the BBR XML (Decor) and extract templates, constraints, and terminology.
2) **IR (Intermediate Representation)**
   - Templates are normalized into a stable IR: root CDA type, element paths, cardinalities,
     fixed values, bindings, includes, and invariants.
3) **FSH generation**
   - IR becomes FSH CDA profiles (FSH `Profile:` + constraints).
   - BBR terminology becomes FSH `ValueSet:` and `CodeSystem:` definitions.

## CDA foundations

We generate profiles **on top of HL7 CDA R2 logical models** from the `hl7.cda.uv.core` package:
- `StructureDefinition/<CDAClass>` is used as the profile `Parent`.
- Paths are validated and normalized against the CDA snapshot.
- Cardinalities, bindings, and fixed values are clamped to avoid weakening the base CDA rules.

## Output layout

By default, output is:
- `Resources<ProjectNameToken>/` profiles (token derived from Decor project name)
- `Invariants/` invariant FSH files
- `ValueSets/` ValueSet FSH files
- `CodeSystems/` CodeSystem FSH files

With `--sushi-repo`, files go under `input/fsh` and a `sushi-config.yaml` is emitted.
When `emitIrSnapshot` (or `--emit-ir`) is enabled, an `axiom-cda-ir.json` snapshot is written to the output directory.

## Business rules and considerations

Key behaviors that keep output valid and aligned with CDA:
- **No hardcoded prefixes**: profile names/ids are generic unless you provide CLI prefixes.
- **Default is all templates**: we do not filter to headers unless you explicitly do so.
- **Paths are normalized** against CDA snapshots, including namespaces and `sdtc` attributes.
- **Fixed values** are emitted only if they do not conflict with base CDA fixed values.
- **Binding strength** is never weakened relative to the CDA base (e.g., required stays required).
- **Cardinality** is clamped to the CDA base cardinality to avoid invalid constraints.
- **Includes** generate `only` constraints only when compatible with the allowed CDA types.
- **Terminology**:
  - ValueSets and CodeSystems are generated from BBR `terminology`.
  - URLs default to `urn:oid:<oid>` unless mapped via config.
  - Duplicate codes are deduplicated within each ValueSet.

### Known limitations

These are intentionally not supported (reported as warnings):
- Predicated elements (slicing) are ignored.
- Choice elements are skipped.
- Some CDA attributes and paths may be unmapped depending on the BBR structure.

## Build

```
mvn -q -pl axiom-cda-cli -am package
```

## Run

Jar (recommended):

```
java -jar axiom-cda-cli/target/axiom-cda-cli-1.0-SNAPSHOT.jar generate \
  --bbr axiom-cda-engine/src/main/resources/head.xml \
  --out /tmp/axiom-cda-out
```

Classpath (dev):

```
java -cp axiom-cda-cli/target/classes:axiom-cda-engine/target/classes:axiom-cda-api/target/classes \
  net.ihe.gazelle.axiomcda.cli.AxiomCdaCli generate \
  --bbr axiom-cda-engine/src/main/resources/head.xml \
  --out /tmp/axiom-cda-out
```

To generate a SUSHI-ready repo:

```
java ... AxiomCdaCli generate --bbr head.xml --out /tmp/out --sushi-repo
```

To emit the intermediate representation JSON:

```
java ... AxiomCdaCli generate --bbr head.xml --out /tmp/out --emit-ir
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
CLI flags override config values for naming and template selection when both are provided.

```
java ... AxiomCdaCli generate --bbr head.xml --out /tmp/out --config config.yaml
```

### CLI options

```
--bbr <path>              Path to the BBR XML (required)
--out <dir>               Output directory (required)
--cda-package <dir>       Override CDA package directory
--ans-reference <dir>     Optional ANS FSH path (comparison only)
--config <yaml>           Generation config override
--profile-prefix <s>      Prefix profile names
--id-prefix <s>           Prefix profile ids
--title-prefix <s>        Prefix profile titles
--resources-dir <s>       Output folder for profiles (default: Resources<ProjectNameToken>)
--invariants-dir <s>      Output folder for invariants (default: Invariants)
--classification-types <csv>  Template classification filters
--template-ids <csv>      Explicit template ids to generate
--all-templates           Ignore classification filters and generate all
--sushi-repo              Emit a SUSHI-ready repo (sushi-config.yaml + input/fsh)
--ig-id <s>               IG id for sushi-config.yaml
--ig-name <s>             IG name for sushi-config.yaml
--ig-title <s>            IG title for sushi-config.yaml
--ig-canonical <url>      IG canonical for sushi-config.yaml
--ig-version <s>          IG version for sushi-config.yaml
--ig-copyright-year <s>   IG copyrightYear for sushi-config.yaml
--ig-release-label <s>    IG releaseLabel for sushi-config.yaml
--emit-ir                 Emit the intermediate representation as axiom-cda-ir.json
```

### Config file structure

The YAML config mirrors `GenerationConfig`:

```
naming:
  profilePrefix: ""
  idPrefix: ""
  titlePrefix: ""
  profileNameOverrides: {}
  idOverrides: {}
nullFlavorPolicy:
  forbiddenPaths: []
valueSetPolicy:
  oidToCanonical: {}
  defaultStrength: EXTENSIBLE
  useOidAsCanonical: true
templateSelection:
  classificationTypes: []
  templateIds: []
emitInvariants: true
emitIrSnapshot: false
```

Config field notes:
- `naming.profilePrefix`, `naming.idPrefix`, `naming.titlePrefix` are global prefixes applied to all generated profiles.
- `naming.profileNameOverrides` and `naming.idOverrides` map BBR templateIds to explicit names/ids.
- `nullFlavorPolicy.forbiddenPaths` lists CDA element paths; each path gets `nullFlavor` forced to 0..0.
- `valueSetPolicy.oidToCanonical` maps ValueSet OIDs to canonical URLs; `useOidAsCanonical` falls back to `urn:oid:<oid>`.
- `valueSetPolicy.defaultStrength` applies when BBR does not specify binding strength.
- `templateSelection.classificationTypes` and `templateSelection.templateIds` filter templates; empty lists mean "all templates".
- `emitInvariants` toggles invariant FSH emission; `emitIrSnapshot` writes `axiom-cda-ir.json` to the output directory.
