# 作業記録

---

## 2026-07-23: v4.1 の自己レビューと修正(実機確認の前に)

実装を積み増す前に直近3コミット(v4.1)を見直し、欠陥4件を修正した。

### ① アーカイブ済み文書が分類の取り込みで復活する(動作の不具合)

`applyAiClassification` が状態を見ずに needs_review へ進めていたため、**古い回答JSONを取り込むと
片付けたはずの文書が「確認待ち」に戻っていた**。アーカイブ済みは適用せず読み飛ばすようにした
(再開したいときはユーザーが明示的に操作する)。

これに伴い `ImportResult.SuccessProposals.unknownDocumentCount` を
`skippedClassificationCount` に改名(「宛先なし」だけでなく「アーカイブ済み」も含むため)。
メッセージも「適用できなかった分類が N 件(宛先の文書が見つからない、またはアーカイブ済み)」に修正。

### ② 未分類の文書に「再分類」ボタンを出していた(UIの不整合)

一度も分類されていない文書で押すと、アップロードしていないのに状態が「分類待ち」になり
**表示が事実と食い違っていた**(documents.json は未整理・分類待ちの両方を送るので実害は自己修復するが、
ユーザーには嘘の表示になる)。現行分類がある文書にだけ出すようにした。
「アーカイブ」は従来どおり全状態で出す。

### ③ composition 中に Compose の状態を書き換えていた(実装の作法)

詳細ダイアログで、対象文書が消えていたら `openedDocumentId = null` を composition 本体で
代入していた。`openedDocument?.let { }` が空振りすれば閉じるので、代入自体を削除した。

### ④ 分類の修正ダイアログが空の要約でも承認できた

`enabled = summary.isNotBlank()` を追加(既存の `ItemEditDialog` と同じ方針)。

### テスト

- `DocumentRepositoryTest` +1件(アーカイブ済みへの分類は適用されず履歴も増えない)、
  `ImportMessageTest` を新しい文言・フィールド名へ更新
- `assembleDebug` + `testDebugUnitTest` + `assembleDebugAndroidTest` 成功(**166件全成功**)

---

## 2026-07-23: Room マイグレーションの自動テスト(androidTest)

### 目的

マイグレーションの抜け(①定義 ②`@Database(version)` ③`addMigrations` 登録 ④スキーマ突き合わせ)は
**コンパイルが通ってしまい、実機更新時に初めてクラッシュする**。過去2回踏んでいるため自動化した。

### 新規ライブラリ

- **`androidx.room:room-testing:2.6.1`**(既存 room と同一バージョン / `androidTestImplementation` のみ)
  - 理由: `MigrationTestHelper` はこの公式アーティファクトにしか無い。
    「旧バージョンのDBを実際に作る → マイグレーションを走らせる → `app/schemas/*.json` の
    期待スキーマと突き合わせる」を行う正攻法。**アプリ本体(APK)には入らない**

### 実施内容

- `app/build.gradle.kts`: androidTest のアセットに `$projectDir/schemas` を追加
  (MigrationTestHelper は期待スキーマを端末上のアセットから読むため)。
  テストAPKに `assets/com.shiro...YosugaDatabase/2〜6.json` が入ることを確認済み
- `app/src/androidTest/.../MigrationTest.kt` 5件:
  1. 各段(2→3 / 3→4 / 4→5 / 5→6)を個別に検証(落ちた段が特定できる)
  2. v2 から最新まで通し
  3. 既存データ(projects / tasks)がマイグレーションで失われない +
     v4 追加列が既存行で NULL になること
  4. v6 の documents / document_classifications が実際に読み書きできること
  5. **実際の Room ビルダーで開けること**(`addMigrations` への登録漏れ=チェックリスト③は
     ここで初めて落ちる)
- `validateDroppedTables = true` で消し忘れテーブルも検出する

### ⚠ v1 は自動テストできない(既知の制約)

`app/schemas/` は **2.json から**しか無い(v1 の時点では `exportSchema` を有効にしていなかった)。
MigrationTestHelper は期待スキーマが無いバージョンのDBを作れないため、**v1→v2 だけは対象外**。
1.json を手書きすると Room の `identityHash` を捏造することになり、誤った安心を生むので**あえて作らない**。
v1 から使い続けている端末の更新は、実機更新でのみ確認できる。

### 検証状況

- `assembleDebugAndroidTest` 成功(**コンパイルとAPK同梱までは確認済み**)
- `assembleDebug` + `testDebugUnitTest` 成功(単体165件全成功、既存に影響なし)
- **テスト自体はまだ実行していない**(実機またはエミュレータが必要):
  ```
  ./gradlew connectedDebugAndroidTest
  ```

---

## 2026-07-23: v4.1 AI分類ワークフロー — 同期・取込・プロンプト(③④⑤)= 実装完了

### 設計判断(合意内容からの調整・要確認)

論点3で「分類結果は回答JSON v2 の `proposals.classifications[]`(既存の取込→pending_proposals→
レビューUIを再利用)」と合意したが、実装では **classifications だけ pending_proposals に積まず、
取込時に文書へ直接適用**する形にした。理由:

- 設計書v4.1 の状態遷移が `(分類結果を取込) → needs_review` / `(承認・修正して確定) → classified`
  であり、取込=適用と定義されている
- pending_proposals を経由すると「ヨスガ画面で提案を承認 → 文書画面でもう一度承認」と
  **承認が二重**になる。文書のレビューUI(実装済み)が承認の唯一の入口である方が筋が通る
- 「初期段階では自動確定せず、必ずユーザー確認を通す」は文書画面の承認で担保されている

再利用したのは**運搬経路**(回答JSON v2 → ResponseImporter → ImportRepository)。
この判断に違和感があれば pending_proposals 経由へ戻せる(ImportRepository の1メソッドの差)。

### 実施内容

**③ documents.json のサーバー同期**
- `DocumentsFile` / `DocumentExport` を追加し、AiExporter を **6ファイル**構成に
- 送るのは **unclassified / classification_pending のみ**(分類済み・確認待ち・アーカイブは
  再分類を誘発するため送らない)。原文をそのまま渡す(要約はヨスガの仕事)
- `ServerSyncRepository` に `onDocumentsUploaded` を追加し、**アップロード成功時のみ**
  `markUnclassifiedAsPending()` を呼ぶ。失敗時は unclassified のまま残り、次回再送される
- 同期中に追加された文書が pending になり得るが、pending も毎回送るため取りこぼさない(コメント済み)
- サーバー側は変更不要(upload.php のファイル名検証は `^[a-z0-9_]+\.json$` の正規表現)
- テスト容易性のため `ServerSyncRepository` の生成依存を `buildFiles: suspend () -> List<AiExportFile>`
  に変更(既存の urlProvider / tokenProvider と同じ供給関数スタイル)。AppContainer で結線

**④ 回答JSON v2 の分類取込**
- `ClassificationProposal` / `RelatedRefImport` を追加。設計書v4.1の例に合わせ **snake_case**
  (`document_id` / `project_ids` / `document_type` / `related_entities`)で受ける
