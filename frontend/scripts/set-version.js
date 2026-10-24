const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// Read package.json
const packageJson = require('../package.json');
const version = packageJson.version;

// Get git commit hash (short, 7 characters)
// First try environment variable (set in Docker build), then fallback to git command
let commitHash = process.env.GIT_COMMIT_HASH
  ? process.env.GIT_COMMIT_HASH.substring(0, 7)
  : 'unknown';
if (commitHash === 'unknown') {
  try {
    commitHash = execSync('git rev-parse --short=7 HEAD', { encoding: 'utf8' }).trim();
  } catch (error) {
    console.warn('Could not retrieve git commit hash:', error.message);
  }
}

// Combine version with commit hash
const fullVersion = `${version}-${commitHash}`;

// Create version.ts
const versionFilePath = path.join(__dirname, '../src/environments/version.ts');
const versionFileContent = `// Auto-generated file. Do not edit manually.
export const version = '${fullVersion}';
`;

fs.writeFileSync(versionFilePath, versionFileContent);
console.log(`Version ${fullVersion} written to ${versionFilePath}`);
