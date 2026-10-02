const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.join(__dirname, '..');
const read = (relative) => fs.readFileSync(path.join(root, relative), 'utf8');
const history = read('app/src/main/java/com/xuanji/app/ui/history/HistoryScreen.kt');
const details = read('app/src/main/java/com/xuanji/app/ui/components/FortuneDetailPage.kt');
const store = read('app/src/main/java/com/xuanji/app/data/local/ReadingFavoriteStore.kt');
const codec = read('app/src/main/java/com/xuanji/app/domain/ReadingFavorite.kt');
const privacy = read('app/src/main/java/com/xuanji/app/domain/MysticPrivacyExport.kt');
const repository = read('app/src/main/java/com/xuanji/app/data/repository/FortuneRepository.kt');

for (const legacySection of ['历史上的今天', '同月同日生', 'SameDayBirth.forToday', 'SameDayWorks.forDate']) {
  assert.ok(history.includes(legacySection), `history must preserve ${legacySection}`);
}
for (const filter of ['"all" to "全部"', '"fortune" to "运势"', '"action" to "行动"', '"mystic" to "玄师"', '"game" to "游戏"']) {
  assert.ok(history.includes(filter), `favorites section must expose ${filter}`);
}
for (const interaction of ['reading-favorite-search', 'reading-favorite-read-', 'reading-favorite-remove-', '已取消收藏', 'actionLabel = "撤销"', '清除本档案收藏？']) {
  assert.ok(history.includes(interaction), `favorites screen must implement ${interaction}`);
}
assert.ok(history.includes('只读快照'), 'opened favorite must display a read-only saved snapshot');
assert.ok(details.includes('ReadingFavoriteCodec.stableId'), 'detail favorite ID must be stable for the same reading');
assert.ok(details.includes('已收藏到「我的收藏」'), 'detail button must confirm persisted favorite state');
assert.ok(store.includes('sha256') || store.includes('MessageDigest.getInstance("SHA-256")'), 'profile keys must be hashed');
assert.ok(store.includes('ReadingFavoriteCodec.MAX_ENTRIES'), 'store must enforce a per-profile cap');
assert.ok(codec.includes('CURRENT_VERSION = 2'), 'favorites need a versioned envelope');
assert.ok(codec.includes('UnsupportedVersion') && codec.includes('Migrated'), 'codec must distinguish future and migratable data');
assert.ok(privacy.includes('readingFavoriteCount') && privacy.includes('readingFavorites'), 'privacy export must include favorite summary and snapshots');
assert.ok(repository.includes('readingFavoriteStore?.clear(favoriteProfileKey)'), 'full data clear must clear the active profile favorites first');

console.log('reading favorites contract: PASS (legacy history preserved, profile-scoped favorites, read-only export/clear)');
