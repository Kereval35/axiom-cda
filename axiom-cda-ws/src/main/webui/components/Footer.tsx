"use client";

import React from 'react';
import Image from 'next/image';
import { useLanguage } from './LanguageProvider';
import { publicAsset } from '@/utils/config';

export const Footer: React.FC = () => {
    const { t } = useLanguage();

    return (
        <footer className="mt-auto border-t border-card-border bg-card/50 backdrop-blur-md py-8">
            <div className="max-w-7xl mx-auto px-6">
                <div className="flex flex-col md:flex-row items-center justify-between gap-6">
                    <div className="flex items-center gap-6">
                        <div className="flex items-center">
                            <Image
                                src={publicAsset("Logo_Kereval_WR.png")}
                                alt="Kereval"
                                width={120}
                                height={40}
                                className="object-contain"
                                unoptimized
                            />
                        </div>

                        <div className="h-8 w-px bg-card-border hidden md:block"></div>

                        <div className="text-sm text-zinc-500 dark:text-zinc-400">
                            <p className="font-medium text-zinc-700 dark:text-zinc-300">© Kereval 2026</p>
                            <p className="text-xs">Advanced Engineering for Health</p>
                        </div>
                    </div>

                    <div className="flex items-center gap-4 text-xs font-medium text-zinc-500 dark:text-zinc-400">
                        <span>{t.footer.copyright}</span>
                        <span className="h-1 w-1 rounded-full bg-zinc-300 dark:bg-zinc-700"></span>
                        <span>{t.common.version}</span>
                    </div>
                </div>
            </div>
        </footer>
    );
};
