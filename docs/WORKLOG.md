# 作業記録

---

## ▶ 次回の再開ポイント(2026-07-23 時点)

### 今どこ
- **設計は v3(AIファースト)+ v3.1(知識ベース・タグ・観察日記)**。
  設計書: `yosuga_hub_android_design_v3.md` / `yosuga_hub_android_design_v3_1.md`(v2は技術資料)。
- **v3-Step 1(Task実体化)完了**: 1-a データ層 / 1-b プロジェクト詳細画面 / 1-c タスク編集 / 1-d プロジェクト編集。
- コード検証は WSL の `assembleDebug` + `testDebugUnitTest` が通っている(BUILD SUCCESSFUL)。

### 未実施の実機確認(次にやると良い)
1. **Room v1→v2 マイグレーション**: 旧DBが入った端末/エミュレーターで更新起動し、既存データが残ることを確認。
2. プロジェクト詳細画面の一連の操作(カードタップ→詳細→タスク追加/編集/チェック/削除→プロジェクト編集)。
- Windows 側 Android Studio で実行(`local.properties` は Windows SDK パスの状態にしてある)。
- Windows の Gradle Sync が失敗する場合は `.gradle` / `build` / `app/build` を削除して再Sync。

### WSL でビルドする場合
```
echo "sdk.dir=/home/note_xylo/android-sdk" > local.properties
JAVA_HOME=/home/note_xylo/tools/jdk-17.0.19+10 ./gradlew assembleDebug testDebugUnitTest --no-daemon
```
※ 終わったら local.properties を Windows 用(`C:\Users\note_\AppData\Local\Android\Sdk`)へ戻す。

### 次の実装: v3-Step 2 の詳細設計(v3.1統合)
- 情報アイテム(kind: memo/idea/decision/shopping/tech…)+ タグ(多対多・AI管理)+
  エンティティ + 観察日記のデータモデル(Room v3 へのマイグレーション)
- 回答JSONスキーマ v2(タスク提案 + タグ付きアイテム / 決定事項 / 日記 / エンティティ関連付け / 健康状態)
- 提案レビュー(承認/棄却)UI
- 着手前に詳細設計をユーザーと合意すること(1ステップ=1コミットの進め方を維持)

---

## 2026-07-23: v3-Step 1-d プロジェクト編集 — v3-Step 1 完了

### 目的

プロジェクト情報(name / currentGoal / health)を編集できるようにし、v3-Step 1 を完了させる。

### 新規ライブラリ

- **なし**

### 実施内容

- `ProjectDao.upsert()` を追加、`Mappers` に `Project.toEntity()` を追加
- `ProjectRepository.upsert()`: lastUpdated をここで刻む(`now` 注入可能、表示形式は
  既存の `formatSyncTime` と同じ "yyyy-MM-dd HH:mm")
- `ProjectDetailViewModel.updateProject()` を追加
- `ui/screen/projectdetail/ProjectEditDialog`: name(必須)/ 目標 / 状態(FilterChip 4値)
  - **inProgress / nextTask は編集対象外**(タスクからの導出へ置き換える予定のため)
  - 手動編集で選べる health は v2 語彙の4値のみ。AI分析由来の自由な値(v3.1)は表示のみで受け入れる
- 詳細画面の情報カードに「プロジェクトを編集」ボタンを追加
- テスト: `ProjectRepositoryTest` に upsert の lastUpdated 刻印・編集対象外フィールド保持を追加
  (FakeProjectDao を書き込み対応に更新)

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)

### v3-Step 1 完了条件の達成状況

- ✅ Task が第一級エンティティ(Room v2 / Repository / 追加・編集・完了・削除)
- ✅ プロジェクト詳細画面(ネストナビ)+ プロジェクト編集
- ⬜ 実機確認(マイグレーション・一連の操作)は未実施 → 次回の再開ポイント参照

**→ v3-Step 1 完了。次は v3-Step 2(v3.1統合の詳細設計)。**

---

## 2026-07-23: v3-Step 1-c タスクの追加・編集・完了・削除

### 目的

プロジェクト詳細画面のタスクを実際に操作できるようにする(初の書き込みUI)。

### 新規ライブラリ

- **なし**

### 実施内容

- `TaskRepository` の書き込みを拡張
  - `create()`: 新規タスクの採番・保存(`newId` を注入可能に。既定は UUID)。source は "manual"
  - `upsert()`: completedAt を status と整合させるよう強化
    (編集で DONE → 刻む / DONE 解除 → クリア / 既に DONE → 元の時刻を保持)
- `ProjectDetailViewModel` に `addTask` / `updateTask` / `setTaskDone` / `deleteTask` を追加
- `ui/screen/projectdetail/TaskEditDialog`: 新規・編集共用ダイアログ
  - タイトル(必須)/ 詳細 / 優先度(高・中・低 FilterChip)/ 状態(未着手・進行中・完了)/
    締切(yyyy-MM-dd テキスト入力、不正時はエラー表示と保存無効)/ 編集時のみ削除ボタン
  - DatePicker は使わない(M3 では experimental API のため。個人アプリの入力頻度なら手入力で十分と判断)
