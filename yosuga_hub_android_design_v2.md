# 制作統合ダッシュボード Androidアプリ
## 設計方針・開発環境・初期実装計画

- 作成日: 2026-07-22
- 用途: Claude Codeへ渡す初期設計書
- 対象: 個人利用のAndroidアプリ
- 仮称: **Yosuga Hub**
- 開発方針: まず小さく動かし、後から自動連携を追加する

---

## 1. このアプリの目的

Googleカレンダー、3つのゲーム制作プロジェクト、ChatGPT（よすが）との会話から生まれた情報を、Androidスマートフォン上で一元管理する。

スマートフォンを「ゲーム制作ダッシュボード」として使い、次の情報をひとつの画面から確認できる状態を目指す。

1. 今日と今後の予定
2. 各ゲームの現在の進捗
3. 次に行うタスク
4. ChatGPTへ渡すための状況JSON
5. ChatGPTから返された提案JSON
6. 将来的には、予定・進捗・会話履歴を前提にしたAI支援

---

## 2. 管理対象

### 2.1 Googleカレンダー

取得対象の例:

- 今日の予定
- 今後7日間の予定
- 過去7日間の予定
- 予定の開始・終了時刻
- タイトル
- 説明
- カレンダー名

初期版は**読み取り専用**とする。

予定の作成・編集は、初期版が安定した後に追加する。

### 2.2 ゲーム制作プロジェクト

現在進行中の3プロジェクトを管理する。

- ANRI
- 紙装甲主人公と不死身のカエル
- げんげきょう

各プロジェクトでは、Claude Codeが生成・更新するMarkdownファイルを進捗情報の元データとして使う。

各ゲームのリポジトリには、最低限、次のファイルを置く。

```text
docs/
  PROJECT_STATUS.md
```

推奨する `PROJECT_STATUS.md` の構成:

```markdown
# Project Status

## Summary
現在の状態を短く記述する。

## Current Goal
現在の目標。

## Completed
- 完了項目

## In Progress
- 作業中項目

## Next Tasks
- 次に行う項目

## Blockers
- 問題や保留事項

## Last Updated
2026-07-22
```

初期版ではMarkdownをそのまま取得・表示する。

アプリ内で高度な集計が必要になった時点で、Claude Codeに同時にJSONも出力させる。

### 2.3 ChatGPTとの情報交換

初期版ではOpenAI APIを使わず、JSONファイルを手動で受け渡す。

#### アプリからChatGPTへ

アプリが、カレンダーとゲーム進捗をまとめたJSONを生成する。

例:

```json
{
  "schemaVersion": 1,
  "generatedAt": "2026-07-22T20:00:00+09:00",
  "userContext": {
    "purpose": "ゲーム制作の状況共有と相談"
  },
  "calendar": {
    "pastDays": 7,
    "futureDays": 7,
    "events": []
  },
  "projects": [
    {
      "id": "anri",
      "name": "ANRI",
      "statusMarkdown": "",
      "lastUpdated": ""
    },
    {
      "id": "paper-armor-frog",
      "name": "紙装甲主人公と不死身のカエル",
      "statusMarkdown": "",
      "lastUpdated": ""
    },
    {
      "id": "gengenkyo",
      "name": "げんげきょう",
      "statusMarkdown": "",
      "lastUpdated": ""
    }
  ],
  "recentAssistantExchange": null
}
```

このJSONをAndroidの共有メニューからChatGPTへ渡す、またはファイルとしてChatGPTに添付する。

#### ChatGPTからアプリへ

ChatGPTが、アプリ取り込み用JSONを返す。

例:

```json
{
  "schemaVersion": 1,
  "generatedAt": "2026-07-22T20:30:00+09:00",
  "summary": "現在の状況の要約",
  "recommendations": [
    {
      "projectId": "anri",
      "title": "次の作業候補",
      "detail": "具体的な提案",
      "priority": "high"
    }
  ],
  "suggestedTasks": [
    {
      "projectId": "anri",
      "title": "タスク名",
      "detail": "実施内容",
      "estimatedMinutes": 30
    }
  ],
  "notes": []
}
```

