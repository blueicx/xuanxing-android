const fs = require('node:fs');
const assert = require('node:assert/strict');
const path = require('node:path');
const root = path.join(__dirname, '..');
const file = path.join(root, 'app/src/main/java/com/xuanji/app/domain/action/OfflineRegionComparison.kt');
assert(fs.existsSync(file), 'sourced offline climate comparison is missing');
const source = fs.readFileSync(file, 'utf8');
assert(source.includes('1991–2020') && source.includes('stationName') && source.includes('sourceUrl'));
assert(source.includes('不计分') && source.includes('非实时天气'));
for (const marker of ['GriddedReanalysis', 'StationNormal', 'NASA POWER / MERRA-2', 'KNMI', 'start=1991&end=2020', 'it.isFinite()']) {
  assert(source.includes(marker), `climate provenance or validation missing: ${marker}`);
}
const panel = fs.readFileSync(path.join(root, 'app/src/main/java/com/xuanji/app/ui/components/RegionPreferenceComparisonSection.kt'), 'utf8');
for (const tag of ['region-show-all', 'region-source-', 'region-evidence-']) assert(panel.includes(tag));
assert(panel.includes('rememberSaveable(profileKey)') && panel.includes('comparisons.size else 6'));
const tests = fs.readFileSync(path.join(root, 'app/src/test/kotlin/com/xuanji/app/domain/action/OfflineRegionComparisonTest.kt'), 'utf8');
assert(tests.includes('assertEquals(24, CityClimateNormals.all.size)'));
const interaction = fs.readFileSync(path.join(root, 'app/src/androidTest/kotlin/com/xuanji/app/ui/components/RegionComparisonInteractionTest.kt'), 'utf8');
assert(interaction.includes('performClick()') && interaction.includes('emulateSavedInstanceStateRestore()'));
console.log('Offline region comparison contract PASS');