- `ui/screen/projectdetail/TaskForm`: 締切検証 `isValidDueDateInput`(LocalDate.parse で実在日チェック)と
  `dueDateForSave`(空欄→null)を純粋関数として切り出し
- `ProjectDetailScreen`: [タスクを追加] ボタン、行チェックボックス(完了⇄未着手)、行タップ→編集ダイアログ
- テスト: `TaskFormTest`(空欄可 / ISO日付 / 不正・実在しない日付)、
  `TaskRepositoryTest` に create の採番・時刻 / DONE作成時のcompletedAt / upsert のcompletedAt保持・クリアを追加

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)
- 実機での操作確認(追加→編集→完了→削除の一連)は未実施(Windows 側 Android Studio で確認する)

### 次にやること

- **1-d**: プロジェクト編集(name / currentGoal / health)→ これで v3-Step 1 完了

---

## 2026-07-23: 追加仕様 v3.1(知識ベース・タグ・観察日記)を設計へ取り込み — 記録のみ

### 経緯

ユーザーから追加仕様(情報整理 / タグシステム / エンティティ / 決定事項ログ / 保留アイデア /
健康状態のAI分析 / Yosuga視点の観察日記 / 開発ログ統合)の相談。影響分析の結果、
1-a/1-b の実装とは衝突せず、**Step 2 のJSONスキーマ設計前の今が取り込みの最適タイミング**と判断。

### ユーザーと合意した決定

1. **知識の置き場所**: Hub=構造化された短い知識(タグ付きアイテム・決定事項・保留アイデア・
   観察日記・エンティティ)を Room DB で。Obsidian=長文(設計思想・仕様書等)。v3の役割分担を精緻化。
2. **タイミング**: Step 1(1-c/1-d)を先に完了させ、v3.1 は Step 2 の再設計に織り込む。

### 実施内容(ドキュメントのみ)

- `yosuga_hub_android_design_v3_1.md` を新設(追加仕様本文 + 付録「v3との整合」)
- `CLAUDE.md`: 設計書参照に v3.1 を追加、ロードマップの Step 2 を「v3.1織り込み再設計」に更新
- タグ生成・統合、エンティティ関連付け、健康分析、日記執筆はすべて AI側の仕事。
  Hub は保存・表示・編集・検索のみ(v3の役割分担・承認フローを踏襲)

### 次にやること

- 1-c(タスク追加・編集・完了・削除)→ 1-d(プロジェクト編集)→ Step 2 詳細設計(v3.1統合)

---

## 2026-07-23: v3-Step 1-b プロジェクト詳細画面(読み取りのみ・初のネストナビ)

### 目的

プロジェクト一覧のカードをタップして詳細(プロジェクト情報 + タスク一覧)を見られるようにする。
表示のみで編集はまだない(1-c / 1-d で追加)。下部ナビ外のネストルートを初導入。

### 新規ライブラリ

- **なし**

### 実施内容

- `ui/navigation/ProjectDetailRoute`: ネストルート定義(`project_detail/{projectId}`)
- `ui/screen/projectdetail/TaskGrouping`: 状態別グルーピング + 並び順(優先度 → 締切(なしは最後)→ タイトル)を
  純粋関数 `groupTasks()` として切り出し(ユニットテスト可能)
- `ui/screen/projectdetail/ProjectDetailViewModel`
  - `SavedStateHandle`(`createSavedStateHandle()`)から projectId を受け取る初の ViewModel
  - projects + tasksForProject を combine し `ProjectDetailUiState`(isLoading / project / GroupedTasks)を公開
- `ui/screen/projectdetail/ProjectDetailScreen`
  - 戻るボタン + プロジェクト情報カード + タスクを「進行中 / 未着手 / 完了」のセクション表示
  - 完了タスクは打ち消し線 + 淡色。メタ行に優先度(高/中/低)・締切・詳細を表示
  - プロジェクトが見つからない場合の表示、タスク0件の表示にも対応
- `ui/component/ProjectHealth`: healthLabel を一覧・詳細で共用化(ProjectsScreen の private 版を削除)
- `ProjectsScreen`: カードを `Card(onClick=...)` 化し `onProjectClick(projectId)` を公開
- `YosugaHubApp`: NavHost に詳細ルートを追加、詳細表示中は「プロジェクト」タブを選択状態に維持
- テスト: `TaskGroupingTest`(状態別グルーピング / 優先度→締切→タイトルの並び / 未知priorityは最後 / isEmpty)

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)
- 実機/エミュレーターでの画面遷移確認は未実施(Windows 側 Android Studio で確認する)

