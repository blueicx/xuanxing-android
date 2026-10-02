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
assert.deepEqual(contract.explicitFeedback, {
  acceptedPointsPerChoice: 1,
  acceptedCap: 5,
  notSuitablePointsPerChoice: -2,
  notSuitableFloor: -8,
  replacementScope: 'profileKey|category|dateKey|candidateKey',
  replacementUndo: 'remove-only-the-replacement-event',
  changesBaseScore: false
});
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
const root = path.resolve(__dirname, '..');
const feedbackSource = fs.readFileSync(path.join(root, 'app/src/main/java/com/xuanji/app/domain/action/ActionFeedback.kt'), 'utf8');
const plannerSource = fs.readFileSync(path.join(root, 'app/src/main/java/com/xuanji/app/domain/action/DailyActionPlanner.kt'), 'utf8');
const cardSource = fs.readFileSync(path.join(root, 'app/src/main/java/com/xuanji/app/ui/components/DailyActionCards.kt'), 'utf8');
const profileSource = fs.readFileSync(path.join(root, 'app/src/main/java/com/xuanji/app/ui/profile/ProfileScreen.kt'), 'utf8');
const repositorySource = fs.readFileSync(path.join(root, 'app/src/main/java/com/xuanji/app/data/repository/ActionRepository.kt'), 'utf8');
const applicationSource = fs.readFileSync(path.join(root, 'app/src/main/java/com/xuanji/app/XuanjiApplication.kt'), 'utf8');
assert.ok(feedbackSource.includes('coerceAtMost(5)'), 'accepted feedback cap must be implemented');
assert.ok(feedbackSource.includes('notSuitable * 2') && feedbackSource.includes('coerceIn(-8, 5)'), 'not-suitable penalty bounds must be implemented');
assert.ok(plannerSource.includes('applyExplicitFeedback'), 'planner must apply explicit feedback after candidate filtering');
assert.ok(repositorySource.includes('feedbackStore?.list(profileKey)'), 'repository must load feedback only for the active profile');
assert.ok(applicationSource.includes('feedbackStore = actionFeedbackStore'), 'application must wire persistent action feedback into the repository');
assert.ok(cardSource.includes('Text("换一个")') && cardSource.includes('Text("撤销上次换选")'), 'daily-action details must expose replacement and undo');
assert.ok(profileSource.includes('清除本机行动反馈') && profileSource.includes('确认清除本机行动反馈'), 'current-profile action feedback must have a visible, confirmed clear action');
console.log(`action/profile contract: PASS (${contract.mealCandidates.length} meals, offline, deterministic ranking, reversible feedback)`);
