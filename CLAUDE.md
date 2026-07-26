# Project Rules

- KotlinとJetpack Composeを使用する。
- UIから直接ネットワークやDBへアクセスしない。
- ViewModelはUI状態を公開する。
- 外部データ取得はRepository経由にする。
- 秘密情報をコード、ログ、Gitへ保存しない。
- 変更後は必ずビルドする。
- 可能な範囲でテストを追加する。
- 一度に大規模な変更を行わない。
- 既存の動作を壊さない。
- 新しいライブラリを追加する前に理由を説明する。
- 作業内容をMarkdownへ記録する(`docs/WORKLOG.md`)。
- 未実装部分を、実装済みのように扱わない。

# Project Overview

個人用のAndroid制作管理アプリ「Yosuga Hub」。
設計書: `yosuga_hub_design_v5_obsidian_bridge.md`(**必読 / 最新の最上位方針: Obsidianブリッジ**)
+ `yosuga_hub_android_design_v4.md`(v5が最上位方針を上書き。技術資料として有効)
+ `yosuga_hub_design_v4_3_role_split.md`(役割分離 ヨスガ/レコル。**レコルは必須から外れた**。
  **Morning Brief は 2026-07-26 に廃止**——現況を Hub からヨスガへ直接渡す)
+ `yosuga_hub_design_v4_1_classification.md`(AI分類ワークフロー / 実装完了・維持)
+ `yosuga_hub_android_design_v3.md` / `_v3_1.md`(基本思想・知識ベース仕様として有効)。
`yosuga_hub_android_design_v2.md` は技術資料として引き続き有効(アーキテクチャ・ライブラリ候補)。

**v5の核心(2026-07-24 転換)**: Hubは**AIではなく「情報を確実に運ぶハブ」**。
判断・要約をせず、**選び・集め・整形し・運ぶ**役割に徹する。
知識の正本は **Room DB ではなく Obsidian の Markdown**。
意味付け(分類)は**生成元の Claude Code が出力時点で行い**、Hub は分類し直さない。
**Hub に AI は載せない**(AI要約を前提にしない設計にする)。

v4の核心(v5が上書き): Hubは人間のUIとAIのデータインターフェースの両方を持つプラットフォーム。
v3の「クラウド同期なし」はv4が上書き(ロリポップ同期導入)。

v3の核心: **AI=頭脳 / Yosuga Hub=記憶・表示装置**。アプリは考えず、AIが整理した結果を保存・表示・編集・検索する。
提案→ユーザーがOK→保存(自動更新しない・安全優先)。Android単体でクラウド同期なし。
知識の置き場所(v3.1): Hub=構造化された短い知識(タグ付きアイテム・決定事項・保留アイデア・観察日記・エンティティ)、Obsidian=長文(設計思想・仕様書など)。

- 技術構成: Kotlin + Jetpack Compose + Material 3 + Gradle Kotlin DSL
- アーキテクチャ: UI / ViewModel / Repository / Data Source の分離
- パッケージ: `com.shiro.yosugahub`
- minSdk 26 / targetSdk 35

# Current Phase

設計を v3(AIファースト)へ転換(2026-07-23)。旧 Phase 0〜2 の完了物は v3 の「土台」として再利用する。
- 土台(実装済み): 5画面の骨組み + Room + DataStore + ViewModel/Repository分離 + 状況JSONエクスポート/回答JSONインポート。
- 確定した方針: AIブリッジは当面「手動JSONブリッジ」を継続 / Obsidian連携は SAF で Vault フォルダ選択 / 提案→承認→保存フロー。

