import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // "standalone" traces the minimal set of files/node_modules actually
  // needed to run the built app and outputs them to .next/standalone -
  // this is what lets the Docker runtime stage copy a small, self-
  // contained folder instead of the full node_modules tree.
  output: "standalone",
};

export default nextConfig;
