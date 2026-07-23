# Yosuga Hub サーバー設置手順(ロリポップ)

Androidアプリが生成する AI 用 JSON(projects / tasks / knowledge / calendar / conversations)を
受け取り、ChatGPT(ヨスガ)へ配信するための PHP 一式。

## 構成

```
server/
  config.php    … トークン設定(必ず編集する)
  upload.php    … アプリからの受け口(POST)
  api.php       … ChatGPT向けの配信口(GET)
  data/
    .htaccess   … data/ への直接アクセス禁止
```

## 設置手順

1. **トークンを作る**: アプリの 設定 → サーバー同期 → 「トークン生成」。
   生成された値をこの後2箇所(アプリとconfig.php)で使う。
2. `config.php` の `$YOSUGA_TOKEN` を生成したトークンに書き換える。
   ※ 初期値(CHANGE_ME...)のままだと upload.php / api.php は全リクエストを 403 で拒否する。
3. ロリポップの FTP(またはロリポップFTP画面)で、`server/` の中身を任意のディレクトリへアップロード。
   例: `/yosuga/` → URL は `https://あなたのドメイン/yosuga`
4. アプリの 設定 → サーバー同期 で
   - 同期先URL: `https://あなたのドメイン/yosuga`
   - トークン: 手順1の値
   を保存 → 「今すぐ同期」。「5 ファイルを同期しました」と出れば成功。
5. 動作確認(ブラウザまたはcurl):
   - `https://あなたのドメイン/yosuga/api.php?file=index&token=トークン` → ファイル一覧
   - `https://あなたのドメイン/yosuga/api.php?file=tasks&token=トークン` → tasks.json

## ChatGPT(ヨスガ)への渡し方

会話で次のように伝える(docs/yosuga_prompt.md にも記載):

```
最新の状況はここから読めます。
一覧: https://あなたのドメイン/yosuga/api.php?file=index&token=トークン
各ファイル: file=projects / tasks / knowledge / calendar / conversations
```

## セキュリティのメモ

- 配信はトークン必須(不一致は 403)。`data/` への直接アクセスは .htaccess で拒否。
- 置いているのは制作の進捗・整理情報のみ(ゲーム本体・秘密情報は置かない)。
- トークンを変えたくなったら: アプリで再生成 → config.php を更新 → アプリ側も保存し直す。
- HTTPS のURLを使うこと(ロリポップの無料SSLで可)。