v3ロードマップの現在地(詳細は設計書v3・v3.1の付録・WORKLOG冒頭の再開ポイント):
- **v3-Step 1 完了**(Task実体化 + プロジェクト詳細/タスク編集/プロジェクト編集)。実機確認済み。
- **v3-Step 2 完了**(2-a〜2-e): Room v3(知識ベース: knowledge_items/tags/entities/diary/pending_proposals)、
  回答JSON v2 取り込み(v1互換維持)、提案レビューUI(ヨスガ画面で承認→反映/棄却)、
  「記録」タブ(6タブ化: アイテム/決定/日記 + タグ絞込)、状況JSONエクスポート v2(tasks/recentDecisions)。
  Step 2 の実機確認は未実施(WORKLOG再開ポイント参照)。
- **v3-Step 3 完了**: Obsidian連携(SAFでVault選択・KnowledgeStore抽象化・targetNote承認時追記)。
- **v3-Step 4 相当 完了**: ホームAI秘書再編(今日やること/承認待ち件数/最近の決定)。記録タブは手動追加・編集・検索対応。
- **GitHub連携 完了**(3-a〜3-d): リポジトリ設定 + トークンKeystore保管 / Ktor で `.yosuga/status.json` 取得 /
  Room v5 キャッシュと詳細画面表示・一括更新 / 状況JSONへの反映(source=github/local を明示)。
- 運用ドキュメント: `docs/yosuga_prompt.md`(ヨスガ=会話の相棒用)、
  `docs/recoru_prompt.md`(レコル=Hub管理・カスタムGPT用)、
  `docs/claude_code_onboarding.md`(各ゲームのClaude Code用)。
- 未検証: 実通信・実機でのマイグレーション通し・Keystore復号・Obsidian SAF書き出し(WORKLOG再開ポイント参照)。
- **カレンダー連携 完了**(旧Phase 4): 設計書v2のOAuth方式ではなく **CalendarContract で端末に同期済みの
  カレンダーを読む**方式を採用(READ_CALENDAR のみ / Google Cloud設定・新規ライブラリ不要)。
  これで仮データは解消済み。
- **v4 Phase2 完了**: AI用JSONを5ファイルに分割(AiExporter / AiExportRepository)、
  ロリポップ同期(SyncApi + ServerSyncRepository + 設定画面。トークンはKeystore、ヘッダー送信)。
  サーバーPHP一式は `server/`(設置手順は `server/README-server.md`)。
- **v4.1 AI分類ワークフロー 実装完了**: Room v6(documents=原文不変 /
  document_classifications=分類履歴)+ DocumentRepository(状態遷移)+
  記録タブ「文書」セクション(承認/修正/保留/再分類/元文表示。記録タブは7区分に)+
  documents.json 同期(AiExporterは6ファイル)+ 回答JSON v2 `classifications[]` 取込 +
  ヨスガ用プロンプトへの分類指示。「未確定の論点」5点は合意済み
  (**分類結果は pending_proposals を経由せず文書へ直接適用**する調整あり。
  理由は WORKLOG 2026-07-23 v4.1「設計判断」参照)。
- **v4.2 指示書 実装完了**: 各ゲームの Claude Code への指示書。Room v7(directives)+
  回答JSON v2 `directives[]` + 承認で配信 + `directives.json`(AiExporterは7ファイル)。
  渡し方は**ロリポップを掲示板にし、各ゲームの Claude Code が curl で読む**
  (GitHub書き込みは読み取り専用方針に反するため不採用)。
  取り込み成功時にサーバーへ自動再同期する。
  記録タブ「指示」で一覧・対応済み・削除ができる(記録タブは6区分に)。
- **実地検証の到達点(2026-07-24)**: ロリポップ設置・実通信 ✅ / カスタムGPT(レコル)の
  Actions からの取得 ✅ / **v4.1 分類ワークフローが端から端まで一周** ✅ /
  Morning Brief の生成 ✅。残りは指示書(v4.2)の通しと、マイグレーションの実機確認。
