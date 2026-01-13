# End-to-end pipeline diagram

This diagram shows how the tool moves from BBR to IR to FSH, including the CDA package
and configuration inputs.

```plantuml
@startuml
left to right direction
skinparam shadowing false
skinparam componentStyle rectangle

rectangle "BBR XML (ART-DECOR)" as BBR
rectangle "JAXB Loader" as JAXB
rectangle "Decor Model" as DECOR
rectangle "Template Selection" as SELECT
rectangle "BbrToIrTransformer" as B2IR
rectangle "IR Templates + Diagnostics" as IR
rectangle "IrToFshGenerator" as IR2FSH
rectangle "Terminology Generator" as TERM
rectangle "FSH Profiles + Invariants" as FSH
rectangle "FSH ValueSets + CodeSystems" as VSCS
rectangle "FshBundle" as BUNDLE
rectangle "FshWriter" as WRITER
rectangle "Output Directory" as OUT
rectangle "SUSHI Repo (optional)" as SUSHI
rectangle "CDA Package" as CDA
rectangle "GenerationConfig" as CFG

BBR --> JAXB
JAXB --> DECOR
DECOR --> SELECT
SELECT --> B2IR
B2IR --> IR
IR --> IR2FSH
IR --> TERM
IR2FSH --> FSH
TERM --> VSCS
FSH --> BUNDLE
VSCS --> BUNDLE
BUNDLE --> WRITER
WRITER --> OUT
OUT --> SUSHI

CDA --> B2IR
CDA --> IR2FSH
CFG --> SELECT
CFG --> B2IR
CFG --> IR2FSH
CFG --> TERM
@enduml
```

Notes:
- The CDA package is used both for validating/normalizing paths (BBR -> IR) and for
  base constraints (IR -> FSH).
- Terminology generation comes from the BBR `terminology` section and is merged with
  profile FSH output.
- CLI options and YAML config influence selection, naming, bindings, and invariant emission.