- `ImportRepository` が分類を `DocumentRepository.applyAiClassification()` へ流す。
  document_id が空・実在しない分類は読み飛ばし、件数を結果に載せる
- `ImportResult.SuccessProposals` に classificationCount / unknownDocumentCount を追加し、
  取り込みメッセージで「記録タブの『文書』で確認してください」と案内

**⑤ docs/yosuga_prompt.md**
- 「# 文書の分類」節を追加(documents.json の読み方 / classifications[] の書式 /
  原文を書き換えない / document_id を推測しない / confidence は正直に / 既存タグの再利用)
- 取り込み仕様の表に classifications の挙動3行を追加。api.php の file 一覧に documents を追加

### テスト

- `ServerSyncRepositoryTest` 4件(成功時のみ副作用 / HTTPエラーで文書を進めない /
  URL・トークン未設定で生成すら走らない)、`ImportMessageTest` 4件、
  `ResponseImporterTest` +2件(snake_case の分類 / 分類なしの回答)、
  `AiExporterTest` +1件と既存1件の更新(6ファイル / 送信対象の絞り込み)
- WSL で `assembleDebug` + `testDebugUnitTest` 成功(**165件全成功**)

### 未検証(実機・実通信が必要)

- Room v6 マイグレーション通し / 文書の一連の操作 / documents.json の実アップロード /
  ChatGPT が documents.json を読んで分類を返す一連の流れ

---

## 2026-07-23: v4.1 AI分類ワークフロー — 文書UI + 分類レビューUI

### 実施内容(記録タブ「文書」セクション / 5項目のうち ①②)

- 記録タブを **7タブ化**(アイテム/決定/日記 + **文書**)。区分チップは横スクロールに変更
- `DocumentAddDialog`: タイトル + 原文のみを入力。**保存後に原文を編集する手段は用意しない**
  (分類・タグ付け・要約はヨスガの仕事、という役割分担をUIでも崩さない)
- `DocumentDetailDialog`: 状態チップ / 現行分類 / **原文** / 分類履歴 を1画面で確認。
  設計書v4.1「UI」の5操作を実装 —
  **承認**(現行分類のまま確定)/ **修正**(ClassificationEditDialog)/
  **保留**(閉じるだけ = needs_review 据え置き。ボタン文言も「保留して閉じる」に変わる)/
  **再分類**(classification_pending へ戻す)/ **元文表示**(常時表示)。
  承認・修正は現行分類があるときだけ出す(空の分類を確定させない)
- `ClassificationEditDialog`: 要約・種別・プロジェクト・カテゴリ・タグを修正して承認。
  関連実体は手入力させず元の値を維持(ヨスガが付けるもの)。承認するとユーザー修正レコードが積まれ、
  AIの分類は履歴に残る
- `DocumentFilters`(純粋関数): 状態絞込 / 検索(タイトル・原文・要約・タグ・カテゴリ)/
  一覧プレビュー(分類済みは要約、未分類は原文冒頭)
- `Document` に `classificationHistory` を追加。DAOは元々 @Relation で全分類行を取得しているため
  追加コストなし。履歴が Flow で流れるので、修正承認するとダイアログの表示も追従する
- 削除された文書を開いたままにしないよう、詳細ダイアログは ID で引き直す

### テスト

- `DocumentFiltersTest` 11件(状態絞込 / 検索の対象と大小文字 / プレビューの分岐・改行畳み・省略 /
  信頼度の%表示とクランプ / 履歴1行の表示)、`DocumentRepositoryTest` に履歴の並び1件追加
- WSL で `assembleDebug` + `testDebugUnitTest` 成功(**154件全成功**)

### 未実装(次回)

- `documents.json` のサーバー同期(AiExporter への追加 + `markUnclassifiedAsPending()` の接続)
- 回答JSON v2 `proposals.classifications[]` の取込
- `docs/yosuga_prompt.md` への分類指示の追記
- 実機確認(Room v6 マイグレーション通し・文書の一連の操作)

---

## 2026-07-23: v4.1 AI分類ワークフロー — データ層(Room v6)

### 経緯・決定(ユーザーと合意 / 設計書v4.1「未確定の論点」5点)

1. **実装範囲**: 今回はデータ層まで(Room v6 + Repository + テスト)。UI・同期・取込は次回。
2. **文書の作成入口**: 記録タブに「文書」セクションを新設(次回のUI実装で対応)。
3. **分類結果の受け取り**: 回答JSON v2 の `proposals.classifications[]`(次回の取込実装で対応。
   既存の 取込→pending_proposals→レビューUI を再利用する)。
4. **承認確定後**: documents のまま(knowledge_items へ昇格しない。長文=文書 / 短い知識=アイテム)。
5. **related_entities の type**: 文書側は自由文字列(既存 EntityType は拡張しない)。

### 新規ライブラリ

- **なし**

### 実施内容

- `domain/model/Document`: DocumentStatus(unclassified / classification_pending / needs_review /
  classified / archived)、ClassificationOrigin(ai / user)、RelatedRef(type は自由文字列)、
  DocumentClassification、Document。未知のDB値は安全側へフォールバック
- Room v6: `documents`(**body=原文・不変**。status にインデックス)+
  `document_classifications`(AI結果とユーザー修正を**別レコード**で積む=そのまま分類履歴。
  現行1件だけ isCurrent。リスト項目は履歴スナップショットとして JSON 列で保持 —
  後からタグを改名しても過去の分類記録が変わらない)
- `DocumentJsonColumns`(純粋ロジック): JSON列の符号化・復号。壊れたJSONは空リストへ
  フォールバック(ProposalPayloads と同方針)
- `DocumentDao`: 原文を更新するSQLは置かない。`saveClassificationAsCurrent`(旧現行を落として積む)/
  `deleteDocumentWithClassifications` は default メソッドでロジックを持つ
- `DocumentRepository`: 状態遷移を集約 — createDocument(→unclassified)/
  markUnclassifiedAsPending(同期成功時に呼ぶ想定)/ applyAiClassification(→needs_review)/
  approve(現行分類が無ければ拒否)/ approveWithEdits(USER レコードを積んで→classified)/
  requestReclassification(→pending。履歴は残す)/ archive / deleteDocument。
  **原文(body)を書き換える操作は提供しない**
- `MIGRATION_5_6` + `@Database(version = 6)` + AppContainer 登録(チェックリスト①〜③)、
  スキーマJSON 6.json と突き合わせ済み(④)
- SampleSeed への documents 追加はなし(実運用データのみ)

### テスト

- `DocumentRepositoryTest` 13件(状態遷移一式 / 原文不変 / ユーザー修正でAI履歴が残る /
  再分類で履歴が積み上がる / markUnclassifiedAsPending が unclassified のみ動かす /
  壊れたJSON列の耐性 / roundtrip / 未知DB値フォールバック)
- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL / **142件全成功**。
  ※前回記録の「145件」は誤記で、実数は129件だった)

### 未実装(次回)

- 記録タブ「文書」セクション(作成・一覧・詳細・元文表示)
- 分類レビューUI(承認 / 修正 / 保留 / 再分類)
- `documents.json` のサーバー同期(AiExporter への追加 + markUnclassifiedAsPending の接続)
- 回答JSON v2 `proposals.classifications[]` の取込(ResponseImporter / ImportRepository)
- `docs/yosuga_prompt.md` への分類指示の追記