### 次にやること

- **1-c**: タスク追加・編集・完了・削除(編集ダイアログ、TaskRepository の書き込みを配線)

---

## 2026-07-23: v3-Step 1-a Task のデータ層(Entity/DAO/Migration/Repository/シード)

### 目的

v3-Step 1 の詳細設計(下記エントリ)どおり、Task を第一級エンティティとしてデータ層に導入。
UIは一切変更していない(見た目は従来どおり)。UI は 1-b 以降で追加する。

### 新規ライブラリ

- **なし**(Room / coroutines 既存分で完結)

### 実施内容

- `domain/model/Task` + `TaskStatus`(enum: TODO/DOING/DONE、未知値は TODO へフォールバック)
- `data/local/db/entity/TaskEntity`(テーブル `tasks`、`projectId` に index)
  - 外部キー制約は張らない: プロジェクトは将来 GitHub 由来へ差し替わるため、参照切れでタスクを失わない方針
- `data/local/db/dao/TaskDao`(observeAll / observeByProject / count / insertAll / upsert / updateStatus / deleteById)
- **Room v1→v2**: `data/local/db/Migrations.kt` に `MIGRATION_1_2`(CREATE TABLE tasks + index、既存データ無傷)
  - `exportSchema = true` 化 + `build.gradle.kts` に `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`
  - 出力された `app/schemas/.../2.json` の createSql がマイグレーションSQLと一致することを確認(Git管理する)
  - v1 のスキーマJSONは存在しない(当時 exportSchema=false)。以後の変更は出力JSONで検証できる
- `data/repository/TaskRepository`(tasks / tasksForProject / upsert / setStatus / delete)
  - `now` を注入可能にしてテスト容易性を確保。upsert は updatedAt を刻む(createdAt は不変)
  - setStatus: DONE で completedAt を刻み、戻したらクリア
- `Mappers`: `TaskEntity.toDomain()` + 書き込み用の `Task.toEntity()`(status は enum⇄文字列変換)
- `SampleSeed.tasks` 5件(3プロジェクトの nextTask/inProgress 相当 + 完了済み1件 + プロジェクト外1件 `projectId=null`)
- `AppContainer`: `taskRepository` 追加、`addMigrations(MIGRATION_1_2)`、空のとき tasks もシード
- テスト: `TaskRepositoryTest`(変換 / フィルタ / upsert の時刻刻印 / setStatus の completedAt 制御 / 未知status フォールバック)、
  `SampleSeedTest` にタスク検証5件を追加(ID一意 / projectId 参照整合 / status・priority 語彙 / completedAt 整合)

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)
- **マイグレーション v1→v2 の実機検証は未実施**: 旧DBが入った端末/エミュレーターでの起動確認は
  Windows 側 Android Studio での実行時に行う(Room の migration テストは instrumentation が必要なため
  ユニットテストでは代替としてスキーマJSONとSQLの一致を確認した)

### メモ(ビルド環境)

- Windows 側 Android Studio のセットアップは完了済み(`local.properties` に Windows SDK パスが生成されていた)。
  今回は WSL ビルドのため一時的に WSL 用へ差し替え、ビルド後に Windows 用へ戻した。
- Windows 側で Gradle Sync に失敗する場合は `.gradle` / `build` / `app/build`(WSLビルドで再生成された
  キャッシュ)を削除してから Sync し直すこと。

### 次にやること

- **1-b**: プロジェクト詳細画面(読み取りのみ: プロジェクト情報 + タスク一覧、初のネストナビ)

---

## 2026-07-23: v3-Step 1(Task実体化)の詳細設計を確定 — 記録のみ(コード変更なし)

### ユーザーと合意した決定

1. **status は3状態**: `todo` / `doing` / `done`(最小構成。停滞はプロジェクト側 health で表現済み)。
2. **プロジェクト外タスクを許可**: `projectId` は null 可(TGS準備・事務など。会話からの抽出で必ず出る)。
3. **UIはプロジェクト詳細画面を新設**: 一覧カードタップ → 詳細(プロジェクト情報編集 + タスク一覧/追加/編集)。
   下部ナビ外のネストルート(初のネストナビ)。プロジェクト外タスクはホームの「今日やること」側で扱う(将来)。

### データモデル(確定)

- `Task`(domain)/ `TaskEntity`(テーブル `tasks`、`projectId` に index):
  `id`(UUID文字列, PK)/ `projectId`(String?)/ `title` / `detail` /
  `status`(todo|doing|done)/ `priority`(high|medium|low、Recommendationと同語彙)/
  `dueDate`(String? "yyyy-MM-dd")/ `createdAt` / `updatedAt`(ISO 8601)/
  `completedAt`(String?、完了履歴の土台)/ `source`(manual|assistant、Step 2 の承認由来記録用)
