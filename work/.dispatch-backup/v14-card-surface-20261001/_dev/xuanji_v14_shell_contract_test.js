const fs = require('fs');
const path = require('path');
const assert = require('assert');

const root = path.join(__dirname, '..');
const app = fs.readFileSync(path.join(root, 'app/src/main/java/com/xuanji/app/ui/XuanjiApp.kt'), 'utf8');
const stickyHeader = fs.readFileSync(path.join(root, 'app/src/main/java/com/xuanji/app/ui/components/FortuneComponents.kt'), 'utf8');
const eastern = fs.readFileSync(path.join(root, 'app/src/main/java/com/xuanji/app/ui/eastern/EasternScreen.kt'), 'utf8');
const composite = fs.readFileSync(path.join(root, 'app/src/main/java/com/xuanji/app/ui/composite/CompositeFortuneScreen.kt'), 'utf8');
const actions = fs.readFileSync(path.join(root, 'app/src/main/java/com/xuanji/app/ui/components/DailyActionCards.kt'), 'utf8');

const itemsMatch = app.match(/val items = listOf\(([\s\S]*?)\n\s*\)/);
assert(itemsMatch, 'main navigation item list is missing');
const mainItems = itemsMatch[1];
['Screen.Today', 'Screen.Charts', 'Screen.Explore', 'Screen.History', 'Screen.Profile'].forEach((item) => {
  assert(mainItems.includes(item), `v14 bottom navigation must include ${item}`);
});
['Screen.Composite', 'Screen.Eastern', 'Screen.Western', 'Screen.Test'].forEach((item) => {
  assert(!mainItems.includes(item), `v14 bottom navigation must not expose ${item} as a primary tab`);
});

assert(app.includes('TodayFortuneHub('), 'today must host the three fortune modes');
assert(app.includes('LifetimeChartHub('), 'life charts must have a separate destination');
assert(app.includes('ExploreHub('), 'divination and tests must be grouped under Explore');
assert(stickyHeader.includes('TodayModeSelector'), 'fortune header must show the top-level mode selector');
assert(app.includes('Screen("history", "记录"'), 'v14 primary navigation labels the history destination as 记录');
['"今日"', '"本周"', '"本月"', '"本年"'].forEach((label) => {
  assert(stickyHeader.includes(label), `period selector must use the full v14 label ${label}`);
});
assert(stickyHeader.includes('FortuneBrandMark'), 'fortune top row must use the framed v14 brand mark');
assert(stickyHeader.includes('DateProfileRow'), 'fortune date and profile chip must share the v14 date row');
assert(stickyHeader.includes('V14_GOLD'), 'selected fortune controls must use the v14 gold accent');
assert(composite.includes('FortunePageIntro('), 'long page title and helper must live in the scroll content');
assert(composite.includes('DailyOutlookHero('), 'v14 separates the score hero from the period-summary detail card');
assert(stickyHeader.includes('previewContent:'), 'fortune cards must support compact inline previews and full detail content');
assert(composite.includes('previewContent = {'), 'composite summary must keep its full report behind a compact preview');
assert(actions.includes('ActionAccent('), 'daily action cards must have distinct v14 category accents');
assert(!actions.includes('SectionTitle("今日行动")'), 'daily action section must not duplicate the card title');
assert(eastern.includes('formatEasternLunarDate'), 'Eastern daily header must show the lunar date');
assert(eastern.includes('chartOnly'), 'Eastern life-chart destination must omit daily fortune content');

console.log('X3 v14 shell contract: PASS (5 primary tabs, 3 today modes, separate lifetime charts)');