---

## 2026-07-23: v4 Phase2 — AI用JSON分割 + ロリポップ同期

### 経緯・決定(ユーザーと合意)

- 設計方針 **v4(AIプラットフォーム構想)** を取り込み(コミット 4877666)。
  Hubは「人間のUI」と「AIのデータインターフェース」の両方を持つ。
- **v3の「クラウド同期なし」を v4 が上書き**し、ロリポップ同期を導入。
- 同期方式: **PHP受け口 + トークン認証**(Android→HTTPS POST、配信は api.php?token=)。
  公開するのは進捗・整理情報のみのため、トークン認証を最低限の保護とする。
- conversations.json は当面「取込・承認履歴」で代用(会話本文は保持しない)。

### 新規ライブラリ

- **なし**(Ktor は GitHub連携で導入済みのものを流用)

### 実施内容(Android)

- `data/file/model/AiExportModels`: 用途別5ファイルのモデル
  (projects / tasks / knowledge(items+diary) / calendar(today+upcoming+past) /
  conversations(exchanges))。各ファイルが schemaVersion / generatedAt を持つ
- `data/file/AiExporter`(純粋ロジック): 5ファイルの組み立て。
  projects / tasks の変換は `ContextExporter.projectExportOf / taskExportOf` として公開化し
  状況JSON(手動ブリッジ)と共有。提案履歴のタイトルは payload から抽出(壊れていても落ちない)
- `PendingProposalDao.recent(limit)` を追加(履歴50件)
- `AiExportRepository`: ドメインRepositoryからスナップショットを集めて5ファイル生成、
  `filesDir/ai/` へ保存(AI Interface の初期実装 = ローカルJSON出力)
- `data/sync/SyncApi`: `upload.php` へ POST(トークンは `X-Yosuga-Token` ヘッダー。URLに載せない)。
  結果を Ok / Unauthorized / HttpError / NetworkError に分類、例外メッセージは伝播させない
- `ServerSyncRepository`: URL・トークンを供給関数で受け、成功時に最終同期時刻を記録
  (ホームの「最終同期」が本来の意味を持つようになった)
- `SyncSettingsRepository`: URL(平文)+ トークン(Keystore暗号化。GitHubと同じ TokenCrypto を共用)。
  `generateToken()`(SecureRandom 64桁hex)
- 設定画面「サーバー同期」セクション: URL / トークン(伏字)/ トークン生成 / 今すぐ同期
- `docs/yosuga_prompt.md`: サーバーAPI経由で状況を読ませる手順を追記

### 実施内容(サーバー / `server/` ディレクトリ)

- `config.php`(トークン設定)/ `upload.php`(受け口: hash_equals検証・ファイル名ホワイトリスト・
  JSON妥当性チェック)/ `api.php`(配信: file=index で一覧+更新時刻)/ `data/.htaccess`(直接アクセス拒否)
- `README-server.md`: ロリポップへの設置手順・動作確認・ChatGPTへの渡し方
- config.php が初期値のままなら全リクエスト403(設定忘れをフェイルセーフに)

### テスト

- `AiExporterTest` 4件(5ファイル生成 / 各ファイルのパース・schemaVersion / 履歴タイトル抽出 /
  壊れたpayload耐性)、`SyncApiTest` 4件(URL・ヘッダー・本文 / 403 / 500 / オフライン)
- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)

### 未検証

- ロリポップへの実設置・実通信(ユーザー作業: README-server.md 参照)
- ChatGPT のブラウズでの api.php 読み取り

---

## 2026-07-23: カレンダー連携(旧Phase 4)— 端末カレンダー読み取りで実装

### 方式の決定(ユーザーと合意)

設計書v2は Google Calendar API + OAuth 前提だったが、**CalendarContract で端末に同期済みの
カレンダー(Googleカレンダー含む)を直接読む方式**を採用した。

- 採用理由: READ_CALENDAR 権限のみで動作し、**Google Cloud プロジェクト / OAuth クライアントID /
  SHA-1登録 / 同意画面の設定がすべて不要**。新規ライブラリもゼロ。オフラインでも動く。
  設計書の完了条件(過去7日〜未来7日を確認 / 状況JSONへ出力)は満たせる。
  「初期版の権限は可能な限り読み取り専用に限定する」(設計書5.1)にも合致。
- 制約: 端末に同期されているカレンダーのみが対象(Pixel で Google カレンダーを使う運用なら問題なし)。
- 将来 API + OAuth へ移す場合も、DataSource を差し替えるだけで済む構造にしてある。

### 新規ライブラリ

- **なし**

### 実施内容

- `AndroidManifest.xml` に **READ_CALENDAR**(読み取り専用)を追加
- `data/calendar/EventFormatting`(純粋ロジック): 今日の日付表示 / 予定時刻の整形
  (今日は時刻のみ・他日は日付+時刻・終日は日付のみ)/ 今日・今後・過去のバケット判定
- `data/calendar/DeviceCalendarDataSource`: `CalendarContract.Instances` を期間指定で問い合わせ
  - **Instances を使うことで繰り返し予定が期間内の個別予定として展開される**
  - 権限チェック、タイトル空欄は「(タイトルなし)」、終日予定は終了時刻を出さない
  - 結果は `Result<List<CalendarEventEntity>>`(例外を投げない)
- `CalendarEventDao.replaceAll()`(@Transaction で全削除→挿入。同期は常に洗い替え)
- `CalendarRepository` を刷新:
  - `today` を**端末時計から**算出(固定文字列のプレースホルダーを廃止)
  - `sync()`: 権限なし → `PermissionDenied` / 失敗 → `Failed`(**キャッシュは変更しない**)/
    成功 → 洗い替え
- `CalendarViewModel`: `sync()` と `hasPermission()`、同期中フラグ
- `CalendarScreen`: 「端末のカレンダーから更新」ボタン(未許可なら権限ダイアログ →
  許可されたらそのまま同期)。0件時の表示、今日の日付をセクション見出しに表示
- `ui/share/CalendarMessage`: 結果 → 短文(0件と失敗を区別、技術的な例外文は出さない)
- **仮カレンダーのシードを停止**(実データに置き換わったため)。`SampleSeed.events` はテスト用に残す
- テスト: `EventFormattingTest` 5件、`CalendarMessageTest` 4件

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)
- **実機/シミュレーター未検証**: 権限ダイアログ、実際の予定取得、繰り返し予定の展開。
  シミュレーターの場合、Googleアカウント未追加だと予定0件になる点に注意。

### これで仮データはすべて解消

- プロジェクト/タスク/知識ベース: 手動編集・AI提案・GitHub取得で実データ化済み
- カレンダー: 端末から実データ取得(本ステップ)
- 残るシードは初回起動時のサンプルのみ(実データ投入後は上書き・削除可能)

---

## ▶ 次回の再開ポイント(2026-07-23 セッション終了時点)

