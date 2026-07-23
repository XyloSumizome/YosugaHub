# Yosuga Hub サーバー設置手順(ロリポップ)

Androidアプリが生成する AI 用 JSON(projects / tasks / knowledge / calendar / conversations)を
受け取り、ChatGPT(ヨスガ)へ配信するための PHP 一式。

## 構成

```
server/
  config.php    … トークン設定(必ず編集する)
  upload.php    … アプリからの受け口(POST)
  api.php       … ChatGPT向けの配信口(GET)
  openapi.yaml  … カスタムGPTの Actions 用の定義(サーバーへは置かない。GPTに貼る)
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

### ⚠ 通常のチャットにURLを貼る方法は使えないことがある(2026-07-23 実地確認)

ChatGPT のブラウズは**検索インデックス経由でページを取ってくる**ことがあり、
その状態では `api.php?file=...&token=...` のような**認証付き・非公開のURLには到達できない**
(検索に載っていないため)。実際に試して届かなかった。
DNS・HTTPS・robots.txt はいずれも正常で、**サーバー側の問題ではない**。

対処は下の A / B のどちらか。

### A. カスタムGPTの Actions として登録する(推奨・ChatGPT Plus が必要)

Actions は検索ではなく**本物のHTTPリクエスト**を送るので確実に届く。
さらに**トークンをURLではなくヘッダーで渡せる**ため、URL経由より安全
(会話・履歴・アクセスログにトークンが残らない)。

1. ChatGPT で「GPTを作成する」→ 設定 → **アクションを作成**
2. `server/openapi.yaml` を開き、`servers.url` を自分のドメインへ書き換えて全文を貼る
   (例: `https://あなたのドメイン/yosuga`)
3. 認証 → **APIキー** → 認証タイプ **カスタム**
   - カスタムヘッダー名: `X-Yosuga-Token`
   - APIキー: 同期トークン(config.php と同じ値)
4. GPTの指示(Instructions)に `docs/recoru_prompt.md` のシステムプロンプトを貼る
   (v4.3: このGPTはHub管理担当「レコル」。会話の相棒ヨスガは通常のChatGPT側)
5. 「Morning Brief を作って」「documents を読んで分類して」と頼めば Actions 経由で取得される

※ api.php は最初からヘッダー認証に対応しているのでサーバー側の変更は不要。

**openapi.yaml を編集するときの注意**(GPT Actions のバリデータは OpenAPI より厳しい):

- `openapi:` は **3.1.0 か 3.1.1 のみ**(3.0.x は弾かれる)
- `description` は **300字以内**(超えると登録できない。詳しい説明は GPT の Instructions 側へ書く)
- `components.schemas` が**必須**(空にできない)
- `type: object` のスキーマには **`properties` が必要**(`{}` だけでは弾かれる)

書き換えたら、貼る前に確認するとよい(ドメイン以外は基本いじらなくてよい)。

#### Actions が `forbidden` を返すときの切り分け

`forbidden` は **api.php が返している**ので、通信自体は成功している(= Actions は正しく動いている)。
トークン照合だけが失敗している状態。次の順で確認する。

1. GPTの編集画面 → アクション → **「認証」が「なし」になっていないか**
   (スキーマだけ貼って認証未設定だと必ずこうなる)
2. 認証タイプが **APIキー → カスタム** になっているか(Basic / Bearer ではない)
3. カスタムヘッダー名が `X-Yosuga-Token` か
4. APIキーの値に**前後の空白や改行が混入していないか**(コピペ時によくある)

それでも直らなければ、**ヘッダーがサーバーまで届いているか**を確認する:

```
https://あなたのドメイン/yosuga/api.php?file=selftest
```

これはトークン不要で、設定状態だけを返す(**トークンの値そのものは返さない**):

- `tokenConfigured`: config.php を書き換えたか
- `headerReceived` / `headerLength`: `X-Yosuga-Token` ヘッダーが届いたか
- `queryReceived`: `?token=` が付いていたか
- `matches`: 受け取ったトークンが config.php と一致するか

ブラウザで開くと `headerReceived: false` になる(ブラウザはヘッダーを送らないので正常)。
**GPT に `selftest` を開かせて `headerReceived` が false なら**、サーバーの構成が
カスタムヘッダーを PHP へ渡していないということ。その場合は下の B(手動)へ切り替える。

### B. 手で渡す(Plus が無い場合・とりあえず動かしたい場合)

ブラウザで下のURLを開き、**表示されたJSONをコピーして会話に貼る**。
api.php は `Content-Type: application/json` を返すので、ブラウザには
ダウンロードではなくそのまま表示される。

```
https://あなたのドメイン/yosuga/api.php?file=documents&token=トークン
```

回答JSONの受け取り(アプリでの取り込み・承認・自動再同期)は、
この方法でも一切変わらず動く。

## セキュリティのメモ

- 配信はトークン必須(不一致は 403)。`data/` への直接アクセスは .htaccess で拒否。
- 置いているのは制作の進捗・整理情報のみ(ゲーム本体・秘密情報は置かない)。
- トークンを変えたくなったら: アプリで再生成 → config.php を更新 → アプリ側も保存し直す。
- HTTPS のURLを使うこと(ロリポップの無料SSLで可)。
