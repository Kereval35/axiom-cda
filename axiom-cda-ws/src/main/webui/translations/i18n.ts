export const translations = {
  en: {
    common: {
      version: "v1.0.0",
      copyright: "© Kereval 2026"
    },
    header: {
      title: "Axiom CDA",
      nav: {
        dashboard: "Dashboard",
        documentation: "Documentation",
        presentation: "Presentation"
      },
      subtitle: "Transform BBR exports into FSH packages."
    },
    dashboard: {
      error: "Error",
      inputModes: {
        upload: "Upload",
        paste: "Paste",
        url: "URL"
      },
      bbrLabel: "Business Backbone Rules (BBR)",
      urlPlaceholder: "https://art-decor.org/...",
      pastePlaceholder: "Paste BBR XML content here...",
      clickToUpload: "Click to upload BBR XML",
      acceptsXml: "Accepts .xml files",
      clickToChange: "Click to change file",
      options: "Options",
      sushiRepoLayout: "Sushi Repository Layout",
      showLogs: "Show Generation Logs",
      includeIr: "Include Intermediate Representation (IR)",
      yamlConfig: "YAML Configuration (Optional)",
      yamlPlaceholder: "profilePrefix: MyIG...",
      secureBackend: "All generation happens securely on our high-performance backend.",
      generating: "Generating...",
      generate: "Generate FSH Package",
      generationComplete: "Generation Complete!",
      generated: "{profiles} profiles and {invariants} invariants generated successfully.",
      downloadZip: "Download ZIP"
    },
    footer: {
      copyright: "© Kereval 2026"
    },
    docs: {
      title: "Documentation",
      subtitle: "Complete guide to Axiom CDA transformation",
      contents: "Contents",
      overview: {
        title: "Overview",
        description: "<p>Axiom CDA is a comprehensive toolchain for transforming ART-DECOR Building Block Repository (BBR) exports into FHIR Shorthand (FSH) profiles. It addresses the complexity of manually converting CDA templates by providing a reliable, automated pipeline.</p>",
        problem: {
          title: "The Problem We Solve",
          manual: "Manual Conversion: Converting CDA templates to FSH is time-consuming and error-prone",
          consistency: "Consistency Issues: Different developers may interpret BBR elements differently",
          validation: "Validation Gaps: Easy to miss constraints or introduce invalid FHIR profiles",
          maintainability: "Maintainability: Tracking changes between BBR versions is difficult"
        },
        features: {
          title: "Key Features",
          pipeline: {
            title: "Two-Stage Pipeline",
            description: "BBR → IR → FSH with intermediate validation"
          },
          validation: {
            title: "Automated Validation",
            description: "Ensures generated profiles are FHIR-compliant"
          },
          binding: {
            title: "Smart Binding Resolution",
            description: "Handles ValueSet bindings with configurable strength"
          },
          sushi: {
            title: "SUSHI Integration",
            description: "Generates ready-to-use SUSHI project structure"
          }
        }
      },
      architecture: {
        title: "Architecture",
        description: "Axiom CDA uses a three-stage transformation pipeline to ensure correctness and maintainability:",
        stage1: {
          title: "Stage 1: BBR to IR",
          description: "Parses ART-DECOR XML and creates a normalized intermediate representation:",
          input: "Input: ART-DECOR BBR XML export",
          processing: "Processing: XML parsing, path normalization, constraint extraction",
          output: "Output: IRTemplate[] (validated JSON)"
        },
        stage2: {
          title: "Stage 2: IR Validation & Enhancement",
          description: "Enriches and validates the IR before FSH generation:",
          pathNormal: "Path Normalization: hl7:recordTarget → recordTarget",
          typeRes: "Type Resolution: Infers FHIR data types from fixed values",
          cardMap: "Cardinality Mapping: Converts BBR multiplicity to FHIR cardinality",
          vsBind: "ValueSet Binding: Maps vocabulary bindings with strength",
          invGen: "Invariant Generation: Converts XPath asserts to FHIRPath"
        },
        stage3: {
          title: "Stage 3: FSH Generation",
          description: "Generates FHIR Shorthand profiles from validated IR:",
          profGen: "Profile Generation: Creates Profile resources with metadata",
          constApp: "Constraint Application: Emits cardinality, fixed values, bindings",
          incRes: "Include Resolution: Converts template includes to 'only' constraints",
          invEmit: "Invariant Emission: Generates FHIRPath invariants"
        }
      },
      bbrToIr: {
        title: "BBR to IR Transformation",
        description: "The first stage parses ART-DECOR BBR XML and converts it into a clean, normalized intermediate representation (IR).",
        templateSelection: {
          title: "Template Selection & Filtering",
          description: "The pipeline intelligently filters templates based on configuration:",
          filterInactive: "1. Filter out inactive templates (unless includeInactiveTemplates: true)",
          keepLatest: "2. Keep only the latest effective version of each template",
          applyFilters: "3. Apply templateIds whitelist if provided in YAML config",
          expandIncludes: "4. Expand include references to resolve nested templates"
        },
        pathNormalization: {
          title: "Path Normalization",
          description: "BBR paths with XML namespaces and attributes are normalized to dot notation:",
          before: "Before",
          after: "After"
        },
        elementMapping: {
          title: "BBR Element Mapping",
          bbrElement: "BBR Element",
          irMapping: "IR Mapping"
        },
        fixedValues: {
          title: "Fixed Value Detection",
          description: "The parser automatically detects fixed values and infers their types:",
          boolean: "Boolean: true/false → IRFixedValue<boolean>",
          code: "Code: #PAT → IRFixedValue<code>",
          string: "String: \"example\" → IRFixedValue<string>"
        }
      },
      irToFsh: {
        title: "IR to FSH Generation"
      },
      usage: {
        title: "How to Use"
      },
      limitations: {
        title: "Limitations",
        subtitle: "Assumed in v1",
        description: "The v1 pipeline intentionally skips a few CDA constructs to keep generated profiles safe and maintainable.",
        list: {
          slicing: "Slicing / predicates ignored",
          choice: "Choice elements ignored",
          mapping: "Mapping may be incomplete depending on the BBR structure"
        },
        note: "Diagnostics capture every ignored element and we already cover roughly 80% of real-world templates; warnings are emitted when something is skipped.",
        roadmapTitle: "What’s next?",
        roadmapDetail: "Add slicing support, extend invariant coverage, and improve mapping heuristics."
      },
      api: {
        title: "API Reference"
      }
    },
    presentation: {
      slide1: {
        title: "Axiom CDA",
        subtitle: "Automated BBR to FSH Transformation Pipeline",
        description: "A comprehensive toolchain for transforming ART-DECOR Building Block Repository exports into FHIR Shorthand profiles",
        presentedBy: "Presented by",
        author: "Achraf ACHKARI-BEGDOURI",
        date: "January 2026"
      },
      slide18: {
        naming: {
          profilePrefix: "Prefix applied to generated profile names",
          idPrefix: "Prefix applied to profile ids (kebab-case)"
        },
        selection: {
          templateIds: "Restrict generation to specific template ids",
          classificationTypes: "Filter templates by classification types"
        },
        output: {
          sushiRepo: "Emit SUSHI-compatible repository layout",
          emitIr: "Export intermediate representation JSON",
          config: "Load YAML configuration overrides",
          out: "Set output folder"
        }
      },
      controls: {
        modeLabel: "Presentation mode:",
        hint: "Minimal hides the deep mapping and IR slides to keep the story short.",
        modes: {
          minimal: "Minimal",
          full: "Full"
        },
        progress: {
          slide: "Slide",
          step: "Step"
        }
      }
    }
  },
  fr: {
    common: {
      version: "v1.0.0",
      copyright: "© Kereval 2026"
    },
    header: {
      title: "Axiom CDA",
      nav: {
        dashboard: "Tableau de bord",
        documentation: "Documentation",
        presentation: "Presentation"
      },
      subtitle: "Transformez les exports BBR en packages FSH de haute qualité."
    },
    dashboard: {
      error: "Erreur",
      inputModes: {
        upload: "Télécharger",
        paste: "Coller",
        url: "URL"
      },
      bbrLabel: "Règles de base métier (BBR)",
      urlPlaceholder: "https://art-decor.org/...",
      pastePlaceholder: "Collez le contenu XML BBR ici...",
      clickToUpload: "Cliquez pour télécharger le XML BBR",
      acceptsXml: "Accepte les fichiers .xml",
      clickToChange: "Cliquez pour changer le fichier",
      options: "Options",
      sushiRepoLayout: "Disposition Sushi Repository",
      showLogs: "Afficher les journaux de génération",
      includeIr: "Inclure la représentation intermédiaire (IR)",
      yamlConfig: "Configuration YAML (Optionnel)",
      yamlPlaceholder: "profilePrefix: MonIG...",
      secureBackend: "Toute la génération se passe en toute sécurité sur notre backend haute performance.",
      generating: "Génération...",
      generate: "Générer le package FSH",
      generationComplete: "Génération terminée !",
      generated: "{profiles} profils et {invariants} invariants générés avec succès.",
      downloadZip: "Télécharger ZIP"
    },
    footer: {
      copyright: "© Kereval 2026"
    },
    docs: {
      title: "Documentation",
      subtitle: "Guide complet de la transformation Axiom CDA",
      contents: "Contenu",
      overview: {
        title: "Aperçu",
        description: "<p>Axiom CDA est une chaîne d'outils complète pour transformer les exports Building Block Repository (BBR) d'ART-DECOR en profils FHIR Shorthand (FSH). Il répond à la complexité de la conversion manuelle des modèles CDA en fournissant un pipeline automatisé et fiable.</p>",
        problem: {
          title: "Le problème que nous résolvons",
          manual: "Conversion manuelle: La conversion des modèles CDA en FSH prend du temps et est sujette aux erreurs",
          consistency: "Problèmes de cohérence: Différents développeurs peuvent interpréter les éléments BBR différemment",
          validation: "Lacunes de validation: Facile de manquer des contraintes ou d'introduire des profils FHIR invalides",
          maintainability: "Maintenabilité: Le suivi des changements entre les versions BBR est difficile"
        },
        features: {
          title: "Fonctionnalités clés",
          pipeline: {
            title: "Pipeline en deux étapes",
            description: "BBR → IR → FSH avec validation intermédiaire"
          },
          validation: {
            title: "Validation automatisée",
            description: "Garantit que les profils générés sont conformes FHIR"
          },
          binding: {
            title: "Résolution intelligente des liaisons",
            description: "Gère les liaisons ValueSet avec une force configurable"
          },
          sushi: {
            title: "Intégration SUSHI",
            description: "Génère une structure de projet SUSHI prête à l'emploi"
          }
        }
      },
      architecture: {
        title: "Architecture",
        description: "Axiom CDA utilise un pipeline de transformation en trois étapes pour garantir l'exactitude et la maintenabilité:",
        stage1: {
          title: "Étape 1: BBR vers IR",
          description: "Analyse le XML ART-DECOR et crée une représentation intermédiaire normalisée:",
          input: "Entrée: Export XML BBR ART-DECOR",
          processing: "Traitement: Analyse XML, normalisation des chemins, extraction des contraintes",
          output: "Sortie: IRTemplate[] (JSON validé)"
        },
        stage2: {
          title: "Étape 2: Validation et amélioration de l'IR",
          description: "Enrichit et valide l'IR avant la génération FSH:",
          pathNormal: "Normalisation des chemins: hl7:recordTarget → recordTarget",
          typeRes: "Résolution de type: Déduit les types de données FHIR à partir des valeurs fixes",
          cardMap: "Mappage de cardinalité: Convertit la multiplicité BBR en cardinalité FHIR",
          vsBind: "Liaison ValueSet: Mappe les liaisons de vocabulaire avec force",
          invGen: "Génération d'invariants: Convertit les assertions XPath en FHIRPath"
        },
        stage3: {
          title: "Étape 3: Génération FSH",
          description: "Génère des profils FHIR Shorthand à partir de l'IR validé:",
          profGen: "Génération de profils: Crée des ressources Profile avec métadonnées",
          constApp: "Application de contraintes: Émet la cardinalité, les valeurs fixes, les liaisons",
          incRes: "Résolution d'inclusion: Convertit les inclusions de modèles en contraintes 'only'",
          invEmit: "Émission d'invariants: Génère des invariants FHIRPath"
        }
      },
      bbrToIr: {
        title: "Transformation BBR vers IR",
        description: "La première étape analyse le XML BBR ART-DECOR et le convertit en une représentation intermédiaire (IR) propre et normalisée.",
        templateSelection: {
          title: "Sélection et filtrage des modèles",
          description: "Le pipeline filtre intelligemment les modèles en fonction de la configuration:",
          filterInactive: "1. Filtrer les modèles inactifs (sauf si includeInactiveTemplates: true)",
          keepLatest: "2. Conserver uniquement la dernière version effective de chaque modèle",
          applyFilters: "3. Appliquer la liste blanche templateIds si fournie dans la config YAML",
          expandIncludes: "4. Développer les références d'inclusion pour résoudre les modèles imbriqués"
        },
        pathNormalization: {
          title: "Normalisation des chemins",
          description: "Les chemins BBR avec espaces de noms XML et attributs sont normalisés en notation pointée:",
          before: "Avant",
          after: "Après"
        },
        elementMapping: {
          title: "Mappage d'éléments BBR",
          bbrElement: "Élément BBR",
          irMapping: "Mappage IR"
        },
        fixedValues: {
          title: "Détection de valeur fixe",
          description: "L'analyseur détecte automatiquement les valeurs fixes et déduit leurs types:",
          boolean: "Booléen: true/false → IRFixedValue<boolean>",
          code: "Code: #PAT → IRFixedValue<code>",
          string: "Chaîne: \"exemple\" → IRFixedValue<string>"
        }
      },
      irToFsh: {
        title: "Génération IR vers FSH"
      },
      usage: {
        title: "Comment utiliser"
      },
      limitations: {
        title: "Limitations",
        subtitle: "Assumées en v1",
        description: "La v1 choisit volontairement de ne pas gérer certains composants CDA pour rester stable et prédictible.",
        list: {
          slicing: "Slicing / prédicats ignorés",
          choice: "Éléments Choice ignorés",
          mapping: "Mappage potentiellement incomplet selon la structure BBR"
        },
        note: "Chaque élément ignoré est tracé dans les diagnostics; on couvre déjà environ 80 % des cas réels et des warnings accompagnent les omissions.",
        roadmapTitle: "Prochaine étape ?",
        roadmapDetail: "Support du slicing, couverture plus large des invariants et meilleures heuristiques de mappage."
      },
      api: {
        title: "Référence API"
      }
    },
    presentation: {
      slide1: {
        title: "Axiom CDA",
        subtitle: "Pipeline de transformation automatisée BBR vers FSH",
        description: "Une chaîne d'outils complète pour transformer les exports Building Block Repository d'ART-DECOR en profils FHIR Shorthand",
        presentedBy: "Présenté par",
        author: "Achraf ACHKARI-BEGDOURI",
        date: "Janvier 2026"
      },
      slide18: {
        naming: {
          profilePrefix: "Préfixe appliqué aux noms de profils générés",
          idPrefix: "Préfixe appliqué aux identifiants (kebab-case)"
        },
        selection: {
          templateIds: "Restreindre la génération à des template ids précis",
          classificationTypes: "Filtrer les templates par types de classification"
        },
        output: {
          sushiRepo: "Émettre une structure compatible SUSHI",
          emitIr: "Exporter l'IR en JSON",
          config: "Charger la configuration YAML",
          out: "Définir le dossier de sortie"
        }
      },
      controls: {
        modeLabel: "Mode de présentation :",
        hint: "Minimal masque les détails techniques et les slides IR pour gagner en concision.",
        modes: {
          minimal: "Minimal",
          full: "Complet"
        },
        progress: {
          slide: "Diapo",
          step: "Étape"
        }
      }
    }
  }
};

export type Language = "en" | "fr";
export type TranslationKey = typeof translations.en;
