import { execFileSync, spawnSync } from 'node:child_process';
import { existsSync, readFileSync, writeFileSync } from 'node:fs';

export function run(command, args = [], options = {}) {
    return execFileSync(command, args, {
        encoding: 'utf8',
        stdio: ['ignore', 'pipe', 'pipe'],
        ...options
    }).trim();
}

export function runInherited(command, args = [], options = {}) {
    execFileSync(command, args, {
        stdio: 'inherit',
        ...options
    });
}

export function firstLines(file, maxLines) {
    if (!existsSync(file)) {
        return '';
    }
    return readFileSync(file, 'utf8').split(/\r?\n/).slice(0, maxLines).join('\n');
}

export function headLines(file, maxLines) {
    return readFileSync(file, 'utf8').split(/\r?\n/).slice(0, maxLines).join('\n');
}

export function runCopilot(prompt, outputPath) {
    const args = [
        '--autopilot',
        '--yolo',
        '--max-autopilot-continues',
        '3',
        '--prompt',
        prompt
    ];

    if (process.env.COPILOT_MODEL) {
        args.unshift('--model', process.env.COPILOT_MODEL);
    }

    const result = spawnSync('copilot', args, {
        encoding: 'utf8',
        env: process.env,
        maxBuffer: 10 * 1024 * 1024
    });
    const output = `${result.stdout || ''}${result.stderr || ''}${result.error ? `\n${result.error.stack || result.error.message}\n` : ''}`;
    writeFileSync(outputPath, output);
    if (result.error) {
        return 127;
    }
    if (result.signal) {
        return 1;
    }
    return result.status ?? 1;
}