### 設計書の階層(読む順)
1. `yosuga_hub_android_design_v4.md` — **最上位方針**(AIプラットフォーム構想)
2. `yosuga_hub_design_v4_1_classification.md` — **追加指示: AI分類ワークフロー(未実装)**
3. `yosuga_hub_android_design_v3.md` / `_v3_1.md` — 基本思想・知識ベース仕様(有効)
4. `yosuga_hub_android_design_v2.md` — 技術資料(アーキテクチャ・ライブラリ候補)

### 実装済み(すべてビルド+テスト通過)
- **v3-Step 1〜4**: Task実体化 / 知識ベース+提案承認フロー / Obsidian連携(SAF) / ホームAI秘書再編
- **GitHub連携 3-a〜3-d**: リポジトリ設定 + トークンKeystore保管 / Ktorで `.yosuga/status.json` 取得 /
  Room v5 キャッシュと表示 / 状況JSONへの反映
- **カレンダー連携**: CalendarContract で端末カレンダーを読む(OAuth不要)
- **v4 Phase2**: AI用JSON 5分割(AiExporter)+ ロリポップ同期(SyncApi / server/ にPHP一式)
- **v4.1 AI分類ワークフロー(実装完了)**: Room v6(documents / document_classifications)+
  DocumentRepository(状態遷移・分類履歴)+ 記録タブ「文書」セクション(承認/修正/保留/再分類/元文表示)
  + documents.json 同期 + 回答JSON v2 `classifications[]` 取込 + ヨスガ用プロンプト。
  論点5点は合意済み(取込の扱いのみ調整あり — 本日のエントリ「設計判断」参照)。
  実装後の自己レビューで欠陥4件を修正済み(本日のエントリ参照)
- **Room マイグレーションの自動テスト**(androidTest / 未実行。実機が必要)
- 仮データはすべて解消済み。単体テストは 166 件。

### ▶ 次にやること

**A. 実環境での検証**(ユーザー作業が必要 / v4.1 が実装完了したので次はこれが本命)
下の「B. 実環境での検証」の手順に加え、v4.1 の通し確認:
1. 記録タブ「文書」で文書を追加 → 設定で「今すぐ同期」→ 状態が「分類待ち」になるか
2. ChatGPT に documents.json を読ませ、`classifications[]` を含む回答JSONをもらう
3. 取り込み → 文書が「確認待ち」になり、分類内容が表示されるか
4. 承認 / 修正して承認 / 再分類 を試し、履歴が積まれるか
5. Room v1〜v6 のマイグレーション通し(既存端末の更新でクラッシュしないか)

**B. 実環境での検証**(ユーザー作業が必要)
1. ロリポップ設置: `server/README-server.md` の手順(トークン生成 → config.php → FTP → 同期)
2. 各ゲームの Claude Code に `docs/claude_code_onboarding.md` を渡し `.yosuga/` を作らせる
3. GitHub Fine-grained PAT(Contents: Read-only)→ アプリ設定 → プロジェクト編集で owner/repo 入力
4. ChatGPT に `docs/yosuga_prompt.md` を貼る → 状況JSON or サーバーAPI経由で運用開始

### 未検証(実機・実通信が必要)
- Room マイグレーション v1→v5 の通し。
- GitHub 実通信 / Keystore のトークン保存→再起動後の復号。
- Obsidian SAF 書き出し(`createFile("text/markdown")` の拡張子挙動はプロバイダ依存)。
- カレンダー(READ_CALENDAR 権限ダイアログ・繰り返し予定)。シミュレーターは
  Googleアカウント未追加だと0件になる。
- **ロリポップへの実設置と実通信**(MockEngine では検証済み)。

### WSL でビルドする場合
```
echo "sdk.dir=/home/note_xylo/android-sdk" > local.properties
JAVA_HOME=/home/note_xylo/tools/jdk-17.0.19+10 ./gradlew assembleDebug testDebugUnitTest --no-daemon
```
※ 終わったら local.properties を Windows 用(`C:\Users\note_\AppData\Local\Android\Sdk`)へ戻す。

### マイグレーション追加時のチェックリスト(2回踏んだ)
①Migration定義 ②`@Database(version)` 更新 ③`addMigrations` 登録 ④スキーマJSON突き合わせ
— ②③はコンパイルが通ってしまい、実機更新時にクラッシュする。
**自動テストあり**: `MigrationTest`(androidTest)。`./gradlew connectedDebugAndroidTest` で
①〜④をまとめて検証できる。新しい版を足したら `MigrationTest.LATEST_VERSION` と
`allMigrations` も更新すること。※ v1→v2 のみ対象外(1.json が無いため。理由は同テストのコメント)。

### 積み残し(任意)
タグのタスク適用 / 取り込み履歴画面 / エンティティ閲覧UI / 観察日記のObsidian書き出し /
FileProvider共有 / status.json の decisions を提案へ流し込む / カレンダーの起動時自動同期 /
MCP対応(v4 Phase4)。

---

## 2026-07-23: GitHub連携 3-d 状況JSONへGitHub進捗を反映 — GitHub連携 完了

### 目的

ヨスガが各ゲームの**実際の**進捗(ブロッカー・質問を含む)を踏まえて提案できるようにする。

### 新規ライブラリ

- **なし**

### 実施内容

- `ProjectExport` に `source`("github" / "local")/ `health` / `blockers[]` /
  `questionsForYosuga[]` を追加
- `ContextExporter.build` に `statuses: Map<projectId, ProjectStatusSnapshot>` を追加
  - 取得済みなら status.json 由来を優先し、`statusMarkdown` を設計書19.3 の見出し構成
    (Summary / Current Goal / In Progress / Next Tasks / Blockers / Questions for Yosuga /
    Source Commit)で生成
  - `lastUpdated` は status の generatedAt を優先、`health` も status 側を優先
  - 未取得なら従来どおり手元の値(source=local)。**未取得を取得済みに見せない**
- `ExportRepository` がキャッシュを読み込んで渡す(`AppContainer` 配線)
- `docs/yosuga_prompt.md` を更新: source / blockers / questionsForYosuga の読み方と、
  「ブロッカー解消を優先度高めで提案」「質問には必ず応答し、決定したものだけ decision にする」を追記
- テスト: `ContextExporterTest` に2件(GitHub由来の優先反映 / 未取得時のローカルフォールバック)

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)

### GitHub連携の完了状況

- ✅ 3-a 設定の器(リポジトリ設定・トークンのKeystore保管)
- ✅ 3-b Ktor で取得・検証(schemaVersion / projectId 突合)
- ✅ 3-c Room v5 キャッシュと詳細画面表示・一括更新
- ✅ 3-d 状況JSONへの反映
- ⬜ 実通信・実機確認は未実施 → 再開ポイント参照

---

## 2026-07-23: GitHub連携 3-c キャッシュと表示(Room v5)

### 目的

取得した status.json を Room にキャッシュし、プロジェクト詳細に表示する。
オフラインでも直近の進捗が見える状態にする。

### 新規ライブラリ

- **なし**

### 設計判断

- **projects テーブルは上書きしない**。手動編集(1-d)と衝突させないため、GitHub 由来の情報は
  詳細画面の独立セクション「GitHub 進捗」として表示する。health の更新は従来どおり
  ヨスガの提案(承認フロー)経由で行う。