アプリはAndroidの共有メニューまたはファイル選択から、このJSONを取り込む。

---

## 3. 採用する技術

### 3.1 結論

Unityは使用しない。

このアプリはゲームではなく、カレンダー・ファイル・ネットワーク・一覧画面を扱う管理アプリであるため、Androidネイティブ開発を採用する。

### 3.2 開発言語とUI

- Android Studio
- Kotlin
- Jetpack Compose
- Material 3
- Gradle Kotlin DSL

Jetpack Composeを画面UIに使用する。

### 3.3 アーキテクチャ

基本構成は次のとおり。

- UI Layer
- ViewModel
- Repository
- Data Source

一般にMVVMと呼ばれる形に近いが、名前にこだわりすぎず、画面とデータ取得処理を分離することを重視する。

```text
Compose Screen
      |
      v
ViewModel
      |
      v
Repository
   /       \
Local      Remote
Data       Data
```

各役割:

#### UI Layer

- 画面を表示する
- ユーザー操作をViewModelへ通知する
- UI状態を監視する
- データ取得処理を直接書かない

#### ViewModel

- 画面の状態を保持する
- Repositoryを呼び出す
- ローディング、成功、エラーを管理する
- UI向けのデータに整形する

#### Repository

- データ取得元の違いを吸収する
- Google Calendar、GitHub、ローカルファイルを同じ考え方で扱う
- 将来、手動JSON連携をOpenAI API連携へ差し替えやすくする

#### Data Source

- Google Calendar API
- GitHub Contents APIまたはRaw URL
- Androidローカルストレージ
- JSONインポート・エクスポート

### 3.4 主なライブラリ候補

初期候補:

- Jetpack Compose
- Navigation Compose
- Lifecycle ViewModel Compose
- Kotlin Coroutines
- Kotlin Flow
- Kotlin Serialization
- Retrofit または Ktor Client
- Room
- DataStore
- Google Identity Services
- Google Calendar API
- WorkManager（後から追加）

依存性注入は、初期版では手動DIでもよい。

画面やRepositoryが増えたらHiltを導入する。

最初からライブラリを盛りすぎない。アプリが「ライブラリ展示会」になるのを避ける。

---

## 4. データの置き場所

### 4.1 アプリ内部データ

Roomに保存する候補:

- 取得済みカレンダー予定のキャッシュ
- プロジェクト情報
- 最後に取得した進捗Markdown
- ChatGPTから受け取った提案
- インポート履歴
- エクスポート履歴

DataStoreに保存する候補:

- GitHubリポジトリ設定
- 対象ブランチ
- 進捗ファイルのパス
- カレンダー取得期間
- 最終同期時刻
- テーマなどの軽量設定

### 4.2 JSONファイル

アプリ専用領域に次のような論理分類で保存する。

```text
exports/
  context_2026-07-22_200000.json

imports/
  response_2026-07-22_203000.json

archive/
  ...
```

ただし、ユーザーがファイル管理アプリから直接探す必要がある場合は、Storage Access Frameworkを利用し、ユーザーが保存先フォルダを選択できるようにする。

初期版では以下の両方に対応する。

1. 「ファイルとして保存」
2. 「共有する」

### 4.3 GitHub

各ゲームの進捗MarkdownをGitHubから取得する。

非公開リポジトリを使う場合、トークンをソースコードやJSONに直接保存しない。

初期版の選択肢:

1. まず公開テスト用リポジトリで取得機能を確認する
2. 次にFine-grained Personal Access Tokenの読み取り権限で非公開リポジトリに対応する
3. トークンはAndroid Keystoreを利用して安全に保持する

長期的にはGitHub AppやOAuthも検討できるが、個人利用の初期版では過剰設計にしない。

---

## 5. Googleカレンダー連携

### 5.1 初期仕様

- Googleアカウントで認証
- カレンダー一覧を取得
- 対象カレンダーを選択
- 過去7日から未来7日のイベントを取得
- アプリ内に一覧表示
- JSONエクスポートへ含める

初期版の権限は、可能な限り読み取り専用に限定する。

### 5.2 後から追加する機能

- ChatGPTの提案を予定候補として表示
- ユーザー確認後に予定を作成
- 予定の編集
- ゲーム別にカレンダー色やタグを分ける

