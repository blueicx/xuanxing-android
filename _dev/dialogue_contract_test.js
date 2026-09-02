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
const testRelPaths = fs.readdirSync(TEST_DIR, { recursive: true })
  .filter((file) => String(file).endsWith('.kt'))
  .map((file) => String(file));
const testFiles = testRelPaths.map((file) => path.basename(file, '.kt'));
const readTest = (suite) => {
  const rel = testRelPaths.find((file) => path.basename(file, '.kt') === suite);
  assert(rel, `no such test suite: ${suite}`);
  return fs.readFileSync(path.join(TEST_DIR, rel), 'utf8');
};
// a verify reference may go as deep as Suite.case, and that case must be the real method name
const requireVerify = (ref, label) => {
  const [suite, testCase] = ref.split('.');
  assert(suite, `${label} must name a verifying test`);
  assert(testFiles.includes(suite), `${label} points at a missing test: ${suite}`);
  if (testCase) {
    assert(readTest(suite).includes(testCase), `${label} names ${suite}.${testCase}, which no longer exists`);
  }
  return suite;
};
const seenInputs = new Set();
contract.golden_wording.forEach((entry) => {
  assert(entry.input && entry.expect, 'golden entries need an input and an expectation');
  assert(!seenInputs.has(entry.input), `duplicate golden input: ${entry.input}`);
  seenInputs.add(entry.input);
  requireVerify(entry.verify, `golden entry "${entry.input}"`);
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

// board UI locators and wording: the instrumented test needs a device to run, so its
// premises are checked here against the sources it taps instead of being trusted.
const UI_DIR = path.join(__dirname, '..', 'app', 'src', 'main', 'java', 'com', 'xuanji', 'app', 'ui', 'components', 'game');
const cardSource = fs.readFileSync(path.join(UI_DIR, 'GameBoardCard.kt'), 'utf8');
const glyphSource = fs.readFileSync(path.join(UI_DIR, 'XiangqiPieceGlyphs.kt'), 'utf8');
const uiTestSource = fs.readFileSync(
  path.join(__dirname, '..', 'app', 'src', 'androidTest', 'kotlin', 'com', 'xuanji', 'app', 'ui', 'components', 'game', 'GameBoardCardTest.kt'),
  'utf8'
);
assert(cardSource.includes(contract.board_ui.square_tag), 'square testTag must stay "square-$file-$rank"');
assert(cardSource.includes(contract.board_ui.difficulty_tag), 'difficulty testTag must stay "difficulty-$level"');
contract.board_ui.status_text.forEach((phrase) => {
  assert(cardSource.includes(`"${phrase}"`), `board status wording missing from GameBoardCard.kt: ${phrase}`);
});
assert(cardSource.includes(contract.board_ui.review_template), 'review label changed in GameBoardCard.kt');
const target = contract.board_ui.legal_target_description;
assert(cardSource.includes(`"${target}"`), 'legal-target description missing from GameBoardCard.kt');
assert(uiTestSource.includes(`"${target}"`), 'the instrumented test no longer asserts the legal-target rings');
contract.board_ui.piece_description_prefixes.forEach((side) => {
  assert(glyphSource.includes(`"${side}"`), `piece description prefix missing: ${side}`);
});
const uiCases = [...uiTestSource.matchAll(/^\s+@Test$/gm)].length;
assert(uiCases >= 12, `board UI must keep at least 12 instrumented cases, found ${uiCases}`);

// ---- 讲棋：解释只能来自规则，绝不来自引擎评分 --------------------------------------
const explanationSource = readGame('BoardExplanation.kt');
assert(!/evaluate|bestScore|Search\b/.test(explanationSource), 'BoardExplanation must never reach an engine evaluation');
['exposed', 'critique', 'safest', 'recapturersOf', 'XiangqiRules.legalMoves'].forEach((symbol) => {
  assert(explanationSource.includes(symbol), `BoardExplanation missing ${symbol}`);
});
assert(
  explanationSource.includes('position.withPiece(attacker, null).withPiece(square, piece)'),
  'the recapture probe must move the attacker onto the square before asking for a reply'
);
['InGameCommand.WHY', 'InGameCommand.SAFER', 'asksWhy', 'asksSafer'].forEach((symbol) => {
  assert(bridgeSource.includes(symbol), `bridge missing the explanation command ${symbol}`);
});
Object.values(contract.explanation.triggers).forEach((cues) => cues.forEach((cue) => {
  assert(bridgeSource.includes(`"${cue}"`), `classification cue changed: ${cue}`);
}));
['没人能吃回', '属于白送', '有子护着'].forEach((phrase) => {
  assert(bridgeSource.includes(phrase), `recapture wording missing from the bridge: ${phrase}`);
});
// a public evaluate() is how a rating would sneak back into a sentence
assert(engineSource.includes(contract.explanation.private_eval), 'SmartBoardEngine.evaluate must stay private');
assert(!/^\s*(?:suspend\s+)?fun evaluate\(/m.test(engineSource), 'evaluate() must not become callable from outside the engine');
const ratingBan = new RegExp(contract.explanation.forbidden_pattern, 'u');
[bridgeSource, explanationSource, engineSource].forEach((source, index) => {
  const hit = source.split('\n').find((line) => ratingBan.test(line));
  assert(!hit, `rating wording leaked into ${['GameDialogueBridge', 'BoardExplanation', 'SmartBoardEngine'][index]}: ${hit}`);
});
[contract.explanation.verify, contract.explanation.determinism_verify, contract.explanation.premise_verify]
  .forEach((ref) => requireVerify(ref, `explanation ${ref}`));

// ---- 本机长期记忆：只存用户自己的话，且必须能清掉 ----------------------------------
const APP_SRC = path.join(__dirname, '..', 'app', 'src', 'main', 'java', 'com', 'xuanji', 'app');
const recollectionSource = fs.readFileSync(path.join(APP_SRC, 'domain', 'MysticRecollection.kt'), 'utf8');
const memoryStoreSource = fs.readFileSync(path.join(APP_SRC, 'data', 'local', 'ConversationMemoryStore.kt'), 'utf8');
const generatorSource = fs.readFileSync(path.join(APP_SRC, 'domain', 'MysticGuideGenerator.kt'), 'utf8');
const mysticCardSource = fs.readFileSync(path.join(APP_SRC, 'ui', 'components', 'MysticGuideCard.kt'), 'utf8');
const cm = contract.conversation_memory;

// the kinds are the red line: a fourth wire means generated copy has found a place to hide
const kindBlock = recollectionSource.split('enum class RecollectionKind')[1].split('\n}')[0];
const declaredKinds = [...kindBlock.matchAll(/\("(\w+)"\)/g)].map((match) => match[1]);
assert.deepStrictEqual(cm.kinds, declaredKinds, 'conversation_memory.kinds must mirror RecollectionKind');
assert(recollectionSource.includes(`const val MAX_ENTRIES = ${cm.max_entries}`), 'the local cap moved without the contract');
assert(recollectionSource.includes('val isEmpty: Boolean'), 'RecallFacts must keep saying when there is nothing to recall');
assert(memoryStoreSource.includes(`KEY_PREFIX = "${cm.key_prefix}"`), 'memory key prefix changed in ConversationMemoryStore');
assert(memoryStoreSource.includes(`Charsets.${cm.digest_charset}`), 'the memory key digest must not follow the platform charset');
Object.entries(cm.neighbours).forEach(([prefix, file]) => {
  const neighbour = fs.readFileSync(path.join(APP_SRC, file), 'utf8');
  assert(neighbour.includes(`"${prefix}`), `${file} no longer writes ${prefix} — the neighbour list is stale`);
  assert(
    prefix !== cm.key_prefix && !prefix.startsWith(cm.key_prefix) && !cm.key_prefix.startsWith(prefix),
    `memory key ${cm.key_prefix} would collide with ${prefix}`
  );
});

// generated session notes must never reach the long-term write path
const rememberCalls = [...mysticCardSource.matchAll(/rememberLongTerm\((?:[^()]|\([^()]*\))*\)/g)].map((m) => m[0]);
assert(rememberCalls.length >= 8, `the memory write path must keep covering every user action, found ${rememberCalls.length}`);
rememberCalls.forEach((call) => {
  assert(!/memoryNote|memoryNotes/.test(call), `generated copy must not be stored long-term: ${call}`);
});
cm.fragments.forEach((fragment) => {
  assert(generatorSource.includes(fragment), `recall wording fragment missing from the generator: ${fragment}`);
});
cm.golden_wording.forEach((line) => {
  const pinned = generatorSource.includes(line) || mysticCardSource.includes(line) ||
    readTest('MysticGuideGeneratorTest').includes(line) ||
    readTest('ConversationMemoryStoreTest').includes(line);
  assert(pinned, `golden memory wording is no longer pinned anywhere: ${line}`);
});
assert(mysticCardSource.includes(`heightIn(min = ${cm.clear_button.min_height_dp}.dp)`), 'the clear button lost its 48dp target');
assert(
  mysticCardSource.includes(`contentDescription = "${cm.clear_button.content_description}"`),
  'the clear button lost its TalkBack label'
);
requireVerify(cm.verify, `conversation_memory ${cm.verify}`);
requireVerify(cm.codec_verify, `conversation_memory ${cm.codec_verify}`);
assert(cm.unverified.length >= 5, 'the unverified list must keep naming what no JVM gate can prove');
assert(
  /本机长期记忆/.test(fs.readFileSync(path.join(__dirname, '..', 'docs', 'SYSTEMS_OVERVIEW.md'), 'utf8')),
  'docs/SYSTEMS_OVERVIEW.md must document the on-device recollection'
);

// ---- 人物身份：称谓只有一套，行为不再靠显示字符串分支 --------------------------------
const pp = contract.persona;
const UI_SRC = path.join(APP_SRC, 'ui');
assert(generatorSource.includes(pp.name_fn), 'personaName must stay the single source of the two persona labels');
Object.values(pp.names).forEach((name) => {
  assert(pp.name_fn.includes(`"${name}"`), `personaName must still return ${name}`);
});
assert(pp.name_fn.includes('mode == "half"'), 'personaName must still branch on the half mode');

// 退役称谓一旦在任何 Kotlin 源码里复活，说明又有人给同两个模式起了第二套名字
const retiredPattern = new RegExp(pp.retired.join('|'), 'u');
fs.readdirSync(APP_SRC, { recursive: true })
  .filter((file) => String(file).endsWith('.kt'))
  .forEach((file) => {
    const source = fs.readFileSync(path.join(APP_SRC, String(file)), 'utf8');
    const hit = source.split('\n').find((line) => retiredPattern.test(line));
    assert(!hit, `${file} still uses a retired persona name: ${hit}`);
  });

const orbSource = fs.readFileSync(path.join(UI_SRC, 'components', 'MysticOrb.kt'), 'utf8');
const floatingSource = fs.readFileSync(path.join(UI_SRC, 'components', 'MysticFloatingGuide.kt'), 'utf8');
const panelSource = fs.readFileSync(path.join(UI_SRC, 'components', 'MysticConversationPanel.kt'), 'utf8');
assert(
  orbSource.includes(pp.orb_motion) && orbSource.includes(pp.orb_amplitude),
  'the orb motion must be driven by the mode itself'
);
assert(!/roleName\s*==/.test(orbSource), 'the orb must never branch on a display string');
assert(
  floatingSource.includes(pp.orb_role_binding) && floatingSource.includes(pp.orb_mode_binding),
  'the orb must take its label from personaName and its motion from the mode'
);
assert(!floatingSource.includes('roleName: String'), 'the stage kept a roleName parameter it never reads');
assert(
  floatingSource.includes('MysticGuideGenerator.personaName(key)') &&
    mysticCardSource.includes('MysticGuideGenerator.personaName(mode)'),
  'the persona buttons and the costume switch must read the labels from personaName'
);
pp.identity_answers.forEach((line) => {
  assert.strictEqual(line.split('$name').length - 1, 1, `an identity answer must name the persona once: ${line}`);
  assert(generatorSource.includes(line), `identity wording changed: ${line}`);
});

// 「玄师」是这套陪伴功能的统称，不落到任何一个模式上
[panelSource, floatingSource].forEach((source) => {
  assert(source.includes(pp.umbrella), `the umbrella name ${pp.umbrella} disappeared from the companion UI`);
});
assert(floatingSource.includes(`label = "${pp.stage_close_label}"`), 'the stage close label changed');
assert(pp.unverified.length >= 2, 'the persona slice must keep naming what only a device can prove');
requireVerify(pp.verify, `persona ${pp.verify}`);

console.log('dialogue contract: PASS (' + contract.golden_wording.length + ' golden entries)');