- キャッシュには**取得した本文をそのまま**保存する(未知項目を失わない)。表示時にパースする。
- 取得失敗時はキャッシュを更新しない → 直近の表示を壊さない。

### 実施内容

- **Room v4→v5**: `project_status_cache`(projectId PK / statusJson / fetchedAt)+ `MIGRATION_4_5`。
  スキーマ `5.json` との一致を確認
- `StatusFetchResult.Success` に `rawJson` を追加(キャッシュ保存用)
- `domain/model/ProjectStatusSnapshot` + `StatusLine`: 表示用ドメインモデル
  (通信DTOを UI へ渡さない方針を維持)
- `data/github/StatusSnapshotMapper`(純粋ロジック): DTO → スナップショット変換。
  空タイトルの項目は落とし、進捗率・優先度・深刻度を詳細行にまとめる
- `ProjectStatusRepository`: `statuses(): Flow<Map<projectId, Snapshot>>`(壊れた行は読み飛ばす)/
  `refresh(project)`(成功時のみキャッシュ更新)/ `refreshAll(projects)`(未設定は通信しない)。
  fetch は関数で受けてテスト可能に
- `ui/share/StatusMessage`: 取得結果 → ユーザー向け短文(次に何をすべきかを含む)、
  一括更新のサマリ文
- `ProjectDetailScreen`: 「GitHub 進捗」カード(要約 / 目標 / 作業中 / 次のタスク / ブロッカー /
  ヨスガへの質問 / 取得・生成時刻 / 状態)+「GitHubから更新」ボタン(取得中は無効)。
  リポジトリ未設定・未取得の案内も表示
- `ProjectsScreen`: 「GitHubからすべて更新」ボタン(設定済みリポジトリがある場合のみ表示)
- テスト: `ProjectStatusRepositoryTest` 6件(変換 / 成功時保存 / **失敗時にキャッシュを壊さない** /
  Flow変換 / 壊れた行のスキップ / 未設定プロジェクトを通信しない)、`StatusMessageTest` 3件

### 気づいた不具合(修正済み)

- `MIGRATION_4_5` を `addMigrations` に登録し忘れていた。**コンパイルは通るが実機更新時に
  起動クラッシュする**種類の不具合。マイグレーション追加時のチェックリストは
  ①Migration定義 ②`@Database(version)` 更新 ③`addMigrations` 登録 ④スキーマJSON突き合わせ の4点。

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)
- **実通信・実機は未検証**。トークンと `.yosuga/status.json` を用意した実リポジトリで要確認。

### 次にやること

- **3-d**: 状況JSONエクスポートへ GitHub 由来の進捗を反映(statusMarkdown の実データ化)

---

## 2026-07-23: GitHub連携 3-b Ktor で status.json を取得・検証

### 目的

GitHub Contents API から各ゲームの `.yosuga/status.json` を取得し、検証してモデル化する。
UI への反映は 3-c(この段階では取得層のみ)。

### 新規ライブラリ(追加理由)

- **Ktor Client 3.0.3**(`ktor-client-core` / `ktor-client-okhttp` /
  `ktor-client-content-negotiation` / `ktor-serialization-kotlinx-json`)
  - 理由: ユーザーと合意した HTTP クライアント。kotlinx.serialization(導入済み)と統合でき、
    エンジン差し替えで MockEngine によるユニットテストが書ける。Android は OkHttp エンジンを使用。
- **ktor-client-mock 3.0.3**(testImplementation): 実通信なしで応答を差し替えるため。
- **kotlinx-coroutines-test 1.9.0**(testImplementation): コルーチンテストの標準ユーティリティ。

### 実施内容

- `AndroidManifest.xml` に **INTERNET 権限**を追加(GitHub 取得に必須)
- `data/github/model/ProjectStatus`: 設計書19.2 の status.json モデル
  (必須は schemaVersion のみ、他はデフォルト値付き。currentGoal / completed / inProgress /
  nextTasks / blockers / recentChanges / risks / decisions / questionsForYosuga)
- `data/github/StatusParser`(純粋ロジック): `ignoreUnknownKeys` で未知項目を無視、
  schemaVersion 検証(1のみ対応)、**projectId の突き合わせ**(設計書20章の必須条件)。
  結果は sealed(Success / InvalidJson / UnsupportedSchema / ProjectIdMismatch)
- `data/github/GitHubApi`: Contents API から `Accept: application/vnd.github.raw` で本文を直接取得
  (Base64デコード不要)。`X-GitHub-Api-Version` 付与、タイムアウト設定、ブランチ指定は `?ref=`。
  HTTPステータスを FetchResult(Success/Unauthorized/NotFound/HttpError/NetworkError)へ分類。
  **例外メッセージを伝播させない**(トークンが載る事故を防ぐ)
- `data/repository/GitHubStatusRepository`: リポジトリ未設定・トークン未設定を**通信前に**判定し、
  取得〜検証結果を `StatusFetchResult` へ集約
  - トークンは `tokenProvider: suspend () -> String?` で受ける(Keystore/DataStore に依存せずテスト可能。
    当初 GitHubSettingsRepository を直接受けていたが、final クラスでフェイク化できず設計変更)
- `AppContainer` に gitHubStatusRepository を配線(tokenProvider は復号関数を渡す)
- テスト: `StatusParserTest` 7件(正常/未知項目無視/最小/壊れたJSON/schemaVersion欠落/非対応版/ID不一致)、
  `GitHubStatusRepositoryTest` 9件(MockEngine で URL・Authorization・Accept ヘッダー検証、
  ref パラメータ、未設定時は通信しない、401/403/404/500/オフライン、JSON異常の分類)

### 詰まった点

- Ktor 3 の MockEngine ハンドラは `MockRequestHandleScope` レシーバが必要で、
  素のラムダ型では `respond` が解決できない。テストの関数型を
  `MockRequestHandleScope.(HttpRequestData) -> HttpResponseData` に修正して解決。

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)
- **実通信は未検証**(実機とトークンが必要)。MockEngine による分岐検証のみ。

### 次にやること

- **3-c**: 取得結果を Room にキャッシュして表示(プロジェクト詳細に status 表示 /
  最終取得時刻 / エラー表示 / 全プロジェクト一括更新)

---

## 2026-07-23: GitHub連携 3-a 設定の器(リポジトリ設定 + トークンのKeystore保管)

### 経緯・決定

- シミュレーターで動作確認済み(5画面 + 新機能)。実機は未入手。
- ユーザーと合意: **HTTPクライアントは Ktor Client**(3-b で追加)、**リポジトリは private**
  → アクセストークンが必須。
- 各ゲームの Claude Code には `docs/claude_code_onboarding.md` を渡して `.yosuga/` を作らせる。

### 新規ライブラリ

- **なし**(3-a は保管と設定UIのみ。Ktor は 3-b で追加)
- トークン保管に `androidx.security-crypto` は**使わない**。非推奨化の経緯があるため、
  設計書9章のとおり **Android Keystore を直接利用**する(依存ゼロ)。

### 実施内容