AIの提案を確認なしで自動的にカレンダーへ書き込まない。

---

## 6. 画面構成

### 6.1 ホーム画面

表示内容:

- 今日の日付
- 今日の予定
- 次の予定
- 3ゲームの進捗概要
- 優先タスク
- 最終同期時刻
- 「ChatGPT用JSONを作成」ボタン
- 「ChatGPT回答JSONを取り込む」ボタン

### 6.2 カレンダー画面

- 今日
- 今後7日
- 過去7日
- イベント詳細
- 手動更新

### 6.3 プロジェクト一覧画面

各カードに表示:

- ゲーム名
- 現在の目標
- 作業中
- 次のタスク
- 最終更新日時
- 同期状態

### 6.4 プロジェクト詳細画面

- Markdown表示
- 元ファイルの更新日時
- 再取得ボタン
- 取得エラー表示
- 将来はタスク単位の表示にも対応

### 6.5 よすが連携画面

- 状況JSONのプレビュー
- JSON作成
- ファイル保存
- Android共有メニューで送信
- 回答JSONの選択
- 検証結果
- 取り込み
- 過去の提案一覧

### 6.6 設定画面

- Googleアカウント
- 取得対象カレンダー
- カレンダー取得期間
- GitHub設定
- 各プロジェクトのリポジトリ
- ブランチ名
- `PROJECT_STATUS.md` のパス
- JSON保存先
- データ削除
- 診断情報

---

## 7. 推奨パッケージ構成

初期版は単一appモジュールで開始する。

```text
app/src/main/java/<package>/
  MainActivity.kt

  ui/
    navigation/
    screen/
      home/
      calendar/
      projects/
      assistant/
      settings/
    component/
    theme/

  data/
    model/
    local/
      db/
      dao/
      entity/
      datastore/
    remote/
      calendar/
      github/
    file/
      import/
      export/
    repository/

  domain/
    model/
    usecase/

  util/
```

`domain` は、処理が単純な間は最小限にする。

UseCaseを意味なく大量生産しない。複雑な処理や複数画面で再利用する処理だけを置く。

---

## 8. エラー処理

最低限、次を明確に区別する。

- ネットワーク接続なし
- Google認証失敗
- Calendar権限なし
- GitHub認証失敗
- リポジトリまたはファイルが見つからない
- Markdownの取得失敗
- JSONの形式不正
- JSONのバージョン非対応
- ローカル保存失敗

画面には技術的な例外文をそのまま表示せず、ユーザーが次に何をすればよいかを表示する。

例:

```text
GitHubから進捗を取得できませんでした。
リポジトリ名、ブランチ、ファイルパスを確認してください。
```

---

## 9. セキュリティ方針

- GitHubトークンをGitへコミットしない
- APIキーをアプリに直書きしない
- `local.properties` や環境変数を利用する
- 保存した認証情報はAndroid Keystoreで保護する
- ChatGPTへ渡すJSONに不要な個人情報を含めない
- カレンダー情報をJSONへ出す前に内容を確認できるようにする
- ログへアクセストークンや予定本文を出さない
- 将来OpenAI APIを使う場合、配布アプリへ秘密鍵を埋め込まない
- 将来クラウド経由でAPIを呼ぶ場合は、自分のバックエンドを用意する

個人利用の試作であっても、秘密情報をGitHubへうっかり公開する事故だけは最初から防ぐ。

---

## 10. 開発環境

### 10.1 PCへインストールするもの

必須:

1. Android Studio 最新安定版
2. Android SDK
3. Android SDK Platform Tools
4. Android Emulator
5. Git
6. Claude Code
7. GitHubアカウント

Android Studioのセットアップ時に、標準セットアップでSDK、エミュレーター、必要なJDKを導入する。

Android Studioに同梱されるJDKを基本的に使い、理由なく別JDKを増やさない。

### 10.2 Android Studio初期設定

- 新規プロジェクト: Empty Activity
- Language: Kotlin
- UI: Jetpack Compose
- Build configuration: Kotlin DSL
- Minimum SDK: Claude Codeと相談して決定
- Package name: 後から変更しにくいため、最初に決める

