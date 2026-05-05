# axiom-cda

`axiom-cda` is a Java 21 multi-module project that turns ART-DECOR BBR exports into FSH artifacts for CDA profiling.

The project now has two ways to use it:
- a CLI for batch or local generation
- a Quarkus web service that also serves the UI through Quinoa

The frontend source is embedded in the `axiom-cda-ws` module and is built as part of the Quarkus application. That means the backend and the UI are packaged together, deployed together, and shipped together in the same runnable artifact or Docker image.

## What It Does

The generator converts an ART-DECOR BBR (Building Block Repository) export into:
- FSH profiles that specialize HL7 CDA logical models from the `hl7.cda.uv.core` package
- FSH `ValueSet` and `CodeSystem` definitions extracted from the BBR `terminology` section
- a SUSHI-ready repository with `sushi-config.yaml` and `input/fsh`
- FHIR FSH generated from selected CDA IR plus a semantic mapping: built-in Observation mappings or an uploaded StructureMap JSON

The conversion pipeline is:

1. **BBR input**
   - Load the ART-DECOR XML export
   - Extract templates, constraints, and terminology
2. **Intermediate representation**
   - Normalize templates into a stable IR
   - Capture root CDA type, element paths, cardinalities, fixed values, bindings, includes, and invariants
3. **FSH generation**
   - Emit FSH CDA profiles from the IR
   - Emit ValueSets and CodeSystems from the terminology section
4. **Optional FHIR conversion**
   - Select a CDA IR template
   - Use a built-in Observation mapping or upload a StructureMap JSON
   - Generate best-effort FHIR FSH and optionally compile it with SUSHI

## Project Structure

- `axiom-cda-api`: shared API contracts, configuration, IR, and FSH model types
- `axiom-cda-engine`: BBR loading, IR transformation, and FSH generation
- `axiom-cda-cli`: Picocli command-line entry point
- `axiom-cda-ws`: Quarkus web service, REST API, and bundled Quinoa UI

## CDA Model Rules

Generated profiles are built on top of HL7 CDA R2 logical models from `hl7.cda.uv.core`.

Important rules:
- profiles use the matching `StructureDefinition/<CDAClass>` as their parent
- paths are normalized against the CDA snapshot
- cardinalities, bindings, and fixed values are clamped so the generated profile never weakens the base CDA constraints

## Output Layout

By default, the generator writes:
- `Resources<ProjectNameToken>/` for profile FSH files
- `Invariants/` for invariant FSH files
- `ValueSets/` for ValueSet FSH files
- `CodeSystems/` for CodeSystem FSH files

When `--sushi-repo` is enabled, output is written in SUSHI layout under `input/fsh`, and `sushi-config.yaml` is emitted.

When `--emit-ir` is enabled, an `axiom-cda-ir.json` snapshot is also written to the output directory.

When project ownership filtering is enabled, generated templates are tagged with an origin:
- `PROJECT`: owned by the project
- `REQUIRED_INCLUDE`: not owned, but required by a project-owned template
- `OTHER`: generated without ownership filtering

## Building the Project

Build everything:

```bash
mvn -q package
```

Build the runnable modules only:

```bash
mvn -q -pl axiom-cda-cli,axiom-cda-ws -am package
```

Build only the web service module:

```bash
mvn -q -pl axiom-cda-ws -am package
```

## Running the CLI

Run the CLI jar:

```bash
java -jar axiom-cda-cli/target/axiom-cda-cli-1.0-SNAPSHOT.jar generate \
  --bbr axiom-cda-engine/src/main/resources/head.xml \
  --out /tmp/axiom-cda-out
```

Run from classes while developing:

```bash
java -cp axiom-cda-cli/target/classes:axiom-cda-engine/target/classes:axiom-cda-api/target/classes \
  net.ihe.gazelle.axiomcda.cli.AxiomCdaCli generate \
  --bbr axiom-cda-engine/src/main/resources/head.xml \
  --out /tmp/axiom-cda-out
```

Common CLI options:

