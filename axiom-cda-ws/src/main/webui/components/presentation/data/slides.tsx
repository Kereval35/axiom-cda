import { Slide } from '@/types/presentation/types';
import { Callout } from '@/components/presentation/ui/Callout';
import { DecisionCard } from '@/components/presentation/ui/DecisionCard';
import { RuleCard } from '@/components/presentation/ui/RuleCard';
import { CodeBlock } from '@/components/presentation/ui/CodeBlock';
import { FlowDiagram } from '@/components/presentation/ui/FlowDiagram';
import { Badge } from '@/components/presentation/ui/Badge';
import { Language } from '@/translations/i18n';
import { slideTranslations } from '@/components/presentation/data/presentationTranslations';

export const getSlides = (language: Language): Slide[] => {
  const t = slideTranslations[language] ?? slideTranslations.en;

  return [
  // Slide 1 - Titre
  {
    id: 'slide-1',
    title: t.slide1.title,
    subtitle: t.slide1.subtitle,
    content: [
      <div key="intro" className="space-y-8 text-center">
        <p className="text-2xl text-slate-700 dark:text-slate-300">
          {t.slide1.description}
        </p>

        <div className="my-8">
          <FlowDiagram />
        </div>

        <div className="mt-12 pt-8 border-t border-slate-300 dark:border-slate-600">
          <p className="text-lg text-slate-600 dark:text-slate-400">{t.slide1.presentedBy}</p>
          <p className="text-3xl font-bold text-blue-700 dark:text-blue-300 mt-2">{t.slide1.author}</p>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-2">{t.slide1.date}</p>
        </div>
      </div>
    ],
    fragments: [],
    stepsCount: 0,
  },

  // Slide 2 - Problème
  {
    id: 'slide-2',
    title: t.slide2.title,
    subtitle: t.slide2.subtitle,
    content: [],
    fragments: [
      {
        step: 1,
        type: 'text',
        content: (
          <div className="space-y-4">
            <h3 className="text-2xl font-semibold text-slate-800 dark:text-slate-200">{t.slide2.currentPain.title}</h3>
            <ul className="list-disc list-inside space-y-2 text-xl text-slate-700 dark:text-slate-300">
              <li>{t.slide2.currentPain.manual}</li>
              <li>{t.slide2.currentPain.notScalable}</li>
              <li>{t.slide2.currentPain.duplication}</li>
            </ul>
          </div>
        ),
      },
      {
        step: 2,
        type: 'text',
        content: (
          <div className="space-y-4">
            <h3 className="text-2xl font-semibold text-slate-800 dark:text-slate-200">{t.slide2.maintenanceImpact.title}</h3>
            <ul className="list-disc list-inside space-y-2 text-xl text-slate-700 dark:text-slate-300">
              <li>{t.slide2.maintenanceImpact.sync}</li>
              <li>{t.slide2.maintenanceImpact.drift}</li>
              <li>{t.slide2.maintenanceImpact.tracking}</li>
            </ul>
          </div>
        ),
      },
      {
        step: 3,
        type: 'callout',
        content: (
          <Callout variant="decision">
            {t.slide2.solution}
          </Callout>
        ),
      },
    ],
    stepsCount: 3,
  },

  {
    id: 'slide-3',
    title: t.slide3.title,
    content: [],
    fragments: [
      {
        step: 1,
        type: 'text',
        content: (
          <div className="grid grid-cols-2 gap-8">
            <div>
              <h3 className="text-2xl font-semibold text-orange-700 dark:text-orange-300 mb-4 flex items-center gap-2">
                <Badge variant="bbr">{t.slide3.inputs}</Badge>
              </h3>
              <ul className="space-y-3 text-lg text-slate-700 dark:text-slate-300">
                <li>{t.slide3.bbrXml}</li>
                <li>{t.slide3.cdaPackage}</li>
                <li>{t.slide3.yamlConfig}</li>
              </ul>
            </div>
            <div>
              <h3 className="text-2xl font-semibold text-green-700 dark:text-green-300 mb-4 flex items-center gap-2">
                <Badge variant="fsh">{t.slide3.outputs}</Badge>
              </h3>
              <ul className="space-y-3 text-lg text-slate-700 dark:text-slate-300">
                <li>{t.slide3.profiles}</li>
                <li>{t.slide3.invariants}</li>
                <li>{t.slide3.terminologies}</li>
                <li>{t.slide3.fhirProfiles}</li>
              </ul>
            </div>
          </div>
        ),
      },
      {
        step: 2,
        type: 'text',
        content: (
          <div className="mt-6">
            <h3 className="text-xl font-semibold text-blue-700 dark:text-blue-300 mb-3">{t.slide3.cliOptions}</h3>
            <div className="grid grid-cols-2 gap-4 text-base">
              <code className="bg-slate-100 dark:bg-slate-700 px-3 py-2 rounded">--sushi-repo</code>
              <code className="bg-slate-100 dark:bg-slate-700 px-3 py-2 rounded">--emit-ir</code>
              <code className="bg-slate-100 dark:bg-slate-700 px-3 py-2 rounded">--profile-prefix</code>
              <code className="bg-slate-100 dark:bg-slate-700 px-3 py-2 rounded">--config</code>
            </div>
          </div>
        ),
      },
      {
        step: 3,
        type: 'callout',
        content: <Callout variant="info">{t.slide3.irInfo}</Callout>,
      },
    ],
    stepsCount: 3,
  },

  {
    id: 'slide-4',
    title: t.slide4.title,
    subtitle: t.slide4.subtitle,
    content: [],
    fragments: [
      {
        step: 1,
        type: 'decision',
        content: (
          <DecisionCard title={t.slide4.separation.title}>
            <p>{t.slide4.separation.description1}</p>
            <p className="mt-2">{t.slide4.separation.description2}</p>
          </DecisionCard>
        ),
      },
      {
        step: 2,
        type: 'decision',
        content: (
          <DecisionCard title={t.slide4.stability.title}>
            <p>{t.slide4.stability.description1}</p>
            <p className="mt-2">{t.slide4.stability.description2}</p>
          </DecisionCard>
        ),
      },
      {
        step: 3,
        type: 'decision',
        content: (
          <DecisionCard title={t.slide4.auditability.title}>
            <p>{t.slide4.auditability.description1}</p>
            <p className="mt-2">{t.slide4.auditability.description2}</p>
          </DecisionCard>
        ),
      },
    ],
    stepsCount: 3,
  },

  // Slide 5 - Structure IR
  {
    id: 'slide-5',
    title: t.slide5.title,
    subtitle: t.slide5.subtitle,
    content: [],
    fragments: [
      {
        step: 1,
        type: 'code',
        content: (
          <CodeBlock language="typescript">
            {`IRTemplate {
  id: string
  name: string
  displayName: string
  rootCdaType: string  // ex: "ClinicalDocument"
  elements: IRElementConstraint[]
  includes: IRTemplateInclude[]
  invariants: IRInvariant[]
}`}
          </CodeBlock>
        ),
      },
      {
        step: 2,
        type: 'code',
        content: (
          <CodeBlock language="typescript">
            {`IRElementConstraint {
  path: string              // ex: "recordTarget.patientRole.id"
  cardinality: { min, max }
  datatype?: string
  fixedValue?: string
  fixedValueType?: "CODE" | "STRING" | "BOOLEAN"
  bindings: IRBinding[]
}`}
          </CodeBlock>
        ),
      },
      {
        step: 3,
        type: 'code',
        content: (
          <CodeBlock language="typescript">
            {`IRBinding {
  strength: "REQUIRED" | "EXTENSIBLE" | "PREFERRED"
  valueSetRef: string       // canonical URL ou urn:oid:...
  codeSystemRef?: string
}`}
          </CodeBlock>
        ),
      },
    ],
    stepsCount: 3,
  },

  {
    id: 'slide-6',
    title: t.slide6.title,
    content: [],
    fragments: [
      {
        step: 1,
        type: 'rule',
        content: (
          <RuleCard
            condition={t.slide6.rules.ignoreStatus.condition}
            result={t.slide6.rules.ignoreStatus.result}
          />
        ),
      },
      {
        step: 2,
        type: 'rule',
        content: (
          <RuleCard
            condition={t.slide6.rules.latestVersion.condition}
            result={t.slide6.rules.latestVersion.result}
          />
        ),
      },
      {
        step: 3,
        type: 'rule',
        content: (
          <RuleCard
            condition={t.slide6.rules.configFilter.condition}
            result={t.slide6.rules.configFilter.result}
          />
        ),
      },
      {
        step: 4,
        type: 'rule',
        content: (
          <RuleCard
            condition={t.slide6.rules.ownershipFilter.condition}
            result={t.slide6.rules.ownershipFilter.result}
          />
        ),
      },
      {
        step: 5,
        type: 'callout',
        content: (
          <Callout variant="info">{t.slide6.includeCallout}</Callout>
        ),
      },
    ],
    stepsCount: 5,
  },

  // Slide 7 - BBR→IR: Root & rootCdaType
  {
    id: 'slide-7',
    title: 'BBR → IR : Root & rootCdaType',
    content: [],
    fragments: [
      {
        step: 1,
        type: 'text',
        content: (
          <div className="space-y-3">
            <h3 className="text-xl font-semibold">Choix de la racine</h3>
            <RuleCard
              condition="Élément racine trouvé dont le nom mappe vers un SD CDA"
              result="✓ Utiliser cet élément comme root"
            />
            <p className="text-lg text-slate-600">Sinon : utiliser le premier élément rencontré</p>
          </div>
        ),
      },
      {
        step: 2,
        type: 'text',
        content: (
          <div className="space-y-3">
            <h3 className="text-xl font-semibold">Normalisation du nom</h3>
            <ul className="list-disc list-inside space-y-2 text-lg">
              <li>Suppression de <code>hl7:</code>, <code>@</code>, prédicats</li>
              <li>Transformation <code>sdtc:</code> → <code>sdtc</code> + UpperFirst du nom</li>
            </ul>
          </div>
        ),
      },
      {
        step: 3,
        type: 'code',
        content: (
          <div>
            <h3 className="text-lg font-semibold mb-3">Exemples de transformation sdtc:</h3>
            <CodeBlock language="text">
              {`sdtc:telecom       → sdtcTelecom
sdtc:birthTime     → sdtcBirthTime
sdtc:deceasedInd   → sdtcDeceasedInd`}
            </CodeBlock>
          </div>
        ),
      },
      {
        step: 4,
        type: 'text',
        content: (
          <div className="space-y-3">
            <h3 className="text-xl font-semibold">Résolution du type CDA via alias + UpperFirst</h3>
            <CodeBlock language="text">
              {`assignedPerson → Person    (alias)
addr           → AD         (alias)
telecom        → TEL        (alias)
patientRole    → PatientRole (UpperFirst directement)`}
            </CodeBlock>
          </div>
        ),
      },
      {
        step: 5,
        type: 'callout',
        content: (
          <Callout variant="warning">
            Si le StructureDefinition CDA est introuvable → template ignoré + diagnostic ERROR
          </Callout>
        ),
      },
    ],
    stepsCount: 5,
  },

  // Slide 8 - BBR→IR: Normalisation chemins
  {
    id: 'slide-8',
    title: 'BBR → IR : Normalisation des chemins',
    content: [],
    fragments: [
      {
        step: 1,
        type: 'text',
        content: (
          <div className="space-y-3">
            <h3 className="text-xl font-semibold">Principes</h3>
            <ul className="list-disc list-inside space-y-2 text-lg">
              <li>Dot-path relatif au root</li>
              <li>Résolution segment par segment contre snapshot CDA</li>
              <li>Règle <code>item.&lt;next&gt;</code> : collapse en un seul segment</li>
              <li>Normalisation <code>-</code> vs <code>_</code></li>
            </ul>
          </div>
        ),
      },
      {
        step: 2,
        type: 'code',
        content: (
          <div>
            <h3 className="text-lg font-semibold mb-3">Exemple 1 : Path simple</h3>
            <CodeBlock language="xml">
              {`<!-- BBR -->
<element name="recordTarget">
  <element name="patientRole">
    <element name="id" minimumMultiplicity="1"/>
  </element>
</element>

<!-- IR path normalisé -->
recordTarget.patientRole.id`}
            </CodeBlock>
          </div>
        ),
      },
      {
        step: 3,
        type: 'code',
        content: (
          <div>
            <h3 className="text-lg font-semibold mb-3">Exemple 2 : Collapse item.next</h3>
            <CodeBlock language="xml">
              {`<!-- BBR : entryRelationship.observation.value -->
<!-- Si "value" a un type item.<typeCode> dans CDA -->
entryRelationship.observation.value.item.CD

<!-- IR path après collapse -->
entryRelationship.observation.value.CD`}
            </CodeBlock>
            <p className="text-sm text-slate-600 dark:text-slate-400 mt-2">
              Les segments <code>item.&lt;next&gt;</code> sont fusionnés automatiquement
            </p>
          </div>
        ),
      },
      {
        step: 4,
        type: 'code',
        content: (
          <div>
            <h3 className="text-lg font-semibold mb-3">Exemple 3 : Normalisation tirets</h3>
            <CodeBlock language="text">
              {`<!-- Si CDA définit effective-time mais BBR a effectiveTime -->
effectiveTime  → effective-time  (matching flexible)`}
            </CodeBlock>
          </div>
        ),
      },
      {
        step: 5,
        type: 'callout',
        content: (
          <Callout variant="warning">
            Si un segment ne peut pas être résolu → élément ignoré + warning &quot;Unmapped CDA path&quot;
          </Callout>
        ),
      },
    ],
    stepsCount: 5,
  },

  // Slide 9 - BBR→IR: Cardinalités
  {
    id: 'slide-9',
    title: 'BBR → IR : Cardinalités',
    content: [],
    fragments: [
      {
        step: 1,
        type: 'rule',
        content: (
          <RuleCard
            condition="minimumMultiplicity et maximumMultiplicity présents"
            result="✓ Mapping direct vers IRCardinality { min, max }"
          />
        ),
      },
      {
        step: 2,
        type: 'rule',
        content: (
          <RuleCard
            condition="Cardinalités manquantes mais isMandatory=true"
            result="→ 1..1"
          />
        ),
      },
      {
        step: 3,
        type: 'rule',
        content: (
          <RuleCard
            condition="Attributs : isProhibited / isOptional"
            result={
              <div>
                <p><code>isProhibited=true</code> → 0..0</p>
                <p><code>isOptional=true</code> → 0..1</p>
                <p><code>isOptional=false</code> → 1..1</p>
              </div>
            }
          />
        ),
      },
      {
        step: 4,
        type: 'callout',
        content: (
          <Callout variant="warning">
            Conflits de cardinalités sur le même path → warning émis
          </Callout>
        ),
      },
    ],
    stepsCount: 4,
  },

  // Slide 10 - BBR→IR: Fixed values
  {
    id: 'slide-10',
    title: 'BBR → IR : Fixed values & types',
    content: [],
    fragments: [
      {
        step: 1,
        type: 'text',
        content: (
          <div className="space-y-3">
            <h3 className="text-xl font-semibold">Inférence du type de fixed value</h3>
            <p className="text-lg">Principalement à partir des attributs XML</p>
          </div>
        ),
      },
      {
        step: 2,
        type: 'rule',
        content: (
          <RuleCard
            condition="datatype = BL/BOOLEAN/BOOL OU nom se termine par 'Ind' ou 'Indicator'"
            result="→ BOOLEAN"
          />
        ),
      },
      {
        step: 3,
        type: 'rule',
        content: (
          <RuleCard
            condition="datatype = CS/CE/CD/CV OU nom = code/classCode/moodCode/typeCode/...Code"
            result="→ CODE"
          />
        ),
      },
      {
        step: 4,
        type: 'code',
        content: (
          <CodeBlock language="xml">
            {`<attribute name="@classCode" value="PAT" datatype="CS"/>
<attribute name="@moodCode" value="EVN" datatype="CS"/>

→ IR: fixedValue="PAT", fixedValueType=CODE`}
          </CodeBlock>
        ),
      },
    ],
    stepsCount: 4,
  },

  // Slide 11 - BBR→IR: Bindings
  {
    id: 'slide-11',
    title: 'BBR → IR : Bindings terminologiques',
    content: [],
    fragments: [
      {
        step: 1,
        type: 'text',
        content: (
          <div className="space-y-3">
            <h3 className="text-xl font-semibold">Mapping OID → Canonical</h3>
            <RuleCard
              condition="ValueSet OID est déjà une URL/URN"
              result="✓ Garder tel quel"
            />
          </div>
        ),
      },
      {
        step: 2,
        type: 'rule',
        content: (
          <RuleCard
            condition="Mapping défini dans valueSetPolicy.oidToCanonical"
            result="✓ Utiliser le mapping configuré"
          />
        ),
      },
      {
        step: 3,
        type: 'rule',
        content: (
          <RuleCard
            condition="useOidAsCanonical=true"
            result={<span>→ <code>urn:oid:&lt;oid&gt;</code></span>}
          />
        ),
      },
      {
        step: 4,
        type: 'text',
        content: (
          <div className="space-y-3">
            <h3 className="text-xl font-semibold">Strength mapping</h3>
            <ul className="list-disc list-inside space-y-2 text-base">
              <li><code>required</code> → REQUIRED</li>
              <li><code>extensible</code> → EXTENSIBLE</li>
              <li><code>preferred</code> ou <code>example</code> → PREFERRED</li>
              <li>Par défaut → <code>valueSetPolicy.defaultStrength</code></li>
            </ul>
          </div>
        ),
      },
      {
        step: 5,
        type: 'callout',
        content: (
          <Callout variant="warning">
            Multi-bindings sur un même path → warning (le premier gagne)
          </Callout>
        ),
      },
    ],
    stepsCount: 5,
  },

  // Slide 12 - BBR→IR: Invariants
  {
    id: 'slide-12',
    title: 'BBR → IR : Invariants',
    subtitle: 'Contraintes FSH attachées aux profils CDA',
    content: [],
    fragments: [
      {
        step: 1,
        type: 'text',
        content: (
          <div className="space-y-3">
            <h3 className="text-xl font-semibold">Qu&apos;est-ce qu&apos;un invariant ?</h3>
            <p className="text-lg">
              Ce sont des <strong>contraintes FSH générées</strong> et attachées aux profils CDA.
            </p>
            <ul className="list-disc list-inside space-y-2 text-base text-slate-700 dark:text-slate-300">
              <li>Proviennent des règles BBR <code>&lt;assert&gt;</code></li>
              <li>Émis comme définitions FSH <code>Invariant:</code></li>
              <li>Référencés dans les profils via <code>* obeys &lt;InvariantName&gt;</code></li>
            </ul>
          </div>
        ),
      },
      {
        step: 2,
        type: 'text',
        content: (
          <div className="space-y-3">
            <h3 className="text-xl font-semibold">Pattern supporté (v0)</h3>
            <p className="text-lg">Uniquement les patterns <strong>count-based</strong></p>
            <CodeBlock language="xpath">
              {`count(<path>) <op> <n>

où <op> ∈ {=, >=, <=, >}`}
            </CodeBlock>
          </div>
        ),
      },
      {
        step: 3,
        type: 'code',
        content: (
          <div>
            <h3 className="text-lg font-semibold mb-3">Exemple de transformation</h3>
            <CodeBlock language="xpath">
              {`<!-- BBR Assert -->
count(//participant) >= 1
count(recordTarget/patientRole/id) = 1

<!-- IR Expression (normalisé) -->
participant.count() >= 1
recordTarget.patientRole.id.count() = 1`}
            </CodeBlock>
          </div>
        ),
      },
      {
        step: 4,
        type: 'text',
        content: (
          <div className="space-y-3">
            <h3 className="text-xl font-semibold">Métadonnées des invariants</h3>
            <ul className="list-disc list-inside space-y-2 text-base">
              <li><strong>Severity :</strong> toujours ERROR</li>
              <li><strong>Nom :</strong> <code>&lt;profilePrefix&gt;&lt;rootCdaType&gt;Inv&lt;index&gt;</code></li>
              <li><strong>Déduplication :</strong> globale, écrits dans <code>Invariants/</code></li>
            </ul>
          </div>
        ),
      },
      {
        step: 5,
        type: 'callout',
        content: (
          <Callout variant="info">
            Quand <code>emitInvariants</code> est activé, chaque template émet <code>* obeys &lt;InvariantName&gt;</code>
          </Callout>
        ),
      },
      {
        step: 6,
        type: 'callout',
        content: (
          <Callout variant="warning">
            <strong>Limitation assumée :</strong> Tout autre pattern d&apos;invariant est ignoré avec warning
          </Callout>
        ),
      },
    ],
    stepsCount: 6,
  },

  // Slide 13 - NullFlavor policy
  {
    id: 'slide-13',
    title: 'NullFlavor Policy',
    subtitle: 'Décision fonctionnelle',
    content: [],
    fragments: [
      {
        step: 1,
        type: 'decision',
        content: (
          <DecisionCard title="Pourquoi ?">
            Certains éléments critiques ne doivent jamais avoir de nullFlavor (identifiants patients, codes obligatoires, etc.)
          </DecisionCard>
        ),
      },
      {
        step: 2,
        type: 'text',
        content: (
          <div className="space-y-3">
            <h3 className="text-xl font-semibold">Comment ?</h3>
            <RuleCard
              condition="Liste de paths définie dans config"
              result={
                <div>
                  <p>Pour chaque path → génération de <code>path.nullFlavor 0..0</code></p>
                  <p className="mt-2 text-sm">Path non mappable → warning</p>
                </div>
              }
            />
          </div>
        ),
      },
      {
        step: 3,
        type: 'callout',
        content: (
          <Callout variant="decision">
            <strong>Gouvernance :</strong> Liste maintenue dans le fichier de configuration YAML
          </Callout>
        ),
      },
    ],
    stepsCount: 3,
  },

  // Slide 14 - IR→FSH: Identité profil
  {
    id: 'slide-14',
    title: 'IR → FSH : Identité du profil',
    content: [],
    fragments: [
      {
        step: 1,
        type: 'text',
        content: (
          <div className="space-y-3">
            <h3 className="text-xl font-semibold">Name / Id / Title / Description</h3>
            <ul className="list-disc list-inside space-y-2 text-lg">
              <li><strong>Name:</strong> <code>profilePrefix + rootCdaType</code></li>
              <li><strong>Id:</strong> <code>idPrefix + kebab-case(rootCdaType)</code></li>
              <li><strong>Title:</strong> <code>titlePrefix + lowerFirst(rootCdaType)</code></li>
              <li><strong>Description:</strong> depuis template.description</li>
            </ul>
          </div>
        ),
      },
      {
        step: 2,
        type: 'callout',
        content: (
          <Callout variant="info">
            Overrides possibles via <code>naming.profileNameOverrides[templateId]</code>
          </Callout>
        ),
      },
      {
        step: 3,
        type: 'code',
        content: (
          <CodeBlock language="fsh">
            {`Profile: ClinicalDocument
Parent: http://hl7.org/cda/stds/core/StructureDefinition/ClinicalDocument
Id: clinical-document
Title: "clinicalDocument"
Description: "Profil pour le document clinique"
* ^status = #draft`}
          </CodeBlock>
        ),
      },
    ],
    stepsCount: 3,
  },

  {
    id: 'slide-15',
    title: t.slide15.title,
    subtitle: t.slide15.subtitle,
    content: [],
    fragments: [
      {
        step: 1,
        type: 'decision',
        content: (
          <DecisionCard title={t.slide15.problem.title}>
            {t.slide15.problem.description}
          </DecisionCard>
        ),
      },
      {
        step: 2,
        type: 'text',
        content: (
          <div className="space-y-3">
            <h3 className="text-xl font-semibold">{t.slide15.cardinalities.title}</h3>
            <ul className="list-disc list-inside space-y-2 text-lg">
              <li>{t.slide15.cardinalities.min}</li>
              <li>{t.slide15.cardinalities.max}</li>
              <li>{t.slide15.cardinalities.guarantee}</li>
            </ul>
          </div>
        ),
      },
      {
        step: 3,
        type: 'text',
        content: (
          <div className="space-y-3">
            <h3 className="text-xl font-semibold">{t.slide15.fixedValues.title}</h3>
            <RuleCard
              condition={t.slide15.fixedValues.baseHas}
              result={t.slide15.fixedValues.notEmit}
            />
          </div>
        ),
      },
      {
        step: 4,
        type: 'text',
        content: (
          <div className="space-y-3">
            <h3 className="text-xl font-semibold">{t.slide15.bindings.title}</h3>
            <p className="text-lg">
              {t.slide15.bindings.effective}
            </p>
            <p className="text-base text-slate-600">
              {t.slide15.bindings.cannotWeaken}
            </p>
          </div>
        ),
      },
    ],
    stepsCount: 4,
  },

  // Slide 16 - IR→FSH: Includes
  {
    id: 'slide-16',
    title: 'IR → FSH : Includes → only',
    content: [],
    fragments: [
      {
        step: 1,
        type: 'text',
        content: (
          <div className="space-y-3">
            <h3 className="text-xl font-semibold">Principe</h3>
            <p className="text-lg">
              <code>IRTemplateInclude</code> → <code>* &lt;path&gt; only &lt;TargetProfile&gt;</code>
            </p>
          </div>
        ),
      },
      {
        step: 2,
        type: 'code',
        content: (
          <CodeBlock language="fsh">
            {`// Exemple
* component.section only FRCoreImmunizationSection
* author only FRCorePractitionerRole`}
          </CodeBlock>
        ),
      },
      {
        step: 3,
        type: 'rule',
        content: (
          <RuleCard
            condition="Type cible compatible avec les types autorisés (snapshot CDA)"
            result="✓ Émission du constraint 'only'"
          />
        ),
      },
      {
        step: 4,
        type: 'callout',
        content: (
          <Callout variant="warning">
            Si incompatibilité → pas de <code>only</code> + warning
          </Callout>
        ),
      },
    ],
    stepsCount: 4,
  },

  // Slide 17 - IR→FSH: Invariants
  {
    id: 'slide-17',
    title: 'IR → FSH : Invariants & obeys',
    content: [],
    fragments: [
      {
        step: 1,
        type: 'text',
        content: (
          <div className="space-y-3">
            <h3 className="text-xl font-semibold">Émission des invariants</h3>
            <p className="text-lg">
              Si <code>emitInvariants</code> activé
            </p>
          </div>
        ),
      },
      {
        step: 2,
        type: 'code',
        content: (
          <CodeBlock language="fsh">
            {`// Dans le profil
* obeys ClinicalDocumentInv1
* obeys ClinicalDocumentInv2

// Fichier Invariants/ClinicalDocumentInv1.fsh
Invariant: ClinicalDocumentInv1
Description: "Au moins un participant requis"
Severity: #error
Expression: "participant.count() >= 1"`}
          </CodeBlock>
        ),
      },
      {
        step: 3,
        type: 'callout',
        content: (
          <Callout variant="success">
            <strong>Valeur :</strong> Règles réutilisables, lisibles et déduplication globale
          </Callout>
        ),
      },
    ],
    stepsCount: 3,
  },

  {
    id: 'slide-18',
    title: t.slide18.title,
    content: [],
    fragments: [
      {
        step: 1,
        type: 'text',
        content: (
          <div className="space-y-3">
            <h3 className="text-xl font-semibold">{t.slide18.structure}</h3>
            <ul className="list-disc list-inside space-y-2 text-lg font-mono text-slate-700 dark:text-slate-300">
              <li>Resources&lt;ProjectToken&gt;/ → profils</li>
              <li>Invariants/ → invariants</li>
              <li>ValueSets/ → value sets</li>
              <li>CodeSystems/ → code systems</li>
            </ul>
          </div>
        ),
      },
      {
        step: 2,
        type: 'text',
        content: (
          <div className="space-y-4">
            <h3 className="text-xl font-semibold">{t.slide18.namingOptions}</h3>
            <div className="grid grid-cols-2 gap-4 text-base font-mono">
              {[
                ['--profile-prefix', t.slide18.naming.profilePrefix],
                ['--id-prefix', t.slide18.naming.idPrefix],
              ].map(([flag, description]) => (
                <div key={flag} className="flex flex-col gap-1">
                  <code className="bg-slate-100 dark:bg-slate-700 px-3 py-2 rounded">{flag}</code>
                  <span className="text-xs italic text-slate-500 dark:text-slate-400 font-sans">{description}</span>
                </div>
              ))}
            </div>
          </div>
        ),
      },
      {
        step: 3,
        type: 'text',
        content: (
          <div className="space-y-4">
            <h3 className="text-xl font-semibold">{t.slide18.selectionOptions}</h3>
            <div className="grid grid-cols-2 gap-4 text-base font-mono">
              {[
                ['--template-ids', t.slide18.selection.templateIds],
                ['--classification-types', t.slide18.selection.classificationTypes],
                ['--project-plus-required-includes', t.slide18.selection.projectOwnership],
                ['--owned-repository-prefixes', t.slide18.selection.ownedPrefixes],
              ].map(([flag, description]) => (
                <div key={flag} className="flex flex-col gap-1">
                  <code className="bg-slate-100 dark:bg-slate-700 px-3 py-2 rounded">{flag}</code>
                  <span className="text-xs italic text-slate-500 dark:text-slate-400 font-sans">{description}</span>
                </div>
              ))}
            </div>
          </div>
        ),
      },
      {
        step: 4,
        type: 'text',
        content: (
          <div className="space-y-4">
            <h3 className="text-xl font-semibold">{t.slide18.outputOptions}</h3>
            <div className="grid grid-cols-2 gap-4 text-base font-mono">
              {[
                ['--sushi-repo', t.slide18.output.sushiRepo],
                ['--emit-ir', t.slide18.output.emitIr],
                ['--config', t.slide18.output.config],
                ['--out', t.slide18.output.out],
              ].map(([flag, description]) => (
                <div key={flag} className="flex flex-col gap-1">
                  <code className="bg-slate-100 dark:bg-slate-700 px-3 py-2 rounded">{flag}</code>
                  <span className="text-xs italic text-slate-500 dark:text-slate-400 font-sans">{description}</span>
                </div>
              ))}
            </div>
            <Callout variant="info">
              {t.slide18.output.convertFhir}
            </Callout>
          </div>
        ),
      },
    ],
    stepsCount: 4,
  },

  // Slide 19 - Limitations
  {
    id: 'slide-19',
    title: 'Limitations connues',
    subtitle: 'Assumées en v1',
    content: [],
    fragments: [
      {
        step: 1,
        type: 'callout',
        content: (
          <Callout variant="warning">
            <ul className="space-y-2">
              <li>❌ Slicing / predicates ignorés</li>
              <li>❌ Choice elements ignorés</li>
              <li>❌ Mapping incomplet possible selon structure BBR</li>
            </ul>
          </Callout>
        ),
      },
      {
        step: 2,
        type: 'text',
        content: (
          <div className="space-y-3">
            <h3 className="text-xl font-semibold">Pourquoi c&apos;est OK en v1</h3>
            <ul className="list-disc list-inside space-y-2 text-lg">
              <li>Tout est tracé dans les diagnostics</li>
              <li>Warnings explicites sur les éléments ignorés</li>
              <li>Permet déjà de couvrir 80% des cas réels</li>
            </ul>
          </div>
        ),
      },
      {
        step: 3,
        type: 'callout',
        content: (
          <Callout variant="info">
            <strong>Next steps :</strong> Support progressif du slicing, meilleure couverture des invariants complexes
          </Callout>
        ),
      },
    ],
    stepsCount: 3,
  },

  {
    id: 'slide-20',
    title: t.slide20.title,
    content: [],
    fragments: [
      {
        step: 1,
        type: 'code',
        content: (
          <div>
            <h3 className="text-lg font-semibold mb-3 flex items-center gap-2">
              <Badge variant="bbr">{t.slide20.bbrExtract}</Badge>
            </h3>
            <CodeBlock language="xml">
              {`<template id="1.2.3.4" name="ExampleHeader">
  <element name="ClinicalDocument">
    <element name="recordTarget" minimumMultiplicity="1"/>
  </element>
</template>`}
            </CodeBlock>
          </div>
        ),
      },
      {
        step: 2,
        type: 'code',
        content: (
          <div>
            <h3 className="text-lg font-semibold mb-3 flex items-center gap-2">
              <Badge variant="ir">{t.slide20.irJson}</Badge>
            </h3>
            <CodeBlock language="json">
              {`{
  "id": "1.2.3.4",
  "rootCdaType": "ClinicalDocument",
  "elements": [
    {
      "path": "recordTarget",
      "cardinality": { "min": 1, "max": "*" }
    }
  ]
}`}
            </CodeBlock>
          </div>
        ),
      },
      {
        step: 3,
        type: 'code',
        content: (
          <div>
            <h3 className="text-lg font-semibold mb-3 flex items-center gap-2">
              <Badge variant="fsh">{t.slide20.fshProfile}</Badge>
            </h3>
            <CodeBlock language="fsh">
              {`Profile: ExampleHeader
Parent: http://hl7.org/cda/stds/core/StructureDefinition/ClinicalDocument
Id: example-header
* ^status = #draft
* recordTarget 1..*`}
            </CodeBlock>
          </div>
        ),
      },
    ],
    stepsCount: 3,
  },

  {
    id: 'slide-21',
    title: t.slide21.title,
    subtitle: t.slide21.subtitle,
    content: [],
    fragments: [
      {
        step: 1,
        type: 'text',
        content: (
          <div className="space-y-4">
            <h3 className="text-2xl font-semibold text-green-700 dark:text-green-300">{t.slide21.solid.title}</h3>
            <ul className="list-disc list-inside space-y-2 text-lg">
              <li>{t.slide21.solid.pipeline}</li>
              <li>{t.slide21.solid.normalisation}</li>
              <li>{t.slide21.solid.generation}</li>
              <li>{t.slide21.solid.diagnostics}</li>
            </ul>
          </div>
        ),
      },
      {
        step: 2,
        type: 'callout',
        content: (
          <Callout variant="warning">
            <strong>{t.slide21.risky.title}</strong>
            <ul className="mt-2 space-y-1">
              <li>{t.slide21.risky.mapping}</li>
              <li>{t.slide21.risky.oid}</li>
              <li>{t.slide21.risky.slicing}</li>
            </ul>
          </Callout>
        ),
      },
      {
        step: 3,
        type: 'text',
        content: (
          <div className="space-y-4">
            <h3 className="text-2xl font-semibold text-blue-700 dark:text-blue-300">{t.slide21.roadmap.title}</h3>
            <ul className="list-disc list-inside space-y-2 text-lg">
              <li>{t.slide21.roadmap.supportSlicing}</li>
              <li>{t.slide21.roadmap.complexInvariants}</li>
              <li>{t.slide21.roadmap.ci}</li>
              <li>{t.slide21.roadmap.improveCoverage}</li>
            </ul>
          </div>
        ),
      },
    ],
    stepsCount: 3,
  },

  {
    id: 'slide-22',
    title: t.slide22.title,
    subtitle: t.slide22.subtitle,
    content: [],
    fragments: [
      {
        step: 1,
        type: 'text',
        content: (
          <div className="text-center space-y-8 py-12">
            <div className="text-6xl">🎯</div>
            <p className="text-3xl font-light text-slate-600 dark:text-slate-400">
              {t.slide22.letsDemo}
            </p>
          </div>
        ),
      },
      {
        step: 2,
        type: 'text',
        content: (
          <div className="grid grid-cols-3 gap-6 mt-12 text-center">
            {[
              [t.slide22.bbrXml, t.slide22.artDecorExport],
              [t.slide22.cliAxiomCda, t.slide22.java21],
              [t.slide22.fshProfiles, t.slide22.sushiReady],
            ].map(([label, sublabel], idx) => (
              <div
                key={label}
                className={[
                  'p-6 rounded-xl border mt-0 text-center',
                  idx === 0
                    ? 'bg-orange-50 border-orange-200 dark:bg-orange-900/40 dark:border-orange-700 text-orange-800 dark:text-orange-200'
                    : idx === 1
                    ? 'bg-blue-50 border-blue-200 dark:bg-blue-900/40 dark:border-blue-700 text-blue-800 dark:text-blue-200'
                    : 'bg-green-50 border-green-200 dark:bg-green-900/40 dark:border-green-700 text-green-800 dark:text-green-200',
                ].join(' ')}
              >
                <div className="text-4xl mb-3">{idx === 0 ? '📄' : idx === 1 ? '⚙️' : '✨'}</div>
                <div className="font-semibold">{label}</div>
                <div className="text-sm text-current/70 mt-2">{sublabel}</div>
              </div>
            ))}
          </div>
        ),
      },
      {
        step: 3,
        type: 'callout',
        content: (
          <Callout variant="success">
            <strong>{t.slide22.readyForDemo}</strong>
            <div className="mt-3 text-sm">
              <p>{t.slide22.weWillSee}</p>
              <ul className="list-disc list-inside mt-2 space-y-1">
                <li>{t.slide22.executeCli}</li>
                <li>{t.slide22.inspectIr}</li>
                <li>{t.slide22.analyzeFsh}</li>
                <li>{t.slide22.validateWithSushi}</li>
              </ul>
            </div>
          </Callout>
        ),
      },
    ],
    stepsCount: 3,
  },
  ];
};

export const slides = getSlides('en');
