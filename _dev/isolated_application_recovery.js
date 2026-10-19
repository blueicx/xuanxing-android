const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const { execFileSync, spawn } = require('node:child_process');
const root = path.join(__dirname, '..');
const adb = process.env.XUANJI_ADB || 'adb';
const port = process.env.XUANJI_ADB_PORT || '5038';
let serial = process.argv[2];
if (serial === '--auto') {
  const list = execFileSync(adb, ['-P', port, 'devices'], { encoding: 'utf8' });
  const emulators = list.split(/\r?\n/).filter(line => /^emulator-\d+\s+device\b/.test(line)).map(line => line.split(/\s+/)[0]);
  if (emulators.length !== 1) throw new Error('Exactly one emulator is required for auto selection');
  serial = emulators[0];
}
if (!serial || !/^(emulator-\d+|127\.0\.0\.1:\d+)$/.test(serial)) throw new Error('Only explicit isolated emulator serials are accepted');
const run = args => execFileSync(adb, ['-P', port, '-s', serial, ...args], { encoding: 'utf8', timeout: 120000 });
if (run(['shell', 'getprop', 'ro.kernel.qemu']).trim() !== '1') throw new Error('Target is not an emulator; no installation or writes allowed');
const pkg = 'com.xuanji.app.regression';
const appDir = path.join(root, 'app/build/outputs/apk/regression');
const testDir = path.join(root, 'app/build/outputs/apk/androidTest/regression');
for (const [dir, expected] of [[appDir, pkg], [testDir, `${pkg}.test`]]) {
  const metadata = JSON.parse(fs.readFileSync(path.join(dir, 'output-metadata.json'), 'utf8'));
  if (metadata.applicationId !== expected) throw new Error(`Refusing APK outside isolated scope: ${metadata.applicationId}`);
}
const appApk = path.join(appDir, 'app-regression.apk');
const testApk = path.join(testDir, 'app-regression-androidTest.apk');
const out = path.join(root, 'work/.dispatch-backup/20261014-application-recovery', new Date().toISOString().replace(/[:.]/g, '-'));
fs.mkdirSync(out, { recursive: true });
const evidence = (name, output) => { fs.writeFileSync(path.join(out, name), output); process.stdout.write(output); };
evidence('install.log', run(['install', '-r', appApk]) + run(['install', '-r', testApk]));
// First-run system education intercepts Back, unrelated to application recovery.
// This is deliberately after the emulator and separate-package guards.
evidence('emulator-immersive-confirmation.log', run(['shell', 'settings', 'put', 'secure', 'immersive_mode_confirmations', 'confirmed']));
function phase(method) {
  const result = run(['shell', 'am', 'instrument', '-w', '-r', '-e', 'class', `com.xuanji.app.ui.ApplicationRecoveryJourneyTest#${method}`, `${pkg}.test/androidx.test.runner.AndroidJUnitRunner`]);
  evidence(`${method}.log`, result);
  if (!/OK \(1 test\)/.test(result) || /FAILURES|INSTRUMENTATION_FAILED/.test(result)) throw new Error(`Interaction phase failed: ${method}; retained evidence: ${out}`);
}
phase('prepare_navigation_and_progress');
evidence('launch-before-kill.log', run(['shell', 'am', 'start', '-W', '-n', `${pkg}/com.xuanji.app.MainActivity`]));
const before = run(['shell', 'pidof', pkg]).trim();
if (!/^\d+$/.test(before)) throw new Error('No live app process to terminate');
evidence('force-stop.log', run(['shell', 'am', 'force-stop', pkg]));
try {
  if (run(['shell', 'pidof', pkg]).trim()) throw new Error('Process survived termination');
} catch (error) {
  // pidof returns exit 1 when the exact isolated process has stopped.
  if (error.status !== 1) throw error;
}
evidence('launch-after-kill.log', run(['shell', 'am', 'start', '-W', '-n', `${pkg}/com.xuanji.app.MainActivity`]));
const after = run(['shell', 'pidof', pkg]).trim();
if (!/^\d+$/.test(after) || before === after) throw new Error('Restart did not produce a different live PID');
phase('verify_after_process_restart');
const memoryFile = path.join(out,'application-memory.json');
spawn(process.execPath,[path.join(root,'_dev/isolated_memory_sampler.js'),serial,memoryFile],{stdio:'ignore',env:process.env});
try { phase('four_roles_handoff_and_independent_game_progress_use_real_application'); }
finally { fs.writeFileSync(memoryFile + '.stop','stop'); }
const memoryReport = JSON.parse(fs.readFileSync(memoryFile,'utf8'));
if(memoryReport.sampleCount < 3) throw Error('Too few actual application PSS samples');
const manifest = { package: pkg, emulator: serial, beforePid: before, afterPid: after, appSha256: crypto.createHash('sha256').update(fs.readFileSync(appApk)).digest('hex'),
  applicationMemory: {sampleCount:memoryReport.sampleCount,observedPeakPssKiB:memoryReport.observedPeakPssKiB,medianPssKiB:memoryReport.medianPssKiB},
  testSha256: crypto.createHash('sha256').update(fs.readFileSync(testApk)).digest('hex'),
  verified: ['real MainActivity navigation', 'real offline food reply and action card', 'temporary food/time constraints without preference writes',
    'second candidate reply/card/evidence agreement', 'constraint/reference recovery', 'real Silk Road progression', 'OS process termination', 'role/drawer/game recovery', 'back to orb and reopen',
    'structured inventory and confirmed purchase survive OS process restart', 'weekly draft parameters survive OS process restart and remain profile isolated',
    'explicit journal fields survive OS process restart',
    'future preference documents stop real daily and weekly recommendations without clearing data',
    'four-role handoff', 'poetry/star-map/Xiangqi progression and local saves', 'second synthetic birth profile does not read original Silk Road save'],
  boundaries: 'separate application data directory; only synthetic fixture; no production app or phone operations' };
evidence('manifest.json', JSON.stringify(manifest, null, 2));
process.stdout.write(`\nApplication process-recovery PASS. Evidence: ${out}\n`);
