import { Language } from '@/translations/i18n';

type Slide2Text = {
  title: string;
  subtitle: string;
  currentPain: {
    title: string;
    manual: string;
    notScalable: string;
    duplication: string;
  };
  maintenanceImpact: {
    title: string;
    sync: string;
    drift: string;
    tracking: string;
  };
  solution: string;
};

type Slide3Text = {
  title: string;
  inputs: string;
  outputs: string;
  bbrXml: string;
  cdaPackage: string;
  yamlConfig: string;
  profiles: string;
  invariants: string;
  terminologies: string;
  fhirProfiles: string;
  cliOptions: string;
  irInfo: string;
};

type Slide4Text = {
  title: string;
  subtitle: string;
  separation: {
    title: string;
    description1: string;
    description2: string;
  };
  stability: {
    title: string;
    description1: string;
    description2: string;
  };
  auditability: {
    title: string;
    description1: string;
    description2: string;
  };
};

type Slide5Text = {
  title: string;
  subtitle: string;
};

type Slide6Text = {
  title: string;
  rules: {
    ignoreStatus: { condition: string; result: string };
    latestVersion: { condition: string; result: string };
    configFilter: { condition: string; result: string };
    ownershipFilter: { condition: string; result: string };
  };
  includeCallout: string;
};

type Slide15Text = {
  title: string;
  subtitle?: string;
  problem: {
    title: string;
    description: string;
  };
  cardinalities: {
    title: string;
    min: string;
    max: string;
    guarantee: string;
  };
  fixedValues: {
    title: string;
    baseHas: string;
    notEmit: string;
  };
  bindings: {
    title: string;
    effective: string;
    cannotWeaken: string;
  };
};

type Slide18Text = {
  title: string;
  structure: string;
  namingOptions: string;
  selectionOptions: string;
  outputOptions: string;
  naming: {
    profilePrefix: string;
    idPrefix: string;
  };
  selection: {
    templateIds: string;
    classificationTypes: string;
    projectOwnership: string;
    ownedPrefixes: string;
  };
  output: {
    sushiRepo: string;
    emitIr: string;
    config: string;
    out: string;
    convertFhir: string;
  };
};

type Slide20Text = {
  title: string;
  bbrExtract: string;
  irJson: string;
  fshProfile: string;
};

type Slide21Text = {
  title: string;
  subtitle: string;
  solid: {
    title: string;
    pipeline: string;
    normalisation: string;
    generation: string;
    diagnostics: string;
  };
  risky: {
    title: string;
    mapping: string;
    oid: string;
    slicing: string;
  };
  roadmap: {
    title: string;
    supportSlicing: string;
    complexInvariants: string;
    ci: string;
    improveCoverage: string;
  };
};

type Slide22Text = {
  title: string;
  subtitle: string;
  letsDemo: string;
  bbrXml: string;
  artDecorExport: string;
  cliAxiomCda: string;
  java21: string;
  fshProfiles: string;
  sushiReady: string;
  readyForDemo: string;
  weWillSee: string;
  executeCli: string;
  inspectIr: string;
  analyzeFsh: string;
  validateWithSushi: string;
};

type Slide23Text = {
  title: string;
  subtitle: string;
  why: {
    title: string;
    description: string;
  };
  flow: {
    title: string;
    bbrToIr: string;
    mappingRules: string;
    fhirProjection: string;
    fshOutput: string;
  };
  poc: string;
};

type Slide24Text = {
  title: string;
  subtitle: string;
  generic: {
    title: string;
    description: string;
  };
  ehdsi: {
    title: string;
    description: string;
  };
  custom: {
    title: string;
    description: string;
  };
  compileNote: string;
};

type Slide25Text = {
  title: string;
  subtitle: string;
  v1: {
    title: string;
    observation: string;
    safety: string;
    business: string;
  };
  community: {
    title: string;
    reusablePacks: string;
    moreRoots: string;
    sharedGovernance: string;
  };
};

type Slide26Text = {
  title: string;
  subtitle: string;
  principle: string;
  ownership: string;
  prefixes: string;
  outcome: string;
};

type PresentationTranslation = {
  slide1: {
    title: string;
    subtitle: string;
    description: string;
    presentedBy: string;
    author: string;
    date: string;
  };
  slide2: Slide2Text;
  slide3: Slide3Text;
  slide4: Slide4Text;
  slide5: Slide5Text;
  slide6: Slide6Text;
  slide15: Slide15Text;
  slide18: Slide18Text;
  slide20: Slide20Text;
  slide21: Slide21Text;
  slide22: Slide22Text;
  slide23: Slide23Text;
  slide24: Slide24Text;
  slide25: Slide25Text;
  slide26: Slide26Text;
};

