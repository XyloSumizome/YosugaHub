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
設計書: `yosuga_hub_android_design_v3.md`(必読 / 最上位方針)。
`yosuga_hub_android_design_v2.md` は技術資料として引き続き有効(アーキテクチャ・ライブラリ候補)。

v3の核心: **AI=頭脳 / Yosuga Hub=記憶・表示装置**。アプリは考えず、AIが整理した結果を保存・表示・編集・検索する。
知識はObsidian(端末内Vault)、制作管理はHub。提案→ユーザーがOK→保存(自動更新しない・安全優先)。Android単体でクラウド同期なし。

- 技術構成: Kotlin + Jetpack Compose + Material 3 + Gradle Kotlin DSL
- アーキテクチャ: UI / ViewModel / Repository / Data Source の分離
- パッケージ: `com.shiro.yosugahub`
- minSdk 26 / targetSdk 35

# Current Phase

設計を v3(AIファースト)へ転換(2026-07-23)。旧 Phase 0〜2 の完了物は v3 の「土台」として再利用する。
- 土台(実装済み): 5画面の骨組み + Room + DataStore + ViewModel/Repository分離 + 状況JSONエクスポート/回答JSONインポート。
- 確定した方針: AIブリッジは当面「手動JSONブリッジ」を継続 / Obsidian連携は SAF で Vault フォルダ選択 / 提案→承認→保存フロー。

次の実装(v3ロードマップ / 詳細は設計書v3の付録・WORKLOG):
- **v3-Step 1: Task を第一級エンティティ化**(TaskEntity/DAO/Repository + タスク/プロジェクト編集の最小UI)。
- 以降: Step 2 提案→承認フロー / Step 3 Obsidian連携(SAF) / Step 4 統合表示。
- 旧「Phase 3 GitHub進捗取得」は後ろへ延期(v3では将来構想の Knowledge Repository 側へ再配置)。

注意: まだ着手していない v3-Step は未実装。実装済みのように扱わないこと。

# Build

```
./gradlew assembleDebug   # ビルド
./gradlew testDebugUnitTest   # 単体テスト
```

WSL側にJDK/Android SDKがない場合、ビルドはWindows側のAndroid Studioで行う。
