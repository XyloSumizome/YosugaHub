# Yosuga Hub 設計書 v5 — Obsidian ブリッジ

作成: 2026-07-24
位置づけ: **最上位方針の差し替え**。v4 の「AIプラットフォーム構想」を上書きする。
v4.1(分類) / v4.2(指示書) / v4.3(役割分離) は**維持するが必須ではない**(後述 §9)。

---

## 1. この変更の目的

Yosuga Hub を、AI そのものではなく **「情報を確実に運ぶハブ」** として整理し直す。

シロは Obsidian へ手作業で入力するのが苦手である。
したがって、**普段どおり会話しゲームを作っているだけで、開発情報と会話内容が
自動的に Obsidian へ蓄積される**状態を目指す。

あわせて、Obsidian に溜まった情報のうち **必要な範囲だけを手動でヨスガ(ChatGPT)へ
貼り付けて渡せる**機能を追加する。

### v4 からの思想の変化

| | v4 まで | v5 |
|---|---|---|
| Hub の役割 | AI のデータインターフェース | **情報の運搬役** |
| 知識の正本 | Hub の Room DB | **Obsidian の Markdown** |
| 意味付け(分類)を行う場所 | Hub に届いてからレコルが分類 | **生成元(Claude Code)が出力時点で行う** |
| Hub に AI は必要か | 前提 | **不要。AI 無しで成立させる** |

---

## 2. 全体構成

```
  各ゲームの Claude Code ──(意味付け済み Markdown)──┐
                                                     ↓
  GitHub / ローカルの開発データ ──────────→  Yosuga Hub  ──(書込)──→ Obsidian Vault
                                                     │                    (Dropbox 上)
                                                     │                        │
  ヨスガ(ChatGPT) ←──(手動で貼付)── コンテキスト Markdown ←──(読取・抽出・結合)─┘
        │
        └─(会話まとめ Markdown)──→ Yosuga Hub ──→ Obsidian Vault
```

### Yosuga Hub の担当

- GitHub またはローカルのゲーム開発データを取得する
- 各ゲームから出力された Markdown を受け取る
- Dropbox 上の Obsidian Vault へ Markdown を書き込む
- Vault 内の指定ノート・フォルダから送信対象を抽出する
- ChatGPT へ貼りやすい Markdown / JSON として出力する
- クリップボードへコピーできるようにする

**Hub に AI は載せない。** 判断・要約をせず、**選び、集め、整形し、運ぶ**役割に徹する。

### Obsidian の担当

人間が読むための知識保管庫。仕様・開発ログ・設計変更・会話ログを Markdown で保存し、
後から検索・閲覧・リンクできる形で蓄積する。

### ヨスガ(ChatGPT)の担当

雑談・相談・設計検討。Hub から貼られたコンテキストを読んで会話へ反映する。
必要に応じて会話内容を Obsidian 保存用 Markdown にまとめる。

### Claude Code(各ゲーム)の担当

コードと設計を最も理解している AI として、開発情報を Markdown で出力する。
**出力時点でカテゴリ・ゲーム名・変更内容・関連ファイルの意味付けを済ませる。**
Hub 側が内容を理解して分類し直さなくてよい形にする。

### レコル

**現時点では必須コンポーネントから外す。**
旧来の分類・タグ付け・整理は各 Claude Code の出力時点で行うため。
将来タグ整理・重複統合・長期的な知識の再編集が必要になったら、
「知識の編集者」として再導入する余地を残す(→ Phase 4)。

---

## 3. 今回追加する機能: Obsidian コンテキスト書き出し

Vault 内の情報から、ユーザーが指定した範囲だけを抽出し、ヨスガへ貼れる形にする。

### 必須要件

- **Vault の参照先を設定できる**(読み書き両対応)
- **送信対象の範囲をユーザーが選べる**
  - ゲーム単位 / フォルダ単位 / ノート単位 / タグ単位 / 日付範囲 / 最近更新 / 任意の複数選択
- **選択内容を 1 つの Markdown へ結合する**(初期対応は Markdown、必要なら JSON も)
- **AI による要約は行わない。原文を必要な範囲だけ連結する**
- **ChatGPT へ貼りやすい構造**: ノートのタイトル / 元パス / 更新日時 / タグ / 本文 / 区切り / 先頭に説明
- **ワンタップでクリップボードへコピー**
- **ファイルとして保存・共有できる**(例: `yosuga_context_2026-07-24.md`)
- **送信前にプレビューできる**。文字数またはサイズを表示する
  - トークン数の厳密計算は不要。**文字数表示でよい**

---

## 4. 出力 Markdown の形式

```markdown
---
type: yosuga-context
generated_at: 2026-07-24T08:00:00+09:00
source: obsidian
vault: YosugaVault
selected_scope:
  games:
    - ANRI
  tags:
    - design
    - lighting
file_count: 3
---

# Yosuga Context

以下はObsidianから抽出した、今回の会話に関連する情報です。
内容を前提として、相談または設計の続きを行ってください。

---

## ANRI / Lighting Design

- Source: `Games/ANRI/Design/Lighting.md`
- Updated: `2026-07-23T21:15:00+09:00`
- Tags: `#ANRI #design #lighting`