- **Room v3→v4**: projects に `repoOwner` / `repoName` / `repoBranch`(いずれも nullable)を追加。
  `MIGRATION_3_4`(ALTER TABLE ADD COLUMN ×3)。スキーマ `4.json` と一致することを確認
  - ※ 途中 `@Database(version)` の更新漏れでスキーマJSONが生成されず気づいた。
    マイグレーション追加時は version 更新とスキーマJSON生成の確認をセットで行うこと
- `Project`(domain)に repo* と `hasRepository` を追加、Mapper 双方向対応
- **トークン保管**(`data/security/`):
  - `SecretEnvelope`(純粋ロジック): IV + 暗号文 を `Base64:Base64` で詰め替え。壊れた入力は null
  - `TokenCrypto`(interface)/ `KeystoreTokenCrypto`: AndroidKeyStore の AES-256-GCM 鍵で暗号化。
    鍵はKeystoreから取り出せない。例外時は null を返し、**平文を例外メッセージにも載せない**
  - `UserPreferencesRepository.gitHubTokenEncrypted`(**暗号化済み文字列のみ**を DataStore へ)
  - `GitHubSettingsRepository`: `hasToken: Flow<Boolean>`(UIへはトークン本体を出さない)/
    `saveToken` / `clearToken` / `currentToken()`(通信直前にのみ復号)
- 設定画面に「GitHub アクセストークン」セクション(PasswordVisualTransformation で伏字入力、
  保存後は入力欄を即クリア、削除ボタン、Fine-grained PAT の作り方を案内)
- プロジェクト編集ダイアログに owner / リポジトリ名 / ブランチ欄を追加(空欄は null = 取得対象外)。
  項目増加に伴いダイアログ内を verticalScroll 化
- テスト: `GitHubSettingsRepositoryTest`(封筒の往復 / 不正入力の拒否 / 暗号化契約)

### セキュリティ上の注意(維持すること)

- 平文トークンは DataStore にもログにも書かない。UI 状態にも保持しない(入力欄のみ、保存後クリア)。
- `KeystoreTokenCrypto` は **ユニットテスト対象外**(Keystore が必要)。実機/エミュレーターで
  保存→再起動→復号できることを確認する。端末バックアップ復元等で鍵が失われた場合は
  復号失敗 → null → 「未設定」扱いになる(再入力で復旧)。

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)

### 次にやること

- **3-b**: Ktor Client 追加 → GitHub Contents API で `.yosuga/status.json` を取得・パース・検証

---

## 2026-07-23: 記録タブの磨き込み(手動追加・編集・検索)

### 目的

記録タブを閲覧専用から編集可能へ。AI経由でなくても短い知識をその場で残せるようにする。

### 新規ライブラリ

- **なし**

### 実施内容

- `RecordsFilters` に純粋関数を追加:
  - `searchItems`(タイトル・本文・タグの部分一致、大文字小文字無視、空なら全件)
  - `parseTagsInput`(カンマ・読点区切り → trim・空除去・重複除去)
- `ItemEditDialog`: 種類チップ(6種・横スクロール)/ タイトル(必須)/ 本文 / タグ(カンマ区切り)。
  編集時のみ削除ボタン。実体(entities)の関連付けは AI 経由のみ(手動編集では触らない)
- `RecordsViewModel` に `addItem`(source=manual)/ `updateItem` / `deleteItem`
- `RecordsScreen`(アイテムセクション): 検索フィールド + [アイテムを追加] ボタン +
  カードタップで編集。検索・タグ絞込は併用可。0件時の文言を「条件に合わない」と区別
- テスト: `RecordsFiltersTest` に検索・タグ入力パースの2件を追加

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)

---

## 2026-07-23: v3-Step 3 Obsidian連携(SAF)を実装 — 動作検証は実機入手後

### 目的

承認した知識のうち長文として残すべきもの(targetNote 指定)を、SAF で選択した
Obsidian Vault のノートへ追記できるようにする(v3設計書付録の確定事項 2)。

### 新規ライブラリ(追加理由)

- **androidx.documentfile:documentfile 1.0.1**
  - 理由: SAF のツリーURI(OpenDocumentTree で選択した Vault)配下のファイル探索・作成を
    安全に行う公式ヘルパー。生の DocumentsContract より簡潔で、依存は極小。

### 実施内容

- `data/obsidian/KnowledgeStore`(interface)+ `AppendOutcome`(WRITTEN / NOT_CONFIGURED / FAILED)
  - 書き出し先の抽象化。将来 GitHub / Drive へ差し替え可能(v3付録の方針)
- `data/obsidian/ObsidianMarkdown`(純粋ロジック):
  - `normalizeNoteName`(パス除去・`.md` 保証・空なら YosugaHub.md)
  - `buildNoteAppendix`(## タイトル + 本文 + #タグ + 日付。タグ中の空白は `_` へ)
- `data/obsidian/ObsidianVaultStore`: DocumentFile でノートを探索/作成し、
  `openOutputStream(uri, "wa")` で末尾追記。権限失効等は FAILED(クラッシュしない)
- `UserPreferencesRepository`: `obsidianVaultUri` を DataStore に追加
- 設定画面: 「Obsidian Vault」セクション新設(OpenDocumentTree で選択 →
  `takePersistableUriPermission` で読み書き権限を永続化 → URI保存。選択中フォルダ名を表示)。
  `SettingsViewModel` を新設
- 回答JSON v2 の `ItemProposal` に `targetNote`(任意)を追加
  - 承認時: Room 保存後、targetNote があれば Vault の該当ノートへ追記
  - **書き出しの成否に関わらず Room 保存は成立**(Obsidianは付加的)
  - `ApproveResult.Applied` が書き出し結果を持ち、Toast で通知
    (追記済み / Vault未設定 / 失敗)
  - 提案カードに「Obsidian: ノート名」を表示
- `docs/yosuga_prompt.md` に targetNote の書き方・挙動を追記
- テスト: `ObsidianMarkdownTest` 3件、`ProposalRepositoryTest` に targetNote 3件
  (書き出し+markdown内容 / Vault未設定でもRoom成立 / targetNote無しはスキップ)

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)
- **実機での動作検証は未実施**(SAF フォルダ選択・実 Vault への追記・Obsidian アプリでの表示確認)。
  `createFile("text/markdown", "xxx.md")` の拡張子挙動はプロバイダ依存のため実機で要確認

### 次にやること

- ④ 記録タブの磨き込み(アイテムの手動追加・編集・検索)

---

## 2026-07-23: ヨスガ用プロンプト仕様書 + ホームのAI秘書再編(v3-Step 4 相当)

### 経緯

実機が手元にないため、実機確認は保留して先へ進む(ユーザーと合意)。
選択された作業: ①プロンプト仕様書 → ②ホーム再編 → ③Obsidian連携 → ④記録タブ磨き込み。

### ① `docs/yosuga_prompt.md` 新設(コミット 5904c56)

- ChatGPT に貼るシステムプロンプト(ヨスガの役割 / 状況JSONの読み方 / 回答JSON v2 の書式とルール /
  観察日記の書き方 / タグ運用ルール)
