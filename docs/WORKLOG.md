# 作業記録

## 2026-07-22: Phase 2-4 JSONエクスポート(状況JSON生成・保存・共有)

### 目的

Phase 2 完了条件の1つ「アプリが状況JSNを生成できる」を満たす。設計書2.3「アプリから
ChatGPTへ」の状況JSONを生成し、アプリ専用領域へ保存 + Android共有メニューで渡せるようにする。
ホーム・よすが画面の「未実装Toast」だったJSN作成ボタンを実機能に置き換える。

### 新規ライブラリ(追加理由を明記)

- **kotlinx.serialization JSON 1.7.3**(`org.jetbrains.kotlinx:kotlinx-serialization-json`)
  + Gradleプラグイン `org.jetbrains.kotlin.plugin.serialization`(Kotlin 2.0.21 に追従)
  - 理由: 設計書2.3/15章のJSN受け渡しがPhase 2の中核。`schemaVersion` 付きの型安全な
    JSN生成・(次ステップの)検証に必要。設計書3.4のライブラリ候補にも記載。

### 実施内容

- JSNモデル `data/file/model/ContextExport`(`@Serializable`)を新設
  - 設計書2.3の状況JN構造(`schemaVersion` / `generatedAt` / `userContext` /
    `calendar{pastDays,futureDays,events}` / `projects[]` / `recentAssistantExchange`)
  - `SCHEMA_VERSION = 1` を定数化(設計書15章: schemaVersion必須)
- `data/file/ContextExporter`(純粋オブジェクト)を新設
  - ドメインモデル → DTO変換 + `Json.encodeToString`(prettyPrint / encodeDefaults)
  - I/O・プラットフォーム非依存でユニットテスト可能
  - `generatedAt` は引数注入(テスト容易性)。現状 `statusMarkdown` は手元の
    goal/inProgress/nextTask から簡易Markdownを生成(status.md は Phase 3 で導入)
- `data/repository/ExportRepository` を新設
  - projects + events(today/upcoming/past を結合)を `Flow.first()` でスナップショット
  - `OffsetDateTime.now()`(ISO 8601 + タイムゾーン)で generatedAt を付与
  - `filesDir/exports/context_<yyyy-MM-dd_HHmmss>.json` へ保存(設計書4.2)
  - `ExportResult(fileName, json)` を返す
- `DefaultAppContainer` / `AppContainer` に `exportRepository` を追加
- `ui/share/ShareJson`: `ACTION_SEND`(`application/json` / EXTRA_TEXT)で共有
  - ファイル添付共有(FileProvider)は次段で検討
- `HomeViewModel` / `AssistantViewModel` に `createExport(onResult)` を追加
  - ホーム「ChatGPT用JSNを作成」/ よすが「状況JSNを作成」ボタンを実機能化
    (生成 → 保存Toast → 共有シート、失敗時はエラーToast)
- テスト: `ContextExporterTest`(schemaVersion / フィールドマッピング / JN往復)を追加

### テスト結果