- **Room v1→v2**: `CREATE TABLE tasks` の追加マイグレーション(既存データ無傷)。
  これを機に `exportSchema = true` + スキーマJSON出力を有効化(Phase 2-2 の予告どおり)。
- `TaskDao` + `TaskRepository`(observeAll / observeByProject / upsert / setStatus / delete)+ Mapper。
- `SampleSeed` に仮タスク数件(既存 nextTask 相当 + プロジェクト外1件)。

### 既存を壊さないための割り切り

- `Project.inProgress` / `nextTask` の文字列は当面残す(タスクからの導出は後段で判断)。
- ホーム「優先タスク」も当面既存表示のまま。
- 編集UIは title / detail / priority / dueDate / status のみ。並び替え・検索・繰り返しは作らない。

### 実装順(1ステップ=1コミット、都度ビルド+テスト)

- **1-a**: Entity / DAO / Migration / Repository / シード(UIなし)+ テスト
- **1-b**: プロジェクト詳細画面(読み取りのみ: 情報 + タスク一覧)
- **1-c**: タスク追加・編集・完了・削除(編集ダイアログ)
- **1-d**: プロジェクト編集(name / currentGoal / health)

新規ライブラリ: **なし**。

---

## 2026-07-23: 設計を v3(AIファースト)へ転換 — 記録のみ(コード変更なし)

### 目的

設計方針を v2(状況JSON ↔ ChatGPT の橋渡しダッシュボード)から
**v3「AIファースト」**(AI=頭脳 / Yosuga Hub=記憶・表示装置)へ転換。今回は方針の確定と
記録に留め、コードは一切変更していない(プロジェクトルール「一度に大規模な変更を行わない」)。

### v3の核心

- **AI=頭脳 / Yosuga Hub=記憶・表示装置**。アプリは考えず、AIが整理した結果を保存・表示・編集・検索する。
- **知識はObsidian(端末内Vault)、制作管理はHub**。会話から知識を適切なノートへ振り分け、リンク/タグも提案。
- **提案 → ユーザーがOK → 保存**(自動更新しない・安全優先)。Android単体でクラウド同期なし。

### 今回の確定事項(ユーザーと合意)

1. **今回の範囲**: 設計記録 + ロードマップ改訂まで。実装は次の合意後。
2. **Obsidian連携**: SAF で実 Vault フォルダを選択し `DocumentFile` で `.md` 読み書き。URI権限は永続化。
3. **AIブリッジ**: 当面は手動JSONブリッジを継続(既存の export/import 機構を再利用)。将来のAPI直結に備え差し替え可能に保つ。

### 実施内容(ドキュメントのみ)

- `yosuga_hub_android_design_v3.md` を新設(v3本文 + 付録「実装方針の確定事項」+ v3ロードマップ)。
- `CLAUDE.md`: 設計書参照を v3 に更新、Current Phase を v3ロードマップへ更新(v2は技術資料として有効と明記)。
- `docs/WORKLOG.md`: 本エントリを追記。

### 現状の実装 → v3 マッピング

- 再利用する土台(実装済み): 5画面 + Room + DataStore + ViewModel/Repository分離 + 状況JSON export/回答JSON import。
- 主な未実装ギャップ:
  - **Task の第一級エンティティ化 + 編集UI**(今は Project に文字列埋め込み、全画面が読み取り専用シード)。
  - **Obsidian連携**(SAFでVault選択・ノート振り分け・リンク/タグ提案)。完全新規。
  - **提案→承認フロー**(回答JSON取り込みを「提案レビュー画面」へ発展)。

### v3ロードマップ(旧 Phase 3「GitHub進捗取得」は後ろへ延期)

- **v3-Step 1**: Task を第一級エンティティ化(TaskEntity/DAO/Repository + タスク/プロジェクト編集の最小UI)。
- **v3-Step 2**: 提案→承認フローの明確化(回答JSONスキーマ拡張: `targetNote`/リンク/タグ、schemaVersion更新)。
- **v3-Step 3**: Obsidian連携(SAF Vault選択・URI権限永続化・ノート追記・`KnowledgeStore`抽象化)。
- **v3-Step 4**: 会話中心の統合表示(ホームをAI秘書視点へ再編)。
- 将来: GitHub Knowledge Repository / Claude Code status統合 / ストレージ抽象化拡張 / AI API直結。

### 次にやること

- v3-Step 1(Task実体化)の着手可否をユーザーと合意 → 合意後に最小実装 + ビルド確認。

---

## 2026-07-22: Phase 2-5 JSONインポート(回答JSON取り込み) — Phase 2 完了

### 目的

Phase 2 の最後の完了条件「ChatGPT回答JSNを取り込める」を満たす。設計書2.3「ChatGPTから
アプリへ」の回答JSNを、設計書15章のルール(schemaVersion検証 / 未知項目は無視 /
不正JSNでクラッシュさせない / 元ファイルを上書きせず履歴を残す)に沿って取り込む。
ホーム・よすが画面の「未実装Toast」だった取り込みボタンを実機能化。

