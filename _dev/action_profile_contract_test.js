const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const contract = JSON.parse(fs.readFileSync(path.join(__dirname, 'action_profile_contract.json'), 'utf8'));
assert.equal(contract.offline, true, 'action/profile must remain offline by default');
const dailySum = Object.values(contract.dailyWeights).reduce((a, b) => a + b, 0);
assert.equal(Math.round(dailySum * 100), 100, 'daily weights must sum to 1');
assert.deepEqual(contract.requiredSources, ['FiveElements', 'Zodiac']);
assert.equal(contract.tieBreak.changesMainScore, false);
assert.equal(contract.tieBreak.scope, 'profileKey|dateKey|candidateKey');
assert.ok(contract.mealCandidates.length >= 8, 'meal directory must contain concrete candidates');
for (const meal of contract.mealCandidates) {
  for (const field of ['key', 'ingredients', 'substitute', 'deliveryKeywords', 'evidence']) {
    assert.ok(meal[field], `${meal.key}: missing ${field}`);
  }
  assert.ok(meal.ingredients.length > 0, `${meal.key}: ingredients empty`);
  assert.ok(meal.substitute.length > 0, `${meal.key}: substitute empty`);
  assert.ok(meal.deliveryKeywords.length > 0, `${meal.key}: delivery keywords empty`);
  for (const source of contract.requiredSources) assert.ok(meal.evidence.includes(source), `${meal.key}: missing ${source}`);
}
assert.equal(contract.regionMax, 3);
for (const word of contract.forbiddenText) assert.ok(word.length > 0, 'forbidden phrase must not be empty');
console.log(`action/profile contract: PASS (${contract.mealCandidates.length} meals, offline, deterministic tie-break)`);