- アプリ側の取り込み挙動の対応表(スキーマ判定・スキップ条件・自動棄却)
- 動作確認用の最小サンプルJSON
- これで手動JSONブリッジの ChatGPT 側の準備が完成

### ② ホームのAI秘書再編

- `ui/screen/home/TodayFocus`(純粋関数): 「今日やること」を本物のタスクから導出
  - 完了除外。作業中 → 期限切れ → 今日締切 → 優先度 → 締切近い順。最大5件
  - 旧「優先タスク」プレースホルダー(`ProjectRepository.priorityTask`)を削除
- `HomeViewModel`: タスク・承認待ち件数・最近の決定(最大3件)を secretaryFlow に集約して combine
  (combine の5フロー制限対策で中間データにまとめる)。`todayDate` は注入可能
- `HomeScreen`:
  - 「承認待ちの提案」カード(1件以上のときだけ表示。ヨスガ画面へ誘導)
  - 「今日やること」カード(タスク名 + プロジェクト名/優先度/締切。0件時の表示あり)
  - 「最近の決定」カード(決定事項ログの新しい3件。0件時は非表示)
  - 「優先タスク」カードを削除。それ以外(予定・進捗・JSONボタン・最終同期)は維持
- テスト: `TodayFocusTest` 4件(完了除外 / doing優先 / 期限切れ→今日→優先度 / 件数制限)

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)

### 次にやること

- ③ Obsidian連携(SAF・KnowledgeStore 抽象化)の実装(動作検証は実機入手後)

---

## 2026-07-23: v3-Step 2-e 状況JSONエクスポート v2 — v3-Step 2 完了

### 目的

状況JSONにタスク・最近の決定事項を含め、AIが現状を踏まえた提案を返せるようにする。

### 新規ライブラリ

- **なし**

### 実施内容

- `ContextExport` を v2 へ: `SCHEMA_VERSION = 2`、`tasks[]` / `recentDecisions[]` を追加。
  `responseSchemaVersion = 2` も追加(「proposals 形式で返してほしい」という宣言)
- `TaskExport`(projectId/title/detail/status/priority/dueDate)/ `DecisionExport`(date/title/body)
- `ContextExporter.build` に tasks / decisions パラメータを追加(既定は空リスト)
- `ExportRepository`: タスク全件 + 決定事項(新しい順に最大20件)をスナップショットに追加
- `AppContainer`: ExportRepository へ taskRepository / knowledgeRepository を配線
- テスト: `ContextExporterTest` を v2 に更新 + タスク/決定事項マッピングのテストを追加

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)

### v3-Step 2 完了条件の達成状況

- ✅ 2-a: Room v3(知識ベースのデータ層)
- ✅ 2-b: 回答JSON v2 取り込み(v1互換維持)
- ✅ 2-c: 提案レビューUI(承認→反映 / 棄却→履歴)
- ✅ 2-d: 「記録」タブ(アイテム・決定・日記、タグ絞込)
- ✅ 2-e: 状況JSONエクスポート v2
- ⬜ 実機確認は未実施 → 再開ポイント参照

**→ v3-Step 2 完了。次は実機確認、その後 Step 3(Obsidian連携)等を相談。**

---

## 2026-07-23: v3-Step 2-d 「記録」タブ新設(知識ベース閲覧・6タブ化)

### 目的

承認済みの知識(情報アイテム・決定事項ログ・観察日記)を閲覧する「記録」タブを追加する。
Step 2 設計の合意どおり6タブ化(ホーム/カレンダー/プロジェクト/記録/ヨスガ/設定)。

### 新規ライブラリ

- **なし**

### 実施内容

- `YosugaDestination` に `Records`(記録、Icons.Filled.Create)を追加し NavHost へ配線
- `ui/screen/records/RecordsViewModel`: items + tagNames + diaryEntries を combine
- `ui/screen/records/RecordsFilters`(純粋関数): `filterItemsByTag`(null=すべて)/
  `decisionsOf`(kind=DECISION 抽出)
- `ui/screen/records/RecordsScreen`:
  - セクション切替チップ(アイテム / 決定 / 日記)。選択状態は rememberSaveable で保持
  - アイテム: タグ絞込チップ(横スクロール、再タップで解除)+ カード
    (日付・種別チップ・本文・#タグ・関連実体)
  - 決定: 決定事項ログを時系列カードで表示(日付 + タイトル + 本文)
  - 日記: 観察日記を日付ごとのカードで表示
  - 各セクションの空状態テキストに対応
- `ui/component/KindLabels`: itemKindLabel を共用化(ProposalCardUi の private 版を移設)
- テスト: `RecordsFiltersTest`(タグ絞込 / 全件 / 決定事項抽出)3件

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)

### 次にやること

- **2-e**: 状況JSONエクスポート v2(タスク・決定事項を含める)→ Step 2 完了

---

## 2026-07-23: v3-Step 2-c 提案レビューUI(ヨスガ画面を承認ハブ化)

### 目的

承認待ち提案をヨスガ画面にカード表示し、承認で本テーブルへ反映・棄却で履歴化する。
v3 の中核「提案→ユーザー承認→保存」を初めて一気通貫にする。

### 新規ライブラリ

- **なし**

### 実施内容

- `data/file/ProposalPayloads`: payloadJson の復号(壊れていたら null、クラッシュさせない)
- `ProposalRepository`(新設): pending監視 / approve / reject
  - 承認の反映先: task→`TaskRepository.create(source=assistant)` / item→`KnowledgeRepository.createItem`
    (タグ・実体は名前解決で自動作成)/ diary→`DiaryRepository.add`(日付欠落は受信日で補完)/
    health→`ProjectRepository.updateHealth`(新設。AI分析由来の自由値も受け入れ、lastUpdatedを刻む)
  - 反映できない提案(壊れたpayload・対象プロジェクト不明)は rejected へ回す(`ApproveResult.NotApplicable`)
- `TaskRepository.create` に `source` パラメータを追加(既定 manual)
- `ProjectDao.updateHealth` を追加(更新行数を返す)
- `ui/screen/assistant/ProposalCardUi`: 表示モデルへの純粋変換
  - 種別ラベル(タスク/メモ/アイデア/決定事項/買い物/技術/観察日記/状態更新)、
    タグは `#タグ名`、壊れたpayloadは「(読み取れない提案)」として承認ボタン無効化
- `AssistantViewModel`: pending + recommendations を combine、approve/reject を追加
- `AssistantScreen`: 「承認待ちの提案(N)」セクションを追加(種別チップ + 内容 + 棄却/承認ボタン、
  結果は Toast)。既存の v1 提案一覧・JSON作成/取り込みはそのまま
- `AppContainer`: proposalRepository を配線
- テスト: `ProposalRepositoryTest`(7件: 4種の承認反映 / 不明プロジェクト / 壊れたpayload / 棄却)、
  `ProposalCardUiTest`(5件)。既存 `ProjectRepositoryTest` の FakeProjectDao に updateHealth を追加

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)

### 次にやること

- **2-d**: 「記録」タブ新設(アイテム一覧・タグ絞込・決定事項ログ・日記)
- **2-e**: 状況JSONエクスポート v2(タスク・決定事項を含める)