### 新規ライブラリ

- **なし**(kotlinx.serialization は導入済み、ファイル選択は既存の activity-compose を使用)

### 実施内容

- 回答JSNモデル `data/file/model/AssistantResponse`(`@Serializable`)+ 補助モデル
  (`RecommendationImport` / `SuggestedTaskImport` / `SchemaProbe`)を新設
  - 設計書2.3の回答JN構造。必須は `schemaVersion` のみ、他はデフォルト値付き
- `data/file/ResponseImporter`(純粋オブジェクト)を新設
  - `Json { ignoreUnknownKeys = true; isLenient = true }` で未知項目を無視
  - まず `SchemaProbe` で schemaVersion を先読みして対応可否を判定
  - 結果を sealed `ParseResult`(Success / InvalidJson / UnsupportedSchema)で返す
  - `SerializationException` 等を捕捉し、不正JSNでもクラッシュさせない
- `data/repository/ImportRepository` を新設
  - 選択された Uri を `contentResolver` で読み、`ResponseImporter` で検証
  - 成功時のみ `imports/response_<日時>.json` へ履歴保存(上書きしない)し、
    提案を Room の recommendations テーブルへ反映(deleteAll → insertAll で置き換え)
  - 結果を sealed `ImportResult`(Success / InvalidJson / UnsupportedSchema / ReadError)で返す
- `RecommendationDao.deleteAll()` を追加
- `DefaultAppContainer` / `AppContainer` に `importRepository` を追加
- `ui/share/ImportMessage`: `ImportResult` をユーザー向け短文へ変換(設計書8章)
- `HomeViewModel` / `AssistantViewModel` に `importResponse(uri, onResult)` を追加
- ホーム「ChatGPT回答JSNを取り込む」/ よすが「回答JSNを取り込む」を実機能化
  - `ActivityResultContracts.OpenDocument()` でファイル選択 → 取り込み → 結果Toast
  - Room が Flow で提案テーブルを監視しているため、取り込み後に一覧が自動更新される
- 設定画面「JSN保存先」の表示を現状に合わせて更新(内部の exports / imports に保存)
- テスト: `ResponseImporterTest`(正常 / 不明項目無視 / 不正JSN / 非対応バージョン /
  schemaVersion欠落)を追加

### テスト結果

- `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)

### Phase 2 完了条件の達成状況

- ✅ アプリが状況JSNを生成できる(Phase 2-4)
- ✅ ChatGPT回答JSNを取り込める(本ステップ)
- ✅ 再起動後もデータが残る(Phase 2-2 Room / 2-3 DataStore)

**→ Phase 2 完了。次は Phase 3(GitHub進捗取得)。**

### 今後の改善候補(任意)

- FileProvider によるファイル添付共有(現状はテキスト共有)
- Storage Access Framework で保存先フォルダを選択(現状は内部固定)
- 取り込み履歴一覧の画面表示、schemaVersion 非対応時の詳細案内

## 2026-07-22: Phase 2-4 JSONエクスポート(状況JSON生成・保存・共有)

### 目的

Phase 2 完了条件の1つ「アプリが状況JSNを生成できる」を満たす。設計書2.3「アプリから
ChatGPTへ」の状況JSONを生成し、アプリ専用領域へ保存 + Android共有メニューで渡せるようにする。
ホーム・よすが画面の「未実装Toast」だったJSN作成ボタンを実機能に置き換える。

### 新規ライブラリ(追加理由を明記)

- **kotlinx.serialization JSON 1.7.3**(`org.jetbrains.kotlinx:kotlinx-serialization-json`)
  + Gradleプラグイン `org.jetbrains.kotlin.plugin.serialization`(Kotlin 2.0.21 に追従)
  - 理由: 設計書2.3/15章のJSN受け渡しがPhase 2の中核。`schemaVersion` 付きの型安全な
    JSN生成・(次ステップの)検証に必要。設計書3.4のライブラリ候補にも記載。

### 実施内容

- JSNモデル `data/file/model/ContextExport`(`@Serializable`)を新設
  - 設計書2.3の状況JN構造(`schemaVersion` / `generatedAt` / `userContext` /
    `calendar{pastDays,futureDays,events}` / `projects[]` / `recentAssistantExchange`)
  - `SCHEMA_VERSION = 1` を定数化(設計書15章: schemaVersion必須)
- `data/file/ContextExporter`(純粋オブジェクト)を新設
  - ドメインモデル → DTO変換 + `Json.encodeToString`(prettyPrint / encodeDefaults)
  - I/O・プラットフォーム非依存でユニットテスト可能
  - `generatedAt` は引数注入(テスト容易性)。現状 `statusMarkdown` は手元の
    goal/inProgress/nextTask から簡易Markdownを生成(status.md は Phase 3 で導入)
- `data/repository/ExportRepository` を新設
  - projects + events(today/upcoming/past を結合)を `Flow.first()` でスナップショット
  - `OffsetDateTime.now()`(ISO 8601 + タイムゾーン)で generatedAt を付与
  - `filesDir/exports/context_<yyyy-MM-dd_HHmmss>.json` へ保存(設計書4.2)
  - `ExportResult(fileName, json)` を返す
- `DefaultAppContainer` / `AppContainer` に `exportRepository` を追加
- `ui/share/ShareJson`: `ACTION_SEND`(`application/json` / EXTRA_TEXT)で共有
  - ファイル添付共有(FileProvider)は次段で検討
- `HomeViewModel` / `AssistantViewModel` に `createExport(onResult)` を追加
  - ホーム「ChatGPT用JSNを作成」/ よすが「状況JSNを作成」ボタンを実機能化
    (生成 → 保存Toast → 共有シート、失敗時はエラーToast)
- テスト: `ContextExporterTest`(schemaVersion / フィールドマッピング / JN往復)を追加

### テスト結果

- `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL、kspDebugKotlin 実行)