- **v5 Obsidianブリッジへ方針転換(2026-07-24)**: 設計書 `yosuga_hub_design_v5_obsidian_bridge.md`。
  Obsidian を知識の正本にし、Hub は Vault への書き込みと、Vault からの抽出→ヨスガへの
  貼り付け用 Markdown 生成を担う。v4.1/v4.2/v4.3・ロリポップ同期は**削除せず「維持するが必須ではない」**。
- **v5 Phase 1-a 完了**: Vault 読み取りの data 層。`VaultReader`(interface)/ `SafVaultReader`
  (DocumentsContract で再帰列挙)/ `Frontmatter`(純粋パーサ)/ `LoadedNote` + `NoteTransformer`
  (**要約の差し込み口。Phase 1 は恒等変換**)/ `ContextMarkdown`(純粋)/ `VaultRepository`。
  既存への変更は AppContainer の配線のみ。**Roomスキーマ変更なし・新規ライブラリなし**。テスト22件追加・全通過。
- **v5 Phase 1 完了・実機確認済み**: 1-b UI(フォルダ別一覧・複数選択・プレビュー・文字数、
  ヨスガ画面から `obsidian_context` へ遷移。**下部ナビは6個のまま**)/ 1-c(コピー・SAF保存・共有)。
  設定画面に「Vaultを読み取れるか確認」を追加(選べる≠読める の切り分け用)。
- **v5 Phase 2 完了・実機確認済み**: 絞り込み(パス検索 / 最近更新1・7・30日)、JSON出力
  (`ContextData` を挟み**形式切替でファイルを読み直さない**)、出力履歴
  (**外へ出した時だけ**記録・最新20件)、タグ絞り込み(`TagIndex` を明示的に作る・複数タグは OR)。
- **仮データ(SampleSeed)は 2026-07-25 に機能ごと廃止**。設定の削除ボタン・
  `SampleDataRepository`・初回起動時のシード・`seeding_disabled` をすべて撤去した。
  `SampleSeed.kt` は **test へ移動**(3つの Repository テストのフィクスチャ。製品には載らない)。
  **経緯**: 仮データの ID(`anri` 等)を実プロジェクトとして使っていたため、
  「サンプルデータを削除」が実運用中の行を消し、全ゲームの取り込みが壊れた。
- **プロジェクト追加UI**(2026-07-25 / `ProjectsScreen` の先頭)。
  それまで**プロジェクトは仮データからしか生まれず、作る手段が無かった**。
  `ProjectEditDialog` は `original = null` で新規作成になり、**新規のときだけ ID を入力**させる
  (ID はサーバー・レコル・分類履歴が参照する正本なので**自動採番にしない**。編集時は変更不可)。
- **プロジェクトの呼称は英字名**(2026-07-25): `ANRI` / `Kamieru` / `GengeKyou`。
  ID(`anri` / `paper-armor-frog` / `gengenkyo`)は**変更禁止**。
  ⚠ **表示名は Obsidian の `Games/<表示名>/` フォルダ名になる**(`NoteRouter`)ので、
  変えると書き込み先が変わる。
- **⚠ エミュレータの黒画面**: ハードウェア描画だと画面が黒くなり `system_server` が ANR する
  (アプリの問題ではない)。`emulator.exe -avd Pixel_10 -no-snapshot-load -gpu swiftshader_indirect`
  で復旧。恒久対処は Device Manager → Edit → Graphics →「Software - GLES 2.0」。
- **v5 Phase 3 完了・実機で通し成功**: GitHub の `.yosuga/notes/` を取得 → type で振り分け →
  Vault へ書込 / 会話ログ保存 / status.json 取得表示。
- **UI をオペレーションコンソール化**(下部ナビ廃止・フォスファーグリーン単色・端末ログ演出)。
  2026-07-24 に **Material の素の部品を全廃**(`TextButton` / `FilterChip` / `Checkbox` /
  `AlertDialog` / `OutlinedTextField` の直接呼び出しはゼロ)。代わりに `ui/component/` の
  `TacticalButton` / `TerminalChip` / `TerminalCheckbox` / `TerminalDialog` / `DialogAction` /
  `TerminalField` / `SubScreenScaffold` を使う。**新しい画面もこれらで組む**。