仮の例:

```text
com.shiro.yosugahub
```

### 10.3 実機テスト

Pixel 10aが届くまではAndroid Emulatorで開発する。

Pixel 10aが届いた後:

1. 開発者向けオプションを有効化
2. USBデバッグを有効化
3. PCとUSB接続
4. Android Studioから実機を選択
5. デバッグビルドを実行

初期段階ではGoogleカレンダー認証やファイル共有の挙動を実機でも確認する。

### 10.4 Gitリポジトリ

アプリ本体用の新しいリポジトリを作る。

例:

```text
yosuga-hub-android
```

最初のコミットに含めるもの:

- Android Studioの初期プロジェクト
- README.md
- この設計書
- `.gitignore`
- ライセンスは必要になった時点で追加

コミット例:

```text
chore: create initial Android project
feat: add home screen skeleton
feat: add project status repository
feat: add JSON export
```

---

## 11. 開発順序

### Phase 0: 環境構築

目標:

- Android Studioを起動できる
- 空のComposeアプリがエミュレーターで動く
- Gitへ最初のコミットができる
- Claude Codeからプロジェクトを編集できる

完了条件:

- 画面に仮タイトルが表示される
- アプリがクラッシュせず起動する

### Phase 1: 画面の骨組み

実装:

- 下部ナビゲーション
- ホーム
- カレンダー
- プロジェクト
- よすが連携
- 設定
- 仮データ表示

この段階では外部APIへ接続しない。

完了条件:

- 全画面へ移動できる
- ダミーの予定、ゲーム進捗、提案が表示される

### Phase 2: ローカルデータ

実装:

- Room
- DataStore
- プロジェクト設定の保存
- JSONモデル
- JSONエクスポート
- JSONインポート
- schemaVersion検証
- Android共有メニュー対応

完了条件:

- アプリが状況JSONを生成できる
- ChatGPT回答JSONを取り込める
- 再起動後もデータが残る

### Phase 3: GitHub進捗取得

実装:

- 1プロジェクトだけで接続確認
- `PROJECT_STATUS.md` の取得
- Markdown表示
- キャッシュ
- 更新日時
- エラー表示
- 3プロジェクトへ拡張

完了条件:

- 3ゲームすべての進捗をスマホから確認できる

### Phase 4: Googleカレンダー読み取り

実装:

- Google認証
- カレンダー選択
- イベント取得
- 一覧表示
- ローカルキャッシュ
- 状況JSONへ含める

完了条件:

- 過去7日と未来7日の予定を確認できる
- ChatGPT用JSONへ予定を出力できる

### Phase 5: ホーム統合

実装:

- 今日の予定
- 3ゲームの要約
- 次のタスク
- ChatGPTからの提案
- 同期状態
- 最終更新時刻

完了条件:

- ホーム画面だけで、その日に何をするか判断できる

### Phase 6: 品質改善

実装:

- 単体テスト
- Repositoryテスト
- JSON互換性テスト
- エラー表示改善
- ダークモード
- ローディング表示
- オフライン表示
- バックアップ

### Phase 7: 将来のOpenAI API連携

初期版が実際に役立つと確認できてから検討する。

手動JSON連携のRepositoryを差し替える形で追加する。

```text
AssistantRepository
  ├─ ManualFileAssistantDataSource
  └─ OpenAiAssistantDataSource
```

OpenAI APIを使う場合も、現在のChatGPT会話履歴が自動的にAPIへ共有される前提にはしない。

アプリ側が必要な状況・要約・設定を明示的に送る。

---

## 12. MVPで「やらないこと」

最初の完成を早くするため、以下は後回しにする。

- Windows版アプリ
- iPhone版アプリ
- Unityでの実装
- OpenAI APIによる完全自動会話
- 自動でカレンダー予定を追加する機能
- GitHubへの自動コミット
- ゲームプロジェクト全ファイルの解析
- 常時バックグラウンド同期
- 複数ユーザー対応
- サーバー構築
- 豪華なアニメーション
- 完璧なモジュール分割

MVPは「今日の予定、ゲーム進捗、よすがとのJSON受け渡しが1台のAndroid端末で動く」ことを成功条件にする。