### 残作業(Phase 2 の最後)

- 回答JSNの取り込み(インポート): 選択 → schemaVersion検証 → 検証結果表示 →
  不正JSNでクラッシュさせず取り込み → Room へ保存
- (任意)FileProvider によるファイル添付共有

## 2026-07-22: Phase 2-3 DataStore 導入(最終同期時刻の永続化)

### 目的

設計書4.1が指定する軽量設定の保存先として DataStore を導入。最初の実用値として
「最終同期時刻」を永続化する。従来ホーム画面の「最終同期」はコード内の固定文字列
(プレースホルダー)だったが、これを DataStore 管理へ移し、**実際にローカルデータを
投入した時刻を本物の値として保存・表示**する(未実装を実装済みに見せない)。

### 新規ライブラリ(追加理由を明記)

- **DataStore Preferences 1.1.1**(`androidx.datastore:datastore-preferences`)
  - 理由: 設計書4.1が最終同期時刻・軽量設定の保存先として明示指定。
    SharedPreferences より coroutines/Flow と相性がよく、非同期・型安全に読める。

### 実施内容

- `data/local/datastore/UserPreferencesRepository` を新設
  - `preferencesDataStore`(ファイル名 `user_prefs`)を用い、`lastSyncedAt: Flow<String>` を公開
  - IO エラー時は `emptyPreferences()` にフォールバックしクラッシュさせない
  - `setLastSyncedAt(value)` で書き込み
- `util/SyncTime`: 最終同期時刻の表示フォーマット(`yyyy-MM-dd HH:mm`)を関数化
- `DefaultAppContainer`
  - `UserPreferencesRepository` を生成・公開(`AppContainer` interface にも追加)
  - 初回シードを実行したとき、`LocalDateTime.now()` を整形して最終同期時刻に記録
- `HomeViewModel`
  - `userPreferencesRepository.lastSyncedAt` を combine に追加(4フロー)
  - 空なら「未同期」と表示
- `CalendarRepository` から `lastSyncedAt` のプレースホルダー const を削除(DataStore へ移管)
- テスト: `SyncTimeTest`(フォーマット検証)を追加

### 設計上の注意

- DataStore の I/O 検証はユニットテスト対象外(Context/ファイルが必要なため)。
  代わりにフォーマッタを純粋関数として切り出し `SyncTimeTest` で検証。
- 「最終同期時刻＝初回シード時刻」は暫定。Phase 3(GitHub)/ Phase 4(Calendar)の
  実同期処理が入ったら、その完了時刻に置き換える。

### テスト結果