export const slideTranslations: Record<Language, PresentationTranslation> = {
  en: {
    slide1: {
      title: 'Axiom CDA',
      subtitle: 'Automated BBR to FSH Transformation Pipeline',
      description: 'A comprehensive toolchain for transforming ART-DECOR Building Block Repository exports into FHIR Shorthand profiles',
      presentedBy: 'Presented by',
      author: 'Achraf ACHKARI-BEGDOURI',
      date: 'January 2026',
    },
    slide2: {
      title: 'The Problem',
      subtitle: 'Why automate?',
      currentPain: {
        title: 'Current pain points',
        manual: 'Manual creation: time-consuming and error-prone',
        notScalable: 'Not scalable for many templates',
        duplication: 'Information duplication (already in ART-DECOR)',
      },
      maintenanceImpact: {
        title: 'Maintenance impact',
        sync: 'Hard to synchronize BBR and FSH profiles',
        drift: 'Risk of drift and inconsistencies',
        tracking: 'Hard to track changes',
      },
      solution: 'Solution: An automated, versioned, testable and diffable pipeline',
    },
    slide3: {
      title: 'Inputs & Outputs',
      inputs: 'Inputs',
      outputs: 'Outputs',
      bbrXml: '✓ BBR XML (ART-DECOR export)',
      cdaPackage: '✓ CDA Package (hl7.cda.uv.core)',
      yamlConfig: '✓ Optional config YAML',
      profiles: '→ FSH-CDA profiles',
      invariants: '→ Invariants (FSH)',
      terminologies: '→ ValueSets / CodeSystems',
      fhirProfiles: '→ FHIR FSH from Observation IR + built-in or custom mapping rules',
      cliOptions: 'CLI options',
      irInfo: 'The IR (Intermediate Representation) can be exported as JSON for audit and debugging',
    },
    slide4: {
      title: 'Why an IR?',
      subtitle: 'Intermediate Representation',
      separation: {
        title: 'Separation of responsibilities',
        description1: 'Understanding the BBR ≠ writing FSH',
        description2: 'Each transformation is isolated and independently testable',
      },
      stability: {
        title: 'Stability & testability',
        description1: 'The IR is a stable contract between the two steps',
        description2: 'Unit tests on BBR→IR and IR→FSH separately',
      },
      auditability: {
        title: 'Auditability & diagnostics',
        description1: 'The IR captures warnings, unmapped elements and decisions',
        description2: 'Allows tracing exactly what was transformed and why',
      },
    },
    slide5: {
      title: 'IR structure',
      subtitle: 'Data model',
    },
    slide6: {
      title: 'BBR → IR: Selection and ownership',
      rules: {
        ignoreStatus: {
          condition: 'Template status = RETIRED | CANCELLED | REJECTED',
          result: '❌ Ignored',
        },
        latestVersion: {
          condition: 'Multiple versions of same templateId',
          result: '✓ Keep the latest effectiveDate',
        },
        configFilter: {
          condition: 'Config: templateIds or classificationTypes',
          result: 'Explicit templateIds override, else filter by classification, else all active templates',
        },
        ownershipFilter: {
          condition: 'projectPlusRequiredIncludes enabled',
          result: 'Keep project-related templates in the visible result while still resolving internal includes in the pipeline',
        },
      },
      includeCallout: 'Owned repository prefixes can be configured explicitly, for example BBR- and BIO-CR-BIO- for CR-BIO',
    },
    slide15: {
      title: 'IR → FSH: Clamp & safety',
      subtitle: 'Never generate fake constraints',
      problem: {
        title: 'Problem',
        description: 'Generating a cardinality or binding that contradicts the base CDA would produce invalid or too permissive profiles',
      },
      cardinalities: {
        title: 'Clamped cardinalities',
        min: 'min raised if needed (no weakening)',
        max: 'max lowered if needed (no widening)',
        guarantee: 'Guarantee: max ≥ min',
      },
      fixedValues: {
        title: 'Fixed values',
        baseHas: 'Base CDA already has a fixed value',
        notEmit: '❌ Do not emit (redundant)',
      },
      bindings: {
        title: 'Bindings: no weakening',
        effective: 'effective strength = max(base, requested)',
        cannotWeaken: 'If base = REQUIRED, cannot weaken to EXTENSIBLE',
      },
    },
    slide18: {
      title: 'Output layout & CLI options',
      structure: 'Folder structure',
      namingOptions: 'CLI options: Naming',
      selectionOptions: 'CLI options: Selection',
      outputOptions: 'CLI options: Output',
      naming: {
        profilePrefix: 'Prefix applied to generated profile names',
        idPrefix: 'Prefix applied to profile ids (kebab-case)',
      },
      selection: {
        templateIds: 'Restrict generation to specific template ids',
        classificationTypes: 'Filter templates by classification types',
        projectOwnership: 'Keep project-owned templates and required includes only',
        ownedPrefixes: 'Declare additional owned ART-DECOR repository prefixes',
      },
      output: {
        sushiRepo: 'Emit SUSHI-compatible repository layout',
        emitIr: 'Export intermediate representation JSON',
        config: 'Load YAML configuration overrides',
        out: 'Set output folder',
        convertFhir: 'Generate and compile FHIR FSH from Observation IR in the WS UI',
      },
    },
    slide20: {
      title: 'Scenario: Template → IR → FSH',
      bbrExtract: 'BBR extract',
      irJson: 'IR JSON',
      fshProfile: 'FSH profile',
    },
    slide21: {
      title: 'Conclusion',
      subtitle: 'Summary & next steps',
      solid: {
        title: 'What is already solid',
        pipeline: 'Pipeline BBR → IR → FSH operational',
        normalisation: 'Path normalisation, cardinalities, fixed values, bindings',
        generation: 'Terminology generation plus SUSHI-ready package emission',
        diagnostics: 'Detailed diagnostics, ownership origins and traceability',
      },
      risky: {
        title: 'What is still risky',
        mapping: '• Path mapping in complex BBR structures',
        oid: '• OID terminology resolution',
        slicing: '• Slicing not supported',
      },
      roadmap: {
        title: 'Roadmap',
        supportSlicing: 'Support BBR → FSH slicing',
        complexInvariants: 'More complex invariants (full FHIRPath)',
        ci: 'Extend SUSHI and FHIR conversion validation coverage',
        improveCoverage: 'Refine ownership heuristics and mapping coverage',
      },
    },
    slide22: {
      title: 'Merci !',
      subtitle: 'Questions ?',
      letsDemo: "Let's move to the demo",
      bbrXml: 'BBR XML',
      artDecorExport: 'ART-DECOR export',
      cliAxiomCda: 'CLI axiom-cda',
      java21: 'Java 21',
      fshProfiles: 'FSH profiles',
      sushiReady: 'SUSHI-ready',
      readyForDemo: 'Ready for the live demonstration!',
      weWillSee: 'We will see:',
      executeCli: 'Execute CLI on a real BBR',
      inspectIr: 'Inspect the generated IR',
      analyzeFsh: 'Analyze the generated FSH profiles',
      validateWithSushi: 'Validate with SUSHI and generate FHIR FSH from an Observation profile',
    },
    slide23: {
      title: 'FHIR FSH transformation',
      subtitle: 'From CDA IR to FHIR-oriented rules',
      why: {
        title: 'What we are prototyping',
        description: 'A true CDA to FHIR rule-mapping flow: BBR is normalized into a CDA IR, mapping rules are applied, a FHIR-oriented projection is computed, and FHIR FSH is emitted as the final artifact.',
      },
      flow: {
        title: 'Pipeline',
        bbrToIr: 'BBR export → CDA IR with normalized paths, datatypes and constraints',
        mappingRules: 'Built-in or custom mapping rules decide how CDA branches project into FHIR semantics',
        fhirProjection: 'FHIR-oriented intermediate projection keeps parent, cardinalities, bindings and fixed values explicit',
        fshOutput: 'Generated FHIR FSH stays traceable to the selected Observation IR template',
      },
      poc: 'This is a PoC for generalized CDA to FHIR transformation rules rather than a one-off profile generator.',
    },
    slide24: {
      title: 'Built-in and custom mappings',
      subtitle: 'How the WS exposes transformation choices',
      generic: {
        title: 'Generic HL7 R4 Observation',
        description: 'Default built-in preset. Targets pure HL7 R4 Observation when no IG-specific parent is required.',
      },
      ehdsi: {
        title: 'eHDSI Laboratory Observation',
        description: 'Shipped out of the box for projects that need the specialized laboratory Observation parent profile.',
      },
      custom: {
        title: 'Custom StructureMap override',
        description: 'A project can still upload its own StructureMap JSON and replace the shipped mapping semantics for that run.',
      },
      compileNote: 'SUSHI compilation remains a separate step, with package selection driven by the generated parent profile.',
    },
    slide25: {
      title: 'Observation-first PoC',
      subtitle: 'V1 scope and community direction',
      v1: {
        title: 'Why Observation first',
        observation: 'Observation gives a realistic CDA to FHIR business case with constrained scope.',
        safety: 'It lets us validate the mapping engine, diagnostics and compile loop without pretending full CDA coverage.',
        business: 'It already demonstrates both generic HL7 output and domain-specific specialization.',
      },
      community: {
        title: 'Where this goes next',
        reusablePacks: 'Turn mapping rules into reusable packs that can be contributed and reviewed.',
        moreRoots: 'Extend the same abstraction to additional CDA roots beyond Observation.',
        sharedGovernance: 'Let the community enrich the rule library while preserving override capability for local projects.',
      },
    },
    slide26: {
      title: 'Ownership filtering',
      subtitle: 'Keep the visible output project-focused',
      principle: 'When ownership filtering is enabled, the selection logic becomes repository-aware instead of showing every technically reachable template.',
      ownership: 'Ownership is inferred from project OID roots, TM base roots, project prefixes, scenario references and configured repository prefixes.',
      prefixes: 'This is useful for projects like CR-BIO where authored content may live under prefixes such as BBR- or BIO-CR-BIO-.',
      outcome: 'The WS/UI then expose only the project-related profiles, while the generation pipeline can still resolve internal dependencies behind the scenes.',
    },
  },
  fr: {
    slide1: {
      title: 'Axiom CDA',
      subtitle: 'Pipeline de transformation automatisée BBR vers FSH',
      description: "Une chaîne d'outils complète pour transformer les exports Building Block Repository d'ART-DECOR en profils FHIR Shorthand",
      presentedBy: 'Présenté par',
      author: 'Achraf ACHKARI-BEGDOURI',
      date: 'Janvier 2026',
    },
    slide2: {
      title: 'Le problème',
      subtitle: 'Pourquoi automatiser ?',
      currentPain: {
        title: 'Douleur actuelle',
        manual: "Création manuelle : chronophage et source d'erreurs",
        notScalable: 'Non-scalable pour de nombreux templates',
        duplication: 'Duplication des informations déjà dans ART-DECOR',
      },
      maintenanceImpact: {
        title: 'Impact sur la maintenance',
        sync: 'Synchronisation difficile entre BBR et profils FSH',
        drift: "Risque de dérive et d'incohérences",
        tracking: 'Difficulté à tracer les modifications',
      },
      solution: 'Solution : Un pipeline automatisé, versionné, testable et diffable',
    },
    slide3: {
      title: 'Entrées & Sorties',
      inputs: 'Entrées',
      outputs: 'Sorties',
      bbrXml: '✓ BBR XML (export ART-DECOR)',
      cdaPackage: '✓ Package CDA (hl7.cda.uv.core)',
      yamlConfig: '✓ Config YAML optionnelle',
      profiles: '→ Profils FSH-CDA',
      invariants: '→ Invariants (FSH)',
      terminologies: '→ ValueSets / CodeSystems',
      fhirProfiles: '→ FHIR FSH depuis un IR Observation + règles de mapping intégrées ou custom',
      cliOptions: 'Options CLI',
      irInfo: "L'IR (Intermediate Representation) peut être exporté en JSON pour audit et debugging",
    },
    slide4: {
      title: 'Pourquoi un IR ?',
      subtitle: 'Intermediate Representation',
      separation: {
        title: 'Séparation des responsabilités',
        description1: 'Comprendre le BBR ≠ Écrire du FSH',
        description2: 'Chaque transformation est isolée et testable indépendamment',
      },
      stability: {
        title: 'Stabilité & testabilité',
        description1: "L'IR est un contrat stable entre les deux étapes",
        description2: 'Tests unitaires sur BBR→IR et IR→FSH séparément',
      },
      auditability: {
        title: 'Auditabilité & diagnostics',
        description1: "L'IR capture warnings, éléments non-mappés, décisions prises",
        description2: 'Permet de tracer exactement ce qui a été transformé et pourquoi',
      },
    },
    slide5: {
      title: "Structure de l'IR",
      subtitle: 'Modèle de données',
    },
    slide6: {
      title: 'BBR → IR : Sélection et ownership',
      rules: {
        ignoreStatus: {
          condition: 'Template status = RETIRED | CANCELLED | REJECTED',
          result: '❌ Ignoré',
        },
        latestVersion: {
          condition: 'Plusieurs versions du même templateId',
          result: '✓ Garder la dernière (effectiveDate)',
        },
        configFilter: {
          condition: 'Config : templateIds ou classificationTypes',
          result: 'Si templateIds défini → sélection explicite, sinon classificationTypes, sinon tous les templates actifs',
        },
        ownershipFilter: {
          condition: 'projectPlusRequiredIncludes activé',
          result: 'Conserver les templates liés au projet dans la sortie visible tout en résolvant les includes internes dans la pipeline',
        },
      },
      includeCallout: "Les prefixes de repository du projet peuvent être configurés explicitement, par exemple BBR- et BIO-CR-BIO- pour CR-BIO",
    },
    slide15: {
      title: 'IR → FSH : Clamp & sécurité',
      subtitle: 'Ne jamais générer du "faux"',
      problem: {
        title: 'Problème',
        description: 'Si on génère une cardinalité ou un binding qui contredit le CDA de base, le profil sera invalide ou trop permissif',
      },
      cardinalities: {
        title: 'Cardinalités clampées',
        min: 'min relevé si nécessaire (ne peut pas affaiblir)',
        max: 'max abaissé si nécessaire (ne peut pas élargir)',
        guarantee: 'Garantie : max ≥ min',
      },
      fixedValues: {
        title: 'Valeurs fixes',
        baseHas: 'CDA base a déjà un fixed value',
        notEmit: '❌ Ne pas émettre (redondant)',
      },
      bindings: {
        title: 'Bindings : no weakening',
        effective: 'effective strength = max(base, demandé)',
        cannotWeaken: 'Si base = REQUIRED, on ne peut pas affaiblir à EXTENSIBLE',
      },
    },
    slide18: {
      title: 'Layout de sortie & Options CLI',
      structure: 'Structure des dossiers',
      namingOptions: 'Options CLI : Naming',
      selectionOptions: 'Options CLI : Sélection',
      outputOptions: 'Options CLI : Output',
      naming: {
        profilePrefix: 'Préfixe appliqué aux noms de profils générés',
        idPrefix: 'Préfixe appliqué aux identifiants (kebab-case)',
      },
      selection: {
        templateIds: 'Restreindre la génération à des template ids précis',
        classificationTypes: 'Filtrer les templates par types de classification',
        projectOwnership: 'Conserver uniquement les templates du projet et les includes requis',
        ownedPrefixes: 'Déclarer des prefixes ART-DECOR supplémentaires comme appartenant au projet',
      },
      output: {
        sushiRepo: 'Émettre une structure compatible SUSHI',
        emitIr: "Exporter l'IR en JSON",
        config: 'Charger la configuration YAML',
        out: 'Définir le dossier de sortie',
        convertFhir: 'Générer et compiler du FHIR FSH depuis un IR Observation dans la WS UI',
      },
    },
    slide20: {
      title: 'Scénario : Template → IR → FSH',
      bbrExtract: 'Extrait BBR',
      irJson: 'IR JSON',
      fshProfile: 'Profil FSH',
    },
    slide21: {
      title: 'Conclusion',
      subtitle: 'Bilan & prochaines étapes',
      solid: {
        title: 'Ce qui est déjà solide',
        pipeline: 'Pipeline BBR → IR → FSH opérationnel',
        normalisation: 'Normalisation des paths, cardinalités, fixed values, bindings',
        generation: 'Génération des terminologies et émission SUSHI-ready',
        diagnostics: 'Diagnostics détaillés, origines ownership et traçabilité',
      },
      risky: {
        title: 'Ce qui reste risqué',
        mapping: '• Path mapping dans structures BBR complexes',
        oid: '• Résolution OID terminologies',
        slicing: '• Slicing non supporté',
      },
      roadmap: {
        title: 'Roadmap',
        supportSlicing: 'Support slicing BBR → FSH',
        complexInvariants: 'Invariants plus complexes (FHIRPath complet)',
        ci: 'Étendre la couverture de validation SUSHI et FHIR conversion',
        improveCoverage: 'Affiner les heuristiques ownership et la couverture de mapping',
      },
    },
    slide22: {
      title: 'Merci !',
      subtitle: 'Questions ?',
      letsDemo: 'Passons à la démo',
      bbrXml: 'BBR XML',
      artDecorExport: 'Export ART-DECOR',
      cliAxiomCda: 'CLI axiom-cda',
      java21: 'Java 21',
      fshProfiles: 'Profils FSH',
      sushiReady: 'SUSHI-ready',
      readyForDemo: 'Prêt pour la démonstration en live !',
      weWillSee: 'Nous allons voir :',
      executeCli: 'Exécution du CLI sur un BBR réel',
      inspectIr: "Inspection de l'IR généré",
      analyzeFsh: 'Analyse des profils FSH produits',
      validateWithSushi: 'Validation avec SUSHI et génération FHIR FSH depuis un profil Observation',
    },
    slide23: {
      title: 'Transformation FHIR FSH',
      subtitle: "De l'IR CDA vers des règles orientées FHIR",
      why: {
        title: 'Ce que nous prototypons',
        description: "Un vrai flux de mapping CDA vers FHIR : le BBR est normalisé en IR CDA, des règles de mapping sont appliquées, une projection orientée FHIR est calculée, puis le FHIR FSH est émis comme artefact final.",
      },
      flow: {
        title: 'Pipeline',
        bbrToIr: 'Export BBR → IR CDA avec normalisation des chemins, datatypes et contraintes',
        mappingRules: 'Des règles de mapping intégrées ou custom décident comment les branches CDA se projettent dans la sémantique FHIR',
        fhirProjection: 'La projection intermédiaire orientée FHIR garde explicites le parent, les cardinalités, les bindings et les fixed values',
        fshOutput: "Le FHIR FSH généré reste traçable jusqu'au template IR Observation sélectionné",
      },
      poc: "C'est une PoC de règles de transformation CDA vers FHIR généralisables, pas seulement un générateur de profils ponctuel.",
    },
    slide24: {
      title: 'Mappings intégrés et personnalisés',
      subtitle: 'Comment la WS expose les choix de transformation',
      generic: {
        title: 'Observation HL7 R4 générique',
        description: "Preset intégré par défaut. Cible l'Observation HL7 R4 pure lorsqu'aucun parent spécifique à un IG n'est requis.",
      },
      ehdsi: {
        title: 'Observation laboratoire eHDSI',
        description: "Fourni nativement pour les projets qui ont besoin du parent Observation spécialisé laboratoire.",
      },
      custom: {
        title: 'Surcharge StructureMap personnalisée',
        description: 'Un projet peut toujours téléverser son propre StructureMap JSON et remplacer la sémantique de mapping fournie pour cette exécution.',
      },
      compileNote: 'La compilation SUSHI reste une étape séparée, avec sélection du package selon le parent généré.',
    },
    slide25: {
      title: 'PoC centrée sur Observation',
      subtitle: 'Périmètre V1 et ouverture communautaire',
      v1: {
        title: 'Pourquoi commencer par Observation',
        observation: 'Observation fournit un cas métier réaliste CDA vers FHIR avec un périmètre maîtrisé.',
        safety: "Cela permet de valider le moteur de mapping, les diagnostics et la boucle de compilation sans prétendre couvrir tout CDA.",
        business: 'Cela démontre déjà à la fois une sortie HL7 générique et une spécialisation métier.',
      },
      community: {
        title: 'La suite visée',
        reusablePacks: 'Transformer les règles de mapping en packs réutilisables, contributifs et revus collectivement.',
        moreRoots: "Étendre la même abstraction à d'autres racines CDA au-delà d'Observation.",
        sharedGovernance: "Permettre à la communauté d'enrichir la librairie de règles tout en conservant la possibilité de surcharge projet.",
      },
    },
    slide26: {
      title: 'Filtrage ownership',
      subtitle: 'Garder la sortie visible centrée projet',
      principle: "Quand le filtrage ownership est activé, la sélection devient repository-aware au lieu d'afficher tous les templates techniquement atteignables.",
      ownership: "L'ownership est inféré à partir des racines OID projet, des racines de base TM, des préfixes projet, des références de scénario et des préfixes de repository configurés.",
      prefixes: "C'est utile pour des projets comme CR-BIO où du contenu métier peut vivre sous des préfixes comme BBR- ou BIO-CR-BIO-.",
      outcome: "La WS/UI n'exposent alors que les profils liés au projet, tout en laissant la pipeline résoudre les dépendances internes en arrière-plan.",
    },
  },
};
