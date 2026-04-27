import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  basePath: "/axiom-cda",
  assetPrefix: "/axiom-cda",
  output: "export",
  trailingSlash: true,
  images: {
    unoptimized: true,
  },
};

export default nextConfig;
