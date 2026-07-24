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
+ `yosuga_hub_design_v4_3_role_split.md`(役割分離 ヨスガ/レコル + Morning Brief。**レコルは必須から外れた**)
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
- **仮データ(SampleSeed)問題 解決(選択A)**: `SampleDataRepository` が **ID 指定で**削除し
  (実データを巻き込まない)、DataStore `seeding_disabled` で再シードを止める。
  プロジェクトの個別削除UIも追加。**「消しても復活する」問題は実機で解消を確認済み**。
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
- **次にやること**(詳細は WORKLOG 冒頭の再開ポイント):
  1. **Morning Brief を実データで一周**。手順は `docs/recoru_prompt.md` の
     「Morning Brief の回し方」。**アプリを新しくしてからレコルに聞く**のが要点。
  2. **フォームUIを実機で確認**(特に `[x]` チェックの当たり判定)。
  3. **実機(スマホ)側**: Obsidian + Remotely Save で Dropbox 同期
     (⚠ Vault は**共有ストレージ上**に作ること。アプリ内フォルダだと Hub から読めない)/
     共有シートに ChatGPT が出るか。
- **v4.3 AI役割分離(2026-07-23)**: 実地検証で「データを読めるAI(カスタムGPT+Actions)」と
  「会話を持つAI(通常ChatGPT)」が分かれることが判明し、役割分担として設計に昇格。
  **ヨスガ=会話の相棒(Hubに触れない) / レコル=Hub管理者(Actionsで読み、回答JSONで更新提案)**。
  レコルの「Hub更新」は既存の 取込→承認→自動再同期 経路(直接書込はしない)。
  **知識を活かすのはレコルではない**(人間かヨスガの仕事)。レコルの役目は
  **Obsidian への入力を自動化すること**(2026-07-24 確定 / `items[].targetNote` を必ず付ける)。
  **観測日記を書くのはヨスガ**(レコルではない)。
  新概念 **Morning Brief**(レコルがHubを要約→ヨスガに貼って1日を開始)。アプリ実装変更なし。
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
