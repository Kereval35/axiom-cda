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
  };
  output: {
    sushiRepo: string;
    emitIr: string;
    config: string;
    out: string;
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
      title: 'BBR → IR: Template selection',
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
      },
      includeCallout: 'Include expansion: referenced templates via <include> are added even if not explicitly selected',
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
      },
      output: {
        sushiRepo: 'Emit SUSHI-compatible repository layout',
        emitIr: 'Export intermediate representation JSON',
        config: 'Load YAML configuration overrides',
        out: 'Set output folder',
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
        generation: 'Terminology generation (ValueSet/CodeSystem)',
        diagnostics: 'Detailed diagnostics and traceability',
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
        ci: 'Integrated SUSHI CI validation',
        improveCoverage: 'Improve mapping coverage',
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
      validateWithSushi: 'Validate with SUSHI',
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
      title: 'BBR → IR : Sélection des templates',
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
      },
      includeCallout: "Expansion des includes : Les templates référencés via <include> sont ajoutés même s'ils ne sont pas explicitement sélectionnés",
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
      },
      output: {
        sushiRepo: 'Émettre une structure compatible SUSHI',
        emitIr: "Exporter l'IR en JSON",
        config: 'Charger la configuration YAML',
        out: 'Définir le dossier de sortie',
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
        generation: 'Génération terminologies (ValueSet/CodeSystem)',
        diagnostics: 'Diagnostics détaillés et traçabilité',
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
        ci: 'Validation SUSHI CI intégrée',
        improveCoverage: 'Amélioration couverture mapping',
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
      validateWithSushi: 'Validation avec SUSHI',
    },
  },
};
