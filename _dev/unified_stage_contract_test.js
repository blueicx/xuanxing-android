const fs = require('fs');
const path = require('path');
const assert = require('assert');

const root = path.join(__dirname, '..');
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8');
const character = read('app/src/main/java/com/xuanji/app/domain/MysticCharacter.kt');
const scene = read('app/src/main/java/com/xuanji/app/ui/components/MysticSceneSpec.kt');
const uiModel = read('app/src/main/java/com/xuanji/app/ui/components/MysticCharacterUiModel.kt');
const stage = read('app/src/main/java/com/xuanji/app/ui/components/MysticStageLayout.kt');

const sceneIds = ['jiangnan_triptych', 'ink_elder', 'academy_triptych', 'silkroad_triptych'];
sceneIds.forEach((id) => {
  assert(character.includes(`sceneId = "${id}"`), `character catalog missing ${id}`);
  assert(scene.includes(`key = MysticSceneId.${{
    jiangnan_triptych: 'Jiangnan',
    ink_elder: 'InkElder',
    academy_triptych: 'Academy',
    silkroad_triptych: 'Silkroad'
  }[id]}.key`), `scene catalog missing ${id}`);
});

const stageLower = stage.toLowerCase();
['scene plate', 'character artwork', 'companion drawer', 'mysticculturebackdrop', 'mysticfigurecanvas'].forEach((marker) => {
  assert(stageLower.includes(marker), `stage layer marker missing: ${marker}`);
});
assert(stage.includes('MysticDrawerState.Peek'), 'stage must have a collapsed drawer state');
assert(stage.includes('stage-drawer-expanded'), 'stage must expose the expanded drawer semantics');
assert(stage.includes('rememberSaveable(saver = mapSaver('), 'independent drawers must survive saved-instance recreation');
const floating = read('app/src/main/java/com/xuanji/app/ui/components/MysticFloatingGuide.kt');
assert(floating.includes('MysticStageCheckpointStore') && floating.includes('checkpointProfileKey') && floating.includes('onDrawerStateChanged'), 'real stage must wire profile-scoped persisted drawers');
const checkpoint = read('app/src/main/java/com/xuanji/app/data/local/MysticStageCheckpointStore.kt');
assert(checkpoint.includes('it.dateKey == dateKey') && checkpoint.includes('fingerprint(profileKey)'), 'stage recovery must reject another date and isolate profiles');
const card = read('app/src/main/java/com/xuanji/app/ui/components/MysticGuideCard.kt');
assert(card.includes('BoardWorkRunner') && card.includes('onDispose { boardWork.cancel()'), 'obsolete board jobs must be cancelled with their owning role');
assert(stage.includes('MysticVisualAssetCatalog'), 'stage must use explicit visual asset modes');
assert(stage.includes('MysticAssetRenderMode.CompleteScene'), 'stage must branch complete-scene assets');
assert(uiModel.includes('sceneSpec = MysticSceneCatalog.forCharacter(profile)'), 'UI model must expose sceneSpec');
assert(!stage.includes('usesScenePlate'), 'stage must not branch on usesScenePlate');
assert(scene.includes('InkFallback'), 'scene fallback must remain available');
console.log('unified stage contract: PASS (4 scenes, 5 layers, fallback)');
