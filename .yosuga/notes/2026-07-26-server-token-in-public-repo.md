---
type: decision
project_id: yosuga-hub
game: Yosuga Hub
category: security
created_at: 2026-07-26T09:40:00+09:00
updated_at: 2026-07-26T09:40:00+09:00
source: claude-code
tags:
  - Yosuga Hub
  - decision
  - security
  - incident
related_files:
  - server/config.php
  - server/config.example.php
  - server/README-server.md
  - .gitignore
commit:
  hash: b8ceaab
  branch: master
---

# 変更概要

**ロリポップの認証トークンを平文で Git に追跡しており、public リポジトリで公開していた。**
`server/config.php` を追跡から外し、ひな形 `server/config.example.php` を代わりに置いた。
公開されたトークンは**作り直した**。

## 変更理由

### 見つけ方

未 push の16コミットを push しようとして、その直前に差分を秘密情報で走査したところ
`server/config.php` の `$YOSUGA_TOKEN` が引っかかった。
**push を止めて origin の中身を確認したら、古い値が既に公開されていた。**

| | 値 | 状態 |
|---|---|---|
| `origin/master`(公開中) | `b0c14ea1…` | **誰でも読める状態だった** |
| ローカル(未 push) | `eaf87eb6…` | push すれば公開されるところだった |

### 何が漏れるか

このトークンは `api.php` / `upload.php` の認証。破られると
projects / tasks / knowledge / calendar / documents / directives の JSON を
**読まれるだけでなく上書きもされる**。

### なぜ即座の被害に至らなかったか

**ロリポップのドメインがリポジトリのどこにも書かれていなかった。**
`README-server.md` は `https://あなたのドメイン/yosuga`、
`openapi.yaml` は `YOUR-DOMAIN.example.com` のままだった。
トークンを手に入れても**投げる先が分からない**。

これは設計の手柄ではなく、**ドキュメントを一般化して書いていたことのたまたまの副産物**。
次も同じ幸運があるとは限らない。

## 実装内容

1. `server/config.example.php` を新設(`$YOSUGA_TOKEN = 'CHANGE_ME'`)。
2. `.gitignore` に `/server/config.php` を追加。
3. `git rm --cached server/config.php`。**ローカルのファイルは消していない**(設置に要るため)。
4. `README-server.md` の手順を「ひな形からコピーして作る」に変更し、
   **public リポジトリなので本物を書くなという警告**を入れた。

## 影響範囲

アプリのコードは変更していない。サーバー設置手順のみ。
`server/data/` は `.htaccess` しか追跡されておらず、**同期した JSON は公開されていなかった**。
`ghp_` の検出はすべてテストの固定値で、本物の GitHub トークンではない。
アプリ側の GitHub トークンは Keystore + DataStore に暗号化保存されており、Git には載らない。

## 未解決事項

**古い値 `b0c14ea1…` は origin の履歴に残っている。**
履歴の書き換え(filter-repo 等)は行わなかった。理由:

- **公開されたものは取り消せない。** フォーク・GitHub の API キャッシュ・
  クローン済みの手元から読めるので、履歴を消しても「読まれていない」保証にはならない。
- トークンを別の値に替えれば、古い値は**ただの無意味な64文字**になる。
- public リポジトリの履歴改変は手間が大きく、それでも穴が残る。

つまり **修復はローテーションであって、履歴の削除ではない。**

## 次に行うこと

**秘密が入りうるファイルを「追跡しない」を既定にする。**

- 設定ファイルは **ひな形を追跡し、実体は `.gitignore`** にする。
  `config.php` は最初からこうしておくべきだった。
- **push の前に差分を秘密情報で走査する。** 今回はそれで止まった。
  リポジトリが public なら、push は取り消せない操作として扱う。
- ⚠ **手元の `server/config.php` は古い値のまま残っている**(追跡外なので実害はない)。
  これをそのまま FTP で上げ直すと**サーバーが 403 になる**。
  上げ直す前に今の値へ直すこと。
