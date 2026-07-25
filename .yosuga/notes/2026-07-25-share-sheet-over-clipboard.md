---
type: decision
project_id: yosuga-hub
game: Yosuga Hub
category: ai-bridge
created_at: 2026-07-25T10:00:00+09:00
updated_at: 2026-07-25T10:00:00+09:00
source: claude-code
tags:
  - Yosuga Hub
  - decision
  - ai-bridge
related_files:
  - app/src/main/AndroidManifest.xml
  - app/src/main/java/com/shiro/yosugahub/SharedText.kt
  - app/src/main/java/com/shiro/yosugahub/data/file/PastedJson.kt
commit:
  hash: b4e2625
  branch: master
---

# 変更概要

レコルの回答JSONを取り込む入口として、**クリップボードの自動読み取りではなく
共有シート(`ACTION_SEND`)を採用した**。ChatGPT から「共有 → Yosuga Hub」で
1タップ取り込みができる。

## 変更理由

「回答をコピーしてアプリを開いて貼る」の手数を減らしたかった。
案は3つ出て、最終的に共有シートを選んだ。

**採らなかった案 A: クリップボードを起動時に自動で読む**

**Android 12(API 31)以降、アプリがクリップボードを読むたびにシステムが
「◯◯ が △△ からコピーしたテキストを貼り付けました」というトーストを出す。
抑制する API は存在しない。** targetSdk 35 なので必ずかかる。
起動のたびに読む設計だと、起動のたびにトーストが出続ける。

さらに「レコルの回答かどうか」を判定するには**まず読む**必要があるため、
パスワードなど無関係な文字列も一度は読むことになる。個人用アプリなので
実害は本人だけだが、設計として気持ちのいいものではない。

**採らなかった案 B: JSONをファイルに保存して起動時に自動読み込み**

SAF で受け取りフォルダを選ばせる実装自体は素直(`SafVaultReader` と同じ作り)。
ただし **ChatGPT アプリから JSON をファイルとして保存するのはコピーより手数が多い**。
スマホ主体の運用では割に合わない。PC 経由の運用なら有効なので、将来必要になれば戻ってよい。

**採った案 C: 共有シート**

- アプリを開く操作すら要らない(共有先として直接出る)
- トーストなし・権限なし・サーバー変更なし
- 「レコルがサーバーへ保存 → アプリが `PULL INBOX`」案と手数がほぼ同じで、
  **サーバー側(`inbox.php`)を作らずに済む**

## 実装内容

- `AndroidManifest.xml` に `ACTION_SEND` / `text/plain` の intent-filter。
  `launchMode="singleTop"` にして、起動済みなら `onNewIntent` で受ける(画面を積まない)。
- `SharedText.kt` — 受け取り条件の判定。`Intent` は素の単体テストで動かないため、
  **素の値を取る `from(action, type, text)` に中身を寄せて**テスト可能にした。
- `ConsoleScreen` に `SHARED` 確認ダイアログ(先頭600文字のプレビュー)。
  共有を選んだ時点で意思表示はあるが、取り込みは後戻りしにくいので一度だけ見せる。
- **中身の判定はダイアログでしない。** レコルの回答かどうかは取り込み側の
  `ResponseImporter` が判定し、違えばエラーになる。UI で二重に判定しない。

## 影響範囲

- 承認フローは変えていない。共有で入るのは**取り込みまで**で、
  そのあとの承認(REVIEW / 文書画面)は従来どおり。「提案→承認→保存」は崩していない。
- `PastedJson` の JSON 抽出を広げる必要が出た(別ノート参照)。

## 未解決事項

- **ChatGPT の共有シートに Yosuga Hub が出るかどうかは実機でしか分からない。**
  出なければこの経路ごと没になる。エミュレータでは `adb` から共有インテントを
  投げて、ダイアログ→取り込み→承認待ちまで通ることを確認済み。
- 共有される本文が「メッセージ全体」か「選択部分」かは ChatGPT アプリの実装次第。

## 次に行うこと

実機の ChatGPT アプリで共有シートに出るか確認する。
通れば `inbox.php`(レコルがサーバーへ直接保存する案)は不要になる見込み。
