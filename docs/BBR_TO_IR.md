# BBR to IR transformation (axiom-cda)

This document explains how the CLI converts an ART-DECOR BBR export into the
Intermediate Representation (IR) used to generate FSH-CDA profiles.

Primary implementation:
`axiom-cda-engine/src/main/java/net/ihe/gazelle/axiomcda/engine/business/DefaultBbrToIrTransformer.java`.

## Inputs and outputs

Inputs:
- ART-DECOR BBR XML (Decor export).
- CDA model package (default: `axiom-cda-engine/src/main/resources/package`).
- Optional generation config (YAML and CLI overrides).

Output:
- A list of `IRTemplate` objects and diagnostics.
- Each `IRTemplate` has:
  - `id`, `name`, `displayName`, `description`
  - `rootCdaType`
  - `elements` (list of `IRElementConstraint`)
  - `includes` (list of `IRTemplateInclude`)
  - `invariants` (list of `IRInvariant`)

IR paths are relative to the root CDA type (for example, `recordTarget.patientRole.id`),
not prefixed with the root type itself.

## Step-by-step transformation

### 1) Collect templates and keep the latest version

From `Decor.rules.templateAssociationOrTemplate`, the transformer keeps only
`TemplateDefinition` entries and ignores inactive templates
(`RETIRED`, `CANCELLED`, `REJECTED`).

If multiple template versions share the same `templateId`, the latest by
`effectiveDate` is selected.

### 2) Template selection and include expansion

Selection follows `GenerationConfig.templateSelection`:
- If `templateIds` is non-empty, only those IDs are selected.
- Else if `classificationTypes` is non-empty, templates whose classification type matches
  are selected.
- Else, all templates are selected.

After the initial selection, the transformer expands includes: any template referenced
by `<include ref="...">` is added even if it was not explicitly selected.

### 3) Find root element and resolve CDA root type

Root rule selection:
- Prefer a rule whose element name maps to a CDA structure definition.
- Otherwise fall back to the first rule encountered.

Root element name parsing:
- Strips predicates (for example, `element[@x='y']`), but predicates are not supported
  and are reported as warnings.
- Removes namespace prefixes (`hl7:`).
- Removes attribute markers (`@`).
- `sdtc:` prefixed elements become `sdtc` + UpperFirst(elementName).

Root CDA type resolution:
- Uses aliases (for example, `assignedPerson` -> `Person`, `addr` -> `AD`, `telecom` -> `TEL`).
- Otherwise uses `UpperFirst(baseName)`.
- The CDA model package must contain a matching `StructureDefinition`; otherwise the
  template is skipped with an error diagnostic.

### 4) Path normalization against CDA

All rule and attribute paths are normalized against CDA snapshots so the IR stays
aligned with the CDA model:

- Paths are dot-delimited and relative to the root CDA type.
- `@` prefixes are removed (attributes become `path.attribute`).
- Each segment is resolved against the CDA snapshot, and the current CDA type is
  advanced to the element's type for the next segment.
- `item.<next>` segments are treated specially:
  - If `item.<next>` exists, the two segments are collapsed into one segment.
- Type names that differ only by `-` versus `_` are normalized during resolution.
- If any segment cannot be resolved, the element is skipped with a warning.

### 5) Map rules and attributes to `IRElementConstraint`

For each rule or attribute under the root:

1) Path
- Derived from rule nesting and normalized against CDA.

2) Cardinality
- Rules:
  - `minimumMultiplicity` and `maximumMultiplicity` map to `IRCardinality`.
  - If missing, `isMandatory=true` maps to `1..1`, else `0..1`.
- Includes:
  - Same logic as rules.
- Attributes:
  - `isProhibited=true` maps to `0..0`.
  - `isOptional=true` maps to `0..1`; `false` maps to `1..1`.

3) Datatype
- If present, the XML QName is stored as its local part in `datatype`.

4) Fixed value
- Only attribute values are used as fixed values.
- Fixed value type is inferred:
  - `BOOLEAN` when datatype is `BL/BOOLEAN/BOOL` or name ends with `Ind` or `Indicator`.
  - `CODE` when datatype is `CS/CE/CD/CV` or name is `code`, `classCode`, `moodCode`,
    `typeCode`, or ends with `Code`.
  - Otherwise `STRING`.

5) Short description
- Uses `desc` in the preferred language:
  - `Project.defaultLanguage` first.
  - Falls back to the first available description.

