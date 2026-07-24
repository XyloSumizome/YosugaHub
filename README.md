# Yosuga Hub

個人ゲーム開発のための制作管理アプリ(Android)。

**v5(2026-07-24)で最上位方針を転換**: Yosuga Hub は AI ではなく
**「情報を確実に運ぶハブ」**。判断・要約をせず、**選び・集め・整形し・運ぶ**役割に徹する。
知識の正本は **Room DB ではなく Obsidian の Markdown**。
情報の意味付け(分類)は**生成元(各ゲームの Claude Code)が出力時点で行い**、Hub は分類し直さない。

## 何をするアプリか

- **Obsidian へ流し込む**: 各ゲームの `.yosuga/notes/` の知識ノートを GitHub 経由で取得し、
  Frontmatter の `type` に従って Vault へ振り分けて保存する(上書きせず新規作成)。
- **Obsidian から取り出す**: Vault の必要な範囲だけを選び、ヨスガ(ChatGPT)へ貼る
  コンテキスト Markdown を作る(要約せず原文を連結。コピー / 保存 / 共有)。
- **会話を残す**: ヨスガのセッションまとめを貼ると `Conversations/Yosuga/` へ保存する。
- **進捗を参照する**: 各ゲームの `.yosuga/status.json` を「GitHubから更新」で取得し、
  プロジェクト詳細の「GitHub 進捗」に表示する(タスクの正は各ゲーム側。Hub は参照)。

## 登場人物

- **各ゲームの Claude Code**: 開発ノート(Frontmatter付き Markdown)と `status.json` を書き、push する
- **Yosuga Hub**(このアプリ): 取得・振り分け・保存・抽出を担う運搬役。AI は載せない
- **Obsidian**(Dropbox 上の Vault): 人間が読む知識の正本
- **ヨスガ**(通常の ChatGPT): 会話の相棒。Hub から貼られた文脈で相談に乗る
- **レコル**(カスタムGPT + Actions): Hub 管理者。**v5 では必須ではない**(将来「知識の編集者」として再登板の余地)

## 知識ノートの流れ(v5)

```
各ゲームのリポジトリ
  .yosuga/notes/2026-07-24-lighting.md   ← Claude Code が Frontmatter 付きで書いて push
        ↓  Hub「ノートを取り込む」で GitHub から取得
  Obsidian Vault / Games/<ゲーム>/Development Logs/...   ← type で振り分け
        ↓  Dropbox 同期(Obsidian の Remotely Save プラグイン)
  PC の Obsidian / スマホの Yosuga Hub から読める
```

保存先は Frontmatter の `type` で決まる: `design`→Design / `development-log`→Development Logs /
`decision`→Decisions / `reference`→Overview / 不明→Inbox。判定キーは `project_id`。

## 設計書(読む順)

1. [`yosuga_hub_design_v5_obsidian_bridge.md`](yosuga_hub_design_v5_obsidian_bridge.md) — **最上位方針(Obsidian ブリッジ)**
2. [`yosuga_hub_android_design_v4.md`](yosuga_hub_android_design_v4.md) — v5 が上書き。技術資料として有効
3. [`yosuga_hub_design_v4_3_role_split.md`](yosuga_hub_design_v4_3_role_split.md) — 役割分離(ヨスガ/レコル)・Morning Brief
4. [`yosuga_hub_design_v4_1_classification.md`](yosuga_hub_design_v4_1_classification.md) — AI分類ワークフロー(維持)
5. [`yosuga_hub_android_design_v3.md`](yosuga_hub_android_design_v3.md) / `_v3_1.md` — 基本思想・知識ベース
6. [`yosuga_hub_android_design_v2.md`](yosuga_hub_android_design_v2.md) — 技術資料

運用プロンプト: [`docs/yosuga_prompt.md`](docs/yosuga_prompt.md)(ヨスガ)/
[`docs/recoru_prompt.md`](docs/recoru_prompt.md)(レコル)/
[`docs/claude_code_onboarding.md`](docs/claude_code_onboarding.md)(各ゲーム。**知識ノートの出力仕様を含む**)

## 技術構成

- Kotlin / Jetpack Compose / Material 3 / Gradle Kotlin DSL
- Room(v8)/ DataStore / kotlinx.serialization / Ktor / Android Keystore
- アーキテクチャ: UI / ViewModel / Repository / Data Source の分離
- minSdk 26 / targetSdk 35 / パッケージ `com.shiro.yosugahub`

## ビルド

```
./gradlew assembleDebug             # ビルド
./gradlew testDebugUnitTest         # 単体テスト
./gradlew connectedDebugAndroidTest # マイグレーションテスト(実機/エミュレータ)
```

WSL側にJDK/Android SDKがない場合、ビルドはWindows側のAndroid Studioで行う。

## 実装状況

- [x] v3: タスク / 知識ベース / 提案承認フロー / Obsidian連携(SAF)
- [x] GitHub連携(`.yosuga/status.json` 取得・Keystoreトークン)
- [x] カレンダー(CalendarContract 読み取り)
- [x] v4 Phase2: AI用JSON分割 + ロリポップ同期
- [x] v4.1: AI分類ワークフロー / v4.2: 指示書 / v4.3: 役割分離 + Morning Brief
- [x] **v5 Phase 1**: Obsidian からコンテキスト抽出(選択・プレビュー・コピー/保存/共有)
- [x] **v5 Phase 2**: 絞り込み(検索・最近更新・タグ)/ JSON出力 / 出力履歴
- [x] **v5 Phase 3**: GitHub の `.yosuga/notes/` を取得 → type で振り分け → Vault へ書込 → 会話ログ保存
- [x] 仮データ(SampleSeed)の削除機能 + 再シード防止 / プロジェクト削除UI
- [x] プロジェクトの「作業中 / 次」をタスクから導出
- [ ] Dropbox 同期構成(Obsidian + Remotely Save)と実機通し検証
- [ ] MCP対応(将来)

作業記録: [`docs/WORKLOG.md`](docs/WORKLOG.md)(冒頭に再開ポイント)
