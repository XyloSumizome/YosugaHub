# 各ゲームの Claude Code への導入指示(Yosuga Hub 連携)

各ゲームプロジェクト(ANRI / 紙装甲主人公と不死身のカエル / げんげきょう)の Claude Code に、
**このファイルの「導入指示」以下をそのまま貼り付けて渡す**。
仕様はこのファイル内で自己完結している(Yosuga Hub 側の設計書を読ませる必要はない)。

- 各プロジェクトの `projectId`(変更禁止): `anri` / `paper-armor-frog` / `gengenkyo`
- Yosuga Hub 側は将来、GitHub 経由で `.yosuga/status.json` を取得して表示する(実装予定)。
  それまでは status.md / status.json を ChatGPT(ヨスガ)への状況共有にも使える。

---

## 導入指示(ここから下を各ゲームの Claude Code へ貼る)

```text
このゲームプロジェクトを、Androidアプリ「Yosuga Hub」から進捗確認できるようにします。

プロジェクトルートに `.yosuga/` ディレクトリを作成し、以下の4ファイルを維持してください。

- .yosuga/project.json
- .yosuga/status.json
- .yosuga/status.md
- .yosuga/schema-version.txt

作業手順:
1. リポジトリと既存ドキュメントを調査する
2. 現在の状態、完了済み、作業中、次タスク、ブロッカーを整理する
3. 下記の共通仕様に従って4ファイルを作成する
4. JSONを機械的に検証する
5. status.json と status.md の内容が矛盾していないか確認する
6. CLAUDE.md へ下記「Yosuga Hub Data Provider Rules」を追加する
7. 変更内容と検証結果を報告する

重要:
- 不明な内容を推測で埋めない。不明点は dataQuality.warnings または
  questionsForYosuga に記録する
- 秘密情報、個人情報、ローカル絶対パスを含めない
- 既存のゲームコードを、この作業のために変更しない

=== 共通仕様 ===

## project.json(頻繁には変わらない情報)

{
  "schemaVersion": 1,
  "projectId": "<このプロジェクトのID>",
  "displayName": "<表示名>",
  "repository": {
    "owner": "<GitHubオーナー名>",
    "name": "<リポジトリ名>",
    "defaultBranch": "main"
  },
  "statusFile": ".yosuga/status.json",
  "humanReadableStatusFile": ".yosuga/status.md",
  "active": true
}

ルール: projectId は一度決めたら変更しない。表示名を変えても projectId は維持。

## status.json(機械可読の進捗データ)

{
  "schemaVersion": 1,
  "projectId": "<project.json と同じID>",
  "generatedAt": "2026-07-23T21:00:00+09:00",
  "sourceCommit": "<対象コミットSHA>",
  "summary": "現在の状況を1〜3文で記述",
  "phase": "prototype",
  "health": "on_track",
  "currentGoal": { "title": "現在の目標", "detail": "目標の具体的な説明" },
  "completed":  [ { "id": "task-001", "title": "完了した作業", "completedAt": "2026-07-22T18:00:00+09:00" } ],
  "inProgress": [ { "id": "task-002", "title": "作業中の項目", "detail": "現在の状態", "progressPercent": 50 } ],
  "nextTasks":  [ { "id": "task-003", "title": "次に行う作業", "detail": "具体的な作業内容",
                    "priority": "high", "estimatedMinutes": 60, "dependencies": [] } ],
  "blockers":   [ { "id": "blocker-001", "title": "進行を妨げている問題", "detail": "何が問題か", "severity": "medium" } ],
  "recentChanges": [ { "date": "2026-07-22", "summary": "変更内容の要約", "commit": "<SHA>" } ],
  "risks": [],
  "decisions": [],
  "questionsForYosuga": [],
  "dataQuality": { "validated": true, "missingFields": [], "warnings": [] }
}

必須条件:
- JSONとして構文が正しい / schemaVersion がある / projectId が project.json と一致
- generatedAt は ISO 8601(タイムゾーン付き)
- summary が空ではない / nextTasks が存在する
- 完了していない問題は blockers または risks に記録する
- 実装済みと未実装を混同しない。実際のコード・テスト結果・Git履歴に基づく
- 更新後にJSON検証を実行し、失敗した状態でコミットしない

推奨:
- 完了タスクには完了日時、次タスクには優先度
- 見積もりが難しければ estimatedMinutes は省略
- 判断が必要な事項は questionsForYosuga へ、重要な設計判断は decisions へ
- 進捗率は根拠がある場合のみ

## status.md(人間可読。status.json と矛盾させない)

# Project Status
## Summary
## Current Goal
## Completed
## In Progress
## Next Tasks
## Blockers
## Recent Changes
## Risks
## Decisions
## Questions for Yosuga
## Last Updated

## schema-version.txt

内容は数字のみ:
1

=== 指示書の受け取り(任意・サーバー同期を使っている場合のみ) ===

Yosuga Hub からこのプロジェクト宛の指示書が届くことがある。
URLとトークンはユーザーから受け取ること(このファイルには書かない)。

セッション開始時、および長い作業の区切りで確認する:

curl -s "https://<ドメイン>/yosuga/api.php?file=directives&token=<トークン>"

- `directives[]` のうち **projectId が自分のものだけ** を読む。他プロジェクト宛は無視する
- 指示書は Markdown の body を持つ。内容が現在のコードと矛盾する場合は
  **勝手に始めず**、questionsForYosuga に疑問を書いて確認を求める
- 対応したら `.yosuga/status.json` の recentChanges に `directiveId` を記録する
  (Yosuga Hub 側で「対応済み」にする判断材料になる)
- 指示書は承認済みのものだけが配信される。届いていない = まだ承認されていない

=== 更新タイミング ===

次のときに .yosuga/ を更新する(小さな変更ごとに毎回は不要。意味のある作業単位で):
1. 一連の作業を完了したとき
2. コミットを作成するとき
3. 現在の目標が変わったとき
4. 新しいブロッカーが発生したとき
5. 次の作業候補が変わったとき
6. ユーザーが作業終了を伝えたとき
7. status.json が24時間以上古い状態で新しい作業を行ったとき

=== CLAUDE.md へ追加する内容 ===

# Yosuga Hub Data Provider Rules

このプロジェクトは、Androidアプリ「Yosuga Hub」へ進捗情報を提供する。

## Required files
- `.yosuga/project.json` / `.yosuga/status.json` / `.yosuga/status.md` / `.yosuga/schema-version.txt` を維持する。

## Update responsibility
意味のある作業単位を完了したとき、コミット前または作業終了前に
`.yosuga/status.json` と `.yosuga/status.md` を更新する。

## Accuracy rules
- 実装済み、作業中、未実装を明確に区別する。
- 実際のコード、テスト結果、Git履歴に基づいて記録する。
- 推測を事実として記載しない。未解決の問題を隠さない。
- 次に行える具体的なタスクを記載する。
- ユーザーまたはヨスガの判断が必要な事項は questionsForYosuga に記録する。

## Validation
更新後はJSONの構文と必須フィールドを検証する。検証に失敗したファイルをコミットしない。

## Directives (Yosuga Hub からの指示書)
サーバー同期が設定されている場合、セッション開始時に directives を確認する。
自分の projectId 宛のものだけを読み、対応したら recentChanges に directiveId を記録する。
指示内容が現在のコードと矛盾する場合は着手せず questionsForYosuga で確認する。

## Git rules
- 秘密情報・アクセストークン・APIキー・個人情報・ローカル絶対パスを `.yosuga/` に含めない。
- sourceCommit は可能なら最新の関連コミットSHAにする。

## Completion report
作業完了時の報告には次を含める:
1. 実装した内容 / 2. テスト結果 / 3. 未解決事項 / 4. `.yosuga/` を更新したか / 5. 次に推奨する作業
```

---

## (参考)Yosuga Hub 側の今後の実装

- GitHub 連携(旧Phase 3): Contents API / Raw URL で各リポジトリの `.yosuga/status.json` を取得 →
  プロジェクト画面と状況JSONへ反映(キャッシュ・更新日時・エラー表示)。
- それまでの間も、status.md を ChatGPT(ヨスガ)との会話に貼れば状況共有に使える。
- `status.json` の `decisions` / `questionsForYosuga` は、将来ヨスガの提案(回答JSON)へ
  つなぎ込む候補。
