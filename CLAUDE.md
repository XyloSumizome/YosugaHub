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
設計書: `yosuga_hub_android_design_v4.md`(必読 / 最上位方針: AIプラットフォーム構想)
+ `yosuga_hub_design_v4_1_classification.md`(追加指示: AI分類ワークフロー / **実装完了・実機未検証**)
+ `yosuga_hub_android_design_v3.md` / `_v3_1.md`(基本思想・知識ベース仕様として有効)。
`yosuga_hub_android_design_v2.md` は技術資料として引き続き有効(アーキテクチャ・ライブラリ候補)。
v4の核心: Hubは**人間のUIとAIのデータインターフェースの両方**を持つプラットフォーム。
「AIが理解しやすい構造か」を最優先。v3の「クラウド同期なし」はv4が上書き(ロリポップ同期導入)。

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
- 運用ドキュメント: `docs/yosuga_prompt.md`(ChatGPT=ヨスガ用)、`docs/claude_code_onboarding.md`(各ゲームのClaude Code用)。
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
- **次にやること**: **実環境検証**(v4.1の通し確認・ロリポップ設置・GitHub実通信・実機。
  手順は WORKLOG 冒頭の再開ポイント)。実装側の積み残しは WORKLOG 参照。
- 将来: MCP(v4 Phase4)。積み残しは WORKLOG 再開ポイント参照。
- アシスタント名は「ヨスガ」(カタカナ表記)。

注意: まだ着手していない v3-Step は未実装。実装済みのように扱わないこと。

# Build

```
./gradlew assembleDebug   # ビルド
./gradlew testDebugUnitTest   # 単体テスト
```

WSL側にJDK/Android SDKがない場合、ビルドはWindows側のAndroid Studioで行う。