6) Vocabulary bindings
- Each `Vocabulary` entry yields an `IRBinding`:
  - ValueSet OID is resolved to a canonical URL:
    - If already a URL (`http` or `urn`), it is used as-is.
    - If mapped via `valueSetPolicy.oidToCanonical`, that mapping is used.
    - Else, when `useOidAsCanonical=true`, it becomes `urn:oid:<oid>`.
  - The code system reference is preserved from the BBR entry.
  - Binding strength:
    - `required` -> `REQUIRED`
    - `extensible` -> `EXTENSIBLE`
    - `preferred` or `example` -> `PREFERRED`
    - Missing strength uses `valueSetPolicy.defaultStrength`.
- If a ValueSet OID cannot be resolved, a warning is emitted.

Diagnostics in this step:
- Conflicting cardinalities on the same path (warning).
- Multiple bindings on the same path (warning, first wins).

### 6) Includes

Each `<include ref="...">` becomes an `IRTemplateInclude`:
- `path` is the normalized path of the included template's root element.
- `templateId` is the include reference.
- `cardinality` is derived from the include multiplicity.

If the included template cannot be found or mapped, a warning is emitted and the include
is skipped.

### 7) Invariants

Only count-based asserts are converted into `IRInvariant`:
- Pattern: `count(<path>) <op> <n>` where `<op>` is `=`, `>=`, `<=`, or `>`.
- The path is normalized (removes `//`, `hl7:`, `@`, predicates, and root prefixes).
- Expression becomes `<normalizedPath>.count() <op> <n>`.
- Severity is always `ERROR`.
- Invariant name: `<profilePrefix><rootCdaType>Inv<index>`.

Other invariant patterns are ignored (no IR output).

### 8) NullFlavor policy

If `nullFlavorPolicy.forbiddenPaths` is configured:
- Each path is converted to `path.nullFlavor` (unless it is `/`, which maps to `nullFlavor`).
- The normalized path gets cardinality forced to `0..0`.
- Unmapped paths produce warnings.

## Example mapping

BBR snippet (simplified):

```xml
<template id="1.2.3.4" name="ExampleHeader">
  <element name="ClinicalDocument">
    <element name="recordTarget" minimumMultiplicity="1" maximumMultiplicity="*">
      <element name="patientRole">
        <attribute name="@classCode" value="PAT" datatype="CS"/>
        <element name="id" minimumMultiplicity="1" maximumMultiplicity="1">
          <vocabulary valueSet="2.16.840.1.113883.1.11.20.12"
                      codeSystem="2.16.840.1.113883.5.1"/>
        </element>
      </element>
    </element>
  </element>
</template>
```

Resulting IR (illustrative):

```json
{
  "id": "1.2.3.4",
  "rootCdaType": "ClinicalDocument",
  "elements": [
    {
      "path": "recordTarget",
      "cardinality": { "min": 1, "max": "*" }
    },
    {
      "path": "recordTarget.patientRole.classCode",
      "fixedValue": "PAT",
      "fixedValueType": "CODE"
    },
    {
      "path": "recordTarget.patientRole.id",
      "cardinality": { "min": 1, "max": "1" },
      "bindings": [
        {
          "strength": "EXTENSIBLE",
          "valueSetRef": "urn:oid:2.16.840.1.113883.1.11.20.12",
          "codeSystemRef": "2.16.840.1.113883.5.1"
        }
      ]
    }
  ]
}
```

Notes:
- `@classCode` becomes `classCode`.
- The ValueSet OID is mapped to `urn:oid:<oid>` when no explicit mapping is provided.
- Binding strength defaults to `valueSetPolicy.defaultStrength` when not specified in the BBR.

## Diagnostics emitted

Examples of diagnostics produced during BBR to IR:
- `CDA structure definition not found for root type` (error)
- `No root element found` (warning)
- `Predicated element not supported (slicing skipped)` (warning)
- `Choice elements are not supported in v0` (warning)
- `Unmapped CDA path` / `Unmapped CDA attribute path` (warning)
- `Included template not found` (warning)
- `Unresolved ValueSet` / `Unmapped ValueSet OID` (warning)
- `Conflicting cardinalities` / `Multiple bindings found` (warning)

Templates with fatal errors (missing CDA root) are skipped.

## What IR does not include

- CodeSystem or ValueSet definitions from the BBR terminology section.
  These are generated separately from BBR `terminology`.
- Slicing and choice elements are not modeled (warnings only).
- CDA base cardinality and fixed value clamping happens later during FSH generation.

## How to export IR

You can emit the IR snapshot in JSON using:

```
java -jar axiom-cda-cli/target/axiom-cda-cli-1.0-SNAPSHOT.jar generate \
  --bbr <bbr.xml> \
  --out <output-dir> \
  --emit-ir
```

This writes `axiom-cda-ir.json` into the output directory.