- `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL、kspDebugKotlin 実行)

### 残作業(Phase 2 の最後)

- 回答JSNの取り込み(インポート): 選択 → schemaVersion検証 → 検証結果表示 →
  不正JSNでクラッシュさせず取り込み → Room へ保存
- (任意)FileProvider によるファイル添付共有

## 2026-07-22: Phase 2-3 DataStore 導入(最終同期時刻の永続化)

### 目的

設計書4.1が指定する軽量設定の保存先として DataStore を導入。最初の実用値として
「最終同期時刻」を永続化する。従来ホーム画面の「最終同期」はコード内の固定文字列
(プレースホルダー)だったが、これを DataStore 管理へ移し、**実際にローカルデータを
投入した時刻を本物の値として保存・表示**する(未実装を実装済みに見せない)。

### 新規ライブラリ(追加理由を明記)

- **DataStore Preferences 1.1.1**(`androidx.datastore:datastore-preferences`)
  - 理由: 設計書4.1が最終同期時刻・軽量設定の保存先として明示指定。
    SharedPreferences より coroutines/Flow と相性がよく、非同期・型安全に読める。

### 実施内容

- `data/local/datastore/UserPreferencesRepository` を新設
  - `preferencesDataStore`(ファイル名 `user_prefs`)を用い、`lastSyncedAt: Flow<String>` を公開
  - IO エラー時は `emptyPreferences()` にフォールバックしクラッシュさせない
  - `setLastSyncedAt(value)` で書き込み
- `util/SyncTime`: 最終同期時刻の表示フォーマット(`yyyy-MM-dd HH:mm`)を関数化
- `DefaultAppContainer`
  - `UserPreferencesRepository` を生成・公開(`AppContainer` interface にも追加)
  - 初回シードを実行したとき、`LocalDateTime.now()` を整形して最終同期時刻に記録
- `HomeViewModel`
  - `userPreferencesRepository.lastSyncedAt` を combine に追加(4フロー)
  - 空なら「未同期」と表示
- `CalendarRepository` から `lastSyncedAt` のプレースホルダー const を削除(DataStore へ移管)
- テスト: `SyncTimeTest`(フォーマット検証)を追加

### 設計上の注意

- DataStore の I/O 検証はユニットテスト対象外(Context/ファイルが必要なため)。
  代わりにフォーマッタを純粋関数として切り出し `SyncTimeTest` で検証。
- 「最終同期時刻＝初回シード時刻」は暫定。Phase 3(GitHub)/ Phase 4(Calendar)の
  実同期処理が入ったら、その完了時刻に置き換える。

### テスト結果

- `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)
- ホーム画面の「最終同期」が固定文字列から DataStore 由来の実時刻表示に変化(それ以外の見た目は不変)

### 次に推奨する作業(Phase 2 の最後)

- JSONモデル + schemaVersion 検証 + エクスポート/インポート + Android共有メニュー対応
  (ホーム・よすが画面の「未実装Toast」ボタンを実機能へ)

## 2026-07-22: Phase 2-2 Room 導入(ローカル永続化)

### 目的

Phase 2 の完了条件「再起動後もデータが残る」を満たすため、ローカル永続化に Room を導入。
前ステップで Repository が `Flow` を返す形にしてあるので、データソースを
インメモリ(`SampleDataSource`)から Room DAO へ差し替えるだけで済む。
**UIの見た目は変えない**(同じ仮データで DB を初回シードする)。

### 新規ライブラリ(追加理由を明記)

- **Room 2.6.1**(`room-runtime` / `room-ktx` / `room-compiler`)
  - 理由: Phase 2 の永続化要件。設計書4.1が Room を明示指定、3.4のライブラリ候補にも記載。
  - `room-ktx` は DAO の `Flow` 返却(coroutines 連携)のために追加。
- **KSP 2.0.21-1.0.28**(`com.google.devtools.ksp`)
  - 理由: Room のアノテーション処理に必要。Kotlin 2.0.21 と整合するバージョンを選択。
  - ルート `build.gradle.kts` に `apply false` で宣言、`app` で適用。

### 実施内容

- Room 層を新設(`data/local/db/`)
  - `entity/`: `ProjectEntity`(id を主キー)/ `CalendarEventEntity`(bucket で今日・今後・過去を区別)/ `RecommendationEntity`
  - `dao/`: `ProjectDao` / `CalendarEventDao` / `RecommendationDao`(一覧は `Flow` で公開、`count()` と `insertAll()` を用意)
  - `YosugaDatabase`(version 1、exportSchema=false)
  - `Mappers.kt`(Entity → ドメインモデル変換。UIへは常にドメインモデルを渡す)
  - `CalendarBucket`(today/upcoming/past の暫定区分)
  - `SampleSeed`(初回シード用の仮データ。旧 `SampleDataSource` を置き換え、同ファイルは削除)
- Repository をDAO利用へ変更(3つとも `dao.observe...().map { it.toDomain() }`)
  - `today` / `lastSyncedAt` / `priorityTask` は各Repositoryの private const に暫定保持(後で時計・DataStore・導出へ)
