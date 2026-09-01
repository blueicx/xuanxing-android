const fs = require('fs');
const assert = require('assert');
const contract = JSON.parse(fs.readFileSync(__dirname + '/dialogue_contract.json', 'utf8'));

// structure
assert.strictEqual(contract.defaultMode, 'offline');
assert(contract.grounded_reply_required === true);
assert.deepStrictEqual(
  contract.game_events,
  ['Start', 'ApplyMove', 'EngineReply', 'Undo', 'Cancel', 'Exit']
);
assert(contract.rejection_codes.includes('self_check'));
assert(contract.rejection_codes.includes('game_over'));

// token discipline
assert(/token != state\.sessionToken/.test(contract.token_drop_rule));

// golden wording coverage (>= 12 entries per plan)
assert(contract.golden_wording.length >= 12, 'golden wording must have >= 12 entries');

// everyday guards: safety intents must keep priority over game misreads
assert(contract.everyday_guards.length >= 4);

// unavailable games must expose explicit reason codes, not fake play
assert.strictEqual(contract.unavailable_games.go, 'go_provider_not_enabled');

// board encoding facts
assert(contract.board_encoding.initial.startsWith('rnbakabnr'));
assert(contract.board_encoding.uci.includes("rank 9 -> '0'"));

console.log('dialogue contract: PASS (' + contract.golden_wording.length + ' golden entries)');
