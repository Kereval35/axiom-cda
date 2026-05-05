# IR to FSH generation (axiom-cda)

This document explains how the Intermediate Representation (IR) is turned into
FSH-CDA profiles and invariants, and how Observation-root IR templates can also
feed the FHIR FSH conversion flow.

Primary implementation:
`axiom-cda-engine/src/main/java/net/ihe/gazelle/axiomcda/engine/business/DefaultIrToFshGenerator.java`.

## Inputs and outputs

Inputs:
- A list of `IRTemplate` objects (from the BBR to IR step).
- CDA model package (structure definitions + element metadata).
- `GenerationConfig` for naming and invariant emission.

Output:
- A `FshBundle` with:
  - `Resources/<ProfileName>.fsh` files
  - `Invariants/<InvariantName>.fsh` files (when enabled)
- Optional SUSHI repository layout (`sushi-config.yaml` + `input/fsh`)
- In the web service, optional FHIR FSH generated from CDA IR plus either a
  built-in Observation mapping or an uploaded StructureMap JSON

The CLI can remap `Resources/` and `Invariants/` to different folders
(`--resources-dir`, `--invariants-dir`) and can emit a SUSHI repo layout
(`--sushi-repo`).

When a SUSHI repo is emitted, the generated content is immediately consumable by SUSHI
without having to rearrange the output tree.

## Step-by-step generation

### 1) Resolve profile identity

For each `IRTemplate`:
- Profile name:
  - `naming.profileNameOverrides[templateId]` if present, else
  - `naming.profilePrefix + rootCdaType`.
- Profile id:
  - `naming.idOverrides[templateId]` if present, else
  - `naming.idPrefix + kebab-case(rootCdaType)`.
- Title: `naming.titlePrefix + lowerFirst(rootCdaType)`.
- Description: `template.description` if present, else `displayName`, else `name`.

### 2) Resolve CDA base

`rootCdaType` is looked up in the CDA package. If not found, the template is skipped.
The CDA StructureDefinition URL becomes the FSH `Parent`.

### 3) Build the FSH header

Each profile starts with:
- `Profile: <ProfileName>`
- `Parent: <CDA StructureDefinition URL>`
- `Id: <ProfileId>`
- `Title: "<Title>"`
- `Description: "<Description>"`
- `* ^status = #draft`

### 4) Apply element constraints

Element constraints and includes are merged by path and emitted in sorted order.

For each path:

1) Cardinality
- If the IR specifies cardinality, it is clamped to the CDA base:
  - `min` is raised to the CDA minimum if needed.
  - `max` is lowered to the CDA maximum if needed.
  - `max` is never less than `min`.
- The resulting cardinality is emitted as `* <path> <min>..<max>`.

2) Fixed values
- Fixed values are emitted only if the CDA base element does not already have a fixed value.
- Formatting:
  - `CODE` -> `#code`
  - `STRING` -> `"value"`
  - `BOOLEAN` -> `true` or `false` (also accepts `1`/`0`).
- If the IR does not specify a fixed value type and the CDA base element is boolean,
  the value is emitted as boolean.

3) ValueSet bindings
- Bindings are emitted as:
  `* <path> from <valueSet> (required|extensible|preferred)`
- If the CDA base element has a stronger binding strength, the output is **not weakened**.
  The effective strength is the stronger of base vs requested.

4) Short description
- If provided, emitted as:
  `* <path> ^short = "<description>"`

### 5) Includes to `only` constraints

Each `IRTemplateInclude` is translated to:
- `* <path> only <TargetProfileName>`

The `only` constraint is emitted only when:
- The target template exists.
- The target root CDA type is compatible with the allowed types of the base element
  (based on the CDA snapshot).

### 6) Invariants

If `emitInvariants` is enabled:
- Each template emits `* obeys <InvariantName>`.
- Invariants are de-duplicated globally and written to
  `Invariants/<InvariantName>.fsh` with description, severity, and expression.

### 7) SUSHI repository emission

When `--sushi-repo` is enabled, the generator writes:
- `sushi-config.yaml`
- `input/fsh/Resources...`
- `input/fsh/Invariants...`
- `input/fsh/ValueSets...`
- `input/fsh/CodeSystems...`

The web service uses the same layout to support SUSHI compilation flows for generated
FHIR FSH.

### 8) CDA IR to FHIR FSH

The web service exposes a second generation path for FHIR profiles:

1. Generate CDA IR from the BBR.
2. Select a CDA IR template.
3. Use a built-in Observation mapping or upload a StructureMap JSON.
4. Build a FHIR profile from the IR plus the semantic mapping.
5. Optionally compile the generated FSH with SUSHI.

Observation keeps a specialized projection path. Other CDA root types use a generic
best-effort projection and require an uploaded StructureMap.

## Example mapping

IR input (simplified):

```json
{
  "id": "1.2.3.4",
  "rootCdaType": "ClinicalDocument",
  "elements": [
    { "path": "recordTarget", "cardinality": { "min": 1, "max": "*" } },
    { "path": "recordTarget.patientRole.classCode",
      "fixedValue": "PAT", "fixedValueType": "CODE" },
    { "path": "recordTarget.patientRole.id",
      "cardinality": { "min": 1, "max": "1" },
      "bindings": [
        { "strength": "EXTENSIBLE",
          "valueSetRef": "urn:oid:2.16.840.1.113883.1.11.20.12",
          "codeSystemRef": "2.16.840.1.113883.5.1" }
      ]
    }
  ]
}
```

FSH output (illustrative):

```
Profile: ClinicalDocument
Parent: <CDA StructureDefinition URL>
Id: clinical-document
Title: "clinicalDocument"
Description: "ExampleHeader"
* ^status = #draft
* recordTarget 1..*
* recordTarget.patientRole.classCode = #PAT
* recordTarget.patientRole.id 1..1
* recordTarget.patientRole.id from urn:oid:2.16.840.1.113883.1.11.20.12 (extensible)
```

Notes:
- The `Parent` URL is taken from the CDA package, not hardcoded.
- The output strength can be elevated if the CDA base binding is stronger.

## Related web service endpoints

- `POST /api/generate`: CDA FSH generation from BBR
- `POST /api/convert/fhir`: FHIR FSH generation from CDA IR + semantic mapping
- `POST /api/convert/fhir/sushi`: SUSHI compilation of generated FHIR FSH
