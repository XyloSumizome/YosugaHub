# 追加設計指示: AI分類ワークフロー(v4.1)

> **状態: 実装完了・実機未検証(2026-07-23)**。
> 本書は v4(`yosuga_hub_android_design_v4.md`)への追加仕様。
> 論点は合意済み(末尾「確定した論点」)。実装の詳細は `docs/WORKLOG.md` の
> 2026-07-23 v4.1 エントリ3件を参照。**実機・実通信での確認は未実施**。

---

## 指示本文(ユーザーより)

Yosuga Hubに保存・アップロードされたドキュメントは、
最初から正しく分類されているとは限りません。

カテゴリ分け、タグ付け、関連付け、要約などは、
AIであるヨスガが担当します。

そのため、以下のワークフローを実装可能な設計にしてください。

### 基本フロー

1. Yosuga Hubが未整理ドキュメントを保存する
2. 未整理ドキュメントをサーバーへアップロードする
3. AIがドキュメントを読む
4. AIが分類・要約・関連付け案を返す
5. Yosuga Hubが分類案を表示する
6. ユーザーが承認、修正、保留する
7. 承認された内容を確定データとして保存する

### ドキュメント状態

各ドキュメントは、少なくとも以下の状態を持てるようにしてください。

- unclassified
- classification_pending
- classified
- needs_review
- archived

### 保存する情報

元のドキュメント本文と、AIの分類結果は分けて保存してください。

以下の情報を保持できるようにしてください。

- 原文
- AIによる要約
- プロジェクト分類
- カテゴリ
- タグ
- 文書種別
- 関連するタスクや仕様
- AIの信頼度
- ユーザーによる修正内容
- 最終承認結果
- 分類日時
- 分類履歴

**AIの判断によって原文を上書きしないでください。**

### AI分類結果

AIから返される分類結果は、
アプリが機械的に処理できるJSON形式を前提としてください。

例:

```json
{
  "document_id": "doc_001",
  "project_ids": ["fragile-hero"],
  "categories": ["game-design", "player-action"],
  "tags": ["grapple", "frog", "rhythm-action"],
  "document_type": "design-discussion",
  "summary": "グラップル仕様に関する検討",
  "related_entities": [
    {
      "type": "feature",
      "id": "grapple"
    }
  ],
  "confidence": 0.91,
  "requires_user_confirmation": true
}
```

### UI

AIの分類結果を確認する画面を用意できる構造にしてください。

必要な操作:

- 承認
- 修正
- 保留
- 再分類
- 元文表示

初期段階では自動確定せず、
必ずユーザー確認を通す設計を基本としてください。

### 将来拡張

将来的には以下も可能にしたいです。

- 信頼度が高い分類のみ自動承認
- 既存タグのみを使う場合の自動確定
- 新規タグ作成時のみ確認
- 分類ルール変更後の再分類
- 複数ドキュメントの一括分類
- MCPやREST API経由での分類結果受け取り

### 最重要原則

ヨスガが考え、
Yosuga Hubが保存・表示・承認管理する。

この役割分担を崩さないでください。

---
---

# 付録: 現状実装との関係(Claude Code による分析 / 2026-07-23)

## 既存機能との位置づけ

- 既存の `knowledge_items` は**分類済みの短い知識**で、AI提案を承認して入る(v3.1)。
- 本仕様の「ドキュメント」は**未整理の原文を先に保存し、後からAIが分類する**逆向きのフロー。
  → **新しい概念**であり、`knowledge_items` とは別テーブルにするのが素直。
  承認確定後に knowledge_items へ昇格させるかは要検討(論点参照)。

## 再利用できる既存インフラ

- **アップロード**: v4のサーバー同期に `documents.json`(未整理文書)を追加するだけ。
  `AiExporter` に1ファイル足す形で載る。
- **AI→アプリの受け取り**: 回答JSON v2 の `proposals` に分類結果を足せば、
  取込(ImportRepository)→ pending_proposals → レビューUI(承認/棄却)の既存機構に乗る。