本文...

---

## ANRI / Recent Development Log

- Source: `Games/ANRI/Logs/2026-07-23.md`
- Updated: `2026-07-23T23:40:00+09:00`
- Tags: `#ANRI #development-log`

本文...
```

---

## 5. Claude Code 側の Markdown 出力仕様

各ゲームの Claude Code は、**情報を作る時点でカテゴリ分けを行う**。

### 推奨 Frontmatter

```yaml
---
type: development-log
game: ANRI
category: lighting
created_at: 2026-07-24T08:00:00+09:00
updated_at: 2026-07-24T08:00:00+09:00
source: claude-code
tags:
  - ANRI
  - development-log
  - lighting
related_files:
  - Assets/Scripts/LightingController.cs
  - Assets/Scenes/Main.unity
commit:
  hash: abc1234
  branch: main
---
```

### 本文の推奨構成

```markdown
# 変更概要
何を変更したか。
## 変更理由
なぜ変更したか。
## 実装内容
具体的な実装内容。
## 影響範囲
影響する機能やファイル。
## 未解決事項
残っている課題。
## 次に行うこと
次の作業候補。
```

---

## 6. Obsidian への保存方針

- **Markdown を人間が読む正本とする。**
- JSON は Hub 内部の状態管理・送受信履歴など、機械処理が必要な部分に限定する。

### フォルダ構成案

```
YosugaVault/
├─ Games/
│  ├─ GameA/
│  │  ├─ Overview/
│  │  ├─ Design/
│  │  ├─ Development Logs/
│  │  ├─ Decisions/
│  │  └─ Commits/
│  ├─ GameB/
│  └─ GameC/
├─ Conversations/
│  ├─ Yosuga/
│  └─ Imported/
├─ Shared/
│  ├─ Ideas/
│  ├─ Decisions/
│  └─ References/
└─ Inbox/
```

分類に失敗したファイル・保存先が決められないファイルは `Inbox/` へ入れる。

**Phase 1 はこの構成に依存しない**(何があっても列挙するだけ)。構成の確定は Phase 3 でよい。

---

## 7. 会話ログの扱い

ヨスガとの会話は、後で Hub 経由で Obsidian へ保存できるようにする。
ただし **ChatGPT アプリ内の会話を Hub が自動取得することは現時点では前提にしない。**

初期運用:

1. ヨスガが会話内容を Markdown にまとめる
2. ユーザーがコピーまたはファイル保存する
3. Hub へ渡す
4. Hub が Vault の適切な場所へ保存する

将来 API 等へ差し替えられるよう、**会話インポート機能は独立モジュールにする**。

---

## 8. 実装フェーズ

### Phase 1(今回の着手範囲)

- Vault へアクセスする
- Markdown ファイルの一覧表示
- フォルダ・ノートの複数選択
- 選択内容を 1 つの Markdown へ結合
- プレビュー / 文字数表示 / クリップボードへコピー / ファイル共有

さらに 3 ステップへ分割する(CLAUDE.md「一度に大規模な変更を行わない」):

| | 内容 | 状態 |
|---|---|---|
| **1-a** | data 層: Vault 読み取り + Frontmatter 解析 + コンテキスト結合(純粋ロジック + テスト) | 着手 |
| **1-b** | UI: 一覧・複数選択・プレビュー・文字数 | 未 |
| **1-c** | コピー / SAF 保存 / 共有 | 未 |

### Phase 2

タグ抽出 / 日付範囲抽出 / 最近更新の抽出 / JSON 出力 / 出力履歴

### Phase 3

GitHub 取得データの Obsidian 自動保存 / Claude Code 出力の自動振り分け /
ヨスガ会話ログの取り込み / 重複検出 / 関連ノート候補の提示

### Phase 4

必要性が明確になった場合のみ AI 要約を追加。レコル再導入の検討。タグ統合・知識再編集。

---

## 9. 既存実装(v4.1〜v4.3)の扱い

**削除しない。** すでに実機で一周しており、動いているものを剥がすのはリスクのみ。

| 機能 | v5 での位置づけ |
|---|---|
| ロリポップ同期 / AI用JSON 7ファイル | 維持。レコル再導入(Phase 4)時に再評価 |
| v4.1 文書分類ワークフロー | 維持。ただし**新規の意味付けは Claude Code 側で行う**のが原則 |
| v4.2 指示書 | 維持 |
| v4.3 役割分離(ヨスガ/レコル) | **ヨスガの定義は維持**。レコルは必須から外れる |
| Morning Brief | 維持。ただし v5 では主役ではない |
| Room DB | **Hub の作業用ストア**へ格下げ。知識の正本は Obsidian |

---

## 10. 設計原則

- **Yosuga Hub は AI でなくても成立する設計にする**
- AI 要約を前提にしない。最初は原文抽出と結合だけでよい
- **情報の意味付けは、可能な限り生成元で行う**
- Hub は複雑な判断を持たない
- 人間が読める正本は Markdown にする
- Obsidian はユーザーが手入力しなくても育つ状態を目指す
- **自動書き込みで既存ノートを壊さない。上書きより追記または新規作成を優先する**
- すべての自動処理に元ファイルと処理日時を残す
- 後から AI や別サービスへ差し替えられる疎結合な構成にする

### 「抽出」と「要約」の境界

```
VaultReader ──→ List<LoadedNote>(原文) ──→ [ NoteTransformer ] ──→ ContextMarkdown
                                              ↑ Phase 1 は恒等変換(何もしない)
                                              ↑ Phase 4 の要約はここへ差し込むだけ
