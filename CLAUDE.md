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
設計書: `yosuga_hub_android_design_v3.md`(必読 / 最上位方針)+ `yosuga_hub_android_design_v3_1.md`(追加仕様: 知識ベース・タグ・観察日記)。
`yosuga_hub_android_design_v2.md` は技術資料として引き続き有効(アーキテクチャ・ライブラリ候補)。

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
- 次: 実機確認 → Step 3 Obsidian連携(SAF・長文の書き出し先)や ホーム再編(AI秘書視点)をユーザーと相談。
- 旧「Phase 3 GitHub進捗取得」は後ろへ延期(v3では将来構想の Knowledge Repository 側へ再配置)。
- アシスタント名は「ヨスガ」(カタカナ表記)。

注意: まだ着手していない v3-Step は未実装。実装済みのように扱わないこと。

# Build

```
./gradlew assembleDebug   # ビルド
./gradlew testDebugUnitTest   # 単体テスト
```

WSL側にJDK/Android SDKがない場合、ビルドはWindows側のAndroid Studioで行う。