- **GitHub取得 → ロリポップ同期は自動**(2026-07-24)。取得が成功した時だけ push する。
- **AI との受け渡しが一通り実データで通った**(2026-07-25):
  Morning Brief / レコルの巡回(合図なしで分類→回答JSON)/ ヨスガの観測日記。
- **コンソールのコマンドは1行表記**(2026-07-25): `GitHub → Obsidian` / `会話 → Obsidian` /
  `ヨスガへ共有` / `回答JSON → Hub` / `レコルを開く` / `REVIEW`。
  **画面名(REVIEW / RECORDS 等)は英字のまま**——`ImportMessage` の案内がその名前で
  行き先を指しているため、変えると案内が壊れる。
- **Vault は更新反映する**(2026-07-25 / 実機未確認): 同じ `sourcePath` の新しい sha は
  **記録済みの場所へ上書き**(枝番を作らない)。削除は**報告のみ**で Vault 側は残す。
  観測日記は**同じ日付なら差し替え**。セッションログは**無尽蔵に追加**し、
  整理はヨスガが Frontmatter(`date`/`games`/`category`/`tags`)で行う。
- **⚠ プロンプトには文字数上限がある**。レコル(カスタムGPT)の Instructions は **8000字**で、
  超えると**黙って末尾が切れる**。編集したら `python3 scripts/measure_prompts.py` で測る。
  ヨスガは**カスタムGPTではない**(Instructions 欄が無い)のでメモリに覚えさせる。
- **⚠ `connectedDebugAndroidTest` はアプリをアンインストールする**。実行後にデータが
  全部消える(プロジェクト・トークン・Vault選択)。トークンは Keystore の鍵ごと消えるため再入力必須。
- **使い方の一覧**: `docs/how-to-use.html`(ブラウザで開く)。
- **実機(Pixel 10a / Android 17)を導入し、未検証だった項目をすべて通した(2026-07-26)**:
  マイグレーション5件 / GitHub取得 → Vault書込11件 / **ノートの上書き反映**(risk-010 解消)/
  **共有シート**と**クリップボード自動入力**の両方 / カレンダー7件 /
  Obsidian + Remotely Save で Dropbox 同期。**実機=運用機 / エミュレータ=テスト機**に分ける
  (`connectedDebugAndroidTest` はアプリのデータを消すため、実機では流さない)。
- **次にやること**(詳細は WORKLOG 冒頭の再開ポイント):
  1. **AI 側の指示を貼り直す**(2026-07-26 に朝の経路を変えたため。レコル・ヨスガ両方)。
  2. **実運用を1日通す**(`> 朝の準備` → 制作 → 観測日記 → 取り込み)。
  3. 現況JSONの**量**を実地で見る(いま約31KB。重すぎるようなら `statusMarkdown` を削る)。
- **v4.3 AI役割分離(2026-07-23)**: 実地検証で「データを読めるAI(カスタムGPT+Actions)」と
  「会話を持つAI(通常ChatGPT)」が分かれることが判明し、役割分担として設計に昇格。
  **ヨスガ=会話の相棒(Hubに触れない) / レコル=Hub管理者(Actionsで読み、回答JSONで更新提案)**。
  レコルの「Hub更新」は既存の 取込→承認→自動再同期 経路(直接書込はしない)。
  **知識を活かすのはレコルではない**(人間かヨスガの仕事)。レコルの役目は
  **Obsidian への入力を自動化すること**(2026-07-24 確定 / `items[].targetNote` を必ず付ける)。
  **観測日記を書くのはヨスガ**(レコルではない)。
