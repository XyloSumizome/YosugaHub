---
type: development-log
project_id: yosuga-hub
game: Yosuga Hub
category: database
created_at: 2026-07-25T12:00:00+09:00
updated_at: 2026-07-25T12:00:00+09:00
source: claude-code
tags:
  - Yosuga Hub
  - development-log
  - database
  - incident
related_files:
  - app/src/main/java/com/shiro/yosugahub/data/local/db/Migrations.kt
  - app/schemas/com.shiro.yosugahub.data.local.db.YosugaDatabase/3.json
  - app/src/androidTest/java/com/shiro/yosugahub/MigrationTest.kt
commit:
  hash: 976c689
  branch: master
---

# 変更概要

5か月近く隠れていた**マイグレーションの実バグ**を見つけて直した。
`3.json`(v3 の期待スキーマ)が誤って書き換わっており、
DB が v3 の端末では起動できなくなる状態だった。

## 変更理由

### 見つけ方

エミュレータを立てていたので、長く未検証だった `connectedDebugAndroidTest` を流した。
**5件中1件が失敗**。段ごとの検証(`each_migration_step_matches_exported_schema`)で
`Migration didn't properly handle: projects`。

### 原因

`43f969f`(GitHub連携 3-a / 2026-07-23)で `repoOwner` / `repoName` / `repoBranch` を
足したとき、**`4.json` だけでなく `3.json` も書き換わって commit されていた**
(差分に `3.json | 24 +-` が残っている)。
まだ version が 3 の状態でスキーマが書き出されたため。

結果、こうなっていた。

| | repo 列 |
|---|---|
| `3.json`(v3 の期待形) | **ある** ← 誤り |
| `MIGRATION_2_3` | 追加しない |
| `MIGRATION_3_4` | 追加する |

`v2 → v3` を検証すると、期待に repo 列があるのに `MIGRATION_2_3` は作らないので不一致。

### なぜ気づかなかったか

**`v2 → 最新` の通しは成功する。** `MIGRATION_3_4` が後から列を足すので
最終形は正しくなる。**後段が前段の誤りを埋め合わせて隠していた。**
段ごとの検証を一度も流していなかったため、ずっと通っているように見えていた。

### 実害

**DB が v3 で止まっている端末**が危ない。当時のビルドが作った v3 の DB には
**既に repo 列がある**ので、そこへ `MIGRATION_3_4` の素の
`ALTER TABLE ADD COLUMN` が走ると `duplicate column name` で落ち、
**アプリが起動できなくなる**。

## 実装内容

2つとも必要だった。片方だけでは不十分。

1. **`3.json` を真の形へ戻した** — `22e5b04` 時点のものを復元。
   差分は projects の repo 3列と identityHash だけで、他は同一であることを確認済み。
2. **`MIGRATION_3_4` を冪等にした** — `PRAGMA table_info` で既存列を見て、
   **無い列だけ**足す。repo 列を持つ v3 から来ても壊れない。

1 だけだと「repo 列を持つ v3」で落ちる。
2 だけだと期待値が誤ったままなので段ごとの検証が通らない。

## 影響範囲

`connectedDebugAndroidTest` **5件すべて成功**(Pixel_10 / Android 17)。
単体テスト 362 件も通過。既存データへの影響はない(列の追加のみ)。

## 未解決事項

なし。

## 次に行うこと

**教訓を運用に落とす。**

- スキーマを書き出したら、**変更されたのが新バージョンの JSON だけか確認する。**
  古いバージョンの JSON が変わっていたら、それは事故。
- **通し(v2→最新)だけでは足りない。** 段ごとに検証しないと、
  後段が前段の誤りを埋め合わせて隠す。マイグレーションを追加したら
  `connectedDebugAndroidTest` を流す。
