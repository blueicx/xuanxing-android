const fs = require('node:fs');
const path = require('node:path');

const fixturePath = path.join(__dirname, '..', 'app', 'src', 'test', 'resources', 'dialogue_relevance_cases.json');
const fixture = JSON.parse(fs.readFileSync(fixturePath, 'utf8'));
const expectedCharacters = ['shen_yanzhou', 'mo_heng', 'evelyn_nova', 'nadir_rashid'];
const requiredIntents = [
  'Greeting', 'Farewell', 'Thanks', 'Identity', 'Smalltalk', 'TodayMeal', 'TodayActivity',
  'TodayOuting', 'LifeProfile', 'Why', 'Fortune', 'Career', 'Love', 'Wealth', 'Study',
  'Mood', 'Health', 'Game', 'Tarot', 'Rune', 'Numerology', 'Weather', 'Place',
  'Personality', 'Outcome', 'Daily', 'Chat'
];
const allowedActions = new Set([
  'none', 'open_conversation', 'meal', 'activity', 'outing', 'life_profile', 'evidence',
  'open_game:poetry_chain', 'open_game:star_map_observation', 'open_game:silkroad_route', 'resume_xiangqi'
]);

function assert(condition, message) {
  if (!condition) throw new Error(message);
}

assert(fixture.version === 1, 'unsupported relevance fixture version');
assert(JSON.stringify(fixture.characters) === JSON.stringify(expectedCharacters), 'four-character roster/order changed unexpectedly');
assert(fixture.cases.length >= 200, 'expanded matrix must contain at least 200 authored input cases');

const ids = fixture.cases.map((testCase) => testCase.id);
assert(ids.every(Boolean) && new Set(ids).size === ids.length, 'each case needs a unique stable id');
assert(fixture.cases.every((testCase) => typeof testCase.input === 'string' && testCase.expectedIntent && testCase.expectedAction), 'every case needs input, intent and action expectations');
assert(fixture.cases.every((testCase) => allowedActions.has(testCase.expectedAction)), 'fixture contains an unknown action route');

const presentIntents = new Set(fixture.cases.map((testCase) => testCase.expectedIntent));
for (const intent of requiredIntents) assert(presentIntents.has(intent), `missing dialogue intent coverage: ${intent}`);
assert(fixture.cases.some((testCase) => testCase.expectedSafety === 'Health'), 'missing health safety case');
assert(fixture.cases.some((testCase) => testCase.expectedSafety === 'Finance'), 'missing finance safety case');
assert(fixture.cases.some((testCase) => testCase.needsClarification === true), 'missing clarification case');
assert(fixture.cases.some((testCase) => testCase.recentTurnKind), 'missing multi-turn follow-up case');
const engineTest = fs.readFileSync(path.join(__dirname, '..', 'app/src/test/kotlin/com/xuanji/app/domain/MysticDialogueRelevanceMatrixTest.kt'), 'utf8');
assert(engineTest.includes('dailyActionPlan = actionPlan()') && engineTest.includes('lifeProfile = lifeProfile()'), 'JVM matrix must inject the same real fixture data used by the action router');
assert(engineTest.includes('first.text.contains(it)'), 'JVM matrix must assert concrete answer content, not only intent labels');

console.log(`Dialogue relevance fixture contract passed: ${fixture.cases.length} inputs × ${fixture.characters.length} characters = ${fixture.cases.length * fixture.characters.length} cases (engine execution is verified by JVM tests)`);
