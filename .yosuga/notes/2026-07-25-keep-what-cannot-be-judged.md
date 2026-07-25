---
type: decision
project_id: yosuga-hub
game: Yosuga Hub
category: data-export
created_at: 2026-07-25T10:30:00+09:00
updated_at: 2026-07-25T10:30:00+09:00
source: claude-code
tags:
  - Yosuga Hub
  - decision
  - data-export
related_files:
  - app/src/main/java/com/shiro/yosugahub/data/file/ContextExporter.kt
  - app/src/main/java/com/shiro/yosugahub/data/file/model/ContextExport.kt
commit:
  hash: 10e85c4
  branch: master
---

# 変更概要

近況報告用のJSONを「期間で絞る」実装を入れるにあたり、
**判断できないものは捨てず残す**という方針を採った。

## 変更理由

近況報告に「最新の状態」しか載っておらず、**何がどう変わったか**が分からなかった。
そこで各ゲームの `status.json` の `recentChanges`(修正のログ)を
**直近2週間分**、カレンダーを**前後2週間**載せるようにした。

ここで「2週間で絞る」を実装すると、必ず**判断がつかない入力**が出てくる。

- `recentChanges` の `date` が空(ゲーム側が書き忘れた)
- `generatedAt` が壊れていて今日の日付が求まらない

素直に実装すると、どちらも「条件を満たさないので落とす」になる。
**だがこれは危険**だと判断した。落とした結果は
**「その期間に変更が無かった」と読める**からで、
読み手(レコル → ヨスガ → 人)は減っていることに気づけない。

**静かに減っているより、多くて正直なほうがいい。**
多すぎるのは読めば分かるが、無いものは気づけない。

## 実装内容

- `StatusChangeLine` を新設。`StatusLine`(title/detail)と分けたのは
  **日付で絞れるようにする**ため。文字列に畳むと期間で切れなくなる。
- `ProjectExport.recentChanges` は `date` / `summary` / `commit` を**畳まずに**持つ。
  AI が自分で期間を絞れるようにするため。
- `ContextExporter.changesCutoff(generatedAt)` が14日前の日付を返す。
  **解釈できない文字列なら null を返し、絞らない。**
- `recentOnly(since)` は **`date` が空の行を残す**。

対応として、各ゲームへの指示書(`docs/claude_code_onboarding.md`)に
「`recentChanges` の各件に `date` を必ず書く」を追加した。
併せて「**古い分を消すのは Hub の仕事なので、あなたは積んでよい**」も明記した。
消す責任を片側に寄せておかないと、両方が遠慮して情報が落ちる。

## 影響範囲

- Room のスキーマ変更なし。`ProjectStatusCacheEntity` は
  **status.json の原文をそのまま保持していた**ため、`recentChanges` は最初から DB にあった。
  捨てていたのは `ProjectStatus → ProjectStatusSnapshot` の変換1箇所だけだった。
- カレンダーの読み取り窓を ±7日 → ±14日(`DeviceCalendarDataSource` の既定値)。
  **再同期しないと反映されない**点に注意。

## 未解決事項

- **2週間の絞り込みが実データで発動していない。** 検証時点でキャッシュ内の
  `recentChanges` は ANRI が5件(最古 2026-07-12)、Kamieru が6件(最古 2026-07-18)で、
  全部が2週間以内だった。「載ること」は確認できたが「古いものが落ちること」は
  単体テストでしか確認できていない。

## 次に行うこと

Morning Brief を実データで一周し、「昨日の成果」に
`completedAt`(Hub のタスク)と `recentChanges`(ゲーム側の修正)が
**別の成果として**書かれるか確認する。