- `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)
- ホーム画面の「最終同期」が固定文字列から DataStore 由来の実時刻表示に変化(それ以外の見た目は不変)

### 次に推奨する作業(Phase 2 の最後)

- JSONモデル + schemaVersion 検証 + エクスポート/インポート + Android共有メニュー対応
  (ホーム・よすが画面の「未実装Toast」ボタンを実機能へ)

## 2026-07-22: Phase 2-2 Room 導入(ローカル永続化)

### 目的

Phase 2 の完了条件「再起動後もデータが残る」を満たすため、ローカル永続化に Room を導入。
前ステップで Repository が `Flow` を返す形にしてあるので、データソースを
インメモリ(`SampleDataSource`)から Room DAO へ差し替えるだけで済む。
**UIの見た目は変えない**(同じ仮データで DB を初回シードする)。

### 新規ライブラリ(追加理由を明記)

- **Room 2.6.1**(`room-runtime` / `room-ktx` / `room-compiler`)
  - 理由: Phase 2 の永続化要件。設計書4.1が Room を明示指定、3.4のライブラリ候補にも記載。
  - `room-ktx` は DAO の `Flow` 返却(coroutines 連携)のために追加。
- **KSP 2.0.21-1.0.28**(`com.google.devtools.ksp`)
  - 理由: Room のアノテーション処理に必要。Kotlin 2.0.21 と整合するバージョンを選択。
  - ルート `build.gradle.kts` に `apply false` で宣言、`app` で適用。

### 実施内容

- Room 層を新設(`data/local/db/`)
  - `entity/`: `ProjectEntity`(id を主キー)/ `CalendarEventEntity`(bucket で今日・今後・過去を区別)/ `RecommendationEntity`
  - `dao/`: `ProjectDao` / `CalendarEventDao` / `RecommendationDao`(一覧は `Flow` で公開、`count()` と `insertAll()` を用意)
  - `YosugaDatabase`(version 1、exportSchema=false)
  - `Mappers.kt`(Entity → ドメインモデル変換。UIへは常にドメインモデルを渡す)
  - `CalendarBucket`(today/upcoming/past の暫定区分)
  - `SampleSeed`(初回シード用の仮データ。旧 `SampleDataSource` を置き換え、同ファイルは削除)
- Repository をDAO利用へ変更(3つとも `dao.observe...().map { it.toDomain() }`)
  - `today` / `lastSyncedAt` / `priorityTask` は各Repositoryの private const に暫定保持(後で時計・DataStore・導出へ)
- `DefaultAppContainer` を `Room.databaseBuilder` で DB 構築するよう変更
  - 引数に `Context` を追加(`YosugaHubApplication` から `this` を渡す)
  - 初回起動時、各テーブルが空の場合のみ `SampleSeed` を投入(`applicationScope` で非同期、再起動後の実データを壊さない)
- テスト更新: `SampleSeedTest`(種データの件数・ID・bucket検証)、`ProjectRepositoryTest`(フェイクDAOで Entity→ドメイン変換を検証)

### 設計上の注意 / 既知の割り切り

- `exportSchema=false`: 現状マイグレーション不要のため。将来スキーマ変更が必要になったら
  スキーマ出力とマイグレーションを追加する。
- シードはあくまで仮データ。Phase 3(GitHub)/ Phase 4(Calendar)/ Phase 2 のJSON取り込みで
  実データに置き換わり、`SampleSeed` は不要になる。

### テスト結果

- `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL、`kspDebugKotlin` 実行を確認)
- UIの表示内容は変更前と同一

### 次に推奨する作業(Phase 2 の続き)

1. DataStore 導入(設定値・最終同期時刻)
2. JSONモデル + schemaVersion 検証 + エクスポート/インポート + Android共有メニュー対応

## 2026-07-22: Phase 2-1 ViewModel / Repository 層の導入

### 目的

Phase 2 の下準備。**UIの見た目は一切変えず**、データ経路を `DummyData` の直接参照から
`UI → ViewModel → Repository → DataSource` へ通す(設計書3.3のアーキテクチャに整える)。
これにより次の Room / DataStore を DataSource 差し替えだけで導入できる状態にする。

### 実施内容

- ドメインモデルを新設(`domain/model/`): `CalendarEvent` / `Project` / `Recommendation`
  - 旧 `data/model/DummyData.kt` の `Dummy*` データクラスを置き換え、同ファイルは削除
- インメモリのデータソース `data/local/SampleDataSource` を新設(旧 `DummyData` の種データを集約)
- Repository を3つ新設(`data/repository/`): `CalendarRepository` / `ProjectRepository` / `AssistantRepository`
  - 一覧は `Flow<List<...>>` で公開(Room 導入時に DAO の Flow へそのまま差し替え可能)
  - `today` / `lastSyncedAt` / `priorityTask` は暫定値。後で端末時計・DataStore・導出ロジックへ置き換える
- 手動DI: `di/AppContainer`(interface + `DefaultAppContainer`)と `YosugaHubApplication` を新設
  - `AndroidManifest.xml` に `android:name=".YosugaHubApplication"` を追加
  - 設計書3.4の方針どおり、初期版は手動DI。画面/Repositoryが増えたら Hilt を検討
- 各画面に ViewModel を追加(`Home` / `Calendar` / `Projects` / `Assistant`)
  - `StateFlow<XxxUiState>` を公開し、画面は `collectAsState()` で監視
  - 画面から `DummyData` 直参照を全廃(設定画面はデータを持たないため据え置き)
- テスト更新: `DummyDataTest` → `SampleDataSourceTest`、加えて `ProjectRepositoryTest`(Flow の emit を検証)を追加

### 新規ライブラリ

- **なし**。`Flow` / `viewModelScope` / coroutines は既存の lifecycle 依存に含まれる推移的依存を利用。
  `collectAsStateWithLifecycle` は追加依存(`lifecycle-runtime-compose`)が必要なため、
  今回は追加せず標準の `collectAsState()` を使用。

### テスト結果

