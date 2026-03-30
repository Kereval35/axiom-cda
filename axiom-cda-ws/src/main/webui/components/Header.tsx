"use client";

import Link from "next/link";
import Image from "next/image";
import { usePathname } from "next/navigation";
import { ThemeToggle } from "./ThemeToggle";
import { LanguageToggle } from "./LanguageToggle";
import { useLanguage } from "./LanguageProvider";
import { publicAsset } from "@/utils/config";

export const Header: React.FC = () => {
    const pathname = usePathname();
    const { t } = useLanguage();

    const navItems = [
        { label: t.header.nav.dashboard, href: "/" },
        { label: t.header.nav.documentation, href: "/docs" },
        { label: t.header.nav.presentation, href: "/presentation" },
    ];

    return (
        <header className="mb-8 border-b border-card-border pb-6">
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-8">
                    <div className="flex items-center gap-3">
                        <Image
                            src={publicAsset("gazelle.png")}
                            alt="Axiom CDA Logo"
                            width={40}
                            height={40}
                            className="object-contain"
                        />
                        <h1 className="text-2xl font-bold tracking-tight text-foreground">
                            {t.header.title}
                        </h1>
                    </div>
                    <nav className="flex items-center gap-1">
                        {navItems.map((item) => (
                            <Link
                                key={item.href}
                                href={item.href}
                                className={`px-4 py-2 rounded-lg text-sm font-medium transition-all ${pathname === item.href
                                    ? "bg-indigo-500/10 text-indigo-600 dark:text-indigo-400"
                                    : "text-zinc-600 dark:text-zinc-400 hover:text-indigo-500 hover:bg-zinc-100 dark:hover:bg-zinc-800"
                                    }`}
                            >
                                {item.label}
                            </Link>
                        ))}
                    </nav>
                </div>
                <div className="flex items-center gap-4">
                    <LanguageToggle />
                    <ThemeToggle />
                    <div className="hidden md:block px-3 py-1 rounded-full border border-card-border bg-card text-xs font-mono text-zinc-500">
                        {t.common.version}
                    </div>
                </div>
            </div>
            {pathname === "/" && (
                <div className="mt-8">
                    <p className="text-zinc-600 dark:text-zinc-400 text-lg">
                        {t.header.subtitle}
                    </p>
                </div>
            )}
        </header>
    );
};
