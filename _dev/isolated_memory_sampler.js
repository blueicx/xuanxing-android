// Runtime-generated evidence only; never read application files or user content.
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const [serial, destination] = process.argv.slice(2);
const allowedRoot = path.resolve(__dirname, '../work/.dispatch-backup') + path.sep;
if (!/^(emulator-\d+|127\.0\.0\.1:\d+)$/.test(serial || '') || !path.resolve(destination || '').startsWith(allowedRoot)) throw Error('Invalid isolated sampling scope');
const adb = process.env.XUANJI_ADB || 'adb';
const port = process.env.XUANJI_ADB_PORT || '5038';
const run = args => execFileSync(adb, ['-P',port,'-s',serial,...args], {encoding:'utf8',timeout:10000});
if (run(['shell','getprop','ro.kernel.qemu']).trim() !== '1') throw Error('Emulator required');
const pkg = 'com.xuanji.app.regression';
const samples = [];
let misses = 0;
const started = Date.now();
(async () => {
  while(Date.now() - started < 180000 && !fs.existsSync(destination + '.stop')) {
    try {
      const pid = run(['shell','pidof',pkg]).trim();
      const memory = run(['shell','dumpsys','meminfo',pkg]);
      const match = memory.match(/TOTAL PSS:\s*(\d+)/);
      if (/^\d+$/.test(pid) && match && Number(match[1]) > 0) samples.push({atMs:Date.now()-started,pid,pssKiB:Number(match[1])});
      else misses++;
    } catch { misses++; }
    if(samples.length) {
      const pss = samples.map(sample => sample.pssKiB).sort((a,b)=>a-b);
      fs.writeFileSync(destination + '.tmp', JSON.stringify({package:pkg, scope:'real regression app during four-role/game/profile journey; 250ms delay after each memory query; observed sampled peak, not absolute peak or physical-device acceptance',
        samples, misses, sampleCount:samples.length, observedPeakPssKiB:pss.at(-1), medianPssKiB:pss[Math.floor(pss.length/2)]},null,2));
      fs.renameSync(destination + '.tmp',destination);
    }
    await new Promise(resolve => setTimeout(resolve,250));
  }
})().catch(() => { process.exitCode = 1; });
