const fs = require('fs');
const path = require('path');
const assert = require('assert');
const contract = JSON.parse(fs.readFileSync(__dirname + '/dialogue_contract.json', 'utf8'));

const GAME_DIR = path.join(__dirname, '..', 'app', 'src', 'main', 'java', 'com', 'xuanji', 'app', 'domain', 'game');
const TEST_DIR = path.join(__dirname, '..', 'app', 'src', 'test', 'kotlin');
const readGame = (name) => fs.readFileSync(path.join(GAME_DIR, name), 'utf8');

// structure
assert.strictEqual(contract.defaultMode, 'offline');
assert(contract.grounded_reply_required === true);
assert(contract.rejection_codes.includes('self_check'));
assert(contract.rejection_codes.includes('game_over'));

// game_events must mirror the real sealed interface, not a remembered copy
const sessionSource = readGame('GameSessionState.kt');
const eventBlock = sessionSource.split('sealed interface GameEvent')[1].split('\n}')[0];
const declaredEvents = [...eventBlock.matchAll(/data (?:class|object)\s+(\w+)/g)].map((m) => m[1]);
assert(declaredEvents.length >= 6, 'GameEvent declarations must be discoverable');
assert.deepStrictEqual(contract.game_events, declaredEvents, 'game_events must mirror GameEvent in GameSessionState.kt');

// token discipline
assert(/token != state\.sessionToken/.test(contract.token_drop_rule));

// rejection codes must be exactly the ones XiangqiRules can return
const rulesSource = readGame('XiangqiRules.kt');
const declaredCodes = [...rulesSource.matchAll(/const val ERR_\w+ = "(\w+)"/g)].map((m) => m[1]).sort();
assert.deepStrictEqual(contract.rejection_codes.slice().sort(), declaredCodes, 'rejection_codes must mirror XiangqiRules');

// engine levels and draw wording must exist in the shipped source
const engineSource = readGame('SmartBoardEngine.kt');
contract.engine.levels.forEach((level) => {
  assert(engineSource.includes(`"${level}"`), `engine level ${level} is not implemented`);
});
assert.strictEqual(contract.engine.default, 'normal');
const bridgeSource = readGame('GameDialogueBridge.kt');
['双方不变作和', '无吃子限着判和', '和棋'].forEach((phrase) => {
  assert(bridgeSource.includes(phrase), `draw wording missing from the bridge: ${phrase}`);
});

// archive codec must stay board-only: the DTO and the honest degrade path are the contract
const archiveSource = readGame('GameArchive.kt');
['GameSave', 'dropped', 'Rejected'].forEach((symbol) => {
  assert(archiveSource.includes(symbol), `archive codec missing ${symbol}`);
});
assert(!/Mystic|fortune|bazi/i.test(archiveSource), 'archive codec must never touch fortune data');

// golden wording coverage (>= 12 entries per plan)
assert(contract.golden_wording.length >= 12, 'golden wording must have >= 12 entries');

// every golden line must name the Kotlin test that proves it, and that test must exist
const testFiles = fs.readdirSync(TEST_DIR, { recursive: true })
  .filter((file) => String(file).endsWith('.kt'))
  .map((file) => path.basename(String(file), '.kt'));
const seenInputs = new Set();
contract.golden_wording.forEach((entry) => {
  assert(entry.input && entry.expect, 'golden entries need an input and an expectation');
  assert(!seenInputs.has(entry.input), `duplicate golden input: ${entry.input}`);
  seenInputs.add(entry.input);
  const suite = entry.verify.split('.')[0];
  assert(suite, `golden entry "${entry.input}" must name a verifying test`);
  assert(testFiles.includes(suite), `golden entry "${entry.input}" points at a missing test: ${suite}`);
});

// everyday guards: safety intents must keep priority over game misreads
assert(contract.everyday_guards.length >= 4);

// unavailable games must expose explicit reason codes, not fake play
assert.strictEqual(contract.unavailable_games.go, 'go_provider_not_enabled');

// board encoding facts
assert(contract.board_encoding.initial.startsWith('rnbakabnr'));
assert(contract.board_encoding.uci.includes("rank 9 -> '0'"));
const notationSource = readGame('XiangqiNotation.kt');
assert(
  notationSource.includes("('9' - rank)") && notationSource.includes("'9' - char"),
  "uci row inversion must stay \"'9' - rank\" as documented"
);

console.log('dialogue contract: PASS (' + contract.golden_wording.length + ' golden entries)');