---

## 2026-07-23: v3-Step 2-b 回答JSON v2 の取り込み(pending_proposals へ)

### 経緯メモ

- 実機確認(v1→v3マイグレーション・Step 1 の操作一連)をユーザーが Windows Studio で実施し、問題なし。
- アシスタント名の表記を「よすが」→「ヨスガ」(カタカナ)に統一(UI 2箇所 + コメント + 設計書)。

### 目的

回答JSON v2(proposals)を取り込み、直接反映せず pending_proposals へ積む。
v1(recommendations)の取り込み互換は維持。UIは変更なし(承認UIは 2-c)。

### 新規ライブラリ

- **なし**

### 実施内容

- `data/file/model/AssistantResponseV2`: v2モデル(proposals.tasks / items / diary / projectHealth、
  各項目デフォルト値付き・未知項目無視)
- `ResponseImporter`: v1/v2 両対応(`SCHEMA_VERSION_V1/V2`、`ParseResult.SuccessV2` 追加)。
  v3以上は従来どおり UnsupportedSchema
- `data/file/ProposalMapper`(純粋オブジェクト): proposals → PendingProposalEntity 行へ変換
  - 意味を持たない提案(空タイトル・空本文・projectId欠落health)は読み飛ばす
  - payloadJson に提案1件分のJSONを保存(解釈は 2-c の承認処理)
- `ImportRepository`: SuccessV2 経路を追加(履歴保存は v1 と共通化 → pending_proposals へ insert)。
  `ImportResult.SuccessProposals(proposalCount, fileName)` を追加
- `ImportMessage`: 「提案を N 件受け取りました。ヨスガ画面で承認してください。」
- `AppContainer`: ImportRepository に pendingProposalDao を配線
- テスト: `ResponseImporterTest` に v2 パース・proposals欠落デフォルト2件、
  `ProposalMapperTest`(4種変換 / payload往復 / 無意味提案スキップ / 空)4件を追加

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)

### 次にやること

- **2-c**: 提案レビューUI(ヨスガ画面を承認ハブ化、承認で本テーブルへ反映)

---

## 2026-07-23: v3-Step 2-a 知識ベースのデータ層(Room v3)

### 目的

Step 2 設計(下記エントリ)のデータモデルを Room に実装。UIは変更なし。

### 新規ライブラリ

- **なし**

### 実施内容

- ドメインモデル: `KnowledgeItem`(+`ItemKind`/`EntityRef`)/ `TrackedEntity`(+`EntityType`)/
  `DiaryEntry` / `PendingProposal`(+`ProposalType`/`ProposalStatus`)
  - enum は未知値フォールバック(ItemKind→OTHER 等)。ProposalType のみ null 返却で読み飛ばし
  - Room の Entity と紛らわしいため実体は Tracked を冠して `TrackedEntity` と命名
- Roomエンティティ7つ: `knowledge_items` / `tags`(name unique)/ `item_tags`(複合PK)/
  `entities`((name,type) unique)/ `item_entities` / `diary_entries`(date index)/ `pending_proposals`
- **Room v2→v3**: `MIGRATION_2_3`(7テーブル+5インデックス追加、既存データ無傷)。
  出力スキーマ `3.json` と SQL の完全一致を確認済み
- `KnowledgeDao`: アイテム+タグ+実体を1つのDAOに集約
  - `observeItemsWithRefs()`(@Relation + Junction で3者をまとめて読む)
  - `saveItemWithRefs()`(@Transaction default メソッド): タグ・実体を名前で解決し
    未登録なら候補を登録、中間テーブルは張り直し
- `DiaryDao` / `PendingProposalDao`(status別監視・件数Flow)
- `KnowledgeRepository`: `createItem`(タグの trim・空除去・重複除去)/ `updateItem`(createdAt不変)/
  `deleteItem`(リンクは消しタグ本体は残す=タグの統合・削除はAIの仕事)
- `DiaryRepository`: `entries` / `add` / `delete`
- `SampleSeed`: 仮アイテム3件(買い物/決定/アイデア)+タグ3+実体2+観察日記1(Yosuga視点の文例)
- `AppContainer`: 両Repository追加、`MIGRATION_2_3` 登録、空のときシード
- テスト: `KnowledgeRepositoryTest`(フェイクDAOで default メソッドの実ロジックを検証:
  タグ名解決・再利用 / 正規化 / リンク張り直し / 削除でタグ残存 / ドメイン変換)、
  `SampleSeedTest` に知識ベース検証5件を追加

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)
- マイグレーション v2→v3 の実機検証は未実施(v1→v2 と合わせて Windows Studio で確認)

### 次にやること

- **2-b**: 回答JSON v2 パース → pending_proposals へ(v1互換維持)

---

## 2026-07-23: v3-Step 2(v3.1統合)の詳細設計を確定 — 記録のみ

### ユーザーと合意した決定

1. **知識ベースの閲覧UIは「記録」タブを新設(6タブ化)**。タブ内で アイテム / 決定 / 日記 をセグメントボタン切替、タグで絞込。
2. **タグは当面 knowledge_items のみ**。タスク・日記への拡張は将来の追加マイグレーションで。

### データモデル(Room v2→v3、テーブル追加のみ・既存データ無傷)

- `knowledge_items`: id(UUID) / kind(memo|idea|decision|shopping|tech|other) / title / body /
  createdAt / updatedAt / source(assistant|manual)。決定事項ログ=kind:decision、保留アイデア=kind:idea
- `tags`: id / name(unique)。AI管理、承認時に自動作成
- `item_tags`: itemId×tagId(複合PK、多対多)
- `entities`: id / name / type(project|person|tech|gear|event|other)。(name,type)でunique
- `item_entities`: itemId×entityId(複合PK)
- `diary_entries`: id / date(yyyy-MM-dd) / body / createdAt。Yosuga視点の観察日記
- `pending_proposals`: id / type(task|item|diary|health) / payloadJson / status(pending|approved|rejected) / receivedAt
  - 取り込みはまずここへ。承認で本テーブルへ反映、棄却は status 変更で履歴として残す
- 外部キー制約は張らない方針を継続

### 回答JSONスキーマ v2(schemaVersion=2)

- `proposals.tasks[]`(projectId/title/detail/priority/dueDate)
- `proposals.items[]`(kind/title/body/tags[名前]/entities[{name,type}])
- `proposals.diary[]`(date/body)
- `proposals.projectHealth[]`(projectId/health/reason)
- v1(recommendations)の取り込み互換は維持。15章ルール踏襲(未知項目無視・不正JSONで落ちない)
- 状況JSON(エクスポート)も v2 へ: タスク一覧・最近の決定事項を含める

### 実装順(1ステップ=1コミット)

- **2-a**: Room v3 + Repository(データ層のみ)
- **2-b**: 回答JSON v2 パース → pending_proposals(v1互換維持)
- **2-c**: 提案レビューUI(よすが画面を承認ハブ化)
- **2-d**: 「記録」タブ新設(アイテム一覧・タグ絞込・決定事項ログ・日記)
- **2-e**: 状況JSONエクスポート v2

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