```

`ContextMarkdown` は「渡された本文を整形して連ねる」だけの純粋関数にする。
要約が入っても、変更箇所は `NoteTransformer` の 1 箇所だけで済む。

---

## 11. 未決事項と Phase 1 での割り切り

| 論点 | Phase 1 の扱い | 最終決定 |
|---|---|---|
| Dropbox API 直利用 か 端末同期フォルダか | **どちらにも依存しない**。SAF で選んだ任意のツリーを読む | ⚠ 実機確認待ち(§12) |
| Vault の正式なフォルダ構成 | 依存しない | Phase 3 |
| 各ゲームの正式名称と識別子 | 依存しない | 未定 |
| Claude Code のファイル名ルール | 依存しない | Phase 3 |
| 同名ファイルの処理 | **Vault は読み取り専用**。出力名に日時を入れて衝突させない | Phase 3 |
| 自動追記と新規作成の使い分け | Phase 1 では書き込まない | Phase 3 |
| ChatGPT へ送る推奨最大文字数 | 制限せず、**約 60,000 字で警告表示のみ** | 運用で調整 |
| Wiki Link / 埋め込みの展開 | **展開せずそのまま残す**(再帰読込は範囲外) | Phase 2 以降 |
| 画像・PDF を抽出対象に含めるか | **含めない。`.md` のみ** | Phase 3 |
| Frontmatter が無い既存ノート | 全文を本文として扱い、タグは本文中の `#tag` から拾う。エラーにしない | 確定 |

---

## 12. ⚠ 実装前に確認が必要な前提: Dropbox × Android

**Dropbox の Android アプリは、PC と違ってローカル同期フォルダを作らない。**
ファイルの実体は基本クラウド上にあり、Android から通常のフォルダとして見えるとは限らない。

### 確認手順(実機で 5 分)

> 設定 → Obsidian Vault → 「Vaultフォルダを選択」 →
> フォルダ選択画面に **Dropbox が候補として出るか**、
> 出るなら **Vault フォルダまで潜って選べるか**

| 結果 | 対応 |
|---|---|
| 選べる | 現状の SAF のまま進む。読み取りが遅い可能性があるため一覧キャッシュを最初から入れる |
| 選べない | **端末ローカルに Vault を置き、同期アプリ(Dropsync / FolderSync 等)で Dropbox と同期**する。**Hub 側の実装は変わらない** |

どちらでも Hub の設計は同一。**Phase 1 の実装はこの確認を待たずに進められる。**

---

## 13. 実装マッピング(既存構造との対応)

### 再利用する既存資産

| 必要なもの | 既存 |
|---|---|
| Vault フォルダ選択 | `SettingsScreen.kt` の `OpenDocumentTree` + 永続権限 |
| Vault URI の永続化 | `UserPreferencesRepository.obsidianVaultUri` |
| Vault への書き込み | `ObsidianVaultStore`(Vault 直下のみ。サブフォルダは Phase 3 で拡張) |
| DocumentFile 依存 | 導入済み |
| クリップボード | Compose `LocalClipboardManager`(**新規ライブラリ不要**) |

### 新規追加(Phase 1)

```
data/obsidian/
  VaultNote.kt          ノートのメタ情報(相対パス / 名前 / uri / 更新日時 / サイズ)
  VaultReader.kt        interface: 一覧 / 本文読み取り(差し替え可能)
  SafVaultReader.kt     SAF 実装。DocumentsContract で再帰列挙
  Frontmatter.kt        純粋パーサ: --- ブロック分離 + tags/game/type/updated_at 抽出
  ContextMarkdown.kt    純粋: 選択ノート群 → コンテキスト Markdown(§4 の形式)
  VaultRepository.kt    一覧キャッシュ + 明示リフレッシュ + 結合の実行

ui/screen/obsidiancontext/
  ObsidianContextScreen.kt / ObsidianContextViewModel.kt
```

### 既存への変更(2 箇所のみ)

- `YosugaHubApp.kt` … NavHost に `obsidian_context` ルートを追加
- `AssistantScreen.kt` … 「Obsidianから文脈を作る」ボタンを追加して遷移

**下部ナビは 6 個で埋まっているため 7 個目は足さない。**
出力先がヨスガである以上、ヨスガ画面から入るのが自然。

### 影響しないもの

- Room スキーマ(**v8 マイグレーション不要**)
- 新規ライブラリ(**追加なし**)
- 既存の同期 / 分類 / 指示書の各経路
