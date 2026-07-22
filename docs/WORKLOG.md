# 作業記録

## 2026-07-22: WSLビルド環境の構築とビルド・テストの検証

### 実施内容

- WSL側にビルド環境を構築(前セッションで途中まで進めていたものを再開して完了)
  - JDK 17.0.19: `/home/note_xylo/tools/jdk-17.0.19+10`
  - Android SDK: `/home/note_xylo/android-sdk`(platform-tools / build-tools 34.0.0・35.0.0 / android-35、ライセンス同意済み)
  - `local.properties` は WSL側SDK(`sdk.dir=/home/note_xylo/android-sdk`)を指す
- `assembleDebug` 成功(BUILD SUCCESSFUL、34タスク、デバッグAPK生成まで確認)
- `testDebugUnitTest` 成功(`DummyDataTest` パス)

### WSLでのビルド方法

```
JAVA_HOME=/home/note_xylo/tools/jdk-17.0.19+10 ./gradlew assembleDebug --no-daemon
JAVA_HOME=/home/note_xylo/tools/jdk-17.0.19+10 ./gradlew testDebugUnitTest --no-daemon
```

※ Windows側のAndroid Studioで開く場合は `local.properties` の `sdk.dir` をWindows側SDKに差し替えること(このファイルはGit管理外)。

### 残作業

- エミュレーターまたは実機での起動確認(WSL環境のためユーザー作業)
- Gitリポジトリの初期化とコミット
- Phase 2(Room / DataStore / JSONエクスポート・インポート)への着手

## 2026-07-22: Phase 0 + Phase 1 初期実装

### 実施内容

- Androidプロジェクト一式をゼロから作成(Android Studio未インストールのため、Claude Codeがファイルを直接生成)
  - Gradle Kotlin DSL + Version Catalog(`gradle/libs.versions.toml`)
  - Gradle Wrapper 8.11.1(jarは gradle/gradle GitHubリポジトリ v8.11.1 タグから取得)
- 5画面の骨組みを実装(下部ナビゲーションで移動可能)
  - ホーム: 今日の予定 / 次の予定 / ゲーム進捗 / 優先タスク / JSONボタン(未実装Toast) / 最終同期
  - カレンダー: 今日 / 今後7日 / 過去7日 / 手動更新ボタン
  - プロジェクト: 3ゲームのカード(目標 / 作業中 / 次タスク / 最終更新 / 状態チップ)
  - よすが連携: 状況JSON説明 / 作成・取込ボタン(未実装Toast) / 受け取った提案一覧
  - 設定: 各設定項目のプレースホルダー
- 仮データを `data/model/DummyData.kt` に集約(Phase 2 でRepository/Roomに置き換える)
- `CLAUDE.md`(設計書14章のルール + プロジェクト概要)、`README.md`、`.gitignore` を作成
- 単体テスト `DummyDataTest` を追加(projectIdの重複・設計書との一致を確認)

### 置いた仮定(設計書で「相談して決める」とされていた項目)

- **パッケージ名**: `com.shiro.yosugahub`(設計書10.2の例をそのまま採用)
- **Minimum SDK**: API 26(Android 8.0)
  - 理由: `java.time` がdesugaringなしで使える / ほぼ全ての現役端末をカバー / Pixel 10aは余裕で対象内
- **compileSdk / targetSdk**: 35(Android 15)
- **バージョン**: AGP 8.7.3 / Kotlin 2.0.21 / Compose BOM 2024.12.01 / Navigation 2.8.5
  - 相互compatibilityが確認されている安定版の組み合わせを採用。Android Studioが新しいバージョンを提案したら、動作確認のうえ追従してよい
- Phase 1では ViewModel / Repository を導入していない(仮データの静的表示のみのため)。Phase 2で実データと同時に導入する

### テスト結果

- **未検証**: この環境(WSL)にJDK・Android SDKがないため、ビルドとテストは未実行
- Android Studioインストール後、最初にGradle Syncと `assembleDebug` の成功を確認すること

### 未解決事項

- Android Studioのインストール(ユーザー作業)
- エミュレーターでの起動確認
- Gitリポジトリの初期化とコミット(この作業の最後に実施)

### 次に推奨する作業

1. Android Studioをインストールし、このフォルダを開いてビルド・起動を確認する
2. ビルドエラーがあれば修正する
3. Phase 2(Room / DataStore / JSONエクスポート・インポート)に着手する
