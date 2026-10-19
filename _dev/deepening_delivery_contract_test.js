const fs = require('fs');
const path = require('path');
const assert = require('assert');
const root = path.resolve(__dirname, '..');
const source = file => fs.readFileSync(path.join(root, 'app/src/main/java/com/xuanji/app', file), 'utf8');
for (const file of ['BigFiveScreen', 'Cattell16Screen', 'HollandScreen', 'MmpiScreen', 'MbtiScreen', 'HogwartsScreen', 'FunTestsScreen', 'FunTests2Screen', 'FunTests3Screen']) {
  const text = source(`ui/test/${file}.kt`);
  assert(text.includes('QuestionnaireReview('), `${file}: missing actual review UI`);
  assert(/remember(?:Int|Boolean|String)Questionnaire/.test(text), `${file}: missing private draft binding`);
}
assert(source('ui/components/MysticGuideCard.kt').includes('authorizedCompanionTest(records)'));
assert(!source('ui/components/MysticGuideCard.kt').includes('records.maxByOrNull { it.date }'));
assert(source('data/repository/FortuneRepository.kt').includes('LocalDataEraseFence.erase'));
assert(source('ui/components/FortuneDetailPage.kt').includes('ReadingExplorationPanel(detail)'));
assert(source('ui/components/FortuneDetailPage.kt').includes('rememberDetailReadingPosition'));
assert(source('ui/components/ReadingPosition.kt').includes('key(owner, reading) { rememberScrollState() }'), 'profile or reading changes must not reuse the previous scroll offset');
assert(source('ui/composite/CompositeFortuneScreen.kt').includes('homeController.state'));
assert(source('ui/components/ResultShare.kt').includes('SelectiveShareDialog'));
assert(source('ui/components/MysticGuideCard.kt').includes('mystic-resume-task'));
assert(source('ui/components/game/GameBoardCard.kt').includes('XiangqiRuleLearningPanel'));
assert(source('ui/components/CompanionGameCard.kt').includes('StarLogicLearningPanel'));
assert(source('ui/components/CompanionGameCard.kt').includes('PoetryStructurePanel'));
const manifest = fs.readFileSync(path.join(root, 'app/src/main/AndroidManifest.xml'), 'utf8');
assert(manifest.includes('DailyQuickEntryWidget'));
const workflow = fs.readFileSync(path.join(root, '.github/workflows/build.yml'), 'utf8');
assert(workflow.includes('node _dev/deepening_delivery_contract_test.js'), 'new delivery contract must run in CI');
console.log('Deepening delivery contract PASS (questionnaires, explicit test consent, calculation exploration, local tasks, game learning, home order, selective sharing and widget wiring; runtime verified separately)');
