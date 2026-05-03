import { mkdirSync, readdirSync, writeFileSync } from 'node:fs';

function listTypeScriptFiles(dir) {
    return readdirSync(dir, { withFileTypes: true }).flatMap(entry => {
        const path = `${dir}/${entry.name}`;
        if (entry.isDirectory()) {
            return listTypeScriptFiles(path);
        }
        return entry.isFile() && entry.name.endsWith('.ts') ? [path] : [];
    });
}

const files = listTypeScriptFiles('src').slice(0, 20);
mkdirSync('reports', { recursive: true });
writeFileSync('reports/files-to-review.txt', files.length ? `${files.join('\n')}\n` : '');
console.log('Files to review:');
console.log(files.join('\n'));