- `assembleDebug` 成功 / `testDebugUnitTest` 成功(BUILD SUCCESSFUL、JDK 17・WSL)
- UIの表示内容は変更前と同一(仮データの見た目を維持)

### 次に推奨する作業(Phase 2 の続き)

1. Room 導入(Entity / DAO / DB)→ `SampleDataSource` を Room 由来の DataSource へ差し替え
2. DataStore 導入(設定値・最終同期時刻)
3. JSONモデル + schemaVersion 検証 + エクスポート/インポート + 共有メニュー対応

## 2026-07-22: WSLビルド環境の構築とビルド・テストの検証

### 実施内容

- WSL側にビルド環境を構築(前セッションで途中まで進めていたものを再開して完了)
  - JDK 17.0.19: `/home/note_xylo/tools/jdk-17.0.19+10`
  - Android SDK: `/home/note_xylo/android-sdk`(platform-tools / build-tools 34.0.0・35.0.0 / android-35、ライセンス同意済み)
  - `local.properties` は WSL側SDK(`sdk.dir=/home/note_xylo/android-sdk`)を指す
- `assembleDebug` 成功(BUILD SUCCESSFUL、34タスク、デバッグAPK生成まで確認)
- `testDebugUnitTest` 成功(`DummyDataTest` パス)

### WSLでのビルド方法

```
JAVA_HOME=/home/note_xylo/tools/jdk-17.0.19+10 ./gradlew assembleDebug --no-daemon
JAVA_HOME=/home/note_xylo/tools/jdk-17.0.19+10 ./gradlew testDebugUnitTest --no-daemon
```

※ Windows側のAndroid Studioで開く場合は `local.properties` の `sdk.dir` をWindows側SDKに差し替えること(このファイルはGit管理外)。

### 残作業

- エミュレーターまたは実機での起動確認(WSL環境のためユーザー作業)
- Gitリポジトリの初期化とコミット
- Phase 2(Room / DataStore / JSONエクスポート・インポート)への着手

## 2026-07-22: Phase 0 + Phase 1 初期実装

### 実施内容

- Androidプロジェクト一式をゼロから作成(Android Studio未インストールのため、Claude Codeがファイルを直接生成)
  - Gradle Kotlin DSL + Version Catalog(`gradle/libs.versions.toml`)
  - Gradle Wrapper 8.11.1(jarは gradle/gradle GitHubリポジトリ v8.11.1 タグから取得)
- 5画面の骨組みを実装(下部ナビゲーションで移動可能)
  - ホーム: 今日の予定 / 次の予定 / ゲーム進捗 / 優先タスク / JSONボタン(未実装Toast) / 最終同期
  - カレンダー: 今日 / 今後7日 / 過去7日 / 手動更新ボタン
  - プロジェクト: 3ゲームのカード(目標 / 作業中 / 次タスク / 最終更新 / 状態チップ)
  - よすが連携: 状況JSON説明 / 作成・取込ボタン(未実装Toast) / 受け取った提案一覧
  - 設定: 各設定項目のプレースホルダー
- 仮データを `data/model/DummyData.kt` に集約(Phase 2 でRepository/Roomに置き換える)
- `CLAUDE.md`(設計書14章のルール + プロジェクト概要)、`README.md`、`.gitignore` を作成
- 単体テスト `DummyDataTest` を追加(projectIdの重複・設計書との一致を確認)

### 置いた仮定(設計書で「相談して決める」とされていた項目)

- **パッケージ名**: `com.shiro.yosugahub`(設計書10.2の例をそのまま採用)
- **Minimum SDK**: API 26(Android 8.0)
  - 理由: `java.time` がdesugaringなしで使える / ほぼ全ての現役端末をカバー / Pixel 10aは余裕で対象内
- **compileSdk / targetSdk**: 35(Android 15)
- **バージョン**: AGP 8.7.3 / Kotlin 2.0.21 / Compose BOM 2024.12.01 / Navigation 2.8.5
  - 相互compatibilityが確認されている安定版の組み合わせを採用。Android Studioが新しいバージョンを提案したら、動作確認のうえ追従してよい
- Phase 1では ViewModel / Repository を導入していない(仮データの静的表示のみのため)。Phase 2で実データと同時に導入する

### テスト結果

- **未検証**: この環境(WSL)にJDK・Android SDKがないため、ビルドとテストは未実行
- Android Studioインストール後、最初にGradle Syncと `assembleDebug` の成功を確認すること

### 未解決事項

- Android Studioのインストール(ユーザー作業)
- エミュレーターでの起動確認
- Gitリポジトリの初期化とコミット(この作業の最後に実施)

### 次に推奨する作業

1. Android Studioをインストールし、このフォルダを開いてビルド・起動を確認する
2. ビルドエラーがあれば修正する
3. Phase 2(Room / DataStore / JSONエクスポート・インポート)に着手する