---

## 13. Claude Codeへの最初の依頼文

以下をClaude Codeへ渡す。

```text
このリポジトリに、個人用のAndroid制作管理アプリを作ります。

添付した設計書を読んでください。
技術構成は Kotlin + Jetpack Compose + Material 3 とし、
UI / ViewModel / Repository / Data Source を分離してください。

ただし、最初から全機能を実装しないでください。
まずPhase 0とPhase 1だけを行います。

最初の作業:
1. 現在のAndroidプロジェクト構成を確認する
2. 必要な最小依存関係だけを提案する
3. ホーム、カレンダー、プロジェクト、よすが連携、設定の
   5画面へ移動できる画面骨組みを作る
4. 各画面にはダミーデータを表示する
5. ビルドとテストを実行する
6. 実施内容をCHANGELOGまたは作業記録Markdownへ残す

作業前に、変更予定のファイルと実装手順を短く提示してください。
不明点があっても、重大でなければ合理的な仮定を置いて進め、
仮定を作業記録に明記してください。
秘密情報、APIキー、アクセストークンはコードへ書かないでください。
```

---

## 14. Claude Codeに守らせるルール

プロジェクトルートに `CLAUDE.md` を作り、次を記載する。

```markdown
# Project Rules

- KotlinとJetpack Composeを使用する。
- UIから直接ネットワークやDBへアクセスしない。
- ViewModelはUI状態を公開する。
- 外部データ取得はRepository経由にする。
- 秘密情報をコード、ログ、Gitへ保存しない。
- 変更後は必ずビルドする。
- 可能な範囲でテストを追加する。
- 一度に大規模な変更を行わない。
- 既存の動作を壊さない。
- 新しいライブラリを追加する前に理由を説明する。
- 作業内容をMarkdownへ記録する。
- 未実装部分を、実装済みのように扱わない。
```

---

## 15. JSON互換性ルール

JSONには必ず `schemaVersion` を含める。

```json
{
  "schemaVersion": 1
}
```

ルール:

- 未知の項目は無視できるようにする
- 必須項目を最小限にする
- 日時はISO 8601形式
- タイムゾーンを含める
- プロジェクトは表示名ではなく固定IDで関連付ける
- インポート前に検証する
- 不正JSONでアプリをクラッシュさせない
- 元ファイルを上書きせず、履歴を残す

---

## 16. 初日に行うこと

1. Android Studioをインストールする
2. 標準セットアップを完了する
3. Empty ActivityのComposeプロジェクトを作る
4. エミュレーターで起動する
5. Gitリポジトリを初期化する
6. この設計書をプロジェクトへ置く
7. `CLAUDE.md` を作る
8. Claude CodeへPhase 0とPhase 1だけ依頼する
9. ビルドが通った状態でコミットする

初日はGoogle認証にもGitHub認証にも着手しなくてよい。

最初の勝利条件は、小さくてもアプリが起動し、5画面を移動できること。

---

## 17. 完成イメージ

朝、Androidアプリを開く。

ホーム画面には以下が表示される。

- 今日の予定
- 空いている時間
- ANRIの現在地
- 紙装甲主人公と不死身のカエルの次の作業
- げんげきょうの保留事項
- 前回よすがから受け取った提案
- 「現在の状況をJSONにする」ボタン

JSONをChatGPTへ渡して相談し、返ってきたJSONを共有メニューからアプリへ取り込む。

APIを使わない段階でも、アプリとChatGPTの間に安定した「受け渡し橋」ができる。

この橋が便利だと確認できたら、その先にAPIによる自動化を追加する。


---

## 18. 各ゲーム側Claude Codeの「データ提供責任」

Androidアプリが正しく情報を取得できるように、各ゲームプロジェクトを管理するClaude Codeには、開発作業だけでなく、**外部アプリ向けの進捗データを一定の形式で更新する責任**を持たせる。

各ゲーム側を「データ提供者」、Androidアプリ側を「データ利用者」と考える。

```text
各ゲームのClaude Code
        |
        | 進捗データを生成・検証・コミット
        v
GitHubリポジトリ
        |
        | 共通形式で取得
        v
Yosuga Hub Androidアプリ
```