- **⚠ 朝の経路からレコルを外した(2026-07-26)**: それまで
  「レコルが Morning Brief を書く → 人がヨスガへ貼る」だったが、**Hub の現況JSONが
  Brief の材料をすべて持っていた**(`tasks.completedAt` / `recentChanges` の日付 /
  `calendar` / `blockers.since` / `recentDecisions`)。間に要約を挟むと情報が落ちるだけ。
  「締切が近い」「停滞している」の指摘も材料があればヨスガができるし、
  `シロの状態` は**ヨスガが書いた観測日記をレコル経由で本人へ返していた**。
  → コンソール **`> 朝の準備`** が カレンダー取込 → GitHub取得 → Vault書込 →
  サーバー同期 を走らせ、そのまま `ヨスガへ共有` へ進む。**Morning Brief は廃止**。
  これは v4.3 の「**知識を活かすのはレコルではない**」に戻す変更。
- **レコルに残る仕事は1つだけ**: 記録タブに**手打ちした** `documents` の分類。
  ゲームの知識ノートは**生成元の Claude Code が Frontmatter で分類済み**(v5 方針)なので、
  レコルは触らない。`documents` が空ならレコルを開く用事は無い。
  ロリポップ同期・Actions・`openapi.yaml` は**維持するが必須ではない**(設備は残す)。
- 将来: MCP(v4 Phase4)。積み残しは WORKLOG 再開ポイント参照。
- アシスタント名は「ヨスガ」「レコル」(カタカナ表記)。
  アプリUIの「ヨスガ画面」等の文言は当面そのまま(改名は実機検証後・積み残し)。

注意: まだ着手していない v3-Step は未実装。実装済みのように扱わないこと。

# Build

```
./gradlew assembleDebug             # ビルド
./gradlew testDebugUnitTest         # 単体テスト
./gradlew connectedDebugAndroidTest # マイグレーションテスト(実機/エミュレータが必要)
```

WSL側にJDK/Android SDKがない場合、ビルドはWindows側のAndroid Studioで行う。

# Yosuga Hub Data Provider Rules

このプロジェクトは**自分自身を Yosuga Hub のプロジェクトとして扱う**(2026-07-25)。
各ゲームに課しているのと同じ規約を、Hub 自身にも適用する。
仕様の本体は `docs/claude_code_onboarding.md`。

## Required files
`.yosuga/project.json` / `.yosuga/status.json` / `.yosuga/status.md` /
`.yosuga/schema-version.txt` を維持する。`projectId` は `yosuga-hub`(変更禁止)。

## Update responsibility
意味のある作業単位を完了したとき、**コミット前または作業終了前に**
`.yosuga/status.json` と `.yosuga/status.md` を更新する。
小さな変更ごとに毎回は不要。目安は WORKLOG に1項目書くのと同じ粒度。

## Accuracy rules
- 実装済み・作業中・未実装を明確に区別する(Project Rules の「未実装部分を、
  実装済みのように扱わない」と同じ)。
- 実際のコード・テスト結果・Git履歴に基づいて記録する。推測を事実として書かない。
- 未解決の問題を隠さない。`blockers` / `risks` に残す。
- 判断が必要な事項は `questionsForYosuga` に記録する。

## recentChanges
各件に `date`(`"yyyy-MM-dd"`)を**必ず**書く。Hub は近況報告に**直近2週間分だけ**を
載せるため、日付が無いと期間で絞れず載り続ける。**古い分を消すのは Hub の仕事**なので
こちらは積んでよい。`commit` は可能なら短縮SHA。

## Validation
更新後に JSON の構文と必須フィールドを検証する。失敗した状態でコミットしない。

## Git rules
- 秘密情報・アクセストークン・APIキー・個人情報・ローカル絶対パスを `.yosuga/` に含めない。
- ⚠ **このリポジトリは GitHub へ未 push(公開リポジトリ・空)**。
  push すると内容が公開される。push するかはシロさんの判断。
  push しない限り Hub は自分自身の `status.json` を取得できない。