- **原則の一致**: 「原文を上書きしない」「必ずユーザー確認」は v3 以来の
  提案→承認→保存フローそのもの。役割分担は既存実装と矛盾しない。

## 状態遷移(案)

```
保存 → unclassified
     → (サーバー同期でアップロード) → classification_pending
     → (分類結果を取込)             → needs_review
     → (承認・修正して確定)          → classified
     → (archived)

保留   : needs_review のまま据え置き
再分類 : classification_pending へ戻す(履歴は残す)
```

## データモデル(案・Room v6)

- `documents`: id / title / **body(原文・不変)** / status / createdAt / updatedAt / source
- `document_classifications`: id / documentId / summary / documentType / confidence /
  projectIds / categories / tags / relatedEntities / classifiedAt /
  **appliedBy(ai|user)** / isCurrent
  - AI結果とユーザー修正を**別レコード**として積む = そのまま「分類履歴」になる
  - 原文テーブルは分類で一切書き換えない
- 承認結果(最終確定)は `documents.status` + 現行分類レコードで表す

## 確定した論点(2026-07-23 合意 / 実装済み)

1. **実装範囲**: データ層 → 文書UI・レビューUI → 同期・取込・プロンプト の順で全部実装した。
2. **文書の作成入口**: 記録タブに「文書」セクションを新設(記録タブは7区分に)。
   他アプリからの共有インテントは**採用せず**(将来拡張として残す)。
3. **分類結果の受け取り方**: 回答JSON v2 の `proposals.classifications[]`。
   ただし **classifications だけは pending_proposals を経由せず、取込時に文書へ直接適用**して
   `needs_review` にする。理由: pending_proposals を通すと「ヨスガ画面で承認 → 文書画面でも承認」と
   **承認が二重**になるため。文書のレビューUIが承認の唯一の入口。
   再利用したのは運搬経路(回答JSON v2 → ResponseImporter → ImportRepository)。
   サーバーの専用受口PHPは採用せず(将来拡張)。
4. **承認確定した文書**: `documents` のまま。`knowledge_items` へは昇格させない
   (長文=文書 / 短い知識=アイテム、と役割を分ける)。
5. **`related_entities` の type**: 文書側は**自由文字列**。既存 EntityType は拡張しない。

## 実装の対応表

| 仕様 | 実装 |
|---|---|
| 文書状態5種 | `DocumentStatus`(未知の値は unclassified へフォールバック) |
| 原文と分類結果を分けて保存 | `documents.body`(不変)/ `document_classifications`(別テーブル) |
| 分類履歴 | AI結果とユーザー修正を別レコードで積む。現行1件だけ `isCurrent` |
| ユーザーによる修正内容 | `appliedBy = user` のレコード(AI結果は履歴に残る) |
| AIの信頼度 | `confidence`(ユーザー修正レコードは null) |
| 原文を上書きしない | Repository・DAO・UI のいずれにも原文を書き換える経路が無い |
| 分類結果のJSON | 回答JSON v2 `proposals.classifications[]`(snake_case) |
| UI(承認/修正/保留/再分類/元文表示) | `DocumentDetailDialog` + `ClassificationEditDialog` |
| 自動確定しない | 取込は `needs_review` まで。確定はユーザーの承認後 |
| ユーザーの決着を尊重 | `classified` / `archived` の文書には取込で分類を適用しない。やり直しは明示的な「再分類」から |

## 未実装(将来拡張 / 指示書「将来拡張」節)

- 信頼度が高い分類のみ自動承認 / 既存タグのみなら自動確定 / 新規タグ作成時のみ確認
- 分類ルール変更後の再分類(現在は文書1件ずつ「再分類」を押す)
- 複数ドキュメントの一括分類(一覧からのまとめ操作)
- MCP や REST API 経由での分類結果受け取り(v4 Phase4)
- 他アプリからの共有インテントでの文書取り込み