Claude Codeが自由形式のMarkdownだけを書いている状態では、見出し名や書き方が変化し、アプリが安定して解釈できない可能性がある。

そのため、人間向けMarkdownとアプリ向けJSONを分けて管理する。

---

## 19. 各ゲームがGitHubへ提供する共通ファイル

各ゲームのリポジトリに、次のディレクトリを必ず用意する。

```text
.yosuga/
  project.json
  status.json
  status.md
  schema-version.txt
```

### 19.1 `project.json`

基本的に頻繁には変わらない、プロジェクト固有の情報を保存する。

```json
{
  "schemaVersion": 1,
  "projectId": "anri",
  "displayName": "ANRI",
  "repository": {
    "owner": "OWNER_NAME",
    "name": "REPOSITORY_NAME",
    "defaultBranch": "main"
  },
  "statusFile": ".yosuga/status.json",
  "humanReadableStatusFile": ".yosuga/status.md",
  "active": true
}
```

必須ルール:

- `projectId` は一度決めたら変更しない
- 表示名を変更しても `projectId` は維持する
- プロジェクトごとに重複しないIDを使う
- リポジトリ移動時は `repository` を更新する

推奨ID:

```text
anri
paper-armor-frog
gengenkyo
```

### 19.2 `status.json`

Androidアプリが主に取得する機械可読データ。

```json
{
  "schemaVersion": 1,
  "projectId": "anri",
  "generatedAt": "2026-07-22T21:00:00+09:00",
  "sourceCommit": "COMMIT_SHA",
  "summary": "現在の状況を1〜3文で記述",
  "phase": "prototype",
  "health": "on_track",
  "currentGoal": {
    "title": "現在の目標",
    "detail": "目標の具体的な説明"
  },
  "completed": [
    {
      "id": "task-001",
      "title": "完了した作業",
      "completedAt": "2026-07-22T18:00:00+09:00"
    }
  ],
  "inProgress": [
    {
      "id": "task-002",
      "title": "作業中の項目",
      "detail": "現在の状態",
      "progressPercent": 50
    }
  ],
  "nextTasks": [
    {
      "id": "task-003",
      "title": "次に行う作業",
      "detail": "具体的な作業内容",
      "priority": "high",
      "estimatedMinutes": 60,
      "dependencies": []
    }
  ],
  "blockers": [
    {
      "id": "blocker-001",
      "title": "進行を妨げている問題",
      "detail": "何が問題か",
      "severity": "medium"
    }
  ],
  "recentChanges": [
    {
      "date": "2026-07-22",
      "summary": "変更内容の要約",
      "commit": "COMMIT_SHA"
    }
  ],
  "risks": [],
  "decisions": [],
  "questionsForYosuga": [],
  "dataQuality": {
    "validated": true,
    "missingFields": [],
    "warnings": []
  }
}
```

### 19.3 `status.md`

人間とClaude Codeが読みやすい形式の進捗情報。

`status.json` と内容が矛盾しないようにする。

```markdown
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
```

### 19.4 `schema-version.txt`

内容は数字だけにする。

```text
1
```

Androidアプリは、未対応のバージョンを検出した場合、無理に読み込まず警告する。

---

## 20. データ提供契約

各ゲーム側Claude Codeは、`.yosuga/status.json` を更新するときに以下を守る。

### 必須条件

- JSONとして構文が正しい
- `schemaVersion` が存在する
- `projectId` が `project.json` と一致する
- `generatedAt` がISO 8601形式である
- 日時にはタイムゾーンを含める
- `summary` が空ではない
- `nextTasks` が存在する
- 完了していない問題は `blockers` または `risks` に記録する
- 不明な内容を推測で事実として書かない
- 実装済みと未実装を混同しない
- 実際のGitの状態と矛盾する記述を避ける
- 更新後にJSON検証を実行する
- 検証に失敗した状態でコミットしない

### 推奨条件

- `sourceCommit` に対象コミットSHAを入れる
- 完了タスクには完了日時を入れる
- 次タスクには優先度を入れる
- 見積もりが難しい場合は `estimatedMinutes` を省略する
- 判断が必要な事項は `questionsForYosuga` に入れる
- 重要な設計判断は `decisions` に残す
- 進捗率は根拠がある場合のみ記載する