```bash
--bbr <path>                 Path to the BBR XML input
--out <dir>                  Output directory
--cda-package <dir>          Override the CDA package directory
--ans-reference <dir>        Optional ANS FSH path for comparison
--config <yaml>              Generation config override
--profile-prefix <s>         Prefix profile names
--id-prefix <s>              Prefix profile ids
--title-prefix <s>           Prefix profile titles
--resources-dir <s>          Output folder for profiles
--invariants-dir <s>         Output folder for invariants
--classification-types <csv>  Template classification filters
--template-ids <csv>         Explicit template ids to generate
--all-templates              Ignore classification filters and generate all
--project-plus-required-includes
                             Keep project-owned templates and required includes
--owned-repository-prefixes <csv>
                             Extra ART-DECOR idents considered project-owned
--sushi-repo                 Emit a SUSHI-ready repo
--ig-id <s>                  IG id for sushi-config.yaml
--ig-name <s>                IG name for sushi-config.yaml
--ig-title <s>               IG title for sushi-config.yaml
--ig-canonical <url>         IG canonical for sushi-config.yaml
--ig-version <s>             IG version for sushi-config.yaml
--ig-copyright-year <s>      IG copyrightYear for sushi-config.yaml
--ig-release-label <s>       IG releaseLabel for sushi-config.yaml
--emit-ir                    Emit axiom-cda-ir.json
```

## Running the Web Service and UI

The web module is a Quarkus application with Quinoa enabled. Quinoa builds the frontend from `axiom-cda-ws/src/main/webui`, exports it as static content, and packages it into the Quarkus runtime.

Run in dev mode:

```bash
mvn -f axiom-cda-ws/pom.xml quarkus:dev
```

The app listens on:
- `http://localhost:8080`

UI routes:
- `http://localhost:8080/axiom-cda/`
- `http://localhost:8080/axiom-cda` redirects to `/axiom-cda/`

API routes:
- `POST /api/generate`
- `POST /api/convert/fhir`
- `POST /api/convert/fhir/sushi`

Example request:

```bash
curl -X POST http://localhost:8080/api/generate \
  -H 'Content-Type: application/json' \
  -d '{
    "bbr": "<Decor>...</Decor>",
    "sushiRepo": true,
    "emitIr": false,
    "emitLogs": true,
    "projectPlusRequiredIncludes": true,
    "ownedRepositoryPrefixes": ["BBR-", "BIO-CR-BIO-"],
    "yamlConfig": null
  }'
```

The response contains:
- `zipBase64`: the generated archive as base64
- `report`: generation diagnostics and counts
- `profiles`: the generated FSH profile names and content
- `profiles[].templateOrigin`: `PROJECT`, `REQUIRED_INCLUDE`, or `OTHER`
- `irTemplates`: the IR templates when `emitIr` is enabled
- `irTemplates[].origin`: `PROJECT`, `REQUIRED_INCLUDE`, or `OTHER`

## Configuration

Generation can be customized with a YAML config file. CLI flags override config values when both are provided.

Example:

```bash
java ... AxiomCdaCli generate --bbr head.xml --out /tmp/out --config config.yaml
```

Relevant config sections:
- `naming`: prefixes and explicit template overrides
- `nullFlavorPolicy`: paths that must be forced to `0..0`
- `valueSetPolicy`: canonical URL mapping and default binding strength
- `templateSelection`: template filters and ownership filtering
- `emitInvariants` and `emitIrSnapshot`: output toggles

Example ownership filter:

```yaml
templateSelection:
  projectPlusRequiredIncludes: true
  ownedRepositoryPrefixes:
    - "BBR-"
    - "BIO-CR-BIO-"
```

## Known Limits

Some inputs are intentionally handled with warnings rather than hard failures:
- predicated elements and slicing are ignored
- choice elements are skipped
- some CDA attributes and paths may remain unmapped depending on the BBR structure

## Docker Image

The Docker image for `axiom-cda-ws` contains both the backend and the built UI, because Quinoa runs the frontend build during the Quarkus build and embeds the static export into the final application.

Use this flow:

1. Build the Quarkus module so Quinoa can compile the frontend and package the static assets:

```bash
mvn -q -pl axiom-cda-ws -am package -DskipTests
```

2. Build the JVM image from the Quarkus module directory:

```bash
docker build -f src/main/docker/Dockerfile.jvm -t axiom-cda-app:${VERSION} axiom-cda-ws
```

3. Run the image:

```bash
docker run -i --rm -p 8080:8080 axiom-cda-ws-jvm
```

The image serves:
- UI at `http://localhost:8080/axiom-cda/`
- API at `http://localhost:8080/api/generate`

The same package output can also be used with the other generated Quarkus Dockerfiles:
- `axiom-cda-ws/src/main/docker/Dockerfile.legacy-jar`
- `axiom-cda-ws/src/main/docker/Dockerfile.native`
- `axiom-cda-ws/src/main/docker/Dockerfile.native-micro`