- `DefaultAppContainer` を `Room.databaseBuilder` で DB 構築するよう変更
  - 引数に `Context` を追加(`YosugaHubApplication` から `this` を渡す)
  - 初回起動時、各テーブルが空の場合のみ `SampleSeed` を投入(`applicationScope` で非同期、再起動後の実データを壊さない)
- テスト更新: `SampleSeedTest`(種データの件数・ID・bucket検証)、`ProjectRepositoryTest`(フェイクDAOで Entity→ドメイン変換を検証)

### 設計上の注意 / 既知の割り切り

- `exportSchema=false`: 現状マイグレーション不要のため。将来スキーマ変更が必要になったら
  スキーマ出力とマイグレーションを追加する。
- シードはあくまで仮データ。Phase 3(GitHub)/ Phase 4(Calendar)/ Phase 2 のJSON取り込みで
  実データに置き換わり、`SampleSeed` は不要になる。

### テスト結果

- `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL、`kspDebugKotlin` 実行を確認)
- UIの表示内容は変更前と同一

### 次に推奨する作業(Phase 2 の続き)

1. DataStore 導入(設定値・最終同期時刻)
2. JSONモデル + schemaVersion 検証 + エクスポート/インポート + Android共有メニュー対応

## 2026-07-22: Phase 2-1 ViewModel / Repository 層の導入

### 目的

Phase 2 の下準備。**UIの見た目は一切変えず**、データ経路を `DummyData` の直接参照から
`UI → ViewModel → Repository → DataSource` へ通す(設計書3.3のアーキテクチャに整える)。
これにより次の Room / DataStore を DataSource 差し替えだけで導入できる状態にする。

### 実施内容

- ドメインモデルを新設(`domain/model/`): `CalendarEvent` / `Project` / `Recommendation`
  - 旧 `data/model/DummyData.kt` の `Dummy*` データクラスを置き換え、同ファイルは削除
- インメモリのデータソース `data/local/SampleDataSource` を新設(旧 `DummyData` の種データを集約)
- Repository を3つ新設(`data/repository/`): `CalendarRepository` / `ProjectRepository` / `AssistantRepository`
  - 一覧は `Flow<List<...>>` で公開(Room 導入時に DAO の Flow へそのまま差し替え可能)
  - `today` / `lastSyncedAt` / `priorityTask` は暫定値。後で端末時計・DataStore・導出ロジックへ置き換える
- 手動DI: `di/AppContainer`(interface + `DefaultAppContainer`)と `YosugaHubApplication` を新設
  - `AndroidManifest.xml` に `android:name=".YosugaHubApplication"` を追加
  - 設計書3.4の方針どおり、初期版は手動DI。画面/Repositoryが増えたら Hilt を検討
- 各画面に ViewModel を追加(`Home` / `Calendar` / `Projects` / `Assistant`)
  - `StateFlow<XxxUiState>` を公開し、画面は `collectAsState()` で監視
  - 画面から `DummyData` 直参照を全廃(設定画面はデータを持たないため据え置き)
- テスト更新: `DummyDataTest` → `SampleDataSourceTest`、加えて `ProjectRepositoryTest`(Flow の emit を検証)を追加

### 新規ライブラリ

- **なし**。`Flow` / `viewModelScope` / coroutines は既存の lifecycle 依存に含まれる推移的依存を利用。
  `collectAsStateWithLifecycle` は追加依存(`lifecycle-runtime-compose`)が必要なため、
  今回は追加せず標準の `collectAsState()` を使用。

### テスト結果

- `assembleDebug` 成功 / `testDebugUnitTest` 成功(BUILD SUCCESSFUL、JDK 17・WSL)
- UIの表示内容は変更前と同一(仮データの見た目を維持)

### 次に推奨する作業(Phase 2 の続き)

1. Room 導入(Entity / DAO / DB)→ `SampleDataSource` を Room 由来の DataSource へ差し替え
2. DataStore 導入(設定値・最終同期時刻)
3. JSONモデル + schemaVersion 検証 + エクスポート/インポート + 共有メニュー対応

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