---

## 21. Claude Codeが更新するタイミング

各ゲーム側Claude Codeは、次のタイミングで `.yosuga/` を更新する。

1. 一連の作業を完了したとき
2. コミットを作成するとき
3. 現在の目標が変わったとき
4. 新しいブロッカーが発生したとき
5. 次の作業候補が変わったとき
6. ユーザーが作業終了を伝えたとき
7. `status.json` が24時間以上古い状態で新しい作業を行ったとき

小さなコード変更ごとに毎回更新する必要はない。

意味のある作業単位で更新し、進捗データだけの無駄なコミットを大量に作らない。

---

## 22. 各ゲームの `CLAUDE.md` に追加する共通ルール

各ゲームプロジェクトの `CLAUDE.md` に、次を追加する。

```markdown
# Yosuga Hub Data Provider Rules

このプロジェクトは、Androidアプリ「Yosuga Hub」へ進捗情報を提供する。

## Required files

以下を維持すること。

- `.yosuga/project.json`
- `.yosuga/status.json`
- `.yosuga/status.md`
- `.yosuga/schema-version.txt`

## Update responsibility

意味のある作業単位を完了したとき、コミット前または作業終了前に、
`.yosuga/status.json` と `.yosuga/status.md` を更新する。

## Accuracy rules

- 実装済み、作業中、未実装を明確に区別する。
- 実際のコード、テスト結果、Git履歴に基づいて記録する。
- 推測を事実として記載しない。
- 未解決の問題を隠さない。
- 次に行える具体的なタスクを記載する。
- ユーザーまたはYosugaの判断が必要な事項は
  `questionsForYosuga` に記録する。

## Validation

更新後はJSONの構文と必須フィールドを検証する。
検証に失敗したファイルをコミットしない。

## Git rules

- 秘密情報を `.yosuga/` に含めない。
- ローカルの絶対パスを含めない。
- アクセストークン、APIキー、個人情報を含めない。
- `sourceCommit` は可能なら最新の関連コミットSHAにする。

## Completion report

作業完了時の報告には次を含める。

1. 実装した内容
2. テスト結果
3. 未解決事項
4. `.yosuga/` を更新したか
5. 次に推奨する作業
```

---

## 23. 各Claude Codeへ最初に渡す導入指示

3つのゲームそれぞれのClaude Codeへ、以下を渡す。

```text
このゲームプロジェクトを、Androidアプリ「Yosuga Hub」から
進捗確認できるようにします。

プロジェクトルートに `.yosuga/` ディレクトリを作成してください。

必要ファイル:
- `.yosuga/project.json`
- `.yosuga/status.json`
- `.yosuga/status.md`
- `.yosuga/schema-version.txt`

この設計書の「各ゲーム側Claude Codeのデータ提供責任」以降を読み、
現在のコード、既存Markdown、Git履歴を確認したうえで、
現状を正確に反映した初期データを作成してください。

作業手順:
1. リポジトリと既存ドキュメントを調査する
2. 現在の状態、完了済み、作業中、次タスク、ブロッカーを整理する
3. 共通仕様に従って4ファイルを作成する
4. JSONを機械的に検証する
5. status.jsonとstatus.mdの内容が矛盾していないか確認する
6. CLAUDE.mdへYosuga Hub Data Provider Rulesを追加する
7. 変更内容と検証結果を報告する

重要:
- 不明な内容を推測で埋めないでください
- 不明点は `dataQuality.warnings` または
  `questionsForYosuga` に記録してください
- 秘密情報、個人情報、ローカル絶対パスを含めないでください
- 既存のゲームコードを、この作業のために変更しないでください
```

---

## 24. 自動検証の仕組み

Claude Codeの自己申告だけに頼らず、GitHubへ送られたデータを自動検証する。

最初は各リポジトリに簡単な検証スクリプトを置く。

```text
scripts/
  validate_yosuga_data.py
```

検証内容:

- 必要ファイルが存在する
- JSONとして読み込める
- 必須フィールドが存在する
- `schemaVersion` が対応値である
- `projectId` が一致する
- 日時形式が正しい
- priority、health、severityが許可値である
- `status.md` が空ではない
- 秘密情報らしい文字列が含まれていない
- ローカル絶対パスが含まれていない

許可値の例:

```text
health:
- on_track
- attention
- blocked
- paused

priority:
- high
- medium
- low

severity:
- high
- medium
- low
```

Claude Codeへは、コミット前に次を実行させる。

```bash
python scripts/validate_yosuga_data.py
```

---

## 25. GitHub Actionsによる継続検証

後から、各ゲームリポジトリにGitHub Actionsを追加する。

```text
.github/workflows/
  validate-yosuga-data.yml
```

動作:

1. `.yosuga/` が変更されたPull RequestまたはPushで起動
2. Pythonをセットアップ
3. 検証スクリプトを実行
4. 不正なJSONや必須項目不足なら失敗
5. GitHub上で赤いチェックを表示

これにより、Claude Codeが誤った形式のデータをGitHubへ送った場合に気づける。

個人開発で直接mainへPushする場合も、Actionsの結果をAndroidアプリ側で確認できるようにする余地がある。

---

## 26. Androidアプリ側のデータ品質表示

Yosuga Hubは、データを表示するだけでなく、データの信頼性も表示する。

プロジェクトカードに次を表示する。

- 最終生成日時
- GitHub上の最終更新日時
- schemaVersion
- 検証済みか
- 警告件数
- データが古いか
- 取得元コミット

状態例:

```text
正常
更新: 2時間前
検証済み
```

```text
要確認
更新: 5日前
警告: 2件
```

```text
読込不可
未対応のschemaVersion: 3
```

古さの暫定基準:

- 24時間以内: 新しい
- 1〜3日: 通常
- 4〜7日: 要確認
- 8日以上: 古い

ただし、作業休止中のプロジェクトは `health: paused` として扱い、単純に警告しすぎない。

---

## 27. プロジェクト一覧マニフェスト

3つのゲームをまとめて取得するため、アプリ用リポジトリまたは専用設定リポジトリにマニフェストを置く。

```text
yosuga-projects.json
```

例:

```json
{
  "schemaVersion": 1,
  "projects": [
    {
      "projectId": "anri",
      "displayName": "ANRI",
      "owner": "OWNER_NAME",
      "repository": "ANRI_REPOSITORY",
      "branch": "main",
      "projectFile": ".yosuga/project.json",
      "statusFile": ".yosuga/status.json"
    },
    {
      "projectId": "paper-armor-frog",
      "displayName": "紙装甲主人公と不死身のカエル",
      "owner": "OWNER_NAME",
      "repository": "PAPER_ARMOR_REPOSITORY",
      "branch": "main",
      "projectFile": ".yosuga/project.json",
      "statusFile": ".yosuga/status.json"
    },
    {
      "projectId": "gengenkyo",
      "displayName": "げんげきょう",
      "owner": "OWNER_NAME",
      "repository": "GENGENKYO_REPOSITORY",
      "branch": "main",
      "projectFile": ".yosuga/project.json",
      "statusFile": ".yosuga/status.json"
    }
  ]
}
```

Androidアプリは、このマニフェストを読めば3つのリポジトリ設定をまとめて取得できる。

初期版ではアプリ設定画面へ手入力してもよい。

マニフェスト方式は、3プロジェクトの取得が安定してから追加する。

---

## 28. この仕組みの成功条件

各ゲーム側で次が満たされていれば、データ提供の仕組みは成功とする。

- 3リポジトリすべてに `.yosuga/` がある
- `projectId` が重複していない
- 3つの `status.json` が同じスキーマで読める
- Claude Codeが作業終了時にデータを更新する
- 検証スクリプトが成功する
- GitHub上のデータだけで現在地と次タスクが分かる
- Androidアプリが個別対応コードなしで3プロジェクトを表示できる
- データが古い、壊れている、不完全である場合に気づける

大切なのは、Claude Codeが「ちゃんと書いたつもり」になることではなく、
Androidアプリが機械的に検証し、安定して利用できることである。
