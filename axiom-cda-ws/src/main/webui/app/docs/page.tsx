"use client";

import React, { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useLanguage } from "../../components/LanguageProvider";

export default function DocsPage() {
    const [activeSection, setActiveSection] = useState("overview");
    const { t } = useLanguage();

    const sections = useMemo(() => [
        { id: "overview", title: t.docs.overview.title },
        { id: "architecture", title: t.docs.architecture.title },
        { id: "bbr-to-ir", title: t.docs.bbrToIr.title },
        { id: "ir-to-fsh", title: t.docs.irToFsh.title },
        { id: "usage", title: t.docs.usage.title },
        { id: "limitations", title: t.docs.limitations.title },
        { id: "api", title: t.docs.api.title },
    ], [t]);

    // Detect which section is in view when scrolling
    useEffect(() => {
        const observerOptions = {
            root: null,
            rootMargin: "-20% 0px -70% 0px", // Trigger when section is near the top
            threshold: 0
        };

        const observerCallback = (entries: IntersectionObserverEntry[]) => {
            entries.forEach((entry) => {
                if (entry.isIntersecting) {
                    setActiveSection(entry.target.id);
                }
            });
        };

        const observer = new IntersectionObserver(observerCallback, observerOptions);

        // Observe all sections
        sections.forEach((section) => {
            const element = document.getElementById(section.id);
            if (element) {
                observer.observe(element);
            }
        });

        return () => {
            observer.disconnect();
        };
    }, [sections]);

    const scrollToSection = (id: string) => {
        setActiveSection(id);
        const element = document.getElementById(id);
        if (element) {
            element.scrollIntoView({ behavior: "smooth", block: "start" });
        }
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-zinc-50 to-zinc-100 dark:from-zinc-950 dark:to-zinc-900">
            {/* Header */}
            <header className="sticky top-0 z-50 glass border-b border-card-border backdrop-blur-xl">
                <div className="max-w-7xl mx-auto px-6 py-4 flex items-center justify-between">
                    <Link href="/" className="flex items-center gap-3 hover:opacity-80 transition-opacity">
                        <span className="text-xl font-bold !text-[#18181b] dark:!text-zinc-100">← {t.docs.title}</span>
                    </Link>
                    <p className="text-sm text-zinc-500 dark:text-zinc-400">
                        {t.docs.subtitle}
                    </p>
                </div>
            </header>

            <div className="max-w-7xl mx-auto px-6 py-8">
                <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
                    {/* Sidebar Navigation */}
                    <aside className="lg:col-span-3">
                        <div className="sticky top-24 glass rounded-2xl p-4 border border-zinc-200 dark:border-zinc-800">
                            <h2 className="text-sm font-semibold !text-[#18181b] dark:!text-zinc-100 mb-3 px-3">
                                {t.docs.contents}
                            </h2>
                            <nav className="space-y-1">
                                {sections.map((section) => (
                                    <button
                                        key={section.id}
                                        onClick={() => scrollToSection(section.id)}
                                        className={`w-full text-left px-3 py-2 rounded-lg text-sm transition-all ${
                                            activeSection === section.id
                                                ? "bg-indigo-500/10 !text-indigo-600 dark:!text-indigo-400 font-medium"
                                                : "!text-zinc-700 dark:!text-zinc-300 hover:bg-zinc-100 dark:hover:bg-zinc-800"
                                        }`}
                                    >
                                        {section.title}
                                    </button>
                                ))}
                            </nav>
                        </div>
                    </aside>

                    {/* Main Content */}
                    <main className="lg:col-span-9 space-y-12">
                        {/* Overview */}
                        <section id="overview" className="glass rounded-2xl p-8 border border-zinc-200 dark:border-zinc-800">
                            <h2 className="text-3xl font-bold !text-[#18181b] dark:!text-zinc-100 mb-4">
                                {t.docs.overview.title}
                            </h2>
                            <div className="prose prose-zinc dark:prose-invert max-w-none" dangerouslySetInnerHTML={{ __html: t.docs.overview.description }}>
                            </div>
                            <div className="bg-indigo-500/5 border border-indigo-500/20 rounded-xl p-6 my-6">
                                <h3 className="!text-[#18181b] dark:!text-zinc-100 text-xl font-semibold mb-3">
                                    {t.docs.overview.problem.title}
                                </h3>
                                <ul className="!text-zinc-700 dark:!text-zinc-300 space-y-2">
                                    <li>
                                        <strong>{t.docs.overview.problem.manual.split(':')[0]}:</strong> {t.docs.overview.problem.manual.split(':')[1]}
                                    </li>
                                    <li>
                                        <strong>{t.docs.overview.problem.consistency.split(':')[0]}:</strong> {t.docs.overview.problem.consistency.split(':')[1]}
                                    </li>
                                    <li>
                                        <strong>{t.docs.overview.problem.validation.split(':')[0]}:</strong> {t.docs.overview.problem.validation.split(':')[1]}
                                    </li>
                                    <li>
                                        <strong>{t.docs.overview.problem.maintainability.split(':')[0]}:</strong> {t.docs.overview.problem.maintainability.split(':')[1]}
                                    </li>
                                </ul>
                            </div>
                            <h3 className="!text-[#18181b] dark:!text-zinc-100 text-xl font-semibold mt-8 mb-4">
                                {t.docs.overview.features.title}
                            </h3>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <div className="bg-zinc-100/50 dark:bg-zinc-800/50 rounded-lg p-4">
                                    <h4 className="font-semibold !text-[#18181b] dark:!text-zinc-100 mb-1">
                                        {t.docs.overview.features.pipeline.title}
                                    </h4>
                                    <p className="text-sm !text-zinc-600 dark:!text-zinc-400">
                                        {t.docs.overview.features.pipeline.description}
                                    </p>
                                </div>
                                <div className="bg-zinc-100/50 dark:bg-zinc-800/50 rounded-lg p-4">
                                    <h4 className="font-semibold !text-[#18181b] dark:!text-zinc-100 mb-1">
                                        {t.docs.overview.features.validation.title}
                                    </h4>
                                    <p className="text-sm !text-zinc-600 dark:!text-zinc-400">
                                        {t.docs.overview.features.validation.description}
                                    </p>
                                </div>
                                <div className="bg-zinc-100/50 dark:bg-zinc-800/50 rounded-lg p-4">
                                    <h4 className="font-semibold !text-[#18181b] dark:!text-zinc-100 mb-1">
                                        {t.docs.overview.features.binding.title}
                                    </h4>
                                    <p className="text-sm !text-zinc-600 dark:!text-zinc-400">
                                        {t.docs.overview.features.binding.description}
                                    </p>
                                </div>
                                <div className="bg-zinc-100/50 dark:bg-zinc-800/50 rounded-lg p-4">
                                    <h4 className="font-semibold !text-[#18181b] dark:!text-zinc-100 mb-1">
                                        {t.docs.overview.features.sushi.title}
                                    </h4>
                                    <p className="text-sm !text-zinc-600 dark:!text-zinc-400">
                                        {t.docs.overview.features.sushi.description}
                                    </p>
                                </div>
                            </div>
                        </section>

                        {/* Architecture */}
                        <section id="architecture" className="glass rounded-2xl p-8 border border-zinc-200 dark:border-zinc-800">
                            <h2 className="text-3xl font-bold !text-[#18181b] dark:!text-zinc-100 mb-4">
                                {t.docs.architecture.title}
                            </h2>
                            <div className="prose prose-zinc dark:prose-invert max-w-none">
                                <p className="!text-zinc-700 dark:!text-zinc-300 text-lg leading-relaxed mb-6">
                                    {t.docs.architecture.description}
                                </p>

                                <div className="bg-gradient-to-r from-indigo-500/10 via-cyan-500/10 to-green-500/10 border border-zinc-300 dark:border-zinc-700 rounded-xl p-6 my-8">
                                    <div className="flex flex-col md:flex-row items-center justify-between gap-4">
                                        <div className="flex-1 text-center">
                                            <div className="inline-block bg-indigo-500/20 border border-indigo-500/40 rounded-xl px-6 py-4 mb-2">
                                                <div className="text-2xl font-bold !text-indigo-600 dark:!text-indigo-400">BBR</div>
                                                <div className="text-xs !text-zinc-600 dark:!text-zinc-400 mt-1">ART-DECOR XML</div>
                                            </div>
                                        </div>
                                        <div className="text-3xl !text-zinc-400">→</div>
                                        <div className="flex-1 text-center">
                                            <div className="inline-block bg-cyan-500/20 border border-cyan-500/40 rounded-xl px-6 py-4 mb-2">
                                                <div className="text-2xl font-bold !text-cyan-600 dark:!text-cyan-400">IR</div>
                                                <div className="text-xs !text-zinc-600 dark:!text-zinc-400 mt-1">Intermediate Rep.</div>
                                            </div>
                                        </div>
                                        <div className="text-3xl !text-zinc-400">→</div>
                                        <div className="flex-1 text-center">
                                            <div className="inline-block bg-green-500/20 border border-green-500/40 rounded-xl px-6 py-4 mb-2">
                                                <div className="text-2xl font-bold !text-green-600 dark:!text-green-400">FSH</div>
                                                <div className="text-xs !text-zinc-600 dark:!text-zinc-400 mt-1">FHIR Shorthand</div>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <h3 className="!text-[#18181b] dark:!text-zinc-100 text-xl font-semibold mt-8 mb-4">
                                    {t.docs.architecture.stage1.title}
                                </h3>
                                <p className="!text-zinc-700 dark:!text-zinc-300">
                                    {t.docs.architecture.stage1.description}
                                </p>
                                <ul className="!text-zinc-700 dark:!text-zinc-300 space-y-2">
                                    <li><strong>{t.docs.architecture.stage1.input.split(':')[0]}:</strong> {t.docs.architecture.stage1.input.split(':')[1]}</li>
                                    <li><strong>{t.docs.architecture.stage1.processing.split(':')[0]}:</strong> {t.docs.architecture.stage1.processing.split(':')[1]}</li>
                                    <li><strong>{t.docs.architecture.stage1.output.split(':')[0]}:</strong> {t.docs.architecture.stage1.output.split(':')[1]}</li>
                                </ul>

                                <h3 className="!text-[#18181b] dark:!text-zinc-100 text-xl font-semibold mt-8 mb-4">
                                    {t.docs.architecture.stage2.title}
                                </h3>
                                <p className="!text-zinc-700 dark:!text-zinc-300">
                                    {t.docs.architecture.stage2.description}
                                </p>
                                <ul className="!text-zinc-700 dark:!text-zinc-300 space-y-2">
                                    <li><strong>{t.docs.architecture.stage2.pathNormal.split(':')[0]}:</strong> {t.docs.architecture.stage2.pathNormal.split(':')[1]}</li>
                                    <li><strong>{t.docs.architecture.stage2.typeRes.split(':')[0]}:</strong> {t.docs.architecture.stage2.typeRes.split(':')[1]}</li>
                                    <li><strong>{t.docs.architecture.stage2.cardMap.split(':')[0]}:</strong> {t.docs.architecture.stage2.cardMap.split(':')[1]}</li>
                                    <li><strong>{t.docs.architecture.stage2.vsBind.split(':')[0]}:</strong> {t.docs.architecture.stage2.vsBind.split(':')[1]}</li>
                                    <li><strong>{t.docs.architecture.stage2.invGen.split(':')[0]}:</strong> {t.docs.architecture.stage2.invGen.split(':')[1]}</li>
                                </ul>

                                <h3 className="!text-[#18181b] dark:!text-zinc-100 text-xl font-semibold mt-8 mb-4">
                                    {t.docs.architecture.stage3.title}
                                </h3>
                                <p className="!text-zinc-700 dark:!text-zinc-300">
                                    {t.docs.architecture.stage3.description}
                                </p>
                                <ul className="!text-zinc-700 dark:!text-zinc-300 space-y-2">
                                    <li><strong>{t.docs.architecture.stage3.profGen.split(':')[0]}:</strong> {t.docs.architecture.stage3.profGen.split(':')[1]}</li>
                                    <li><strong>{t.docs.architecture.stage3.constApp.split(':')[0]}:</strong> {t.docs.architecture.stage3.constApp.split(':')[1]}</li>
                                    <li><strong>{t.docs.architecture.stage3.incRes.split(':')[0]}:</strong> {t.docs.architecture.stage3.incRes.split(':')[1]}</li>
                                    <li><strong>{t.docs.architecture.stage3.invEmit.split(':')[0]}:</strong> {t.docs.architecture.stage3.invEmit.split(':')[1]}</li>
                                </ul>
                            </div>
                        </section>

                        {/* BBR to IR */}
                        <section id="bbr-to-ir" className="glass rounded-2xl p-8 border border-zinc-200 dark:border-zinc-800">
                            <h2 className="text-3xl font-bold !text-[#18181b] dark:!text-zinc-100 mb-4">
                                {t.docs.bbrToIr.title}
                            </h2>
                            <div className="prose prose-zinc dark:prose-invert max-w-none">
                                <p className="!text-zinc-700 dark:!text-zinc-300 text-lg leading-relaxed">
                                    {t.docs.bbrToIr.description}
                                </p>

                                <h3 className="!text-[#18181b] dark:!text-zinc-100 text-xl font-semibold mt-8 mb-4">
                                    {t.docs.bbrToIr.templateSelection.title}
                                </h3>
                                <div className="bg-zinc-100/50 dark:bg-zinc-800/50 rounded-lg p-4 my-4">
                                    <p className="!text-zinc-700 dark:!text-zinc-300 mb-3">
                                        {t.docs.bbrToIr.templateSelection.description}
                                    </p>
                                    <ol className="!text-zinc-700 dark:!text-zinc-300 space-y-2">
                                        <li>{t.docs.bbrToIr.templateSelection.filterInactive}</li>
                                        <li>{t.docs.bbrToIr.templateSelection.keepLatest}</li>
                                        <li>{t.docs.bbrToIr.templateSelection.applyFilters}</li>
                                        <li>{t.docs.bbrToIr.templateSelection.expandIncludes}</li>
                                    </ol>
                                </div>

                                <h3 className="!text-[#18181b] dark:!text-zinc-100 text-xl font-semibold mt-8 mb-4">
                                    {t.docs.bbrToIr.pathNormalization.title}
                                </h3>
                                <p className="!text-zinc-700 dark:!text-zinc-300 mb-4">
                                    {t.docs.bbrToIr.pathNormalization.description}
                                </p>
                                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 my-4">
                                    <div className="bg-red-500/5 border border-red-500/20 rounded-lg p-4">
                                        <div className="text-sm font-semibold !text-red-600 dark:!text-red-400 mb-2">
                                            {t.docs.bbrToIr.pathNormalization.before}
                                        </div>
                                        <code className="text-xs !text-zinc-700 dark:!text-zinc-300">
                                            hl7:recordTarget/@classCode
                                        </code>
                                    </div>
                                    <div className="bg-green-500/5 border border-green-500/20 rounded-lg p-4">
                                        <div className="text-sm font-semibold !text-green-600 dark:!text-green-400 mb-2">
                                            {t.docs.bbrToIr.pathNormalization.after}
                                        </div>
                                        <code className="text-xs !text-zinc-700 dark:!text-zinc-300">
                                            recordTarget.classCode
                                        </code>
                                    </div>
                                </div>

                                <h3 className="!text-[#18181b] dark:!text-zinc-100 text-xl font-semibold mt-8 mb-4">
                                    {t.docs.bbrToIr.elementMapping.title}
                                </h3>
                                <div className="space-y-4">
                                    <div className="border border-zinc-300 dark:border-zinc-700 rounded-lg overflow-hidden">
                                        <table className="w-full">
                                            <thead className="bg-zinc-100 dark:bg-zinc-800">
                                                <tr>
                                                    <th className="px-4 py-2 text-left text-sm font-semibold !text-[#18181b] dark:!text-zinc-100">
                                                        {t.docs.bbrToIr.elementMapping.bbrElement}
                                                    </th>
                                                    <th className="px-4 py-2 text-left text-sm font-semibold !text-[#18181b] dark:!text-zinc-100">
                                                        {t.docs.bbrToIr.elementMapping.irMapping}
                                                    </th>
                                                </tr>
                                            </thead>
                                            <tbody className="divide-y divide-zinc-200 dark:divide-zinc-700">
                                                <tr>
                                                    <td className="px-4 py-3 text-sm !text-zinc-700 dark:!text-zinc-300">
                                                        minimumMultiplicity / maximumMultiplicity
                                                    </td>
                                                    <td className="px-4 py-3 text-sm !text-zinc-700 dark:!text-zinc-300">
                                                        IRCardinality (min..max)
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td className="px-4 py-3 text-sm !text-zinc-700 dark:!text-zinc-300">
                                                        attribute @value
                                                    </td>
                                                    <td className="px-4 py-3 text-sm !text-zinc-700 dark:!text-zinc-300">
                                                        fixedValue + type inference
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td className="px-4 py-3 text-sm !text-zinc-700 dark:!text-zinc-300">
                                                        Vocabulary binding
                                                    </td>
                                                    <td className="px-4 py-3 text-sm !text-zinc-700 dark:!text-zinc-300">
                                                        IRBinding (valueSet + strength)
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td className="px-4 py-3 text-sm !text-zinc-700 dark:!text-zinc-300">
                                                        include ref
                                                    </td>
                                                    <td className="px-4 py-3 text-sm !text-zinc-700 dark:!text-zinc-300">
                                                        IRTemplateInclude
                                                    </td>
                                                </tr>
                                                <tr>
                                                    <td className="px-4 py-3 text-sm !text-zinc-700 dark:!text-zinc-300">
                                                        assert count expression
                                                    </td>
                                                    <td className="px-4 py-3 text-sm !text-zinc-700 dark:!text-zinc-300">
                                                        IRInvariant (FHIRPath)
                                                    </td>
                                                </tr>
                                            </tbody>
                                        </table>
                                    </div>
                                </div>

                                <h3 className="!text-[#18181b] dark:!text-zinc-100 text-xl font-semibold mt-8 mb-4">
                                    {t.docs.bbrToIr.fixedValues.title}
                                </h3>
                                <div className="bg-amber-500/5 border border-amber-500/20 rounded-lg p-4">
                                    <p className="!text-zinc-700 dark:!text-zinc-300 mb-3">
                                        {t.docs.bbrToIr.fixedValues.description}
                                    </p>
                                    <ul className="!text-zinc-700 dark:!text-zinc-300 space-y-2">
                                        <li>
                                            <strong>{t.docs.bbrToIr.fixedValues.boolean.split(':')[0]}:</strong> {t.docs.bbrToIr.fixedValues.boolean.split(':')[1]}
                                        </li>
                                        <li>
                                            <strong>{t.docs.bbrToIr.fixedValues.code.split(':')[0]}:</strong> {t.docs.bbrToIr.fixedValues.code.split(':')[1]}
                                        </li>
                                        <li>
                                            <strong>{t.docs.bbrToIr.fixedValues.string.split(':')[0]}:</strong> {t.docs.bbrToIr.fixedValues.string.split(':')[1]}
                                        </li>
                                    </ul>
                                </div>
                            </div>
                        </section>

                        {/* IR to FSH */}
                        <section id="ir-to-fsh" className="glass rounded-2xl p-8 border border-zinc-200 dark:border-zinc-800">
                            <h2 className="text-3xl font-bold !text-[#18181b] dark:!text-zinc-100 mb-4">
                                {t.docs.irToFsh.title}
                            </h2>
                            <div className="prose prose-zinc dark:prose-invert max-w-none">
                                <p className="!text-zinc-700 dark:!text-zinc-300 text-lg leading-relaxed">
                                    The final stage converts validated IR into FHIR Shorthand profiles.
                                </p>

                                <h3 className="!text-[#18181b] dark:!text-zinc-100 text-xl font-semibold mt-8 mb-4">
                                    Profile Identity Resolution
                                </h3>
                                <div className="bg-zinc-100/50 dark:bg-zinc-800/50 rounded-lg p-4 my-4">
                                    <p className="!text-zinc-700 dark:!text-zinc-300 mb-3">
                                        Each IR template generates a profile with:
                                    </p>
                                    <ul className="!text-zinc-700 dark:!text-zinc-300 space-y-2">
                                        <li>
                                            <strong>Profile Name:</strong> profileNameOverrides[templateId] or profilePrefix + rootCdaType
                                        </li>
                                        <li>
                                            <strong>Profile ID:</strong> idOverrides[templateId] or idPrefix + kebab-case(rootCdaType)
                                        </li>
                                        <li>
                                            <strong>Title:</strong> titlePrefix + lowerFirst(rootCdaType)
                                        </li>
                                        <li>
                                            <strong>Description:</strong> template.description or displayName or name
                                        </li>
                                    </ul>
                                </div>

                                <h3 className="!text-[#18181b] dark:!text-zinc-100 text-xl font-semibold mt-8 mb-4">
                                    FSH Constraint Generation
                                </h3>
                                <div className="space-y-4">
                                    <div className="border border-zinc-300 dark:border-zinc-700 rounded-lg p-4">
                                        <h4 className="font-semibold !text-[#18181b] dark:!text-zinc-100 mb-3">
                                            1. Cardinality Constraints
                                        </h4>
                                        <p className="!text-zinc-700 dark:!text-zinc-300 text-sm mb-2">
                                            IR cardinality is clamped to CDA base limits:
                                        </p>
                                        <div className="bg-zinc-50 dark:bg-zinc-900 rounded p-3 font-mono text-sm !text-zinc-700 dark:!text-zinc-300">
                                            * recordTarget 1..*
                                        </div>
                                    </div>

                                    <div className="border border-zinc-300 dark:border-zinc-700 rounded-lg p-4">
                                        <h4 className="font-semibold !text-[#18181b] dark:!text-zinc-100 mb-3">
                                            2. Fixed Values
                                        </h4>
                                        <p className="!text-zinc-700 dark:!text-zinc-300 text-sm mb-2">
                                            Only applied if CDA base doesn&apos;t have a fixed value:
                                        </p>
                                        <div className="bg-zinc-50 dark:bg-zinc-900 rounded p-3 font-mono text-sm !text-zinc-700 dark:!text-zinc-300">
                                            * recordTarget.patientRole.classCode = #PAT
                                        </div>
                                    </div>

                                    <div className="border border-zinc-300 dark:border-zinc-700 rounded-lg p-4">
                                        <h4 className="font-semibold !text-[#18181b] dark:!text-zinc-100 mb-3">
                                            3. ValueSet Bindings
                                        </h4>
                                        <p className="!text-zinc-700 dark:!text-zinc-300 text-sm mb-2">
                                            Strength is never weakened from CDA base:
                                        </p>
                                        <div className="bg-zinc-50 dark:bg-zinc-900 rounded p-3 font-mono text-sm !text-zinc-700 dark:!text-zinc-300">
                                            * code from http://example.org/vs (required)
                                        </div>
                                    </div>

                                    <div className="border border-zinc-300 dark:border-zinc-700 rounded-lg p-4">
                                        <h4 className="font-semibold !text-[#18181b] dark:!text-zinc-100 mb-3">
                                            4. Template Includes
                                        </h4>
                                        <p className="!text-zinc-700 dark:!text-zinc-300 text-sm mb-2">
                                            Converted to &quot;only&quot; constraints:
                                        </p>
                                        <div className="bg-zinc-50 dark:bg-zinc-900 rounded p-3 font-mono text-sm !text-zinc-700 dark:!text-zinc-300">
                                            * component.structuredBody only StructuredBodyProfile
                                        </div>
                                    </div>
                                </div>

                                <h3 className="!text-[#18181b] dark:!text-zinc-100 text-xl font-semibold mt-8 mb-4">
                                    Complete Example
                                </h3>
                                <div className="bg-zinc-50 dark:bg-zinc-900 border border-zinc-300 dark:border-zinc-700 rounded-lg p-4 overflow-x-auto">
                                    <pre className="text-sm !text-zinc-700 dark:!text-zinc-300 whitespace-pre-wrap">
{`Profile: ClinicalDocumentExample
Parent: http://hl7.org/cda/stds/core/StructureDefinition/ClinicalDocument
Id: clinical-document-example
Title: "clinicalDocumentExample"
Description: "Example CDA Clinical Document profile"
* ^status = #draft
* recordTarget 1..*
* recordTarget.patientRole.classCode = #PAT
* recordTarget.patientRole.id 1..1
* recordTarget.patientRole.id from urn:oid:2.16.840.1.113883.1.11.20.12 (extensible)
* obeys ClinicalDocumentInv1

Invariant: ClinicalDocumentInv1
Description: "recordTarget must have at least one id"
Severity: #error
Expression: "recordTarget.patientRole.id.count() >= 1"`}
                                    </pre>
                                </div>
                            </div>
                        </section>

                        {/* Usage */}
                        <section id="usage" className="glass rounded-2xl p-8 border border-zinc-200 dark:border-zinc-800">
                            <h2 className="text-3xl font-bold !text-[#18181b] dark:!text-zinc-100 mb-4">
                                {t.docs.usage.title}
                            </h2>
                            <div className="prose prose-zinc dark:prose-invert max-w-none">
                                <h3 className="!text-[#18181b] dark:!text-zinc-100 text-xl font-semibold mb-4">
                                    Web Interface
                                </h3>
                                <div className="space-y-4">
                                    <div className="bg-indigo-500/5 border border-indigo-500/20 rounded-lg p-4">
                                        <ol className="!text-zinc-700 dark:!text-zinc-300 space-y-2">
                                            <li><strong>Upload BBR:</strong> Upload your ART-DECOR BBR XML file</li>
                                            <li><strong>Configure Options:</strong> Select SUSHI layout, enable IR/logs output</li>
                                            <li><strong>Optional YAML:</strong> Provide custom generation configuration</li>
                                            <li><strong>Generate:</strong> Click &quot;Generate FSH Package&quot; button</li>
                                            <li><strong>Download:</strong> Get your FSH profiles as a ZIP file</li>
                                        </ol>
                                    </div>
                                </div>

                                <h3 className="!text-[#18181b] dark:!text-zinc-100 text-xl font-semibold mt-8 mb-4">
                                    Configuration Options
                                </h3>
                                <div className="space-y-3">
                                    <div className="border border-zinc-300 dark:border-zinc-700 rounded-lg p-4">
                                        <h4 className="font-semibold !text-[#18181b] dark:!text-zinc-100 mb-2">
                                            Sushi Repository Layout
                                        </h4>
                                        <p className="text-sm !text-zinc-700 dark:!text-zinc-300">
                                            Generate a complete SUSHI project with sushi-config.yaml and input/fsh/ structure
                                        </p>
                                    </div>
                                    <div className="border border-zinc-300 dark:border-zinc-700 rounded-lg p-4">
                                        <h4 className="font-semibold !text-[#18181b] dark:!text-zinc-100 mb-2">
                                            Show Generation Logs
                                        </h4>
                                        <p className="text-sm !text-zinc-700 dark:!text-zinc-300">
                                            Display detailed transformation logs with warnings and errors
                                        </p>
                                    </div>
                                    <div className="border border-zinc-300 dark:border-zinc-700 rounded-lg p-4">
                                        <h4 className="font-semibold !text-[#18181b] dark:!text-zinc-100 mb-2">
                                            Include IR (Intermediate Representation)
                                        </h4>
                                        <p className="text-sm !text-zinc-700 dark:!text-zinc-300">
                                            Output axiom-cda-ir.json snapshot and view IR in the UI for debugging
                                        </p>
                                    </div>
                                </div>

                                <h3 className="!text-[#18181b] dark:!text-zinc-100 text-xl font-semibold mt-8 mb-4">
                                    YAML Configuration
                                </h3>
                                <div className="bg-zinc-50 dark:bg-zinc-900 border border-zinc-300 dark:border-zinc-700 rounded-lg p-4 overflow-x-auto">
                                    <pre className="text-sm !text-zinc-700 dark:!text-zinc-300">
{`naming:
  profilePrefix: "Custom"
  idPrefix: "custom-"
  titlePrefix: "Custom "

valueSetPolicy:
  defaultStrength: EXTENSIBLE
  useOidAsCanonical: true
  oidToCanonical:
    "2.16.840.1.113883.1.11.1": "http://hl7.org/fhir/ValueSet/administrative-gender"

templateSelection:
  templateIds:
    - "1.2.3.4.5"
    - "2.3.4.5.6"

emitInvariants: true`}
                                    </pre>
                                </div>
                            </div>
                        </section>

                        {/* Limitations */}
                        <section id="limitations" className="glass rounded-2xl p-8 border border-zinc-200 dark:border-zinc-800">
                            <h2 className="text-3xl font-bold !text-[#18181b] dark:!text-zinc-100 mb-4">
                                {t.docs.limitations.title}
                            </h2>
                            <div className="prose prose-zinc dark:prose-invert max-w-none">
                                <p className="!text-zinc-700 dark:!text-zinc-300 mb-4">
                                    {t.docs.limitations.description}
                                </p>
                                <ul className="list-disc list-inside space-y-2 !text-zinc-700 dark:!text-zinc-300">
                                    <li>{t.docs.limitations.list.slicing}</li>
                                    <li>{t.docs.limitations.list.choice}</li>
                                    <li>{t.docs.limitations.list.mapping}</li>
                                </ul>
                                <p className="text-sm !text-zinc-700 dark:!text-zinc-300 mt-4">
                                    {t.docs.limitations.note}
                                </p>
                                <p className="text-sm !text-zinc-700 dark:!text-zinc-300 mt-2">
                                    <strong>{t.docs.limitations.roadmapTitle}</strong> {t.docs.limitations.roadmapDetail}
                                </p>
                            </div>
                        </section>

                        {/* API Reference */}
                        <section id="api" className="glass rounded-2xl p-8 border border-zinc-200 dark:border-zinc-800">
                            <h2 className="text-3xl font-bold !text-[#18181b] dark:!text-zinc-100 mb-4">
                                {t.docs.api.title}
                            </h2>
                            <div className="prose prose-zinc dark:prose-invert max-w-none">
                                <h3 className="!text-[#18181b] dark:!text-zinc-100 text-xl font-semibold mb-4">
                                    POST /api/generate
                                </h3>
                                <div className="bg-zinc-50 dark:bg-zinc-900 border border-zinc-300 dark:border-zinc-700 rounded-lg p-4">
                                    <p className="!text-zinc-700 dark:!text-zinc-300 mb-4">
                                        Generate FSH profiles from BBR input
                                    </p>

                                    <h4 className="font-semibold !text-[#18181b] dark:!text-zinc-100 mt-4 mb-2">Request Body</h4>
                                    <pre className="text-sm !text-zinc-700 dark:!text-zinc-300 bg-zinc-100 dark:bg-zinc-800 rounded p-3 overflow-x-auto">
{`{
  "bbr": "<BBR XML content or URL>",
  "sushiRepo": true,
  "emitIr": false,
  "emitLogs": true,
  "yamlConfig": "profilePrefix: Custom\\n..."
}`}
                                    </pre>

                                    <h4 className="font-semibold !text-[#18181b] dark:!text-zinc-100 mt-4 mb-2">Response</h4>
                                    <pre className="text-sm !text-zinc-700 dark:!text-zinc-300 bg-zinc-100 dark:bg-zinc-800 rounded p-3 overflow-x-auto">
{`{
  "zipBase64": "UEsDBBQAAAAI...",
  "report": {
    "templatesConsidered": 10,
    "templatesGenerated": 8,
    "templatesSkipped": 2,
    "templatesOk": 7,
    "profilesGenerated": 8,
    "invariantsGenerated": 12,
    "warnings": ["..."],
    "errors": []
  },
  "profiles": [
    {
      "name": "ClinicalDocument",
      "content": "Profile: ClinicalDocument\\n..."
    }
  ],
  "irTemplates": [...]
}`}
                                    </pre>
                                </div>
                            </div>
                        </section>
                    </main>
                </div>
            </div>

            {/* Footer */}
            <footer className="mt-16 border-t border-card-border bg-card/50 backdrop-blur-md py-6">
                <div className="max-w-7xl mx-auto px-6 text-center text-sm text-zinc-500 dark:text-zinc-400">
                    <p>© Kereval 2026 - Axiom CDA FSH Transformation</p>
                </div>
            </footer>
        </div>
    );
}
