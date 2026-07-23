# Yosuga Hub 設計方針 v4.0

**AIプラットフォーム構想版**

> 本書は v3 / v3.1 の上位方針である。基本思想(AI=頭脳 / 提案→承認→保存)は変わらないが、
> v3 の「Android単体・クラウド同期なし」は**本書が明示的に上書きする**(サーバー同期を導入)。
> 末尾の付録に確定事項と現状実装との対応を記す。

---

# コンセプト

Yosuga Hubはタスク管理アプリではない。

**ゲーム制作全体をAIが理解し、制作を支援するためのプラットフォーム**である。

目的は

> **「シロさんは会話するだけで制作が整理される」**

環境を作ることである。

---

# 基本思想

Yosuga Hubには2人の利用者が存在する。

## 人間

画面を見る / 編集する / タスクを確認する / 進捗を見る

## AI(ChatGPT)

知識を読む / 制作状況を理解する / 会話から情報を整理する /
タスクを生成する / 知識を関連付ける / 優先順位を提案する

つまり Yosuga Hub は **人間のためのUI** と **AIのためのデータインターフェース** の両方を持つ。

---

# AI First Design

今後追加するすべての機能は、まず「AIはどう利用するか」を考え、
その後「人間はどう見るか」を設計する。AI利用を第一優先とする。

---

# システム構成

```
             ChatGPT
                 ↑
        AI Interface(API)
                 ↑
         Export Repository
                 ↑
          Domain Repository
          ↑            ↑
       Room        Obsidian
          ↑
      Android UI
```

Roomは保存用。Obsidianは知識用。AI InterfaceはAIへの公開窓口。

---

# Repository構成

Repositoryは用途別に責務を分離する。

## UI Repository(画面表示用)

ProjectRepository / TaskRepository / KnowledgeRepository

## Export Repository(AI向けデータ生成)

ProjectExportRepository / TaskExportRepository / KnowledgeExportRepository /
ScheduleExportRepository / ConversationExportRepository

Export Repositoryは、RoomやObsidianから取得した情報をAIが理解しやすい形式へ変換する。

---

# AI Interface

Export Repositoryから取得した情報は AI Interface を通して公開する。
初期実装ではローカルJSON出力だけでも構わない。
しかし最終的には REST API として公開できる構造にする。

---

# JSONはAI用

JSONは画面表示用ではない。AIが読むために設計する。

```
projects.json
tasks.json
knowledge.json
calendar.json
conversations.json
documents.json   ← v4.1(未整理文書。分類待ちのものだけを原文のまま載せる)
```

巨大なJSON一つにはしない。

---

# REST API

JSONファイルではなく REST API を最終インターフェースとする。

```
GET /api/projects
GET /api/tasks
GET /api/knowledge
GET /api/calendar
GET /api/conversations
GET /api/documents
```

返却形式はJSON。Androidも将来的にはこのAPIを利用できる構造にする。

---

# Roomの位置付け

Roomは永続化DBであり、システムの中心ではない。中心となるのは Domain Repository である。
将来RoomからSQLiteやクラウドDBへ変更しても、Export Repository以下は変更不要とする。

---

# Obsidian

Obsidianは知識の保管庫。

保存対象: 設計思想 / 仕様書 / 調査資料 / アイデア / 制作メモ / Markdown

AIはObsidianの分類を支援する(タグ付け / 関連ノート / リンク候補 / 保存先候補を提案)。

---

# GitHub

GitHubはソースコード管理だけでなく知識共有にも利用する。
Claude Codeが生成する進捗情報 / 作業履歴 / 決定事項なども取り込める設計とする。

---

# 会話中心の制作

ユーザーは会話するだけ。AIは会話から
タスク生成 / 知識整理 / 締切候補 / Obsidian保存先 / タグ / 優先順位 を提案する。

---

# AI Knowledge API

Yosuga Hub最大の特徴はAI専用APIを持つことである。

将来的には ChatGPT が

```
knowledge.search()
tasks.list()
projects.context()
conversation.latest()
```

のような形で Yosuga Hub へアクセスできることを目標とする。
初期段階では REST API として設計しておけば十分。MCP対応は後から追加する。

---

# サーバー同期

```
Android → Export Repository → REST API → ロリポップ → JSON → ChatGPT
```

この流れを基本構成とする。同期方法は将来変更可能。

---

# 将来対応

MCP / GitHub / Obsidian Sync / NAS / Google Drive / Dropbox

保存場所が変わっても AI Interface は変更不要な設計にする。

---

# 開発優先順位

- **Phase1**: Android / Compose / Room / Repository / 画面
- **Phase2**: Export Repository / JSON生成 / REST API / ロリポップ同期
- **Phase3**: AI分類 / Obsidian整理 / GitHub同期 / Claude Code統合
- **Phase4**: MCP / AI Tool / リアルタイム検索 / AIによる知識更新

---

# 開発原則

Yosuga Hubは「Androidアプリ」ではない。「REST API」でもない。「Obsidian管理ツール」でもない。

Yosuga Hubとは

**ゲーム制作に関するあらゆる知識を、人間とAIの両方が利用できる形で管理するAIプラットフォームである。**

---

# 最重要原則

設計に迷った場合は必ず以下を優先する。

> **「AIが理解しやすい構造になっているか」**

画面よりもAI。機能よりもデータ構造。一時的な実装よりも将来の拡張性。これを最優先とする。

---
---

# 付録: 確定事項と現状実装との対応(2026-07-23)

## ユーザーと合意した決定

1. **v3の「クラウド同期なし」を本書が上書き**し、ロリポップへのサーバー同期を導入する。
2. **同期方式: PHP受け口 + トークン認証**。
   - Android から HTTPS POST(ヘッダーにトークン)で JSON 群を `upload.php` へ送信
   - 配信は `api.php?file=xxx&token=xxx`(トークン不一致は 403)
   - 公開するのは**進捗・整理情報**でありゲーム本体ではないため過度に神経質にはならないが、
     トークン認証を最低限の保護とする。トークンは Android Keystore で暗号化保管
     (GitHubトークンと同じ仕組み)。サーバー側の生成物にも秘密情報を含めない
3. **conversations.json は当面「取込・承認履歴」で代用**(会話本文は ChatGPT 側にあり保持しない)。
   MCP 導入時に再検討。

## Phase と現状実装の対応

- **Phase1: 完了**(v3-Step 1〜4 で達成)
- **Phase2: 実装済み**
  - Export Repository の用途別分離(巨大な単一JSONをやめ、projects / tasks / knowledge /
    calendar / conversations の5ファイルへ。**v4.1 で documents を加えて6ファイル**)
  - ローカルJSON出力(AI Interface の初期実装)
  - ロリポップ同期(Ktor で POST。サーバーPHPは `server/` ディレクトリに同梱)
  - ※ 従来の「状況JSON」(手動ブリッジ)は互換のため残す。サーバー同期が使えない場面の
    フォールバックとして機能する
- **Phase3: ほぼ先行達成済み**(提案承認によるAI分類 / Obsidian SAF / GitHub status.json 取得 /
  `.yosuga/` による Claude Code 統合)
- **Phase4: 将来**(REST API を先に設けることで MCP を載せやすくする)
