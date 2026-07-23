# Yosuga Hub

個人ゲーム開発のための制作管理アプリ(Android)。

**AI=頭脳 / Yosuga Hub=記憶・表示装置**。アプリは考えず、AIが整理した結果を
保存・表示・承認管理する。提案→ユーザーが承認→保存(自動更新しない・安全優先)。

## 構成(v4.3)

- **ヨスガ**(通常のChatGPT): 会話の相棒。Hubに触れない
- **レコル**(カスタムGPT + Actions): Hub管理者。状況を読み、回答JSONで更新を提案
- **Yosuga Hub**(このアプリ): 唯一の記憶。承認と配信を管理
- **ロリポップ**(`server/`): AI向けJSONの配信口(7ファイル)
- **各ゲームの Claude Code**: `.yosuga/status.json` で進捗を提供し、指示書を受け取る

## 設計書(読む順)

1. [`yosuga_hub_android_design_v4.md`](yosuga_hub_android_design_v4.md) — 最上位方針(AIプラットフォーム構想)
2. [`yosuga_hub_design_v4_3_role_split.md`](yosuga_hub_design_v4_3_role_split.md) — 最新の運用(ヨスガ/レコル・Morning Brief)
3. [`yosuga_hub_design_v4_1_classification.md`](yosuga_hub_design_v4_1_classification.md) — AI分類ワークフロー
4. [`yosuga_hub_android_design_v3.md`](yosuga_hub_android_design_v3.md) / `_v3_1.md` — 基本思想・知識ベース
5. [`yosuga_hub_android_design_v2.md`](yosuga_hub_android_design_v2.md) — 技術資料

運用プロンプト: [`docs/yosuga_prompt.md`](docs/yosuga_prompt.md)(ヨスガ)/
[`docs/recoru_prompt.md`](docs/recoru_prompt.md)(レコル)/
[`docs/claude_code_onboarding.md`](docs/claude_code_onboarding.md)(各ゲーム)

## 技術構成

- Kotlin / Jetpack Compose / Material 3 / Gradle Kotlin DSL
- Room(v7)/ DataStore / kotlinx.serialization / Ktor / Android Keystore
- minSdk 26 / targetSdk 35

## ビルド

```
./gradlew assembleDebug             # ビルド
./gradlew testDebugUnitTest         # 単体テスト
./gradlew connectedDebugAndroidTest # マイグレーションテスト(実機/エミュレータ)
```

## 実装状況

- [x] v3: タスク / 知識ベース / 提案承認フロー / Obsidian連携(SAF)
- [x] GitHub連携(`.yosuga/status.json` 取得・Keystoreトークン)
- [x] カレンダー(CalendarContract 読み取り)
- [x] v4 Phase2: AI用JSON分割 + ロリポップ同期
- [x] v4.1: AI分類ワークフロー(文書・分類履歴・レビューUI)
- [x] v4.2: Claude Code への指示書 + 取り込み後の自動同期
- [x] v4.3: ヨスガ/レコル役割分離 + Morning Brief(運用)
- [ ] 実機での通し検証(進行中)
- [ ] MCP対応(v4 Phase4・将来)

作業記録: [`docs/WORKLOG.md`](docs/WORKLOG.md)(冒頭に再開ポイント)
