# 作業記録

---

## ▶▶ 次回の再開ポイント(2026-07-24 時点・ここだけ読めば再開できる)

### いまアプリはどういう状態か

- **設計は v5「Obsidian ブリッジ」**(`yosuga_hub_design_v5_obsidian_bridge.md`)。
  Hub は AI ではなく「情報を運ぶハブ」。知識の正本は Obsidian の Markdown。
- **v5 Phase 1〜3 まで実装・実機で通し成功済み**。GitHub 実通信も成功。
  - Phase1: Obsidian からコンテキスト抽出(選択→生成→コピー/保存/共有)
  - Phase2: 絞り込み(検索/最近更新/タグ)/ JSON出力 / 出力履歴
  - Phase3: 各ゲームの `.yosuga/notes/` を GitHub 取得 → type で振り分け → Vault 書込 /
            会話ログ保存 / status.json 取得表示
- **UI を全面刷新**: 下部ナビ廃止 → **オペレーションコンソール**(コマンドランチャー)。
  フォスファーグリーン単色・全面等幅・角丸ゼロ・カード廃止・コマンド実行行。
  取り込み/保存/生成/GitHub取得で**本物のログが端末に流れ、走査線が走る**演出付き。
- **仮データ(SampleSeed)は削除機能で一掃済み**。再シードも停止。
  プロジェクトの「作業中/次」はタスクから自動導出(案C)。
- テスト **354件** 通過。Room は **v8**。新規ライブラリなし。ビルドは Windows 側 JBR
  (`cmd.exe /c "set JAVA_HOME=...jbr&& gradlew.bat ..."`)。エミュは黒画面対策で
  `fastboot.forceColdBoot=yes`(詳細は [[build-environments]] メモ)。

### 画面の構成(下部ナビは無い)

- 起動 = **コンソール**。`Yosuga` + 状態表示(PROJECT/PENDING/SYNC/STATUS) +
  `> COMMAND` の実行行。
  - オペレーション: IMPORT NOTES / SAVE SESSION / BUILD CONTEXT / EXPORT STATUS /
    IMPORT RESPONSE / REVIEW
  - DATA: PROJECTS / RECORDS / CALENDAR / SETTINGS
- サブ画面は上辺 `<BACK`(緑の反転ブロック)で戻る。

### 役割(重要・混同注意)

- **ヨスガ**(通常ChatGPT) = 会話の相棒。**観測日記(記録タブ「観測」)を書くのはヨスガ**。
  ゲームのアップデート状況(状況JSON)があれば材料に、無ければボイス/会話から書く。
  出力は `diary[]` の回答JSON → コンソール `> IMPORT RESPONSE` に貼る → 承認。
  ヨスガへの指示文は `docs/yosuga_prompt.md`(観測日記の節あり)。
- **レコル**(カスタムGPT+Actions) = 情報の仕分け。日記は書かない。`docs/recoru_prompt.md`。
- 各ゲームの **Claude Code** = `.yosuga/notes/`(知識ノート)と `.yosuga/status.json` を書く。
  指示文は `docs/claude_code_onboarding.md`。

### 未検証・残っている宿題

- **実機(スマホ)側**: Obsidian + Remotely Save で Dropbox 同期の構成
  (⚠ Vault は**共有ストレージ上**に。アプリ内フォルダだと Hub から読めない)。
  共有シートに ChatGPT が出るか。
- ~~**紙エルの status.json**: `questionsForYosuga` の型ゆれ~~ → **2026-07-24 解決**。
  オブジェクト配列 / 単体文字列 / null でも読めるようにした(下記の項参照)。実機での取得は未確認。
- **UI の細部**: 複雑なフォーム(タスク編集/プロジェクト編集/記録追加)は Material の
  ドロップダウン・チェックボックスが残る。AlertDialog も Material のまま(角丸ゼロ・緑)。
  プロジェクト詳細の戻るは旧アイコン(`<BACK` ブロックに揃える余地)。
- **Morning Brief を実データで作り直す**(仮データ一掃後の確認)。
- 将来: MCP(v4 Phase4)/ レコル再導入時のタグ整理。

### すぐ試せること(実機)

- コンソール `> IMPORT RESPONSE` にヨスガの `diary[]` JSON を貼る → 記録タブ「観測」へ。
- 各コマンドで端末ログ + 走査線の演出を確認。
- プロジェクト詳細 → GitHubから更新 で GITHUB FETCH の端末ログ。

---

## 2026-07-24: status.json の `questionsForYosuga` の型ゆれを吸収(紙エル対策)

### 問題

`ProjectStatus.questionsForYosuga` が `List<String>` 固定だったため、ゲーム側の Claude Code が
`[{"id":"q1","question":"..."}]` のようにオブジェクト配列で書くと **kotlinx.serialization が
例外を投げ、status.json 全体が `InvalidJson` になって取得が丸ごと失敗**していた。
質問1項目の書き方の違いで、summary も nextTasks も読めなくなるのは害が大きい。

### 直し方

`FlexibleTextSerializer` / `FlexibleTextListSerializer` を新規追加
(`data/github/model/FlexibleTextSerializer.kt`)。`questionsForYosuga` にだけ適用した。

- 文字列 / 数値 / 真偽値 → そのまま文字列に
- オブジェクト → `question` `text` `title` `summary` … の順で本文らしいキーを探す。
  無ければ、`id` `type` `date` などの管理用キーを除いた最初の非空文字列を拾う
  (`{"id":"q9"}` だけのような中身の無い項目は空になり、既存の空落としで消える)
- 配列 → 各要素を同じ規則で文字列化して `" / "` 連結
- 配列で書き忘れて単体の文字列 / オブジェクトになっていても読む
- null / 該当なし → 空文字(`toSnapshot` が既に空を落としている)

**適用範囲は `questionsForYosuga` のみ**。他フィールドは型ゆれの実例が無いので広げていない
(全体を緩めると、本当に壊れた status.json を「読めた」ことにしてしまう)。

`docs/claude_code_onboarding.md` にも「正しい形は文字列配列」と明記した(Hub 側が
読めることを、そう書いてよい理由にしないため)。

### ついでに直したもの

`ProposalCardUiTest.diary_card_uses_received_date_when_blank` が失敗していた。
「観察日記 → 観測」の表記変更(06bfc54)でテストが取り残されていた **既存の失敗**で、
今回の変更とは無関係。期待値を「観測」に更新した。

### テスト

`StatusParserTest` に **5件追加**(オブジェクト配列 / 未知キーのオブジェクト / 単体文字列 /
空・null・数値の混在)。全体 **354件** 通過。`assembleDebug` 成功。
新規ライブラリなし・Roomスキーマ変更なし。

**未確認**: 実機で紙エルの status.json を実際に取得して読めること。

---

## 2026-07-24: v5 へ方針転換(Obsidian ブリッジ)+ Phase 1-a 実装

### 方針転換

最上位方針を **「AIプラットフォーム」から「情報を確実に運ぶハブ」へ差し替えた**。
設計書: `yosuga_hub_design_v5_obsidian_bridge.md`(新規)。

- 知識の正本を **Room DB → Obsidian の Markdown** へ移す
- 意味付け(分類)は **生成元の Claude Code が出力時点で行う**。Hub は分類し直さない
- **Hub に AI は載せない**。選び・集め・整形し・運ぶだけ
- レコルは**必須コンポーネントから外す**(将来「知識の編集者」として再導入の余地は残す)
- ヨスガの定義は維持(会話の相棒)

v4.1(分類)/ v4.2(指示書)/ v4.3(役割分離)/ ロリポップ同期は **削除しない**。
実機で一周しており、動いているものを剥がすのはリスクだけが増えるため。
v5 では「維持するが必須ではない」に格下げする(設計書v5 §9)。

### ▶ 判断待ち: Dropbox × Android(実機で5分)

**Dropbox の Android アプリは PC と違ってローカル同期フォルダを作らない。**
ファイルの実体はクラウド上にあり、Android から通常のフォルダとして見えるとは限らない。

> 設定 → Obsidian Vault → 「Vaultフォルダを選択」 →
> フォルダ選択画面に **Dropbox が出るか**、出るなら **Vault まで潜って選べるか**

| 結果 | 対応 |
|---|---|
| 選べる | 現状の SAF のまま。読み取りが遅い可能性があるので一覧キャッシュは実装済み |
| 選べない | 端末ローカルに Vault を置き、同期アプリ(Dropsync 等)で Dropbox と同期。**Hub の実装は変わらない** |

`VaultReader` を interface にしてあるため、どちらでも設計は同一。
**この確認を待たずに Phase 1 は進められる**ので先に実装した。

### Phase 1-a 完了(data 層 + 純粋ロジック + テスト)

| 追加 | 役割 |
|---|---|
| `data/obsidian/VaultNote.kt` | ノートのメタ情報 + `VaultListing`(未設定/失敗を区別) |
| `data/obsidian/Frontmatter.kt` | 純粋パーサ。スカラー / リスト / 1段ネスト(`commit.hash`)/ インラインタグ |
| `data/obsidian/LoadedNote.kt` | 本文込みの素データ + `NoteTransformer`(**要約の差し込み口**) |
| `data/obsidian/ContextMarkdown.kt` | 純粋: 選択ノート → コンテキストMarkdown(設計書v5 §4 の形式) |
| `data/obsidian/VaultReader.kt` | 読み取りの抽象化(書き込み側 `KnowledgeStore` と対) |
| `data/obsidian/SafVaultReader.kt` | SAF 実装。`DocumentsContract` で幅優先に再帰列挙 |
| `data/repository/VaultRepository.kt` | 一覧キャッシュ + 明示リフレッシュ + コンテキスト生成 |

- 既存への変更は **`AppContainer` への配線のみ**
- **Room スキーマ変更なし(v8 マイグレーション不要)/ 新規ライブラリなし**
- テスト **22件追加**(`FrontmatterTest` 8 / `ContextMarkdownTest` 8 / `VaultRepositoryTest` 6)。全通過

### 設計判断

- **抽出と要約の境界を `NoteTransformer` 1点に集約した。**
  Phase 1 は `NoteTransformer.Identity`(恒等変換)。Phase 4 で AI 要約を入れるなら
  差し替えるのはここだけで、`ContextMarkdown` は純粋関数のまま触らない。
  `VaultRepositoryTest.transformer_is_the_only_place_that_touches_the_body` で担保。
- **`DocumentFile.listFiles()` を使わず `DocumentsContract` で直接 query した。**
  前者は1件ごとに URI を組み立てるため Vault 規模で遅い。
- **`.` で始まる名前をスキップ**(`.obsidian` / `.trash` を拾わないため)。
- **Vault は読み取り専用**として扱う(Phase 1 では書き込まない)。
  出力ファイル名に日付を入れて衝突させない。
- 暴走防止に上限を設けた: 最大 5,000 ノート / 深さ 12 / 1ファイル 2MB。
- 文字数は **60,000 字で警告のみ**。制限はしない。

### Phase 1-a 実機確認 ✅(エミュレータ / Android 17)

設定画面に **「Vaultを読み取れるか確認」ボタン**を追加した(`SettingsViewModel.checkVault()`)。
「選べた」と「読める」は別なので、選択とは独立に列挙を試せるようにしたもの。

`Documents/TestVault` にテスト用 Vault を置いて確認 → **5件ちょうど検出**。

| 確認項目 | 結果 |
|---|---|
| サブフォルダの再帰列挙 | ✅ `Games/ANRI/Design/...` まで到達 |
| 隠しフォルダの除外(`.obsidian/hidden.md`) | ✅ 拾わない |
| 拡張子フィルタ(`reference.png`) | ✅ 拾わない |
| 日本語ファイル名(`思いつき.md` / `未分類メモ.md`) | ✅ 化けない |

### ▶ Dropbox との同期方式(判断待ち)

**Yosuga Hub 自身は同期しない。** SAF で選んだフォルダを読み書きするだけで、
そのフォルダが Dropbox と同期しているかは Hub の関知するところではない。選択肢は3つ:

| | 方式 | 評価 |
|---|---|---|
| A | Dropbox アプリの SAF を直接読む | 未検証。オフライン不可・低速の懸念。Obsidian モバイルからは使えない |
| B | **端末ローカル Vault + 同期アプリ**(FolderSync / Dropsync 等) | **推奨**。高速・オフライン可。Obsidian モバイルも同じフォルダを開ける |
| C | Hub に Dropbox API を実装 | 却下。OAuth・競合解決を Hub が抱える = v5 §10「Hubは複雑な判断を持たない」に反する |

**いずれを選んでも Hub の実装は変わらない**(`VaultReader` が interface のため)。

### Phase 1-b 完了(UI: 一覧・複数選択・プレビュー・文字数)

| 追加 | 役割 |
|---|---|
| `ui/screen/obsidiancontext/ObsidianContextViewModel.kt` | 一覧読込 / 選択状態 / プレビュー生成 |
| `ui/screen/obsidiancontext/ObsidianContextScreen.kt` | フォルダ見出し付き一覧・チェック選択・プレビューダイアログ |
| `ui/navigation/ObsidianContextRoute.kt` | ネストルート `obsidian_context` |

既存への変更3箇所:
- `YosugaHubApp.kt` … ルート追加 + ネスト時にヨスガタブを選択状態に保つ
- `AssistantScreen.kt` … 「Obsidianの文脈」セクションと遷移ボタンを追加(引数 `onOpenObsidianContext`)
- `SettingsScreen/ViewModel` … 1-a の確認ボタン(前述)

**下部ナビは6個のまま**(7個目にしない)。出力先がヨスガなのでヨスガ画面からのネストにした。

設計判断:
- **選択を変えたらプレビューを捨てる**(古い内容を貼ってしまう事故を防ぐ)。
  `changing_selection_drops_a_stale_preview` で担保。
- **再読込時、消えたノートの選択は落とす**が、残っているものは維持する。
- フォルダ見出しをタップで**フォルダ単位の一括選択/解除**。

テスト **8件追加**(`ObsidianContextViewModelTest`)。全体で **248件** 通過。

### Phase 1-c 完了(コピー / 保存 / 共有)

プレビューダイアログに **[コピー] [保存] [共有]** を追加。

| 追加 | 役割 |
|---|---|
| `data/file/DocumentWriter.kt` | `TextDocumentWriter`(interface)+ SAF 実装 |
| `ui/share/ShareMarkdown.kt` | `ACTION_SEND` でコンテキストを渡す |

設計判断:
- **保存は SAF の `CreateDocument` にした。**保存先をその場で選ばせるため
  **FileProvider も追加パーミッションも不要**。manifest は変更なし。
- **共有の MIME は `text/plain`。**`text/markdown` を受け付けないアプリが多いため。
- **書き込みは data 層(`DocumentWriter`)に置いた。**UI から contentResolver を直接触らない
  (CLAUDE.md「UIから直接ネットワークやDBへアクセスしない」)。
- `TextDocumentWriter` を interface にしたのは ViewModel をテストで組み立てられるようにするため。

**これで Phase 1 は完了。** ビルド ✅ / テスト 248件 全通過。

### Phase 1 実機確認(エミュレータ)

| 項目 | 結果 |
|---|---|
| 一覧・フォルダ別表示・複数選択 | ✅ |
| プレビューの中身(v5 §4 の形式) | ✅ 想定どおり |
| コピー → 他アプリへ貼り付け | ✅ |
| 保存(SAF / `yosuga_context_YYYY-MM-DD.md`) | ✅ |
| 共有 | △ 共有シートは正常動作。ただし**エミュレータに ChatGPT 未インストール**のため候補に出ず。実機待ち |

※ "no recommended people to share with" は Direct Share 欄が空なだけでエラーではない。

### Phase 2-a 完了(絞り込み: 検索 / 最近更新)

**設計判断: ファイルを開かずに判定できる条件だけを先に実装した。**
タグ絞り込みは全ノートの Frontmatter を読む必要があり、SAF 経由では列挙とは別次元の
コストになる(N 件のファイルを開く)。インデックス作成を伴う別機能として後回しにする。

| 追加 | 役割 |
|---|---|
| `data/obsidian/VaultNoteFilters.kt` | `NoteFilter`(query / recentDays)+ 純粋な適用ロジック |

- パス・ファイル名の部分一致(大小文字無視)/ 1・7・30日以内の絞り込み
- **更新時刻が取れないノートは期間絞り込みで除外**する
  (「いつ更新されたか不明」を「最近更新された」に混ぜない)
- **絞り込みは表示だけを変え、選択には触らない。**隠れている選択は件数で明示する
- フォルダ一括選択は**表示中のノートだけ**を対象にする

テスト **13件追加**(`VaultNoteFiltersTest` 8 / ViewModel 5)。全体 **261件** 通過。

### Phase 2-b 完了(JSON出力)

プレビュー内で **Markdown / JSON** を切り替えられるようにした。

| 追加 | 役割 |
|---|---|
| `data/obsidian/ContextFormat.kt` | 形式の enum + `ContextData`(形式に依存しない中間表現)+ ファイル名 |
| `data/obsidian/ContextJson.kt` | Markdown と同じ情報を JSON で出す純粋関数 |

**設計判断: 読み取りと整形を分けた。**
`VaultRepository` を `loadContext()`(ファイルを読む)と `format()`(純粋)に割った。
おかげで**形式を切り替えてもファイルを読み直さない**。
`switching_format_reformats_without_reading_files_again` が読み取り回数で担保している。

- 正本は Markdown。JSON は機械処理向けの追加であって置き換えではない(v5 §6)
- `CreateDocument` は MIME を生成時に固定するため、保存ランチャーを形式ごとに用意した
- `ContextBuildResult.markdown` → `.content` へ改名(JSON も入るため)

テスト **9件追加**(`ContextJsonTest` 7 / ViewModel 2)。全体 **270件** 通過。

### 仮データ(SampleSeed)の後片付け: **選択A で実装完了**

長く未決だった A/B の宿題。**A(削除機能 + 再シード防止)**を実装した。

| 追加 | 役割 |
|---|---|
| `data/repository/SampleDataRepository.kt` | 残存件数の集計 + ID 指定での削除 |
| `SampleSeed.projectIds / taskIds / itemIds / diaryIds` | 後片付け用の ID 一覧 |
| DataStore `seeding_disabled` | 再投入の停止フラグ |
| 設定画面「サンプルデータ」 | 残存件数の表示 + 確認ダイアログ付き削除 |

設計判断:
- **「テーブルを空にする」ではなく ID を指定して消す。**
  ユーザーが作った行や AI 取り込みで増えた行を巻き込まないため。
- **タグ・エンティティは「どこからも参照されなくなったもの」だけ消す**
  (`deleteTagIfUnused` / `deleteEntityIfUnused`)。実データのタグを道連れにしない。
- **recommendations は自動採番のため `projectId + title` で特定**して消す。
- **プロジェクトは既定で残す。**確認ダイアログのチェックで明示的に選んだときだけ消す。
  ANRI / 紙装甲主人公と不死身のカエル / げんげきょう が実在するゲームである可能性があり、
  消すと GitHub 設定などもやり直しになるため。消す場合は紐づくタスクと進捗キャッシュも消す。
- **削除後に `seeding_disabled` を立て、`seedIfEmpty()` を完全に止める。**
  これが無いと「消しても次回起動で復活する」問題が残る。

テスト **4件追加**(`SampleSeedIdsTest`: ID の網羅・一意性・タスクの親子関係・提案の特定可能性)。
全体 **274件** 通過。

**未検証: 実機での削除動作**(次回、設定画面から実行して確認する)。

### プロジェクト削除UI 実装(A の残り)

長らく「削除手段が無い」ままだったプロジェクトを、**個別に削除できる**ようにした。
プロジェクト詳細 → 「プロジェクトを編集」の隣に「削除」。確認ダイアログ付き。

| 追加 | 役割 |
|---|---|
| `ProjectRepository.delete()` | プロジェクト行だけを消す |
| `TaskRepository.deleteByProject()` | 配下タスクの後始末 |
| `ProjectStatusRepository.deleteCache()` | GitHub 進捗キャッシュの後始末 |
| `GroupedTasks.size` | 確認文の件数表示用 |

設計判断:
- **タスクと進捗キャッシュを先に消してから本体を消す。**
  逆順だと途中で失敗したときに親のいないタスクが残る。
- 削除後は `onBack` で一覧へ戻る(消えたプロジェクトの画面に留まらせない)。
- 確認ダイアログに**消えるタスク件数を出す**(戻せないため)。

テスト1件追加。全体 **275件** 通過。**実機での削除動作は未検証。**

### Phase 2 完了(出力履歴 / タグ絞り込み)

#### 出力履歴

| 追加 | 役割 |
|---|---|
| `data/file/ContextHistoryNames.kt` | 履歴ファイル名の純粋ロジック + ディレクトリ外参照の番人 |
| `data/repository/ContextHistoryRepository.kt` | 控えの保存・一覧・読み出し・削除・古い分の間引き |

設計判断:
- **記録するのはプレビュー生成時ではなく「実際に外へ出した時」**(コピー/保存/共有)。
  プレビューは何度も作り直すため、全部残すと「何を渡したか」が読み取れなくなる。
- 保存先はアプリ専用領域 `context_history/`。**最新20件で頭打ち**にする
  (コンテキストは大きくなりうるので溜め続けない)。
- ファイル名の正規表現を通らないものは読ませない(取り込み履歴と同じ番人方式)。

#### タグ絞り込み

| 追加 | 役割 |
|---|---|
| `TagIndex`(VaultNoteFilters.kt) | 相対パス → タグ の対応表 |
| `VaultRepository.buildTagIndex()` | 全ノートを開いて索引を作る |

設計判断:
- **索引は明示的に作らせる。**タグは本文を開かないと分からず、一覧の列挙
  (フォルダを辿るだけ)とは桁違いに重い。画面表示のたびには走らせない。
  ボタン文言も「タグで絞り込む(全ノートを読みます)」と明示した。
- **複数タグは OR。**AND だと2つ選んだ時点でほぼ0件になり使い物にならない。
- **チップの並びは出現数の多い順**(同数は名前順)。
- 索引が無い状態でもクラッシュせず、他の条件だけが効く。

テスト **12件追加**(`ContextHistoryNamesTest` 5 / `VaultNoteFiltersTest` 7)。全体 **287件** 通過。

**これで設計書v5 の Phase 1・Phase 2 は実装完了。**

### 実機確認 ✅ 全項目パス(エミュレータ / Android 17)

| 確認項目 | 結果 |
|---|---|
| プロジェクト削除(げんげきょう) | ✅ 一覧へ戻り消えている |
| サンプルデータ削除 | ✅ |
| **削除後にアプリ再起動 → 復活しないか** | ✅ **「残っていません。再投入も停止済みです。」** |
| 出力履歴(コピー → 履歴に残る) | ✅ |
| タグ絞り込み(索引作成 → チップ) | ✅ |

**これで長らく残っていた「仮データが消しても復活する」問題は解消。**
v5 Phase 1・Phase 2 も実機で通しで動作を確認できた。

### ⚠ 環境メモ: エミュレータの黒画面

確認の途中でエミュレータの画面が真っ黒になり、`Process system isn't responding` に陥った。
**アプリの問題ではない**(`uiautomator dump` では UI ツリーが取れていた)。
ハードウェア描画(ホストGPU経由)が原因。次で復旧する:

```
adb.exe emu kill
cd <SDK>/emulator && ./emulator.exe -avd Pixel_10 -no-snapshot-load -gpu swiftshader_indirect
```

Cold Boot はアプリのデータを消さない(消えるのは Wipe Data)。
恒久対処は Device Manager → Edit → Graphics → 「Software - GLES 2.0」。

### 追撃: 仮の**予定**が消し漏れていた

実機確認のあとホーム画面を見ると、タスク・アイテム・日記は消えているのに
**「今日の予定」に歯医者 / ANRI 作業時間 / 買い物 / げんげきょう ミーティングが残っていた。**

原因: `seedIfEmpty()` は現在カレンダーをシードしないが、**過去のバージョンが入れた行が
DB に残っている**。`SampleDataRepository` はカレンダーを見ていなかった。
このままだと設定画面が「残っていません」と言うのに Morning Brief には架空の予定が載る。

対処: `SampleSeed.events` を **bucket + title + start** で特定して削除する
(自動採番のため ID が使えない)。件数表示・削除結果にも「予定」を追加。

※ 端末カレンダーと同期すれば洗い替わるが、**同期前・権限拒否のときは残り続ける**ため、
  削除機能側でも面倒を見る必要がある。

テスト1件追加(全 **288件** 通過)。

### エミュレータの黒画面: 真因はスナップショットだった

`-gpu swiftshader_indirect` で直ったので描画方式が原因かと思ったが、**違った**。

- このバージョンの emulator は `swiftshader_indirect` を**無効な値として拒否**し `auto` に戻す
  (有効値は `auto` / `host` / `software` / `swiftshader`)
- `auto`(ホストGPU)のままコールドブートしたら**正常に描画された**

つまり効いていたのは `-no-snapshot-load`(コールドブート)のほう。
`fastboot.forceFastBoot=yes` によるスナップショット復元で、Android が壊れた状態から起き上がっていた。

**最終的な AVD 設定**(`~/.android/avd/Pixel_10.avd/config.ini`、変更前は `.bak-before-gpu-change`):

```
hw.gpu.mode=auto              ← 描画は速いまま
fastboot.forceColdBoot=yes    ← 毎回コールドブート(黒画面の真因を潰す)
fastboot.forceFastBoot=no
vm.heapSize=512               ← 228 から引き上げ
```

この設定で起動15秒・描画正常を確認済み。

### 実機確認 ✅ 予定も含めて仮データは一掃できた

ホーム画面: 「未完了のタスクはありません」/ 今日の予定・次の予定とも空。
タスク・予定・アイテム・日記・提案はすべて消えた。

### ⚠ 残っている架空データ: プロジェクトの中身

プロジェクトは**残す選択**をしたため ANRI / 紙装甲主人公と不死身のカエル が残っているが、
**中身がシードされたままの文言**になっている。

```
ANRI                       作業中: メインシナリオ第2章の執筆   ← 架空
紙装甲主人公と不死身のカエル   作業中: カエルのアニメーション実装   ← 架空
```

これはホーム画面にも AI 向けエクスポートにも載るため、Morning Brief に混ざる。

**しかも現状では編集できない。** `ProjectEditDialog` は name / currentGoal / health /
GitHubリポジトリ情報しか編集させておらず、`inProgress` / `nextTask` は
「タスクからの導出へ置き換える予定」として意図的に外してある(v3-Step 1 の判断)。

対応案(未決):
- **A. 削除時に空にする** — プロジェクトを残す選択をしたとき、シード由来の
  currentGoal / inProgress / nextTask / lastUpdated を空にする。名前だけの器にする
- **B. 編集できるようにする** — ProjectEditDialog に inProgress / nextTask を足す
- **C. 当初の予定どおりタスクから導出する** — 一番きれいだが変更は大きい

### 選択A で実装(プロジェクトの中身を空にする)

`ProjectDao.clearSeededText()` を追加。サンプルデータ削除時、**プロジェクトを残す選択をした場合は
「目標 / 作業中 / 次」を空にする**。

設計判断:
- **シード時の値と一致するときだけ更新する。**ユーザーが既に書き換えていたら触らない
  (`WHERE currentGoal = :currentGoal AND ...`)。他の削除と同じ「実データを巻き込まない」方針。
- `lastUpdated` と `health` は触らない。架空の"主張"ではなく状態値のため。
- 設定画面の件数にも「中身が仮のプロジェクト N件」を出す。空にすべきものが残っていれば
  ボタンが出続ける。

`inProgress` / `nextTask` を**タスクから導出する(案C)のは Phase 3 以降の宿題**として残す。
設計当初からその予定だったので、A は暫定処置。

テスト総数 288件 通過。

実機確認 ✅ ホーム画面の「ゲーム進捗」から架空の文言が消えた。
**これで Hub に架空データは残っていない。**

### 運用ドキュメントを v5 へ追随 + Phase 3 の前提を確定

v5 の核心は「意味付けは生成元でやる」。各ゲームの Claude Code が Frontmatter 付き
Markdown を出さなければ、Phase 3 を作っても振り分ける中身が来ない。まずここを定義した。

- `docs/claude_code_onboarding.md` に「v5: 知識ノートの出力」を追加
  (status.json との役割の違い / Frontmatter 規則 / 本文構成 / ファイル名規則)
- `docs/yosuga_prompt.md`: 入力に Obsidian コンテキストが加わった旨 +
  「渡されるのは原文の一部。書かれていないことを断定しない」注意
- `docs/recoru_prompt.md`: v5 でレコルが必須から外れた旨

#### ▶ 決定事項(これで Phase 3 の前提はすべて揃った)

| 論点 | 決定 |
|---|---|
| **知識ノートの受け渡し** | **リポジトリ経由(B)**。`.yosuga/notes/` に置き、**Hub が GitHub 経由で取得して Vault へ書く** |
| Vault のフォルダ構成 | 設計書v5 §6 のとおり |
| 保存先の決まり方 | Frontmatter の `type` で機械的に決定。不明なら `Inbox/` |
| ゲームの識別子 | 既存 projectId を流用: `anri` / `paper-armor-frog` / `gengenkyo` |
| ファイル名 | `YYYY-MM-DD-<英語スラッグ>.md`。上書き禁止 |

**設計判断: Frontmatter に `project_id` を追加した。**
`game`(表示名)は改名されうるが、Hub が振り分けに使うキーは安定していなければならない。
表示名だけだと改名のたびに振り分けが壊れる。

**設計判断: Claude Code に Vault の場所を知らせない。**
B を選んだ最大の理由。A(PC上の Vault へ直接書く)だと各ゲームの CLAUDE.md に
PC 固有の絶対パスが載り、マシンが変わると壊れる。
B なら Claude Code は「`.yosuga/notes/` に書いて push」だけでよい。

**運用ルール: 一度 push したノートは書き換えない。**
Hub 側を「取得済みは再取得しない」設計にするため、後から直しても Vault へ反映されない。
訂正は新しいノートを書き、旧ノートを参照する。

#### Phase 3 の分割(設計書v5 §8 に追記)

| | 内容 |
|---|---|
| 3-a | 各リポジトリの `.yosuga/notes/` を GitHub から一覧・取得 |
| 3-b | `type` から保存先を決めて Vault へ書く。取得済みは再取得しない |
| 3-c | 取り込み結果の画面(何を・どこへ / Inbox 行きの件数) |
| 3-d | ヨスガ会話ログの取り込み(独立モジュール) |

### Phase 3-a 完了(GitHub から知識ノートを取得)

| 追加 | 役割 |
|---|---|
| `GitHubApi.listDirectory()` | ディレクトリ一覧の取得。`GitHubApi.NOTES_PATH = ".yosuga/notes"` |
| `data/github/NoteListParser.kt` | 一覧JSONの純粋パーサ + `RemoteNote`(name/path/sha/size) |
| `data/repository/RepoNoteRepository.kt` | 一覧 → 未取得だけ本文取得。`NoteFetchResult` |

設計判断:
- **取得済み判定はブロブSHA。**Git のブロブSHAは内容が変わらない限り不変なので、
  ファイル名や更新日時より確実。「一度 push したノートは書き換えない」運用と対になる。
- **判定材料(`knownShas`)は呼び出し側から渡す。**永続化の持ち方を 3-a に持ち込まない
  (どこに記録するかは 3-b で決める)。
- **`.yosuga/notes/` が無い(404)のはエラーではない。**まだノートを書いていないゲームでは
  普通に起こるので `NoNotesDirectory` として区別する。
- **1件の本文取得が失敗しても全体を止めない。**`failed` に積んで次回また拾う。
- 1回の取得は**最大30件**。API を叩きすぎない歯止め。
- ディレクトリ一覧は `Accept: application/vnd.github+json`(本文取得の `raw` とは別)。
  サブディレクトリと `.md` 以外は無視するので、ノート置き場に何が置かれても落ちない。

テスト **16件追加**(`NoteListParserTest` 8 / `RepoNoteRepositoryTest` 8、MockEngine)。
全体 **304件** 通過。**実通信は未検証**(GitHub 連携全体がまだ実通信未確認)。

### Phase 3-b 完了(振り分け + Vault へ書き込み)

**Room v8**(`imported_notes`)を追加。マイグレーション手順4つ(定義 / version / addMigrations /
スキーマJSON)はすべて実施済み。`8.json` 出力を確認、`MigrationTest` も v8 へ更新。

| 追加 | 役割 |
|---|---|
| `entity/ImportedNoteEntity` + `dao/ImportedNoteDao` | 取り込み記録(**主キー = ブロブSHA**) |
| `MIGRATION_7_8` | `imported_notes` の作成 |
| `data/obsidian/NoteRouter.kt` | `type` → 保存先の純粋ロジック |
| `data/obsidian/VaultWriter.kt` | `SafVaultWriter`。サブフォルダを作りながら新規作成 |
| `data/repository/NoteImportRepository.kt` | 取得 → 振り分け → 書き込み → 記録 |

設計判断:
- **取り込み記録は Room。**件数が増える前提で、3-c の結果画面でもそのまま使える。
  v5 §10「すべての自動処理に元ファイルと処理日時を残す」に沿って
  `sourcePath` と `vaultPath` の両方を持つ。
- **`project_id` が無いノートは Inbox 送りにしない。**どのリポジトリから取ったかは
  分かっているので、そのプロジェクトのものとして扱う。
  ただし**宣言と取得元が食い違う場合は Inbox**(取り違えの可能性があるので人に見せる)。
- **既存ファイルを絶対に上書きしない。**同名があれば `-2`, `-3` と枝番を付けて別ファイルにする。
  人が Obsidian で書いたノートを壊さないため(v5 §10)。
- **書き込みに失敗したら記録しない。**次回やり直せるようにするため。
- **Vault 未設定を検出したらそのプロジェクトの処理を打ち切る。**以降も全部失敗するので
  無駄に試さない。
- ファイル名は必ずサニタイズする(`../../etc/passwd` → `passwd.md`)。
  リポジトリ側の名前を信用してディレクトリ外へ書かせない。

テスト **16件追加**(`NoteRouterTest` 10 / `NoteImportRepositoryTest` 6)。全体 **320件** 通過。
**実通信・実機は未検証。**

### Phase 3-c 完了(実行ボタンと結果表示)

ヨスガ画面の先頭に **「ゲームのノートを取り込む」** セクションを追加。
`ui/screen/assistant/NoteImportSummaryDialog.kt` で結果を出す。

設計判断:
- **Inbox 行きは「失敗」ではなく「人が仕分けるもの」として見せる。**
  エラー色にせず、「Obsidian で仕分けてください」と行き先を示す。
- 失敗件数には「次回もう一度試されます」と添える(記録を残していないため事実)。
- プロジェクトごとの内訳を出す。3ゲームのうち1つだけ設定済み、という状況が普通なので
  「リポジトリ未設定」「ノートなし」も**エラーではない表示**として区別する。

**これで Phase 3-a〜3-c が繋がり、画面から実行できるようになった。**

### ▶ 次回ここから: GitHub 連携の実通信(初)

ANRI に知識ノートが push 済み。アプリ側の設定は2か所:

1. **設定 → GitHub アクセストークン**
   非公開リポジトリなので必須。fine-grained PAT なら
   Repository access で該当リポジトリを選び、**Permissions → Contents: Read-only**。
   classic PAT なら `repo` スコープ。
2. **プロジェクト → ANRI → プロジェクトを編集** で
   **リポジトリのオーナー / リポジトリ名 / ブランチ** を入力。

確認の順序:
- まず **プロジェクト詳細の「GitHubから更新」**(status.json 取得)で疎通を見る。
  `.yosuga/status.json` が無ければ「ファイルが見つかりません」になるが、
  **401/403 でなければ認証は通っている**ので切り分けになる。
- 次に **ヨスガ画面 →「ノートを取り込む」**。
- Vault(TestVault でよい)に `Games/ANRI/...` が作られるか、
  設定 →「Vaultを読み取れるか確認」で件数が増えるかを見る。

⚠ **GitHub 連携は今日まで一度も実通信していない。**ここが最初の実地確認になる。

### GitHub 実通信 初成功(部分)

ANRI で「GitHubから更新」を実行 → **`status.json を読み取れませんでした`**(= `InvalidJson`)。
これは **HTTP 200 が返った**ということで、以下がすべて通ったことを意味する:
トークン / オーナー名 / リポジトリ名 / ブランチ / ファイルの存在。
**GitHub 連携は今日はじめて実通信できた**(長らく未検証だった項目)。
`status.json` の中身が想定形式でないだけなので、ノート取り込みには影響しない。

ノート取り込みの結果: `ANRI ノートなし` / `紙装甲主人公と不死身のカエル リポジトリ未設定`。
→ 原因は **ノートを push したのが ANRI ではなく Kamieru(paper-armor-frog)のリポジトリ**だった。
Hub の不具合ではない。両ゲームへ依頼済み。

**判明した運用上の注意**: 依頼文の `project_id` は**渡す相手のリポジトリに合わせる**こと。
`NoteRouter` は宣言と取得元の食い違いを Inbox 送りにするため、間違えると全件 Inbox になる。

### Phase 3-d 完了(会話ログの取り込み)

**GitHub に依存しないので、ノート待ちの間に実装できる唯一の残り部分だった。**

| 追加 | 役割 |
|---|---|
| `data/obsidian/ConversationNote.kt` | 貼り付け本文 → 保存用ノート(純粋ロジック) |
| `data/repository/ConversationImportRepository.kt` | `Conversations/Yosuga/` へ保存 |
| ヨスガ画面「会話ログをObsidianへ」 | 貼り付けダイアログ + 結果表示 |

設計判断:
- **v5 §7 のとおり独立モジュールにした。**将来 ChatGPT API 連携へ差し替えるとき、
  ここだけを置き換えればよい。ノート取り込み(3-b)とは経路を共有していない。
- **既に Frontmatter がある場合は二重に付けない。**ヨスガが付けてくる場合があるため。
- ファイル名は最初の見出しから作る(`2026-07-24-今日のまとめ.md`)。
  見出しが無ければ `session`。日本語はそのまま残す(Obsidian で読めるほうを優先)。
- パスを壊す文字は落とし、長い見出しは40文字で切る。
- `SafVaultWriter` は 3-b と共用(既存ノートを上書きしない責務もそこに集約)。

テスト **11件追加**(`ConversationNoteTest`)。全体 **331件** 通過。

`docs/yosuga_prompt.md` の1日の流れを更新:
セッションまとめは **(a) Obsidian へ保存(記録)** と **(b) レコルへ渡す(Hubのデータ更新)** の
両方をやってよい、と目的の違いを明記した。

### 案C 完了(作業中 / 次 をタスクから導出)

長い積み残し(v3-Step 1 から「タスクからの導出へ置き換える予定」だった項目)。
今日の「空にする」暫定処置を、本来の姿へ置き換えた。

| 追加 | 役割 |
|---|---|
| `domain/model/ProjectProgress.kt` | タスク一覧 → `inProgress` / `nextTask` の純粋ロジック |

設計判断:
- **導出は `ProjectRepository.projects()` で一度だけ行う。**
  ホーム・一覧・詳細・AI向けエクスポート・状況JSON はすべてこの Flow を通るので、
  1箇所直せば全部そろう。画面ごとに計算すると必ずどこかで食い違う。
  (`AiExportRepository` / `ExportRepository` も projects() 経由なので**自動的に導出値を見る**。追加配線なし)
- 並び順は `groupTasks` と同じ規約(優先度 → 締切 → タイトル)。
  画面の一番上のタスクと「次」が食い違わないようにするため。
- **「作業中」= doing のタスク(最大3件)/ 「次」= 未着手の先頭。**完了済みは選ばない。
- **タスクが1件も無いプロジェクトは保存済みの文字列を残す。**
  まだタスク管理を始めていないプロジェクトを、導出で空にしないため。
- `projectId` が null のタスク(プロジェクト外)はどのプロジェクトにも混ざらない。

テスト共通化: `ProjectRepository` がタスクを見るようになったため、
関心のないテスト用に `EmptyTaskDao`(空のタスク一覧)を追加。
テスト **9件追加**(`ProjectProgressTest`)。全体 **340件** 通過。

### 潜在バグ修正: GitHub URL のパスがエンコードされていなかった

`GitHubApi.fetchFile` / `listDirectory` が `.yosuga/notes/<ファイル名>` を
**URLエンコードせずに**繋いでいた。ファイル名が ASCII なら問題ないが、
日本語スラッグや空白を含むと URL が壊れる(会話ログ側は日本語ファイル名を許すため現実に踏みうる)。
調査用テストで `.../notes/2026-07-24-光の設計.md` が**生のまま**URLに載ることを確認 → 修正。

- パスを `/` で分割し、区画ごとに `encodeURLPathPart` する(区切りは残す)。
- ASCII パスは従来と同一の URL になる(既存テストは不変)。
- `RepoNoteRepositoryTest` に日本語ファイル名のエンコード確認を追加。
- ※ 以前の「2件失敗」はボタン取り違えが原因で、これとは別。これは未然に潰した潜在バグ。

### README を v5 へ更新

v4.3 のままだったので Obsidian ブリッジ方針・知識ノートの流れ・実装状況(Phase1〜3)・
Room v8 へ更新。設計書の読む順も v5 を先頭にした。

### A運用の確定 + 空フィールドの見た目を整えた

**運用方針を A に確定**(ユーザー選択): タスク・進捗の「正」は各ゲームの
`.yosuga/status.json`(Claude Code が管理)。**Hub は「GitHubから更新」で取得して参照するだけ**。
Hub のローカル項目(目標/作業中/次)は基本使わない。

ANRI / Kamieru の Claude Code へ status.json を正しい schemaVersion:1 形式で
作り直す依頼を送付済み(WORKLOG 外の会話で依頼文を発行)。

そのため Hub のローカル項目は空のことが多くなる。空でも壊れて見えないよう整えた:
- 詳細画面: 目標が空なら「未設定」。作業中/次が両方空なら
  「残タスク・進捗は下の GitHub 進捗に表示されます」と促す。埋まっていれば従来どおり表示
- 一覧・ホーム: 空フィールドは行ごと隠す。作業中は `inProgressLine()` で
  空なら `作業中: -`(「作業中: 」で途切れる見た目を回避)

`inProgressLine()` は `ui/component/ProjectHealth.kt` に共通化。テスト2件追加。全 **342件** 通過。

⚠ 積み残し: A運用では GitHub進捗カードが主役なので、将来
**ローカルの「プロジェクト情報」カードをさらに控えめにする**候補(今回は見た目のみ整えた)。

### UI: 「ボタン」→「コマンド実行行」+ 角ばった状態タグ

ユーザー指摘: まだ Android アプリ感が残る(巨大な塗りCTA)。
「ボタンを押す」より「コマンドを走らせる」印象にする。

- `TacticalButton` / `TacticalOutlinedButton` を **Material Button から自作の
  コマンド実行行へ**作り替え(全画面が自動で切り替わる):
  - 主コマンド `▶ ラベル` … 左に細いアクセント帯 + うっすら地色 + 細枠。塗りCTAは廃止
  - 副コマンド `> ラベル` … トークン + 細枠だけ
  - content 内の Text は `LocalContentColor` / `LocalTextStyle` を継ぐので、
    既存の呼び出し(`{ Text("...") }`)を1つも書き換えずに見た目だけ変わった
- `AssistChip`(丸い Material チップ)を **`[ TEXT ]` の角ばった `StatusTag`** へ全置換

新規ライブラリなし。全349件通過。実機で確認。

### UI: 下部ナビ廃止 → オペレーションコンソール + フォスファーグリーン端末

ユーザー要望: 一般アプリではなく「開発ツール / オペレーションコンソール」。
下部ナビ(ホーム/カレンダー/プロジェクト/記録/ヨスガ)は SNS 的なので**全廃**。
ホーム概念も廃止し、起動画面を**コマンドランチャー**にする。参考画像=緑単色の端末UI。

構造変更:
- `ui/screen/console/ConsoleScreen.kt` … ルート画面。`YOSUGA HUB` + 状態表示
  (PROJECT/PENDING/SYNC/STATUS) + `> COMMAND` の実行行を区切り線で列挙。カード不使用。
  IMPORT NOTES はその場で実行(端末ログ)、他はコマンド実行 or サブ画面を開く。
- `YosugaHubApp` から Scaffold/NavigationBar を撤去。startDestination=console。
  サブ画面は `SubScreenScaffold`(上辺 `<BACK` の反転ブロック + タイトル)で戻る。
- `YosugaDestination` を route だけの enum に(アイコン/ラベル廃止)。
- edge-to-edge のインセットは NavHost に systemBarsPadding で確保。
- 旧 HomeScreen/HomeViewModel は不使用に(削除は次回)。
- `AssistantViewModel` に projectCount / lastSync を追加(コンソール状態表示用)。

配色(参考画像に合わせフォスファーグリーン単色へ):
- **primary をアンバー→グリーン**。アンバーは BUSY/警告のみに降格。
- **白/灰の文字を全廃**。本文=1トーン落ちたグリーン、ラベル=さらに沈めたグリーン、
  項目/値/見出し=明るいグリーン。区切り線も緑がかった暗線に。
- `<BACK` を緑の反転ブロック(黒文字)に。LED を丸ドットに。
- 各サブ画面の内部タイトル(設定/記録/…)はバーと重複するので削除。

新規ライブラリなし。全349件通過。実機で全画面確認。

### 端末ログ演出を全操作へ拡張(B/C) + 端末部品の共通化

A(ノート取り込み)だけだった「システムが動いている演出」を、他の操作にも広げた。

共通化:
- `ui/component/OpTerminal.kt` … 汎用の端末ログパネル(title 可変)。LogLine/LogTone もここへ集約。
  旧 `ImportTerminal` は削除。
- `ui/component/OpLogState.kt` … VM が1つ持つログ流し込みヘルパ。
  `run { emit -> 本物の処理; emit(line) }` で各行にタメを入れて流す。多重起動を無視。

適用(すべて本物のログ):
- **SAVE SESSION**(会話ログ保存) / **EXPORT STATUS**(状況JSON生成) … コンソール上の共通端末に流す
- **BUILD CONTEXT**(Obsidianコンテキスト生成) … 読込数・結合文字数・形式を流す
- **GITHUB FETCH**(status.json 取得) … CONNECT/FETCH/200・404・401 等を流す(結果で色が変わる)

構造整理:
- 旧ヨスガ画面(AssistantScreen)はコンソールとコマンドが重複するため、**REVIEW(提案確認)専用**に縮小。
- 文言: コンソール見出しを `YOSUGA HUB`→`Yosuga`、`ChatGPTとの会話`→`ヨスガとの会話`、
  `ChatGPTへ貼るMarkdown`→`ヨスガへ貼るMarkdown` に修正。

新規ライブラリなし。全349件通過。実機で各演出を確認。

### 仕上げ3点(掃除 / スキャンライン / 入力欄の端末化)

1. **不要ファイル削除**: 未使用の `HomeScreen` / `HomeViewModel` を削除
   (コンソール化で不要に)。`TodayFocus` は純粋ロジックとして残す。
2. **スキャンライン**: `OpTerminal` に CRT 走査線を追加(`Modifier.scanlines`)。
   常時=3px 間隔の薄い横縞、実行中=明るい掃引ラインが上→下へ動く(データ転送感)。
   いずれも低アルファで可読性は保つ。
3. **入力欄の端末化**: `TerminalField`(浮くラベルをやめ `> ` プロンプト +
   プレースホルダ・等幅・緑・直角)を追加。貼り付けダイアログと検索欄に適用。
   ※ ドロップダウンのアンカー欄や isError/supportingText 付きの欄は
     TerminalField の対象外(そのまま OutlinedTextField)。一括置換は行き過ぎたので
     プレーンな入力だけに絞った。

新規ライブラリなし。全349件通過。実機で確認。

### 役割の訂正: 観測日記を書くのはヨスガ(レコルではない)

ユーザー指摘で判明。**観測日記(旧・観察日記)を書くのはヨスガの仕事。**
ヨスガがボイスチャットのログや Hub の JSON を材料にシロを観察して書く。
レコルは情報の仕分け役で、日記は書かない。ドキュメントが逆だったので訂正。

取り込み経路(ユーザー選択A): **ヨスガが直接 diary[] の回答JSONで出す** →
コンソール `> IMPORT RESPONSE` に貼る → 承認 → 記録タブ「観測」へ。レコルを通さない。

- `docs/yosuga_prompt.md`: 役割に「観測日記を書く」を追加。
  「観測日記」節を新設(diary[] の出力仕様・規則・貼り付け先)。
- `docs/recoru_prompt.md`: 「観察日記を書く(主語はレコル)」を削除。
  schema の diary に「書くのはヨスガ。レコルは代筆しない」注記。
- 記録タブのラベルは「観測」に変更済み(UIのみ。DBキーは diary のまま)。

アプリ実装の変更なし(既存の IMPORT RESPONSE 経路がそのまま使える)。

### UI方針を Brutalist / 仕事道具 へ転換

ユーザー指摘: 前のネオンUIは「洗練されすぎ・お洒落すぎ」。
目指すのは映画的サイバーパンクではなく **開発現場の武骨なユーティリティ**
(VS Code / Wireshark / nmap TUI のような仕事道具)。方針を全面差し替えた。

適用:
- 配色を **Amber(主)/Cyan/Green/Red の4色のみ・低彩度・グラデ禁止**、地は #0A0C0E。
  マゼンタ廃止。
- **角丸を全廃**(`Shapes` を全て 0dp)。カード・ダイアログ・テキスト欄が直角に。
- 書体を**全面等幅**(日本語は既定へフォールバックし可読性維持、ASCIIが等幅化)。密度UP。
- `SectionCard` を**計器パネル**へ: 塗り+影のカードをやめ、細枠 + 見出し帯(LED + ラベル + 区切り線)。
- ボタンを**直角・等幅・低め**に(`TacticalButton` / `TacticalOutlinedButton` ラッパー、
  全画面を一括変換。`colors=` 未使用で機械置換が安全だった)。
- 下ナビの**ピル型インジケータを撤去**、選択はアンバー文字 + 上辺に1px線。
- `StatusLed` / `AsciiDivider` を追加。ImportTerminal も新配色へ(マゼンタ→アンバー)。

新規ライブラリなし。全 349 件通過。実機で全画面の見た目を確認。

### サイバーパンクUI化 開始(v5 UI刷新)

ユーザー要望: 取り込み・仕分け時のハッキング風演出 + 全体をサイバーパンクに。
選択: ネオン(シアン×マゼンタ)/ アプリ全体ダーク固定 / 本物のログを流す / 少しタメる。
段階: A(ノート取り込み)→ B(会話ログ・文脈)→ C(GitHub更新)。今回は **A まで**。

**テーマ(第1弾)**: `Color.kt` / `Theme.kt` / `Type.kt` / `themes.xml` を差し替え。
ダーク固定・ダイナミックカラー無効・見出しは等幅。新規ライブラリなし。実機確認済み。

**ハッキング演出(A)**:
- `data/repository/ImportEvent.kt` … 取り込みの進捗イベント(本物の処理を1件ずつ)
- `NoteImportRepository.importAll(onEvent)` … **suspend コールバックを追加**。
  既定は何もしないので、演出無しの呼び出し・既存テストに影響しない。
  実処理イベント(Connect/Target/Scan/Fetch/Route/Written/Skip/Fail/Done)を emit
- `ui/.../ImportLog.kt` … イベント → 端末ログ行(文字列+種別)の純粋整形。テスト可能
- `ui/.../ImportTerminal.kt` … 端末風パネル。種別で色分け・最新行へ自動スクロール・
  カーソル点滅。ViewModel が各行に 160ms のタメを入れて1行ずつ流す
- **演出は本物**: ログのファイル名・振り分け先は実際に処理したものと一致する

テスト **6件追加**(`ImportLogTest`)。全体 **349件** 通過。実機で演出確認済み。

### v5 Phase 3 実機で端から端まで成功 ✅

各ゲームの Claude Code が `.yosuga/notes/` に push → ヨスガ画面「ノートを取り込む」で
**取り込み成功**。GitHub 取得 → 振り分け → Vault 書き込み → 記録が一周した。
**GitHub 連携の実通信も、これが初の完全成功。**

途中「0件成功・2件失敗」が出たが、原因は**ボタンの取り違え**
(プロジェクト詳細の「GitHubから更新」= status.json と、ヨスガ画面の「ノートを取り込む」
= notes を混同)。実装のバグではなかった。診断ログを入れて確認 → 正しいボタンで成功 → ログ除去。

※ UI 改善メモ: 2つの取得ボタンが別画面にあり紛らわしい。将来ラベルや配置を見直す候補。

### 次にやること(実機が使えるようになったら)

- **会話ログ保存の実機確認**(ヨスガ画面 →「会話ログを保存」→ 貼り付け → Vault を確認)
  ※ GitHub 不要なので単体で試せる
- 両ゲームのノートが push されたら **「ノートを取り込む」** を再実行
- **Morning Brief を作り直す**(実データ + 導出された「作業中/次」で組み立てられるか)
- **案C の副作用チェック**: タスクを追加・完了させたとき、プロジェクトの「作業中/次」が
  即座に追従するか(Flow なので自動のはず)。実機で目視確認。レコルが実データだけを見て要約するか確認
  (これまでは中身がほぼ全部仮データだった)
- **Phase 3**: GitHub取得データのObsidian自動保存 / Claude Code出力の自動振り分け /
  会話ログ取り込み / 重複検出
  - ⚠ **着手前に Vault のフォルダ構成を確定する必要がある**(設計書v5 §6 の案でよいか)
  - ⚠ Hub から Obsidian へ**書き込む**フェーズなので、実運用の Vault を対象にする前に
    テスト用 Vault で通すこと
- **実機(スマホ)側の宿題**: Obsidian + Remotely Save で Dropbox 同期 /
  共有シートに ChatGPT が出るか
- **Phase 2 の残り**: 出力履歴 / タグ絞り込み(インデックス作成が必要)
- **Phase 1 の実機通し確認の残り**(共有シートに ChatGPT が出るか)
- **Phase 2**: タグ抽出 / 日付範囲 / 最近更新 / JSON出力 / 出力履歴
- 並行: Obsidian モバイル + Remotely Save で Dropbox 同期の構成を作る
  (⚠ Vault は**共有ストレージ上**に作ること。アプリ内フォルダだと Hub から読めない)
- 上記と独立に、**SampleSeed(仮データ)の A/B 判断は未決のまま残っている**(下記参照)

---

## 2026-07-24: Morning Brief 実地確認 → 仮データ(SampleSeed)が残っている問題が判明

### Morning Brief は成功

レコル(カスタムGPT + Actions)に Morning Brief を生成させ、狙いどおりの構成・情報量で出力された
(昨日の成果 / 今日の予定 / 未完了タスク / 最新の決定事項 / 現在の課題 / 注意事項 / シロの状態)。
**v4.3 の朝のフローは成立している**。

### ただし中身がほぼ全部 SampleSeed だった

出力された内容を照合したところ、大半が初回起動時に投入される仮データだった:

| Brief の項目 | 実体 |
|---|---|
| カエルの歩行モーション / 戦闘バランスの調整 / TGS出展資料 / 序盤マップ | SampleSeed.tasks |
| 歯医者 10:00-11:00 / Pixel 10a 受け取り | SampleSeed.events |
| 「設計をv3へ転換」等の決定事項 | SampleSeed.knowledgeItems |

**レコルの動作は正しい**(Hubにあるものを忠実に要約した)。問題は Hub の中身。
このまま貼ると、ヨスガが架空の予定・タスクを前提に伴走を始めてしまう。

※ CLAUDE.md の「仮データはすべて解消済み」は**画面のプレースホルダ表示**のことで、
  DB にシードされる行のことではなかった。表現が紛らわしいので注意。

### 削除手段の現状

| データ | 削除 |
|---|---|
| タスク | ✅ タスク編集ダイアログ |
| 知識アイテム | ✅ 記録タブ |
| カレンダー | ✅ 端末カレンダーと同期すれば洗い替え(READ_CALENDAR 許可が必要) |
| **プロジェクト** | ❌ **削除手段が無い**(編集のみ) |

さらに `AppContainer.seedIfEmpty()` は**テーブルが空なら投入する**作りのため、
手で消しても・アプリのデータを消して入れ直しても**仮データが復活する**。

### ▶ 次回ここから: ユーザーの選択待ち

**A. 「サンプルデータを削除」を設定に追加**(推奨)
- 消したあと二度とシードしないフラグ(DataStore)を立てる
- プロジェクトの削除手段も同時に用意する
- 実データだけの状態から実運用を始められる

**B. シード自体をやめる**
- 新規インストール時に何も入らない。実装は最小
- ただし既に DB にある仮データは手で消す必要があり、プロジェクトは消せないまま

どちらも小さい変更。選択後すぐ着手できる。

### カレンダーについての補足(切り分け)

エミュレーターで一度もカレンダー同期していない(権限を許可していない)なら、
シード時の行が残ったままになる。カレンダー画面で同期すれば端末の実カレンダー
(エミュレーターなら空)で洗い替えられる。
同期が失敗・権限拒否のときはキャッシュを touch しない設計なので、仮データが残る。

---

## 2026-07-24: status.json の decisions が捨てられていた問題を修正

### 見つけた問題

`docs/claude_code_onboarding.md` は各ゲームの Claude Code に
「重要な設計判断は decisions へ記録する」と指示しているのに、
**Hub は `StatusDecision` をパースするだけで、どこにも使っていなかった**
(スナップショットにも、画面にも、AI向けJSONにも流れていない)。

v4.3 で実害がある: レコルが projects.json を読んで提案するとき、
**ゲーム側で確定済みの設計判断が見えない**ため、それに矛盾する提案をしかねない。

### 実施内容

- `ProjectStatusSnapshot.decisions` を追加(既定値 emptyList のため既存コードは無影響)
- `StatusSnapshotMapper`: 空タイトルは落とし、日付を詳細の先頭に置く
  (`2026-07-20 / 操作を単純に保つため` の形)
- `ProjectExport.decisions` を追加 + statusMarkdown に `## Decisions` 節
  → **状況JSON と AI向け projects.json の両方に載る**
- プロジェクト詳細画面に「決定事項」を表示
- `docs/recoru_prompt.md`: decisions は**ゲーム側で既に確定した設計判断**であり
  「これに矛盾する提案をしない。見直しが必要なら勝手に覆さず会話で問題提起する」と明記

### 保留(ユーザー判断待ち)

decisions を**承認待ち提案として流し込む**案は見送った。同じ決定を毎回取り込まないための
照合キー(projectId + date + title?)の設計が要り、運用の好みにも依存するため。
まずは「読める」状態にして、必要になったら提案化を検討する。

### テスト

- `ContextExporterTest` / `ProjectStatusRepositoryTest` に decisions の検証を追加
  (空タイトルを落とす / 日付の整形 / Markdown と JSON の両方に出る)

---

## 2026-07-24: 回答JSONの貼り付け取り込み

### 経緯(実運用で見つかった摩擦)

レコル(カスタムGPT)は会話内でダウンロード可能なファイルを生成できないことが判明。
従来の取り込みはファイル選択(SAF)のみだったため、
「コピー → どこかに保存 → ファイル選択」という迂回が毎回必要だった。

### 実施内容

- `PastedJson.normalize`(純粋ロジック): コードブロックの囲い(``` / ```json)を外す。
  閉じ忘れのコピーにも耐える。JSON文字列内のバッククォートには触れない
- `ImportRepository.importResponseText(raw)`: 正規化 → 空なら InvalidJson →
  以降は**ファイル取り込みと完全に同じ経路**(履歴保存・承認待ち・分類適用・自動同期)。
  `importResponse(uri)` はファイルを読んで委譲するだけに
- `PasteImportDialog`(共通コンポーネント)をホームとヨスガ画面の両方に配線。
  既存ボタンは「回答JSONを取り込む(ファイル)」と改名して併存

### テスト

- `PastedJsonTest` 7件(素通し / 言語タグ付きフェンス / タグなし / 閉じ忘れ /
  前後空白 / JSON内のバッククォート非破壊 / 空入力)

---

## 2026-07-23: v4.3 AI役割分離 — ヨスガ(会話)/ レコル(Hub管理)+ Morning Brief

### 経緯(ユーザー指示)

実地検証で判明した制約 —— カスタムGPT(Actions)はHubを読めるが会話・ボイスのログを
共有せず、通常のChatGPTは会話を持つが認証付きURLへ到達できない —— を受け、
ユーザーがAIの役割分離を指示(指示本文は `yosuga_hub_design_v4_3_role_split.md` に全文)。

- **ヨスガ**: 会話の相棒(通常ChatGPT・ボイス可)。Hubに触れない。JSONを書かない
- **レコル**: Hub管理者(カスタムGPT+Actions)。分類・指示書・diary・projectHealth・
  Morning Brief 生成。すべてのJSON更新を担当
- **Morning Brief**(新概念): レコルがHubを読んで1〜5KBの要約を生成 →
  シロがヨスガへ貼って1日を開始
- 設計思想: **Hub=長期記憶 / ヨスガ=短期思考 / レコル=長期記憶の管理者**

### 実装との対応(付録に詳細)

- **アプリ実装の変更なし**。レコルの「Hub更新」は既存の
  回答JSON→取込→承認→自動再同期の経路をそのまま使う(サーバー直接書込はしない
  —— 提案→承認→保存の原則と「Roomが唯一の正」を守る)
- Morning Brief はプロンプトで実現(アプリに brief 生成は持たせない。様子見)
- 観察日記の主語はレコルへ
- アプリUI「ヨスガ画面」等の文言は当面そのまま(改名は実機検証後・積み残しへ)

### ドキュメント

- `yosuga_hub_design_v4_3_role_split.md` 新規(指示本文 + 現状実装との対応)
- `docs/recoru_prompt.md` 新規: 回答JSON v2 全書式・分類・指示書・取り込み仕様表を
  旧ヨスガプロンプトから移設 + Actions の使い方 + Morning Brief 生成手順
- `docs/yosuga_prompt.md` を会話の相棒用に全面書き換え
  (Morning Brief の受け取り / セッション終了時の「セッションまとめ」出力。
  旧運用は手動ブリッジのフォールバックとして参照可能と注記)
- `server/README-server.md` の Actions 手順の参照先を recoru_prompt.md へ

### テスト

- 実際にヨスガ(当時。今後はレコルの役目)が Actions 経由で返してきた回答JSONを
  そのまま回帰テストに追加(`ClassificationImportTest.real_response_from_yosuga_is_applied`)。
  空配列だらけ / confidence 0.1 / 要約に別言語の文字が混入、という実データ特有の形が
  取り込み経路を通ることを固定

---

## 2026-07-23: GPT Actions からの取得に成功 — v4 の AI 連携が実際に成立

`file=index` の呼び出しが成功し、7ファイルの一覧を ChatGPT 側で取得できた。
**v4 設計の中核(Hubが「AIのデータインターフェース」を持つ)が実運用で成立した**。

### 経路の確定

```
Yosuga Hub --(POST + X-Yosuga-Token)--> upload.php --> data/*.json
                                                          |
ChatGPT (カスタムGPT + Actions) --(GET + X-Yosuga-Token)--> api.php
```

通常のチャットのブラウズは検索インデックス経由で到達できなかったが、
**Actions は本物のHTTPリクエストを送る**ため解決した。

### つまずいた点(次に同じことをする人向け)

- **スキーマを貼るだけでは認証は有効にならない**。GPTの
  「アクション → 認証 → APIキー → 認証タイプ: カスタム → ヘッダー名 `X-Yosuga-Token`」
  で値を別途登録する必要がある。未設定だと api.php が `forbidden` を返し続ける
  (通信自体は届いているので、Actions の設定ミスと切り分けにくい)
- GPT Actions のバリデータは OpenAPI より厳しい:
  `openapi` は 3.1.0/3.1.1 のみ / `description` は300字以内 /
  `components.schemas` 必須 / `type: object` には `properties` 必須
- 切り分け用に `api.php?file=selftest` を追加(トークン不要。有無・長さ・一致のみ返す)

### 副次的な利点

トークンを**ヘッダーで渡せるようになった**ため、URLに載せる必要がなくなった
(会話・ブラウザ履歴・アクセスログにトークンが残らない)。
**ただし今のトークンは URL 経由で露出済みなので、一度作り直すこと**
(config.php / アプリ / GPTのAPIキー の3か所)。

---

## 2026-07-23: ChatGPTのブラウズでは api.php に到達できない(実地確認)+ GPT Actions 対応

### 判明したこと(実地確認・設計の前提が1つ崩れた)

ロリポップへの同期は成功し、ブラウザからは JSON を取得できるのに、
**ChatGPT からは api.php を読めなかった**。切り分けた結果:

- DNS: `sumi-zome.com` → 118.27.125.213 で外部から解決可能 ✅
- HTTPS: ブラウザ以外の一般クライアントからも HTTP 200 ✅
- robots.txt: WordPress既定。`/wp-admin/` のみ禁止で `/yosuga/` は許可 ✅
- ChatGPT は `example.com` は読める ✅ / `sumi-zome.com` は検索結果の関連ページに飛ぶ ⚠

→ **サーバー側の問題ではない**。その会話のブラウズが**検索インデックス経由**で動いており、
**認証付き・非公開のURLは原理的に到達できない**(検索に載らないため)。

v4 設計の「ChatGPT がサーバーの JSON を適宜読む」は、この経路では成立しない。

### 対処(サーバー側の変更は不要だった)

`api.php` は当初から `X-Yosuga-Token` ヘッダー認証に対応していたため、そのまま使える。

- **`server/openapi.yaml` を追加**: カスタムGPTの Actions 用の定義。
  Actions は検索ではなく**本物のHTTPリクエスト**を送るので確実に届く。
  さらに**トークンをURLではなくヘッダーで渡せる**ので、
  会話・ブラウザ履歴・アクセスログにトークンが残らない = 現状より安全になる
- `server/README-server.md` に手順 A(Actions・推奨/Plus必要)と
  手順 B(ブラウザのJSONをコピペ・Plus不要)を明記
- `docs/yosuga_prompt.md` の冒頭にも同じ注意を追記

### 当面の運用

手順 B(コピペ)でも v4.1 / v4.2 は完全に動く。
**回答JSONの取り込み・承認・自動再同期はブラウズと無関係**に動作するため。

### 将来

MCP(v4 Phase4)はこの問題に対する本来の答えでもある。
Actions で足りるならそちらが手軽。

---

## 2026-07-23: 記録タブに「指示」(指示書の管理UI)を追加 — v4.2 完了

### 経緯

v4.2 で指示書の配信までは通ったが、**一覧・完了操作のUIが無く、承認した指示が配信され続ける**
状態だった。そこを塞ぐ。

### 実施内容

- 記録タブを **6区分**に(アイテム/決定/日記/文書/関連/**指示**)
- `DirectiveFilters`(純粋関数):
  - `sortDirectivesForDisplay` — **配信中を先に**(対応が必要なものが上)、その中は新しい順
  - `filterDirectivesByProject` / `directiveTargetsPresent`(指示書がある宛先だけチップに出す)
  - `directiveTargetName` — プロジェクトが消えていても **projectId にフォールバック**して
    宛先が空欄にならないようにする
- `DirectiveDetailDialog`: 状態 / 宛先・優先度・承認日 / **本文(Claude Code へ届く Markdown)** /
  対応済みにする ⇄ 配信中に戻す / 削除。
  **ここでは編集させない**(指示を書くのはヨスガの仕事、Hub は承認と配信管理を担う)
- 「対応済みにすると、次の同期から配信されなくなります」と明記
  (Room を更新しただけでは配信は止まらないため)
- `RecordsViewModel` の combine が5フロー上限を超えたので、
  アイテム・タグ・実体を `KnowledgeData` として内側にまとめた(ホームの secretaryFlow と同じ手)

### テスト

- `DirectiveFiltersTest` 6件(並び順 / 宛先絞込 / 表示名とフォールバック /
  チップの出し分け / 未知の優先度をそのまま出す)
- `assembleDebug` + `testDebugUnitTest` 成功(**210件全成功**)

---

## 2026-07-23: 取り込み後の自動同期 + Claude Code への指示書(v4.2)

### 経緯(ユーザーと合意した運用)

- ヨスガ連携は**回答JSONだけ**。状況はロリポップにアップロードしたものを ChatGPT が適宜読む
- 回答JSONを受け取ったら、それを元に**ロリポップ上のデータも更新**する
- 各ゲームの Claude Code への**指示書**を書き、それを渡す経路を用意する

### ③ 取り込み後の自動同期

- `ImportRepository` に `syncAfterImport: suspend () -> SyncResult?` を追加(AppContainer で結線)。
  ホーム・ヨスガ画面のどちらから取り込んでも同じ挙動になる
- **Room への反映が済んでから**同期する(送る内容に取り込み結果を含めるため)
- メッセージは追記型: 成功なら「サーバーへ反映しました」、失敗なら理由 +「今すぐ同期でやり直せます」。
  **同期未設定の人には何も言わない**(未設定は失敗ではない)

### ④ 指示書(v4.2)

**渡し方の設計**: ロリポップを掲示板にする。GitHub へ書き込む案は**書き込みトークンが必要になり
「権限は読み取り専用に限定する」方針に反する**ため採らなかった。手動コピペは毎回の手間になる。

- Room v7: `directives`(承認済みのみ / status: open|done)
- 回答JSON v2 に `directives[]`(projectId / title / body(Markdown) / priority)
- **承認して初めて配信される**(pending_proposals 経由 = 既存の提案フローに乗せた)。
  宛先が実在しないプロジェクトの指示は承認時に自動棄却(`ProjectRepository.exists` を追加)
- `directives.json` として7ファイル目を配信。**配信するのは未完了のみ**。
  ファイル先頭に読み手向けの `note`(自分の projectId のものだけ読む / 対応したら記録する)
- 配信順は**作成順**(優先度で並べ替えない = 出した順に読ませる)
- `docs/claude_code_onboarding.md` に受け取り手順を追記
  (curl で取得 / 自分宛だけ読む / 矛盾したら着手せず questionsForYosuga で確認 /
  対応したら recentChanges に directiveId を記録)。**URLとトークンは文書に書かせない**
- `docs/yosuga_prompt.md` に「# 指示書」節(読み手はコードを触れるAI。目的と完了条件を書く。
  手段は現場に委ねる。1回に2〜3件まで)

### テスト

- `DirectiveRepositoryTest` 7件 / `ProposalRepositoryTest` +2件(承認で配信対象になる・
  宛先不明は棄却)/ `AiExporterTest` +1件と更新(7ファイル・Markdown がそのまま届く)/
  `ImportMessageTest` +4件(自動同期の文言。未設定時は黙る)
- `MigrationTest` を v7 対応に更新(`LATEST_VERSION` と `allMigrations`)
- スキーマJSON 7.json とマイグレーションSQLの一致を確認済み
- `assembleDebug` + `testDebugUnitTest` + `assembleDebugAndroidTest` 成功(**204件全成功**)

### 未検証

- 実通信での自動同期 / directives.json を Claude Code が curl で読む一連の流れ
- Room v6→v7 のマイグレーション(`connectedDebugAndroidTest` で検証可能)

---

## 2026-07-23: 分類済みの文書には取り込みで分類を適用しない(ユーザー判断)

### 決定

前エントリで挙げた「承認して確定させた文書に、あとから分類が届くと確認待ちへ戻る」挙動について、
**戻さない**方針を選択(ユーザー判断)。確定させたものが取り込みで揺り戻されるのを避ける。

### 実施内容

- `DocumentRepository.SETTLED_STATUSES = { classified, archived }` を導入し、
  `applyAiClassification` はこの状態の文書に**適用しない**(アーカイブのみだった条件を拡張)
- やり直しの経路は塞がっていない: ユーザーが「再分類」を押す → `classification_pending` へ戻る
  → 次の同期でヨスガへ渡る → 分類を受け取れる(テストで固定した)
- 取り込みメッセージに次の行動を追記:
  「適用できなかった分類が N 件…やり直すには文書画面で『再分類』を押してください」
- `docs/yosuga_prompt.md` の取り込み仕様表と、設計書v4.1 の対応表にもこの規則を明記

### テスト

- `DocumentRepositoryTest` +1件(確定済みは適用されず、現行分類も履歴も変わらない)
- `ClassificationImportTest`: 揺り戻しのテストを差し替え、
  **「再分類を押した後なら新しい分類を受け取れる」**を追加(やり直し経路が塞がっていないこと)
- `assembleDebug` + `testDebugUnitTest` 成功(**190件全成功**)

---

## 2026-07-23: 分類取り込み経路の結合テスト + テストの穴埋め

### 経緯

v4.1 の各部品には単体テストがあるが、**回答JSON → 文書への反映という経路そのもの**は
どのテストも通っていなかった。実機で最初に触る経路なので先に固める。

### リファクタ(テスト可能にするため)

`ImportRepository` の分類適用ロジックは Context に縛られてテストできなかったが、
中身はファイル入出力を持たない。`ClassificationApplier` として切り出し、
ImportRepository は委譲するだけにした(`ServerSyncRepository` の `buildFiles` と同じ理由)。
**振る舞いは変えていない**(件数の数え方・読み飛ばし条件はそのまま移設)。

### 実施内容

- `ClassificationImportTest` 6件。回答JSONの**文字列**から
  `ResponseImporter` → `ClassificationApplier` → `DocumentRepository` まで**本番コードを通す**
  (範囲外はファイル入出力のみ):
  1. 設計書v4.1 の例の形の回答JSONが、文書を「確認待ち」にし全項目が正しく入ること。
     **原文が変わらないこと** / type が空の関連が落ちること / 宛先なし・ID空を読み飛ばすこと
  2. 宛先の文書が一つも無ければ何も起きない
  3. **同じ回答を二度取り込んでも履歴が積まれるだけ**(現行はちょうど1件)
  4. 承認して確定させた後に取り込むと「確認待ち」へ戻る(※下記)
  5. **アーカイブ済みは復活しない**(自己レビューで直した挙動を経路ごと固定)
  6. 分類を含まない回答(従来どおりの提案だけ)では何も起きない
- `SyncTokenTest` 3件。同期トークンは**ロリポップ上の受け口・配信の唯一の保護**なので
  性質を固定した(64桁hex / 毎回異なる / URL・ヘッダー・PHP設定にそのまま貼れる文字だけ)

### ※ 要確認の挙動(テスト4)

**承認して「分類済み」にした文書に、あとから分類が届くと「確認待ち」へ戻る。**
新しい分類が来た以上もう一度確認してもらう方が安全と判断してこの挙動にしてあるが、
アーカイブ(= 明示的に片付けた)とは違い、確定済みが揺り戻される点は好みが分かれる。
気になるようなら「分類済みには適用しない」へ変更できる(`DocumentRepository` の1行)。

### テスト

- `assembleDebug` + `testDebugUnitTest` 成功(**188件全成功**)

---

## 2026-07-23: 設定に「取り込み履歴」を追加 — 積み残しから

### 経緯

回答JSONは取り込み時に `filesDir/imports/` へ保存していたが(設計書15章: 元ファイルを上書きしない)、
**アプリからは一切見えなかった**。「さっき何を取り込んだか」を確認できるようにする。

### 実施内容

- `ImportHistoryNames`(純粋ロジック): 保存名 `response_yyyy-MM-dd_HHmmss.json` の
  検証と表示用時刻への変換。想定外の名前は空文字を返し、一覧では名前だけ見せる
- `ImportRepository.history(limit=20)`: 新しい順に一覧。
  **ファイル名が時刻そのものなので、名前の降順 = 時系列の降順**(テストで固定した)
- `ImportRepository.readHistory(fileName)`: 中身を読む。
  **`isValidHistoryName` を通らない名前は受け付けない**(`imports/` の外を読ませない番人)
- 設定画面に「取り込み履歴」セクション(保存時刻 + サイズ / タップで中身をダイアログ表示)。
  ファイル一覧は Flow ではないので `LaunchedEffect` で画面を開いたときに読み直す
  (SettingsUiState の combine は5フロー上限のため、履歴は独立した StateFlow にした)

### テスト

- `ImportHistoryNamesTest` 5件(時刻の整形 / 想定外の名前 / 受け付ける名前の限定 /
  **パストラバーサルの拒否** / 名前の降順が時系列と一致すること)
- `assembleDebug` + `testDebugUnitTest` 成功(**179件全成功**)

---

## 2026-07-23: 記録タブに「関連」(エンティティ閲覧UI)を追加 — 積み残しから

### 経緯

実体(エンティティ)は v3-Step 2 から AI が関連付けて Room に溜まっていたが、
**画面のどこにも出ていなかった**(入るだけで出てこないデータ)。積み残しリストの
「エンティティ閲覧UI」を消化する。

### 実施内容

- `entityTypeLabel`(プロジェクト / 人物 / 技術 / 機材 / イベント / その他)
- `EntityIndex.kt`(純粋関数): `buildEntityIndex` で実体ごとに関連アイテムを集める。
  - 実体の一意性は **名前 + 種別**(DBの unique index と同じ規則)
  - 並びは関連の多い順 → 名前順
  - **関連ゼロの実体も残す**(アイテムを消しても実体本体は残る仕様なので、存在を隠さない)
  - `filterEntitiesByType` / `entityTypesPresent`(実際に使われている種別だけをチップに出す)
- 記録タブを **8区分**に(アイテム/決定/日記/文書/**関連**)。読み取り専用
  (実体の生成・統合は AI の仕事という役割分担を崩さない)
- `RecordsUiState.entities` を追加(combine は5フロー上限ちょうど)

### テスト

- `EntityIndexTest` 7件(名前+種別での集約 / 同名別種別は別物 / 並び順 / 関連ゼロを残す /
  種別絞込 / 使われている種別だけ出す / 実体テーブルに無い参照を無視)
- `assembleDebug` + `testDebugUnitTest` 成功(**174件全成功**)

---

## 2026-07-23: ホームに「確認待ちの文書」を追加(v4.1 の統合漏れ)

### 経緯

v4.1 を入れたことでホーム画面に抜けができていた。ホームは AI秘書として
「承認待ちの提案」件数を出すが、**確認待ちの文書には触れない**ため、
ヨスガの分類が届いても記録タブを開くまで気づけなかった。

### 実施内容

- `DocumentDao.observeCountByStatus`(既存 `PendingProposalDao` と同じ形)+
  `DocumentRepository.needsReviewCount()`。**件数だけ数える**ので分類行までは読まない
- `HomeUiState.documentsNeedingReviewCount` を追加し、secretaryFlow に合流
  (外側の combine は5フロー上限のため、内側にまとめる既存の作りに従った)
- ホームに「確認待ちの文書」カード(0件なら出さない。文言は既存の提案カードに合わせ、
  「記録タブの『文書』で確認してください」と次の行動を書く)

### テスト

- `DocumentRepositoryTest` +1件(needs_review だけを数える / 承認すると減る)
- `assembleDebug` + `testDebugUnitTest` 成功(**167件全成功**)

---

## 2026-07-23: v4.1 の自己レビューと修正(実機確認の前に)

実装を積み増す前に直近3コミット(v4.1)を見直し、欠陥4件を修正した。

### ① アーカイブ済み文書が分類の取り込みで復活する(動作の不具合)

`applyAiClassification` が状態を見ずに needs_review へ進めていたため、**古い回答JSONを取り込むと
片付けたはずの文書が「確認待ち」に戻っていた**。アーカイブ済みは適用せず読み飛ばすようにした
(再開したいときはユーザーが明示的に操作する)。

これに伴い `ImportResult.SuccessProposals.unknownDocumentCount` を
`skippedClassificationCount` に改名(「宛先なし」だけでなく「アーカイブ済み」も含むため)。
メッセージも「適用できなかった分類が N 件(宛先の文書が見つからない、またはアーカイブ済み)」に修正。

### ② 未分類の文書に「再分類」ボタンを出していた(UIの不整合)

一度も分類されていない文書で押すと、アップロードしていないのに状態が「分類待ち」になり
**表示が事実と食い違っていた**(documents.json は未整理・分類待ちの両方を送るので実害は自己修復するが、
ユーザーには嘘の表示になる)。現行分類がある文書にだけ出すようにした。
「アーカイブ」は従来どおり全状態で出す。

### ③ composition 中に Compose の状態を書き換えていた(実装の作法)

詳細ダイアログで、対象文書が消えていたら `openedDocumentId = null` を composition 本体で
代入していた。`openedDocument?.let { }` が空振りすれば閉じるので、代入自体を削除した。

### ④ 分類の修正ダイアログが空の要約でも承認できた

`enabled = summary.isNotBlank()` を追加(既存の `ItemEditDialog` と同じ方針)。

### テスト

- `DocumentRepositoryTest` +1件(アーカイブ済みへの分類は適用されず履歴も増えない)、
  `ImportMessageTest` を新しい文言・フィールド名へ更新
- `assembleDebug` + `testDebugUnitTest` + `assembleDebugAndroidTest` 成功(**166件全成功**)

---

## 2026-07-23: Room マイグレーションの自動テスト(androidTest)

### 目的

マイグレーションの抜け(①定義 ②`@Database(version)` ③`addMigrations` 登録 ④スキーマ突き合わせ)は
**コンパイルが通ってしまい、実機更新時に初めてクラッシュする**。過去2回踏んでいるため自動化した。

### 新規ライブラリ

- **`androidx.room:room-testing:2.6.1`**(既存 room と同一バージョン / `androidTestImplementation` のみ)
  - 理由: `MigrationTestHelper` はこの公式アーティファクトにしか無い。
    「旧バージョンのDBを実際に作る → マイグレーションを走らせる → `app/schemas/*.json` の
    期待スキーマと突き合わせる」を行う正攻法。**アプリ本体(APK)には入らない**

### 実施内容

- `app/build.gradle.kts`: androidTest のアセットに `$projectDir/schemas` を追加
  (MigrationTestHelper は期待スキーマを端末上のアセットから読むため)。
  テストAPKに `assets/com.shiro...YosugaDatabase/2〜6.json` が入ることを確認済み
- `app/src/androidTest/.../MigrationTest.kt` 5件:
  1. 各段(2→3 / 3→4 / 4→5 / 5→6)を個別に検証(落ちた段が特定できる)
  2. v2 から最新まで通し
  3. 既存データ(projects / tasks)がマイグレーションで失われない +
     v4 追加列が既存行で NULL になること
  4. v6 の documents / document_classifications が実際に読み書きできること
  5. **実際の Room ビルダーで開けること**(`addMigrations` への登録漏れ=チェックリスト③は
     ここで初めて落ちる)
- `validateDroppedTables = true` で消し忘れテーブルも検出する

### ⚠ v1 は自動テストできない(既知の制約)

`app/schemas/` は **2.json から**しか無い(v1 の時点では `exportSchema` を有効にしていなかった)。
MigrationTestHelper は期待スキーマが無いバージョンのDBを作れないため、**v1→v2 だけは対象外**。
1.json を手書きすると Room の `identityHash` を捏造することになり、誤った安心を生むので**あえて作らない**。
v1 から使い続けている端末の更新は、実機更新でのみ確認できる。

### 検証状況

- `assembleDebugAndroidTest` 成功(**コンパイルとAPK同梱までは確認済み**)
- `assembleDebug` + `testDebugUnitTest` 成功(単体165件全成功、既存に影響なし)
- **テスト自体はまだ実行していない**(実機またはエミュレータが必要):
  ```
  ./gradlew connectedDebugAndroidTest
  ```

---

## 2026-07-23: v4.1 AI分類ワークフロー — 同期・取込・プロンプト(③④⑤)= 実装完了

### 設計判断(合意内容からの調整・要確認)

論点3で「分類結果は回答JSON v2 の `proposals.classifications[]`(既存の取込→pending_proposals→
レビューUIを再利用)」と合意したが、実装では **classifications だけ pending_proposals に積まず、
取込時に文書へ直接適用**する形にした。理由:

- 設計書v4.1 の状態遷移が `(分類結果を取込) → needs_review` / `(承認・修正して確定) → classified`
  であり、取込=適用と定義されている
- pending_proposals を経由すると「ヨスガ画面で提案を承認 → 文書画面でもう一度承認」と
  **承認が二重**になる。文書のレビューUI(実装済み)が承認の唯一の入口である方が筋が通る
- 「初期段階では自動確定せず、必ずユーザー確認を通す」は文書画面の承認で担保されている

再利用したのは**運搬経路**(回答JSON v2 → ResponseImporter → ImportRepository)。
この判断に違和感があれば pending_proposals 経由へ戻せる(ImportRepository の1メソッドの差)。

### 実施内容

**③ documents.json のサーバー同期**
- `DocumentsFile` / `DocumentExport` を追加し、AiExporter を **6ファイル**構成に
- 送るのは **unclassified / classification_pending のみ**(分類済み・確認待ち・アーカイブは
  再分類を誘発するため送らない)。原文をそのまま渡す(要約はヨスガの仕事)
- `ServerSyncRepository` に `onDocumentsUploaded` を追加し、**アップロード成功時のみ**
  `markUnclassifiedAsPending()` を呼ぶ。失敗時は unclassified のまま残り、次回再送される
- 同期中に追加された文書が pending になり得るが、pending も毎回送るため取りこぼさない(コメント済み)
- サーバー側は変更不要(upload.php のファイル名検証は `^[a-z0-9_]+\.json$` の正規表現)
- テスト容易性のため `ServerSyncRepository` の生成依存を `buildFiles: suspend () -> List<AiExportFile>`
  に変更(既存の urlProvider / tokenProvider と同じ供給関数スタイル)。AppContainer で結線

**④ 回答JSON v2 の分類取込**
- `ClassificationProposal` / `RelatedRefImport` を追加。設計書v4.1の例に合わせ **snake_case**
  (`document_id` / `project_ids` / `document_type` / `related_entities`)で受ける
- `ImportRepository` が分類を `DocumentRepository.applyAiClassification()` へ流す。
  document_id が空・実在しない分類は読み飛ばし、件数を結果に載せる
- `ImportResult.SuccessProposals` に classificationCount / unknownDocumentCount を追加し、
  取り込みメッセージで「記録タブの『文書』で確認してください」と案内

**⑤ docs/yosuga_prompt.md**
- 「# 文書の分類」節を追加(documents.json の読み方 / classifications[] の書式 /
  原文を書き換えない / document_id を推測しない / confidence は正直に / 既存タグの再利用)
- 取り込み仕様の表に classifications の挙動3行を追加。api.php の file 一覧に documents を追加

### テスト

- `ServerSyncRepositoryTest` 4件(成功時のみ副作用 / HTTPエラーで文書を進めない /
  URL・トークン未設定で生成すら走らない)、`ImportMessageTest` 4件、
  `ResponseImporterTest` +2件(snake_case の分類 / 分類なしの回答)、
  `AiExporterTest` +1件と既存1件の更新(6ファイル / 送信対象の絞り込み)
- WSL で `assembleDebug` + `testDebugUnitTest` 成功(**165件全成功**)

### 未検証(実機・実通信が必要)

- Room v6 マイグレーション通し / 文書の一連の操作 /
  ChatGPT が documents.json を読んで分類を返す一連の流れ
- ※ documents.json の実アップロードは 2026-07-23 に**検証済み**(再開ポイント参照)

---

## 2026-07-23: v4.1 AI分類ワークフロー — 文書UI + 分類レビューUI

### 実施内容(記録タブ「文書」セクション / 5項目のうち ①②)

- 記録タブを **7タブ化**(アイテム/決定/日記 + **文書**)。区分チップは横スクロールに変更
- `DocumentAddDialog`: タイトル + 原文のみを入力。**保存後に原文を編集する手段は用意しない**
  (分類・タグ付け・要約はヨスガの仕事、という役割分担をUIでも崩さない)
- `DocumentDetailDialog`: 状態チップ / 現行分類 / **原文** / 分類履歴 を1画面で確認。
  設計書v4.1「UI」の5操作を実装 —
  **承認**(現行分類のまま確定)/ **修正**(ClassificationEditDialog)/
  **保留**(閉じるだけ = needs_review 据え置き。ボタン文言も「保留して閉じる」に変わる)/
  **再分類**(classification_pending へ戻す)/ **元文表示**(常時表示)。
  承認・修正は現行分類があるときだけ出す(空の分類を確定させない)
- `ClassificationEditDialog`: 要約・種別・プロジェクト・カテゴリ・タグを修正して承認。
  関連実体は手入力させず元の値を維持(ヨスガが付けるもの)。承認するとユーザー修正レコードが積まれ、
  AIの分類は履歴に残る
- `DocumentFilters`(純粋関数): 状態絞込 / 検索(タイトル・原文・要約・タグ・カテゴリ)/
  一覧プレビュー(分類済みは要約、未分類は原文冒頭)
- `Document` に `classificationHistory` を追加。DAOは元々 @Relation で全分類行を取得しているため
  追加コストなし。履歴が Flow で流れるので、修正承認するとダイアログの表示も追従する
- 削除された文書を開いたままにしないよう、詳細ダイアログは ID で引き直す

### テスト

- `DocumentFiltersTest` 11件(状態絞込 / 検索の対象と大小文字 / プレビューの分岐・改行畳み・省略 /
  信頼度の%表示とクランプ / 履歴1行の表示)、`DocumentRepositoryTest` に履歴の並び1件追加
- WSL で `assembleDebug` + `testDebugUnitTest` 成功(**154件全成功**)

### 未実装(次回)

- `documents.json` のサーバー同期(AiExporter への追加 + `markUnclassifiedAsPending()` の接続)
- 回答JSON v2 `proposals.classifications[]` の取込
- `docs/yosuga_prompt.md` への分類指示の追記
- 実機確認(Room v6 マイグレーション通し・文書の一連の操作)

---

## 2026-07-23: v4.1 AI分類ワークフロー — データ層(Room v6)

### 経緯・決定(ユーザーと合意 / 設計書v4.1「未確定の論点」5点)

1. **実装範囲**: 今回はデータ層まで(Room v6 + Repository + テスト)。UI・同期・取込は次回。
2. **文書の作成入口**: 記録タブに「文書」セクションを新設(次回のUI実装で対応)。
3. **分類結果の受け取り**: 回答JSON v2 の `proposals.classifications[]`(次回の取込実装で対応。
   既存の 取込→pending_proposals→レビューUI を再利用する)。
4. **承認確定後**: documents のまま(knowledge_items へ昇格しない。長文=文書 / 短い知識=アイテム)。
5. **related_entities の type**: 文書側は自由文字列(既存 EntityType は拡張しない)。

### 新規ライブラリ

- **なし**

### 実施内容

- `domain/model/Document`: DocumentStatus(unclassified / classification_pending / needs_review /
  classified / archived)、ClassificationOrigin(ai / user)、RelatedRef(type は自由文字列)、
  DocumentClassification、Document。未知のDB値は安全側へフォールバック
- Room v6: `documents`(**body=原文・不変**。status にインデックス)+
  `document_classifications`(AI結果とユーザー修正を**別レコード**で積む=そのまま分類履歴。
  現行1件だけ isCurrent。リスト項目は履歴スナップショットとして JSON 列で保持 —
  後からタグを改名しても過去の分類記録が変わらない)
- `DocumentJsonColumns`(純粋ロジック): JSON列の符号化・復号。壊れたJSONは空リストへ
  フォールバック(ProposalPayloads と同方針)
- `DocumentDao`: 原文を更新するSQLは置かない。`saveClassificationAsCurrent`(旧現行を落として積む)/
  `deleteDocumentWithClassifications` は default メソッドでロジックを持つ
- `DocumentRepository`: 状態遷移を集約 — createDocument(→unclassified)/
  markUnclassifiedAsPending(同期成功時に呼ぶ想定)/ applyAiClassification(→needs_review)/
  approve(現行分類が無ければ拒否)/ approveWithEdits(USER レコードを積んで→classified)/
  requestReclassification(→pending。履歴は残す)/ archive / deleteDocument。
  **原文(body)を書き換える操作は提供しない**
- `MIGRATION_5_6` + `@Database(version = 6)` + AppContainer 登録(チェックリスト①〜③)、
  スキーマJSON 6.json と突き合わせ済み(④)
- SampleSeed への documents 追加はなし(実運用データのみ)

### テスト

- `DocumentRepositoryTest` 13件(状態遷移一式 / 原文不変 / ユーザー修正でAI履歴が残る /
  再分類で履歴が積み上がる / markUnclassifiedAsPending が unclassified のみ動かす /
  壊れたJSON列の耐性 / roundtrip / 未知DB値フォールバック)
- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL / **142件全成功**。
  ※前回記録の「145件」は誤記で、実数は129件だった)

### 未実装(次回)

- 記録タブ「文書」セクション(作成・一覧・詳細・元文表示)
- 分類レビューUI(承認 / 修正 / 保留 / 再分類)
- `documents.json` のサーバー同期(AiExporter への追加 + markUnclassifiedAsPending の接続)
- 回答JSON v2 `proposals.classifications[]` の取込(ResponseImporter / ImportRepository)
- `docs/yosuga_prompt.md` への分類指示の追記

---

## 2026-07-23: v4 Phase2 — AI用JSON分割 + ロリポップ同期

### 経緯・決定(ユーザーと合意)

- 設計方針 **v4(AIプラットフォーム構想)** を取り込み(コミット 4877666)。
  Hubは「人間のUI」と「AIのデータインターフェース」の両方を持つ。
- **v3の「クラウド同期なし」を v4 が上書き**し、ロリポップ同期を導入。
- 同期方式: **PHP受け口 + トークン認証**(Android→HTTPS POST、配信は api.php?token=)。
  公開するのは進捗・整理情報のみのため、トークン認証を最低限の保護とする。
- conversations.json は当面「取込・承認履歴」で代用(会話本文は保持しない)。

### 新規ライブラリ

- **なし**(Ktor は GitHub連携で導入済みのものを流用)

### 実施内容(Android)

- `data/file/model/AiExportModels`: 用途別5ファイルのモデル
  (projects / tasks / knowledge(items+diary) / calendar(today+upcoming+past) /
  conversations(exchanges))。各ファイルが schemaVersion / generatedAt を持つ
- `data/file/AiExporter`(純粋ロジック): 5ファイルの組み立て。
  projects / tasks の変換は `ContextExporter.projectExportOf / taskExportOf` として公開化し
  状況JSON(手動ブリッジ)と共有。提案履歴のタイトルは payload から抽出(壊れていても落ちない)
- `PendingProposalDao.recent(limit)` を追加(履歴50件)
- `AiExportRepository`: ドメインRepositoryからスナップショットを集めて5ファイル生成、
  `filesDir/ai/` へ保存(AI Interface の初期実装 = ローカルJSON出力)
- `data/sync/SyncApi`: `upload.php` へ POST(トークンは `X-Yosuga-Token` ヘッダー。URLに載せない)。
  結果を Ok / Unauthorized / HttpError / NetworkError に分類、例外メッセージは伝播させない
- `ServerSyncRepository`: URL・トークンを供給関数で受け、成功時に最終同期時刻を記録
  (ホームの「最終同期」が本来の意味を持つようになった)
- `SyncSettingsRepository`: URL(平文)+ トークン(Keystore暗号化。GitHubと同じ TokenCrypto を共用)。
  `generateToken()`(SecureRandom 64桁hex)
- 設定画面「サーバー同期」セクション: URL / トークン(伏字)/ トークン生成 / 今すぐ同期
- `docs/yosuga_prompt.md`: サーバーAPI経由で状況を読ませる手順を追記

### 実施内容(サーバー / `server/` ディレクトリ)

- `config.php`(トークン設定)/ `upload.php`(受け口: hash_equals検証・ファイル名ホワイトリスト・
  JSON妥当性チェック)/ `api.php`(配信: file=index で一覧+更新時刻)/ `data/.htaccess`(直接アクセス拒否)
- `README-server.md`: ロリポップへの設置手順・動作確認・ChatGPTへの渡し方
- config.php が初期値のままなら全リクエスト403(設定忘れをフェイルセーフに)

### テスト

- `AiExporterTest` 4件(5ファイル生成 / 各ファイルのパース・schemaVersion / 履歴タイトル抽出 /
  壊れたpayload耐性)、`SyncApiTest` 4件(URL・ヘッダー・本文 / 403 / 500 / オフライン)
- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)

### 未検証

- ロリポップへの実設置・実通信(ユーザー作業: README-server.md 参照)
- ChatGPT のブラウズでの api.php 読み取り

---

## 2026-07-23: カレンダー連携(旧Phase 4)— 端末カレンダー読み取りで実装

### 方式の決定(ユーザーと合意)

設計書v2は Google Calendar API + OAuth 前提だったが、**CalendarContract で端末に同期済みの
カレンダー(Googleカレンダー含む)を直接読む方式**を採用した。

- 採用理由: READ_CALENDAR 権限のみで動作し、**Google Cloud プロジェクト / OAuth クライアントID /
  SHA-1登録 / 同意画面の設定がすべて不要**。新規ライブラリもゼロ。オフラインでも動く。
  設計書の完了条件(過去7日〜未来7日を確認 / 状況JSONへ出力)は満たせる。
  「初期版の権限は可能な限り読み取り専用に限定する」(設計書5.1)にも合致。
- 制約: 端末に同期されているカレンダーのみが対象(Pixel で Google カレンダーを使う運用なら問題なし)。
- 将来 API + OAuth へ移す場合も、DataSource を差し替えるだけで済む構造にしてある。

### 新規ライブラリ

- **なし**

### 実施内容

- `AndroidManifest.xml` に **READ_CALENDAR**(読み取り専用)を追加
- `data/calendar/EventFormatting`(純粋ロジック): 今日の日付表示 / 予定時刻の整形
  (今日は時刻のみ・他日は日付+時刻・終日は日付のみ)/ 今日・今後・過去のバケット判定
- `data/calendar/DeviceCalendarDataSource`: `CalendarContract.Instances` を期間指定で問い合わせ
  - **Instances を使うことで繰り返し予定が期間内の個別予定として展開される**
  - 権限チェック、タイトル空欄は「(タイトルなし)」、終日予定は終了時刻を出さない
  - 結果は `Result<List<CalendarEventEntity>>`(例外を投げない)
- `CalendarEventDao.replaceAll()`(@Transaction で全削除→挿入。同期は常に洗い替え)
- `CalendarRepository` を刷新:
  - `today` を**端末時計から**算出(固定文字列のプレースホルダーを廃止)
  - `sync()`: 権限なし → `PermissionDenied` / 失敗 → `Failed`(**キャッシュは変更しない**)/
    成功 → 洗い替え
- `CalendarViewModel`: `sync()` と `hasPermission()`、同期中フラグ
- `CalendarScreen`: 「端末のカレンダーから更新」ボタン(未許可なら権限ダイアログ →
  許可されたらそのまま同期)。0件時の表示、今日の日付をセクション見出しに表示
- `ui/share/CalendarMessage`: 結果 → 短文(0件と失敗を区別、技術的な例外文は出さない)
- **仮カレンダーのシードを停止**(実データに置き換わったため)。`SampleSeed.events` はテスト用に残す
- テスト: `EventFormattingTest` 5件、`CalendarMessageTest` 4件

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)
- **実機/シミュレーター未検証**: 権限ダイアログ、実際の予定取得、繰り返し予定の展開。
  シミュレーターの場合、Googleアカウント未追加だと予定0件になる点に注意。

### これで仮データはすべて解消

- プロジェクト/タスク/知識ベース: 手動編集・AI提案・GitHub取得で実データ化済み
- カレンダー: 端末から実データ取得(本ステップ)
- 残るシードは初回起動時のサンプルのみ(実データ投入後は上書き・削除可能)

---

## ▶ 次回の再開ポイント

### ▶ 次回の最初の一手(2026-07-24 出勤前に中断)

**仮データ(SampleSeed)の扱いを決めるところから。** 上記 2026-07-24 のエントリ
「Morning Brief 実地確認」の末尾にある **A / B の選択**に答えれば、すぐ着手できる。
- A: 設定に「サンプルデータを削除」+ 再シード防止フラグ + プロジェクト削除(推奨)
- B: シード自体をやめる

これが片付くと、実データだけで Morning Brief → ヨスガ の運用を開始できる。

### 実地検証の残り

- **指示書(v4.2)の通し**: レコルに「ANRIのClaude Codeに指示を出して」→ 回答JSONを貼り付け →
  ヨスガ画面で承認 → 同期 → `api.php?file=directives&token=...` に出れば配信成功。
  記録タブ「指示」から「対応済み」にできる。
  ここまで通ると **Hub → AI → Hub → 各ゲームのClaude Code** の全経路が実証される
- **マイグレーション**: `./gradlew connectedDebugAndroidTest`(7件)。新規インストールでは
  検証されないため、既存端末の更新事故を防ぐ意味で一度流しておきたい
- GitHub 実通信 / Obsidian SAF 書き出し / カレンダー権限

### ⚠ ビルド時の注意(2026-07-24 に衝突が起きた)

Claude Code が WSL でビルドする間、`local.properties` を WSL 用のパスへ一時的に書き換える。
**その最中に Android Studio でビルドすると「SDK location not found」になる**。
同時に走らせないこと。Claude 側は今後ビルド前に確認する運用にした。

### ユーザーの判断待ち(実装せず保留中)
- タグのタスク適用(アイテムのタグと使い分ける意味があるか、運用次第)
- 観察日記のObsidian書き出し(いつ・どのノートへ書くかのUX)
- status.json の decisions を**提案として流し込む**か(表示とAI連携は 2026-07-24 に実装済み。
  提案化には重複取込を防ぐ照合キーの設計が要る)
- アプリUI文言の「ヨスガ」→「レコル」改名(実機検証が落ち着いてから)

---

## (旧)再開ポイント(2026-07-23 セッション終了時点)

### 設計書の階層(読む順)
1. `yosuga_hub_android_design_v4.md` — **最上位方針**(AIプラットフォーム構想)
1.5. `yosuga_hub_design_v4_3_role_split.md` — **最新の運用方針**(ヨスガ/レコル役割分離・Morning Brief)
2. `yosuga_hub_design_v4_1_classification.md` — **追加指示: AI分類ワークフロー(未実装)**
3. `yosuga_hub_android_design_v3.md` / `_v3_1.md` — 基本思想・知識ベース仕様(有効)
4. `yosuga_hub_android_design_v2.md` — 技術資料(アーキテクチャ・ライブラリ候補)

### 実装済み(すべてビルド+テスト通過)
- **v3-Step 1〜4**: Task実体化 / 知識ベース+提案承認フロー / Obsidian連携(SAF) / ホームAI秘書再編
- **GitHub連携 3-a〜3-d**: リポジトリ設定 + トークンKeystore保管 / Ktorで `.yosuga/status.json` 取得 /
  Room v5 キャッシュと表示 / 状況JSONへの反映
- **カレンダー連携**: CalendarContract で端末カレンダーを読む(OAuth不要)
- **v4 Phase2**: AI用JSON 5分割(AiExporter)+ ロリポップ同期(SyncApi / server/ にPHP一式)
- **v4.1 AI分類ワークフロー(実装完了)**: Room v6(documents / document_classifications)+
  DocumentRepository(状態遷移・分類履歴)+ 記録タブ「文書」セクション(承認/修正/保留/再分類/元文表示)
  + documents.json 同期 + 回答JSON v2 `classifications[]` 取込 + ヨスガ用プロンプト。
  論点5点は合意済み(取込の扱いのみ調整あり — 本日のエントリ「設計判断」参照)。
  実装後の自己レビューで欠陥4件を修正済み(本日のエントリ参照)
- **ホーム**: 「確認待ちの文書」件数を表示(v4.1 の統合)
- **記録タブ「関連」**: エンティティ閲覧UI(読み取り専用)
- **設定「取り込み履歴」**: 過去の回答JSONの一覧と中身の確認
- **Room マイグレーションの自動テスト**(androidTest / 未実行。実機が必要)
- **v4.2 指示書**: 各ゲームの Claude Code への指示書(承認→directives.json で配信)。
  記録タブ「指示」で一覧・対応済み・削除ができる
- **取り込み後の自動同期**: 回答JSONを取り込むとサーバーへ即反映
- 仮データはすべて解消済み。単体テストは 210 件(分類取り込みは経路の結合テストあり)。
  **確定済み・アーカイブ済みの文書には取り込みで分類を適用しない**(やり直しは「再分類」から)。

### ▶ 次にやること

**A. 実環境での検証**(ユーザー作業が必要 / v4.1 が実装完了したので次はこれが本命)
※ 2026-07-23: **エミュレーターで起動・動作することは確認済み**(ユーザー確認)。
   ただし新規インストールは v6 を直接作るため**マイグレーションの通しは未検証のまま**
   (`./gradlew connectedDebugAndroidTest` をエミュレーターに向けて実行すれば検証できる)。
下の「B. 実環境での検証」の手順に加え、v4.1 の通し確認:
1. 記録タブ「文書」で文書を追加 → 設定で「今すぐ同期」→ 状態が「分類待ち」になるか
2. ChatGPT に documents.json を読ませ、`classifications[]` を含む回答JSONをもらう
3. 取り込み → 文書が「確認待ち」になり、分類内容が表示されるか
4. 承認 / 修正して承認 / 再分類 を試し、履歴が積まれるか
5. Room v1〜v6 のマイグレーション通し(既存端末の更新でクラッシュしないか)

**B. 実環境での検証**(ユーザー作業が必要)
1. ロリポップ設置: `server/README-server.md` の手順(トークン生成 → config.php → FTP → 同期)
2. 各ゲームの Claude Code に `docs/claude_code_onboarding.md` を渡し `.yosuga/` を作らせる
3. GitHub Fine-grained PAT(Contents: Read-only)→ アプリ設定 → プロジェクト編集で owner/repo 入力
4. ChatGPT に `docs/yosuga_prompt.md` を貼る → 状況JSON or サーバーAPI経由で運用開始

### ✅ 検証済み(2026-07-24 実機)
- **v4.1 AI分類ワークフローが端から端まで一周した**:
  文書を追加 → 同期でアップロード(分類待ちへ遷移)→ レコルが Actions で読んで分類 →
  **回答JSONを貼り付けて取り込み**(確認待ちへ)→ 承認 → **分類済み**。
  これにより、貼り付け取り込み経路 / 分類の適用 / 状態遷移一式 / 承認 が実機で実証された。

### ✅ 検証済み(2026-07-23 実機・実通信)
- **カスタムGPTの Actions から api.php の取得に成功**(`file=index` で7ファイルの一覧を取得)。
  これで **v4 の「ChatGPT がサーバーの状況データを読む」経路が実際に成立**した。
  通常のチャットのブラウズ(検索インデックス経由)では到達できなかったが、
  Actions は本物のHTTPリクエストを送るため到達できる。
  認証は `X-Yosuga-Token` ヘッダー(ロリポップのPHPはカスタムヘッダーを正しく渡す)。
  **つまずいた点**: スキーマを貼るだけでは認証は有効にならず、
  GPTの「アクション → 認証 → APIキー → カスタム」でヘッダー名と値の登録が別途必要
  (未設定だと api.php が forbidden を返し続ける)。
- **ロリポップ設置と実通信が通った**。エミュレーターのアプリから「今すぐ同期」→
  `api.php?file=index` に **7ファイルすべてが並ぶ**ことを確認。
  これにより次がまとめて実証された:
  Keystore へのトークン保存→復号 / `X-Yosuga-Token` ヘッダー認証 / PHP側の検証と保存 /
  配信(api.php)/ AiExporter の7ファイル生成。
- エミュレーターでのアプリ起動・動作。

### 未検証(実機・実通信が必要)
- Room マイグレーションの通し(**新規インストールは最新版を直接作るため未検証のまま**。
  `./gradlew connectedDebugAndroidTest` で `MigrationTest` を流せば確認できる)。
- ChatGPT が読んだ状況をもとに**回答JSONを返し、アプリが取り込む**一連の流れ
  (v4.1 分類 / v4.2 指示書)。取得側(Actions)は検証済み、返す側はこれから。
- GitHub 実通信(`.yosuga/status.json` の取得)。
- Obsidian SAF 書き出し(`createFile("text/markdown")` の拡張子挙動はプロバイダ依存)。
- カレンダー(READ_CALENDAR 権限ダイアログ・繰り返し予定)。エミュレーターは
  Googleアカウント未追加だと0件になる。

### WSL でビルドする場合
```
echo "sdk.dir=/home/note_xylo/android-sdk" > local.properties
JAVA_HOME=/home/note_xylo/tools/jdk-17.0.19+10 ./gradlew assembleDebug testDebugUnitTest --no-daemon
```
※ 終わったら local.properties を Windows 用(`C:\Users\note_\AppData\Local\Android\Sdk`)へ戻す。

### マイグレーション追加時のチェックリスト(2回踏んだ)
①Migration定義 ②`@Database(version)` 更新 ③`addMigrations` 登録 ④スキーマJSON突き合わせ
— ②③はコンパイルが通ってしまい、実機更新時にクラッシュする。
**自動テストあり**: `MigrationTest`(androidTest)。`./gradlew connectedDebugAndroidTest` で
①〜④をまとめて検証できる。新しい版を足したら `MigrationTest.LATEST_VERSION` と
`allMigrations` も更新すること。※ v1→v2 のみ対象外(1.json が無いため。理由は同テストのコメント)。

### 積み残し(任意)
タグのタスク適用 / 観察日記のObsidian書き出し / FileProvider共有 /
status.json の decisions を提案へ流し込む / カレンダーの起動時自動同期 / MCP対応(v4 Phase4)。
※ 2026-07-23 に消化: エンティティ閲覧UI(記録タブ「関連」)/ 取り込み履歴画面(設定)。

---

## 2026-07-23: GitHub連携 3-d 状況JSONへGitHub進捗を反映 — GitHub連携 完了

### 目的

ヨスガが各ゲームの**実際の**進捗(ブロッカー・質問を含む)を踏まえて提案できるようにする。

### 新規ライブラリ

- **なし**

### 実施内容

- `ProjectExport` に `source`("github" / "local")/ `health` / `blockers[]` /
  `questionsForYosuga[]` を追加
- `ContextExporter.build` に `statuses: Map<projectId, ProjectStatusSnapshot>` を追加
  - 取得済みなら status.json 由来を優先し、`statusMarkdown` を設計書19.3 の見出し構成
    (Summary / Current Goal / In Progress / Next Tasks / Blockers / Questions for Yosuga /
    Source Commit)で生成
  - `lastUpdated` は status の generatedAt を優先、`health` も status 側を優先
  - 未取得なら従来どおり手元の値(source=local)。**未取得を取得済みに見せない**
- `ExportRepository` がキャッシュを読み込んで渡す(`AppContainer` 配線)
- `docs/yosuga_prompt.md` を更新: source / blockers / questionsForYosuga の読み方と、
  「ブロッカー解消を優先度高めで提案」「質問には必ず応答し、決定したものだけ decision にする」を追記
- テスト: `ContextExporterTest` に2件(GitHub由来の優先反映 / 未取得時のローカルフォールバック)

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)

### GitHub連携の完了状況

- ✅ 3-a 設定の器(リポジトリ設定・トークンのKeystore保管)
- ✅ 3-b Ktor で取得・検証(schemaVersion / projectId 突合)
- ✅ 3-c Room v5 キャッシュと詳細画面表示・一括更新
- ✅ 3-d 状況JSONへの反映
- ⬜ 実通信・実機確認は未実施 → 再開ポイント参照

---

## 2026-07-23: GitHub連携 3-c キャッシュと表示(Room v5)

### 目的

取得した status.json を Room にキャッシュし、プロジェクト詳細に表示する。
オフラインでも直近の進捗が見える状態にする。

### 新規ライブラリ

- **なし**

### 設計判断

- **projects テーブルは上書きしない**。手動編集(1-d)と衝突させないため、GitHub 由来の情報は
  詳細画面の独立セクション「GitHub 進捗」として表示する。health の更新は従来どおり
  ヨスガの提案(承認フロー)経由で行う。
- キャッシュには**取得した本文をそのまま**保存する(未知項目を失わない)。表示時にパースする。
- 取得失敗時はキャッシュを更新しない → 直近の表示を壊さない。

### 実施内容

- **Room v4→v5**: `project_status_cache`(projectId PK / statusJson / fetchedAt)+ `MIGRATION_4_5`。
  スキーマ `5.json` との一致を確認
- `StatusFetchResult.Success` に `rawJson` を追加(キャッシュ保存用)
- `domain/model/ProjectStatusSnapshot` + `StatusLine`: 表示用ドメインモデル
  (通信DTOを UI へ渡さない方針を維持)
- `data/github/StatusSnapshotMapper`(純粋ロジック): DTO → スナップショット変換。
  空タイトルの項目は落とし、進捗率・優先度・深刻度を詳細行にまとめる
- `ProjectStatusRepository`: `statuses(): Flow<Map<projectId, Snapshot>>`(壊れた行は読み飛ばす)/
  `refresh(project)`(成功時のみキャッシュ更新)/ `refreshAll(projects)`(未設定は通信しない)。
  fetch は関数で受けてテスト可能に
- `ui/share/StatusMessage`: 取得結果 → ユーザー向け短文(次に何をすべきかを含む)、
  一括更新のサマリ文
- `ProjectDetailScreen`: 「GitHub 進捗」カード(要約 / 目標 / 作業中 / 次のタスク / ブロッカー /
  ヨスガへの質問 / 取得・生成時刻 / 状態)+「GitHubから更新」ボタン(取得中は無効)。
  リポジトリ未設定・未取得の案内も表示
- `ProjectsScreen`: 「GitHubからすべて更新」ボタン(設定済みリポジトリがある場合のみ表示)
- テスト: `ProjectStatusRepositoryTest` 6件(変換 / 成功時保存 / **失敗時にキャッシュを壊さない** /
  Flow変換 / 壊れた行のスキップ / 未設定プロジェクトを通信しない)、`StatusMessageTest` 3件

### 気づいた不具合(修正済み)

- `MIGRATION_4_5` を `addMigrations` に登録し忘れていた。**コンパイルは通るが実機更新時に
  起動クラッシュする**種類の不具合。マイグレーション追加時のチェックリストは
  ①Migration定義 ②`@Database(version)` 更新 ③`addMigrations` 登録 ④スキーマJSON突き合わせ の4点。

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)
- **実通信・実機は未検証**。トークンと `.yosuga/status.json` を用意した実リポジトリで要確認。

### 次にやること

- **3-d**: 状況JSONエクスポートへ GitHub 由来の進捗を反映(statusMarkdown の実データ化)

---

## 2026-07-23: GitHub連携 3-b Ktor で status.json を取得・検証

### 目的

GitHub Contents API から各ゲームの `.yosuga/status.json` を取得し、検証してモデル化する。
UI への反映は 3-c(この段階では取得層のみ)。

### 新規ライブラリ(追加理由)

- **Ktor Client 3.0.3**(`ktor-client-core` / `ktor-client-okhttp` /
  `ktor-client-content-negotiation` / `ktor-serialization-kotlinx-json`)
  - 理由: ユーザーと合意した HTTP クライアント。kotlinx.serialization(導入済み)と統合でき、
    エンジン差し替えで MockEngine によるユニットテストが書ける。Android は OkHttp エンジンを使用。
- **ktor-client-mock 3.0.3**(testImplementation): 実通信なしで応答を差し替えるため。
- **kotlinx-coroutines-test 1.9.0**(testImplementation): コルーチンテストの標準ユーティリティ。

### 実施内容

- `AndroidManifest.xml` に **INTERNET 権限**を追加(GitHub 取得に必須)
- `data/github/model/ProjectStatus`: 設計書19.2 の status.json モデル
  (必須は schemaVersion のみ、他はデフォルト値付き。currentGoal / completed / inProgress /
  nextTasks / blockers / recentChanges / risks / decisions / questionsForYosuga)
- `data/github/StatusParser`(純粋ロジック): `ignoreUnknownKeys` で未知項目を無視、
  schemaVersion 検証(1のみ対応)、**projectId の突き合わせ**(設計書20章の必須条件)。
  結果は sealed(Success / InvalidJson / UnsupportedSchema / ProjectIdMismatch)
- `data/github/GitHubApi`: Contents API から `Accept: application/vnd.github.raw` で本文を直接取得
  (Base64デコード不要)。`X-GitHub-Api-Version` 付与、タイムアウト設定、ブランチ指定は `?ref=`。
  HTTPステータスを FetchResult(Success/Unauthorized/NotFound/HttpError/NetworkError)へ分類。
  **例外メッセージを伝播させない**(トークンが載る事故を防ぐ)
- `data/repository/GitHubStatusRepository`: リポジトリ未設定・トークン未設定を**通信前に**判定し、
  取得〜検証結果を `StatusFetchResult` へ集約
  - トークンは `tokenProvider: suspend () -> String?` で受ける(Keystore/DataStore に依存せずテスト可能。
    当初 GitHubSettingsRepository を直接受けていたが、final クラスでフェイク化できず設計変更)
- `AppContainer` に gitHubStatusRepository を配線(tokenProvider は復号関数を渡す)
- テスト: `StatusParserTest` 7件(正常/未知項目無視/最小/壊れたJSON/schemaVersion欠落/非対応版/ID不一致)、
  `GitHubStatusRepositoryTest` 9件(MockEngine で URL・Authorization・Accept ヘッダー検証、
  ref パラメータ、未設定時は通信しない、401/403/404/500/オフライン、JSON異常の分類)

### 詰まった点

- Ktor 3 の MockEngine ハンドラは `MockRequestHandleScope` レシーバが必要で、
  素のラムダ型では `respond` が解決できない。テストの関数型を
  `MockRequestHandleScope.(HttpRequestData) -> HttpResponseData` に修正して解決。

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)
- **実通信は未検証**(実機とトークンが必要)。MockEngine による分岐検証のみ。

### 次にやること

- **3-c**: 取得結果を Room にキャッシュして表示(プロジェクト詳細に status 表示 /
  最終取得時刻 / エラー表示 / 全プロジェクト一括更新)

---

## 2026-07-23: GitHub連携 3-a 設定の器(リポジトリ設定 + トークンのKeystore保管)

### 経緯・決定

- シミュレーターで動作確認済み(5画面 + 新機能)。実機は未入手。
- ユーザーと合意: **HTTPクライアントは Ktor Client**(3-b で追加)、**リポジトリは private**
  → アクセストークンが必須。
- 各ゲームの Claude Code には `docs/claude_code_onboarding.md` を渡して `.yosuga/` を作らせる。

### 新規ライブラリ

- **なし**(3-a は保管と設定UIのみ。Ktor は 3-b で追加)
- トークン保管に `androidx.security-crypto` は**使わない**。非推奨化の経緯があるため、
  設計書9章のとおり **Android Keystore を直接利用**する(依存ゼロ)。

### 実施内容

- **Room v3→v4**: projects に `repoOwner` / `repoName` / `repoBranch`(いずれも nullable)を追加。
  `MIGRATION_3_4`(ALTER TABLE ADD COLUMN ×3)。スキーマ `4.json` と一致することを確認
  - ※ 途中 `@Database(version)` の更新漏れでスキーマJSONが生成されず気づいた。
    マイグレーション追加時は version 更新とスキーマJSON生成の確認をセットで行うこと
- `Project`(domain)に repo* と `hasRepository` を追加、Mapper 双方向対応
- **トークン保管**(`data/security/`):
  - `SecretEnvelope`(純粋ロジック): IV + 暗号文 を `Base64:Base64` で詰め替え。壊れた入力は null
  - `TokenCrypto`(interface)/ `KeystoreTokenCrypto`: AndroidKeyStore の AES-256-GCM 鍵で暗号化。
    鍵はKeystoreから取り出せない。例外時は null を返し、**平文を例外メッセージにも載せない**
  - `UserPreferencesRepository.gitHubTokenEncrypted`(**暗号化済み文字列のみ**を DataStore へ)
  - `GitHubSettingsRepository`: `hasToken: Flow<Boolean>`(UIへはトークン本体を出さない)/
    `saveToken` / `clearToken` / `currentToken()`(通信直前にのみ復号)
- 設定画面に「GitHub アクセストークン」セクション(PasswordVisualTransformation で伏字入力、
  保存後は入力欄を即クリア、削除ボタン、Fine-grained PAT の作り方を案内)
- プロジェクト編集ダイアログに owner / リポジトリ名 / ブランチ欄を追加(空欄は null = 取得対象外)。
  項目増加に伴いダイアログ内を verticalScroll 化
- テスト: `GitHubSettingsRepositoryTest`(封筒の往復 / 不正入力の拒否 / 暗号化契約)

### セキュリティ上の注意(維持すること)

- 平文トークンは DataStore にもログにも書かない。UI 状態にも保持しない(入力欄のみ、保存後クリア)。
- `KeystoreTokenCrypto` は **ユニットテスト対象外**(Keystore が必要)。実機/エミュレーターで
  保存→再起動→復号できることを確認する。端末バックアップ復元等で鍵が失われた場合は
  復号失敗 → null → 「未設定」扱いになる(再入力で復旧)。

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)

### 次にやること

- **3-b**: Ktor Client 追加 → GitHub Contents API で `.yosuga/status.json` を取得・パース・検証

---

## 2026-07-23: 記録タブの磨き込み(手動追加・編集・検索)

### 目的

記録タブを閲覧専用から編集可能へ。AI経由でなくても短い知識をその場で残せるようにする。

### 新規ライブラリ

- **なし**

### 実施内容

- `RecordsFilters` に純粋関数を追加:
  - `searchItems`(タイトル・本文・タグの部分一致、大文字小文字無視、空なら全件)
  - `parseTagsInput`(カンマ・読点区切り → trim・空除去・重複除去)
- `ItemEditDialog`: 種類チップ(6種・横スクロール)/ タイトル(必須)/ 本文 / タグ(カンマ区切り)。
  編集時のみ削除ボタン。実体(entities)の関連付けは AI 経由のみ(手動編集では触らない)
- `RecordsViewModel` に `addItem`(source=manual)/ `updateItem` / `deleteItem`
- `RecordsScreen`(アイテムセクション): 検索フィールド + [アイテムを追加] ボタン +
  カードタップで編集。検索・タグ絞込は併用可。0件時の文言を「条件に合わない」と区別
- テスト: `RecordsFiltersTest` に検索・タグ入力パースの2件を追加

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)

---

## 2026-07-23: v3-Step 3 Obsidian連携(SAF)を実装 — 動作検証は実機入手後

### 目的

承認した知識のうち長文として残すべきもの(targetNote 指定)を、SAF で選択した
Obsidian Vault のノートへ追記できるようにする(v3設計書付録の確定事項 2)。

### 新規ライブラリ(追加理由)

- **androidx.documentfile:documentfile 1.0.1**
  - 理由: SAF のツリーURI(OpenDocumentTree で選択した Vault)配下のファイル探索・作成を
    安全に行う公式ヘルパー。生の DocumentsContract より簡潔で、依存は極小。

### 実施内容

- `data/obsidian/KnowledgeStore`(interface)+ `AppendOutcome`(WRITTEN / NOT_CONFIGURED / FAILED)
  - 書き出し先の抽象化。将来 GitHub / Drive へ差し替え可能(v3付録の方針)
- `data/obsidian/ObsidianMarkdown`(純粋ロジック):
  - `normalizeNoteName`(パス除去・`.md` 保証・空なら YosugaHub.md)
  - `buildNoteAppendix`(## タイトル + 本文 + #タグ + 日付。タグ中の空白は `_` へ)
- `data/obsidian/ObsidianVaultStore`: DocumentFile でノートを探索/作成し、
  `openOutputStream(uri, "wa")` で末尾追記。権限失効等は FAILED(クラッシュしない)
- `UserPreferencesRepository`: `obsidianVaultUri` を DataStore に追加
- 設定画面: 「Obsidian Vault」セクション新設(OpenDocumentTree で選択 →
  `takePersistableUriPermission` で読み書き権限を永続化 → URI保存。選択中フォルダ名を表示)。
  `SettingsViewModel` を新設
- 回答JSON v2 の `ItemProposal` に `targetNote`(任意)を追加
  - 承認時: Room 保存後、targetNote があれば Vault の該当ノートへ追記
  - **書き出しの成否に関わらず Room 保存は成立**(Obsidianは付加的)
  - `ApproveResult.Applied` が書き出し結果を持ち、Toast で通知
    (追記済み / Vault未設定 / 失敗)
  - 提案カードに「Obsidian: ノート名」を表示
- `docs/yosuga_prompt.md` に targetNote の書き方・挙動を追記
- テスト: `ObsidianMarkdownTest` 3件、`ProposalRepositoryTest` に targetNote 3件
  (書き出し+markdown内容 / Vault未設定でもRoom成立 / targetNote無しはスキップ)

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)
- **実機での動作検証は未実施**(SAF フォルダ選択・実 Vault への追記・Obsidian アプリでの表示確認)。
  `createFile("text/markdown", "xxx.md")` の拡張子挙動はプロバイダ依存のため実機で要確認

### 次にやること

- ④ 記録タブの磨き込み(アイテムの手動追加・編集・検索)

---

## 2026-07-23: ヨスガ用プロンプト仕様書 + ホームのAI秘書再編(v3-Step 4 相当)

### 経緯

実機が手元にないため、実機確認は保留して先へ進む(ユーザーと合意)。
選択された作業: ①プロンプト仕様書 → ②ホーム再編 → ③Obsidian連携 → ④記録タブ磨き込み。

### ① `docs/yosuga_prompt.md` 新設(コミット 5904c56)

- ChatGPT に貼るシステムプロンプト(ヨスガの役割 / 状況JSONの読み方 / 回答JSON v2 の書式とルール /
  観察日記の書き方 / タグ運用ルール)
- アプリ側の取り込み挙動の対応表(スキーマ判定・スキップ条件・自動棄却)
- 動作確認用の最小サンプルJSON
- これで手動JSONブリッジの ChatGPT 側の準備が完成

### ② ホームのAI秘書再編

- `ui/screen/home/TodayFocus`(純粋関数): 「今日やること」を本物のタスクから導出
  - 完了除外。作業中 → 期限切れ → 今日締切 → 優先度 → 締切近い順。最大5件
  - 旧「優先タスク」プレースホルダー(`ProjectRepository.priorityTask`)を削除
- `HomeViewModel`: タスク・承認待ち件数・最近の決定(最大3件)を secretaryFlow に集約して combine
  (combine の5フロー制限対策で中間データにまとめる)。`todayDate` は注入可能
- `HomeScreen`:
  - 「承認待ちの提案」カード(1件以上のときだけ表示。ヨスガ画面へ誘導)
  - 「今日やること」カード(タスク名 + プロジェクト名/優先度/締切。0件時の表示あり)
  - 「最近の決定」カード(決定事項ログの新しい3件。0件時は非表示)
  - 「優先タスク」カードを削除。それ以外(予定・進捗・JSONボタン・最終同期)は維持
- テスト: `TodayFocusTest` 4件(完了除外 / doing優先 / 期限切れ→今日→優先度 / 件数制限)

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)

### 次にやること

- ③ Obsidian連携(SAF・KnowledgeStore 抽象化)の実装(動作検証は実機入手後)

---

## 2026-07-23: v3-Step 2-e 状況JSONエクスポート v2 — v3-Step 2 完了

### 目的

状況JSONにタスク・最近の決定事項を含め、AIが現状を踏まえた提案を返せるようにする。

### 新規ライブラリ

- **なし**

### 実施内容

- `ContextExport` を v2 へ: `SCHEMA_VERSION = 2`、`tasks[]` / `recentDecisions[]` を追加。
  `responseSchemaVersion = 2` も追加(「proposals 形式で返してほしい」という宣言)
- `TaskExport`(projectId/title/detail/status/priority/dueDate)/ `DecisionExport`(date/title/body)
- `ContextExporter.build` に tasks / decisions パラメータを追加(既定は空リスト)
- `ExportRepository`: タスク全件 + 決定事項(新しい順に最大20件)をスナップショットに追加
- `AppContainer`: ExportRepository へ taskRepository / knowledgeRepository を配線
- テスト: `ContextExporterTest` を v2 に更新 + タスク/決定事項マッピングのテストを追加

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)

### v3-Step 2 完了条件の達成状況

- ✅ 2-a: Room v3(知識ベースのデータ層)
- ✅ 2-b: 回答JSON v2 取り込み(v1互換維持)
- ✅ 2-c: 提案レビューUI(承認→反映 / 棄却→履歴)
- ✅ 2-d: 「記録」タブ(アイテム・決定・日記、タグ絞込)
- ✅ 2-e: 状況JSONエクスポート v2
- ⬜ 実機確認は未実施 → 再開ポイント参照

**→ v3-Step 2 完了。次は実機確認、その後 Step 3(Obsidian連携)等を相談。**

---

## 2026-07-23: v3-Step 2-d 「記録」タブ新設(知識ベース閲覧・6タブ化)

### 目的

承認済みの知識(情報アイテム・決定事項ログ・観察日記)を閲覧する「記録」タブを追加する。
Step 2 設計の合意どおり6タブ化(ホーム/カレンダー/プロジェクト/記録/ヨスガ/設定)。

### 新規ライブラリ

- **なし**

### 実施内容

- `YosugaDestination` に `Records`(記録、Icons.Filled.Create)を追加し NavHost へ配線
- `ui/screen/records/RecordsViewModel`: items + tagNames + diaryEntries を combine
- `ui/screen/records/RecordsFilters`(純粋関数): `filterItemsByTag`(null=すべて)/
  `decisionsOf`(kind=DECISION 抽出)
- `ui/screen/records/RecordsScreen`:
  - セクション切替チップ(アイテム / 決定 / 日記)。選択状態は rememberSaveable で保持
  - アイテム: タグ絞込チップ(横スクロール、再タップで解除)+ カード
    (日付・種別チップ・本文・#タグ・関連実体)
  - 決定: 決定事項ログを時系列カードで表示(日付 + タイトル + 本文)
  - 日記: 観察日記を日付ごとのカードで表示
  - 各セクションの空状態テキストに対応
- `ui/component/KindLabels`: itemKindLabel を共用化(ProposalCardUi の private 版を移設)
- テスト: `RecordsFiltersTest`(タグ絞込 / 全件 / 決定事項抽出)3件

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)

### 次にやること

- **2-e**: 状況JSONエクスポート v2(タスク・決定事項を含める)→ Step 2 完了

---

## 2026-07-23: v3-Step 2-c 提案レビューUI(ヨスガ画面を承認ハブ化)

### 目的

承認待ち提案をヨスガ画面にカード表示し、承認で本テーブルへ反映・棄却で履歴化する。
v3 の中核「提案→ユーザー承認→保存」を初めて一気通貫にする。

### 新規ライブラリ

- **なし**

### 実施内容

- `data/file/ProposalPayloads`: payloadJson の復号(壊れていたら null、クラッシュさせない)
- `ProposalRepository`(新設): pending監視 / approve / reject
  - 承認の反映先: task→`TaskRepository.create(source=assistant)` / item→`KnowledgeRepository.createItem`
    (タグ・実体は名前解決で自動作成)/ diary→`DiaryRepository.add`(日付欠落は受信日で補完)/
    health→`ProjectRepository.updateHealth`(新設。AI分析由来の自由値も受け入れ、lastUpdatedを刻む)
  - 反映できない提案(壊れたpayload・対象プロジェクト不明)は rejected へ回す(`ApproveResult.NotApplicable`)
- `TaskRepository.create` に `source` パラメータを追加(既定 manual)
- `ProjectDao.updateHealth` を追加(更新行数を返す)
- `ui/screen/assistant/ProposalCardUi`: 表示モデルへの純粋変換
  - 種別ラベル(タスク/メモ/アイデア/決定事項/買い物/技術/観察日記/状態更新)、
    タグは `#タグ名`、壊れたpayloadは「(読み取れない提案)」として承認ボタン無効化
- `AssistantViewModel`: pending + recommendations を combine、approve/reject を追加
- `AssistantScreen`: 「承認待ちの提案(N)」セクションを追加(種別チップ + 内容 + 棄却/承認ボタン、
  結果は Toast)。既存の v1 提案一覧・JSON作成/取り込みはそのまま
- `AppContainer`: proposalRepository を配線
- テスト: `ProposalRepositoryTest`(7件: 4種の承認反映 / 不明プロジェクト / 壊れたpayload / 棄却)、
  `ProposalCardUiTest`(5件)。既存 `ProjectRepositoryTest` の FakeProjectDao に updateHealth を追加

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)

### 次にやること

- **2-d**: 「記録」タブ新設(アイテム一覧・タグ絞込・決定事項ログ・日記)
- **2-e**: 状況JSONエクスポート v2(タスク・決定事項を含める)

---

## 2026-07-23: v3-Step 2-b 回答JSON v2 の取り込み(pending_proposals へ)

### 経緯メモ

- 実機確認(v1→v3マイグレーション・Step 1 の操作一連)をユーザーが Windows Studio で実施し、問題なし。
- アシスタント名の表記を「よすが」→「ヨスガ」(カタカナ)に統一(UI 2箇所 + コメント + 設計書)。

### 目的

回答JSON v2(proposals)を取り込み、直接反映せず pending_proposals へ積む。
v1(recommendations)の取り込み互換は維持。UIは変更なし(承認UIは 2-c)。

### 新規ライブラリ

- **なし**

### 実施内容

- `data/file/model/AssistantResponseV2`: v2モデル(proposals.tasks / items / diary / projectHealth、
  各項目デフォルト値付き・未知項目無視)
- `ResponseImporter`: v1/v2 両対応(`SCHEMA_VERSION_V1/V2`、`ParseResult.SuccessV2` 追加)。
  v3以上は従来どおり UnsupportedSchema
- `data/file/ProposalMapper`(純粋オブジェクト): proposals → PendingProposalEntity 行へ変換
  - 意味を持たない提案(空タイトル・空本文・projectId欠落health)は読み飛ばす
  - payloadJson に提案1件分のJSONを保存(解釈は 2-c の承認処理)
- `ImportRepository`: SuccessV2 経路を追加(履歴保存は v1 と共通化 → pending_proposals へ insert)。
  `ImportResult.SuccessProposals(proposalCount, fileName)` を追加
- `ImportMessage`: 「提案を N 件受け取りました。ヨスガ画面で承認してください。」
- `AppContainer`: ImportRepository に pendingProposalDao を配線
- テスト: `ResponseImporterTest` に v2 パース・proposals欠落デフォルト2件、
  `ProposalMapperTest`(4種変換 / payload往復 / 無意味提案スキップ / 空)4件を追加

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)

### 次にやること

- **2-c**: 提案レビューUI(ヨスガ画面を承認ハブ化、承認で本テーブルへ反映)

---

## 2026-07-23: v3-Step 2-a 知識ベースのデータ層(Room v3)

### 目的

Step 2 設計(下記エントリ)のデータモデルを Room に実装。UIは変更なし。

### 新規ライブラリ

- **なし**

### 実施内容

- ドメインモデル: `KnowledgeItem`(+`ItemKind`/`EntityRef`)/ `TrackedEntity`(+`EntityType`)/
  `DiaryEntry` / `PendingProposal`(+`ProposalType`/`ProposalStatus`)
  - enum は未知値フォールバック(ItemKind→OTHER 等)。ProposalType のみ null 返却で読み飛ばし
  - Room の Entity と紛らわしいため実体は Tracked を冠して `TrackedEntity` と命名
- Roomエンティティ7つ: `knowledge_items` / `tags`(name unique)/ `item_tags`(複合PK)/
  `entities`((name,type) unique)/ `item_entities` / `diary_entries`(date index)/ `pending_proposals`
- **Room v2→v3**: `MIGRATION_2_3`(7テーブル+5インデックス追加、既存データ無傷)。
  出力スキーマ `3.json` と SQL の完全一致を確認済み
- `KnowledgeDao`: アイテム+タグ+実体を1つのDAOに集約
  - `observeItemsWithRefs()`(@Relation + Junction で3者をまとめて読む)
  - `saveItemWithRefs()`(@Transaction default メソッド): タグ・実体を名前で解決し
    未登録なら候補を登録、中間テーブルは張り直し
- `DiaryDao` / `PendingProposalDao`(status別監視・件数Flow)
- `KnowledgeRepository`: `createItem`(タグの trim・空除去・重複除去)/ `updateItem`(createdAt不変)/
  `deleteItem`(リンクは消しタグ本体は残す=タグの統合・削除はAIの仕事)
- `DiaryRepository`: `entries` / `add` / `delete`
- `SampleSeed`: 仮アイテム3件(買い物/決定/アイデア)+タグ3+実体2+観察日記1(Yosuga視点の文例)
- `AppContainer`: 両Repository追加、`MIGRATION_2_3` 登録、空のときシード
- テスト: `KnowledgeRepositoryTest`(フェイクDAOで default メソッドの実ロジックを検証:
  タグ名解決・再利用 / 正規化 / リンク張り直し / 削除でタグ残存 / ドメイン変換)、
  `SampleSeedTest` に知識ベース検証5件を追加

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)
- マイグレーション v2→v3 の実機検証は未実施(v1→v2 と合わせて Windows Studio で確認)

### 次にやること

- **2-b**: 回答JSON v2 パース → pending_proposals へ(v1互換維持)

---

## 2026-07-23: v3-Step 2(v3.1統合)の詳細設計を確定 — 記録のみ

### ユーザーと合意した決定

1. **知識ベースの閲覧UIは「記録」タブを新設(6タブ化)**。タブ内で アイテム / 決定 / 日記 をセグメントボタン切替、タグで絞込。
2. **タグは当面 knowledge_items のみ**。タスク・日記への拡張は将来の追加マイグレーションで。

### データモデル(Room v2→v3、テーブル追加のみ・既存データ無傷)

- `knowledge_items`: id(UUID) / kind(memo|idea|decision|shopping|tech|other) / title / body /
  createdAt / updatedAt / source(assistant|manual)。決定事項ログ=kind:decision、保留アイデア=kind:idea
- `tags`: id / name(unique)。AI管理、承認時に自動作成
- `item_tags`: itemId×tagId(複合PK、多対多)
- `entities`: id / name / type(project|person|tech|gear|event|other)。(name,type)でunique
- `item_entities`: itemId×entityId(複合PK)
- `diary_entries`: id / date(yyyy-MM-dd) / body / createdAt。Yosuga視点の観察日記
- `pending_proposals`: id / type(task|item|diary|health) / payloadJson / status(pending|approved|rejected) / receivedAt
  - 取り込みはまずここへ。承認で本テーブルへ反映、棄却は status 変更で履歴として残す
- 外部キー制約は張らない方針を継続

### 回答JSONスキーマ v2(schemaVersion=2)

- `proposals.tasks[]`(projectId/title/detail/priority/dueDate)
- `proposals.items[]`(kind/title/body/tags[名前]/entities[{name,type}])
- `proposals.diary[]`(date/body)
- `proposals.projectHealth[]`(projectId/health/reason)
- v1(recommendations)の取り込み互換は維持。15章ルール踏襲(未知項目無視・不正JSONで落ちない)
- 状況JSON(エクスポート)も v2 へ: タスク一覧・最近の決定事項を含める

### 実装順(1ステップ=1コミット)

- **2-a**: Room v3 + Repository(データ層のみ)
- **2-b**: 回答JSON v2 パース → pending_proposals(v1互換維持)
- **2-c**: 提案レビューUI(よすが画面を承認ハブ化)
- **2-d**: 「記録」タブ新設(アイテム一覧・タグ絞込・決定事項ログ・日記)
- **2-e**: 状況JSONエクスポート v2

---

## 2026-07-23: v3-Step 1-d プロジェクト編集 — v3-Step 1 完了

### 目的

プロジェクト情報(name / currentGoal / health)を編集できるようにし、v3-Step 1 を完了させる。

### 新規ライブラリ

- **なし**

### 実施内容

- `ProjectDao.upsert()` を追加、`Mappers` に `Project.toEntity()` を追加
- `ProjectRepository.upsert()`: lastUpdated をここで刻む(`now` 注入可能、表示形式は
  既存の `formatSyncTime` と同じ "yyyy-MM-dd HH:mm")
- `ProjectDetailViewModel.updateProject()` を追加
- `ui/screen/projectdetail/ProjectEditDialog`: name(必須)/ 目標 / 状態(FilterChip 4値)
  - **inProgress / nextTask は編集対象外**(タスクからの導出へ置き換える予定のため)
  - 手動編集で選べる health は v2 語彙の4値のみ。AI分析由来の自由な値(v3.1)は表示のみで受け入れる
- 詳細画面の情報カードに「プロジェクトを編集」ボタンを追加
- テスト: `ProjectRepositoryTest` に upsert の lastUpdated 刻印・編集対象外フィールド保持を追加
  (FakeProjectDao を書き込み対応に更新)

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)

### v3-Step 1 完了条件の達成状況

- ✅ Task が第一級エンティティ(Room v2 / Repository / 追加・編集・完了・削除)
- ✅ プロジェクト詳細画面(ネストナビ)+ プロジェクト編集
- ⬜ 実機確認(マイグレーション・一連の操作)は未実施 → 次回の再開ポイント参照

**→ v3-Step 1 完了。次は v3-Step 2(v3.1統合の詳細設計)。**

---

## 2026-07-23: v3-Step 1-c タスクの追加・編集・完了・削除

### 目的

プロジェクト詳細画面のタスクを実際に操作できるようにする(初の書き込みUI)。

### 新規ライブラリ

- **なし**

### 実施内容

- `TaskRepository` の書き込みを拡張
  - `create()`: 新規タスクの採番・保存(`newId` を注入可能に。既定は UUID)。source は "manual"
  - `upsert()`: completedAt を status と整合させるよう強化
    (編集で DONE → 刻む / DONE 解除 → クリア / 既に DONE → 元の時刻を保持)
- `ProjectDetailViewModel` に `addTask` / `updateTask` / `setTaskDone` / `deleteTask` を追加
- `ui/screen/projectdetail/TaskEditDialog`: 新規・編集共用ダイアログ
  - タイトル(必須)/ 詳細 / 優先度(高・中・低 FilterChip)/ 状態(未着手・進行中・完了)/
    締切(yyyy-MM-dd テキスト入力、不正時はエラー表示と保存無効)/ 編集時のみ削除ボタン
  - DatePicker は使わない(M3 では experimental API のため。個人アプリの入力頻度なら手入力で十分と判断)
- `ui/screen/projectdetail/TaskForm`: 締切検証 `isValidDueDateInput`(LocalDate.parse で実在日チェック)と
  `dueDateForSave`(空欄→null)を純粋関数として切り出し
- `ProjectDetailScreen`: [タスクを追加] ボタン、行チェックボックス(完了⇄未着手)、行タップ→編集ダイアログ
- テスト: `TaskFormTest`(空欄可 / ISO日付 / 不正・実在しない日付)、
  `TaskRepositoryTest` に create の採番・時刻 / DONE作成時のcompletedAt / upsert のcompletedAt保持・クリアを追加

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)
- 実機での操作確認(追加→編集→完了→削除の一連)は未実施(Windows 側 Android Studio で確認する)

### 次にやること

- **1-d**: プロジェクト編集(name / currentGoal / health)→ これで v3-Step 1 完了

---

## 2026-07-23: 追加仕様 v3.1(知識ベース・タグ・観察日記)を設計へ取り込み — 記録のみ

### 経緯

ユーザーから追加仕様(情報整理 / タグシステム / エンティティ / 決定事項ログ / 保留アイデア /
健康状態のAI分析 / Yosuga視点の観察日記 / 開発ログ統合)の相談。影響分析の結果、
1-a/1-b の実装とは衝突せず、**Step 2 のJSONスキーマ設計前の今が取り込みの最適タイミング**と判断。

### ユーザーと合意した決定

1. **知識の置き場所**: Hub=構造化された短い知識(タグ付きアイテム・決定事項・保留アイデア・
   観察日記・エンティティ)を Room DB で。Obsidian=長文(設計思想・仕様書等)。v3の役割分担を精緻化。
2. **タイミング**: Step 1(1-c/1-d)を先に完了させ、v3.1 は Step 2 の再設計に織り込む。

### 実施内容(ドキュメントのみ)

- `yosuga_hub_android_design_v3_1.md` を新設(追加仕様本文 + 付録「v3との整合」)
- `CLAUDE.md`: 設計書参照に v3.1 を追加、ロードマップの Step 2 を「v3.1織り込み再設計」に更新
- タグ生成・統合、エンティティ関連付け、健康分析、日記執筆はすべて AI側の仕事。
  Hub は保存・表示・編集・検索のみ(v3の役割分担・承認フローを踏襲)

### 次にやること

- 1-c(タスク追加・編集・完了・削除)→ 1-d(プロジェクト編集)→ Step 2 詳細設計(v3.1統合)

---

## 2026-07-23: v3-Step 1-b プロジェクト詳細画面(読み取りのみ・初のネストナビ)

### 目的

プロジェクト一覧のカードをタップして詳細(プロジェクト情報 + タスク一覧)を見られるようにする。
表示のみで編集はまだない(1-c / 1-d で追加)。下部ナビ外のネストルートを初導入。

### 新規ライブラリ

- **なし**

### 実施内容

- `ui/navigation/ProjectDetailRoute`: ネストルート定義(`project_detail/{projectId}`)
- `ui/screen/projectdetail/TaskGrouping`: 状態別グルーピング + 並び順(優先度 → 締切(なしは最後)→ タイトル)を
  純粋関数 `groupTasks()` として切り出し(ユニットテスト可能)
- `ui/screen/projectdetail/ProjectDetailViewModel`
  - `SavedStateHandle`(`createSavedStateHandle()`)から projectId を受け取る初の ViewModel
  - projects + tasksForProject を combine し `ProjectDetailUiState`(isLoading / project / GroupedTasks)を公開
- `ui/screen/projectdetail/ProjectDetailScreen`
  - 戻るボタン + プロジェクト情報カード + タスクを「進行中 / 未着手 / 完了」のセクション表示
  - 完了タスクは打ち消し線 + 淡色。メタ行に優先度(高/中/低)・締切・詳細を表示
  - プロジェクトが見つからない場合の表示、タスク0件の表示にも対応
- `ui/component/ProjectHealth`: healthLabel を一覧・詳細で共用化(ProjectsScreen の private 版を削除)
- `ProjectsScreen`: カードを `Card(onClick=...)` 化し `onProjectClick(projectId)` を公開
- `YosugaHubApp`: NavHost に詳細ルートを追加、詳細表示中は「プロジェクト」タブを選択状態に維持
- テスト: `TaskGroupingTest`(状態別グルーピング / 優先度→締切→タイトルの並び / 未知priorityは最後 / isEmpty)

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)
- 実機/エミュレーターでの画面遷移確認は未実施(Windows 側 Android Studio で確認する)

### 次にやること

- **1-c**: タスク追加・編集・完了・削除(編集ダイアログ、TaskRepository の書き込みを配線)

---

## 2026-07-23: v3-Step 1-a Task のデータ層(Entity/DAO/Migration/Repository/シード)

### 目的

v3-Step 1 の詳細設計(下記エントリ)どおり、Task を第一級エンティティとしてデータ層に導入。
UIは一切変更していない(見た目は従来どおり)。UI は 1-b 以降で追加する。

### 新規ライブラリ

- **なし**(Room / coroutines 既存分で完結)

### 実施内容

- `domain/model/Task` + `TaskStatus`(enum: TODO/DOING/DONE、未知値は TODO へフォールバック)
- `data/local/db/entity/TaskEntity`(テーブル `tasks`、`projectId` に index)
  - 外部キー制約は張らない: プロジェクトは将来 GitHub 由来へ差し替わるため、参照切れでタスクを失わない方針
- `data/local/db/dao/TaskDao`(observeAll / observeByProject / count / insertAll / upsert / updateStatus / deleteById)
- **Room v1→v2**: `data/local/db/Migrations.kt` に `MIGRATION_1_2`(CREATE TABLE tasks + index、既存データ無傷)
  - `exportSchema = true` 化 + `build.gradle.kts` に `ksp { arg("room.schemaLocation", "$projectDir/schemas") }`
  - 出力された `app/schemas/.../2.json` の createSql がマイグレーションSQLと一致することを確認(Git管理する)
  - v1 のスキーマJSONは存在しない(当時 exportSchema=false)。以後の変更は出力JSONで検証できる
- `data/repository/TaskRepository`(tasks / tasksForProject / upsert / setStatus / delete)
  - `now` を注入可能にしてテスト容易性を確保。upsert は updatedAt を刻む(createdAt は不変)
  - setStatus: DONE で completedAt を刻み、戻したらクリア
- `Mappers`: `TaskEntity.toDomain()` + 書き込み用の `Task.toEntity()`(status は enum⇄文字列変換)
- `SampleSeed.tasks` 5件(3プロジェクトの nextTask/inProgress 相当 + 完了済み1件 + プロジェクト外1件 `projectId=null`)
- `AppContainer`: `taskRepository` 追加、`addMigrations(MIGRATION_1_2)`、空のとき tasks もシード
- テスト: `TaskRepositoryTest`(変換 / フィルタ / upsert の時刻刻印 / setStatus の completedAt 制御 / 未知status フォールバック)、
  `SampleSeedTest` にタスク検証5件を追加(ID一意 / projectId 参照整合 / status・priority 語彙 / completedAt 整合)

### テスト結果

- WSL で `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)
- **マイグレーション v1→v2 の実機検証は未実施**: 旧DBが入った端末/エミュレーターでの起動確認は
  Windows 側 Android Studio での実行時に行う(Room の migration テストは instrumentation が必要なため
  ユニットテストでは代替としてスキーマJSONとSQLの一致を確認した)

### メモ(ビルド環境)

- Windows 側 Android Studio のセットアップは完了済み(`local.properties` に Windows SDK パスが生成されていた)。
  今回は WSL ビルドのため一時的に WSL 用へ差し替え、ビルド後に Windows 用へ戻した。
- Windows 側で Gradle Sync に失敗する場合は `.gradle` / `build` / `app/build`(WSLビルドで再生成された
  キャッシュ)を削除してから Sync し直すこと。

### 次にやること

- **1-b**: プロジェクト詳細画面(読み取りのみ: プロジェクト情報 + タスク一覧、初のネストナビ)

---

## 2026-07-23: v3-Step 1(Task実体化)の詳細設計を確定 — 記録のみ(コード変更なし)

### ユーザーと合意した決定

1. **status は3状態**: `todo` / `doing` / `done`(最小構成。停滞はプロジェクト側 health で表現済み)。
2. **プロジェクト外タスクを許可**: `projectId` は null 可(TGS準備・事務など。会話からの抽出で必ず出る)。
3. **UIはプロジェクト詳細画面を新設**: 一覧カードタップ → 詳細(プロジェクト情報編集 + タスク一覧/追加/編集)。
   下部ナビ外のネストルート(初のネストナビ)。プロジェクト外タスクはホームの「今日やること」側で扱う(将来)。

### データモデル(確定)

- `Task`(domain)/ `TaskEntity`(テーブル `tasks`、`projectId` に index):
  `id`(UUID文字列, PK)/ `projectId`(String?)/ `title` / `detail` /
  `status`(todo|doing|done)/ `priority`(high|medium|low、Recommendationと同語彙)/
  `dueDate`(String? "yyyy-MM-dd")/ `createdAt` / `updatedAt`(ISO 8601)/
  `completedAt`(String?、完了履歴の土台)/ `source`(manual|assistant、Step 2 の承認由来記録用)
- **Room v1→v2**: `CREATE TABLE tasks` の追加マイグレーション(既存データ無傷)。
  これを機に `exportSchema = true` + スキーマJSON出力を有効化(Phase 2-2 の予告どおり)。
- `TaskDao` + `TaskRepository`(observeAll / observeByProject / upsert / setStatus / delete)+ Mapper。
- `SampleSeed` に仮タスク数件(既存 nextTask 相当 + プロジェクト外1件)。

### 既存を壊さないための割り切り

- `Project.inProgress` / `nextTask` の文字列は当面残す(タスクからの導出は後段で判断)。
- ホーム「優先タスク」も当面既存表示のまま。
- 編集UIは title / detail / priority / dueDate / status のみ。並び替え・検索・繰り返しは作らない。

### 実装順(1ステップ=1コミット、都度ビルド+テスト)

- **1-a**: Entity / DAO / Migration / Repository / シード(UIなし)+ テスト
- **1-b**: プロジェクト詳細画面(読み取りのみ: 情報 + タスク一覧)
- **1-c**: タスク追加・編集・完了・削除(編集ダイアログ)
- **1-d**: プロジェクト編集(name / currentGoal / health)

新規ライブラリ: **なし**。

---

## 2026-07-23: 設計を v3(AIファースト)へ転換 — 記録のみ(コード変更なし)

### 目的

設計方針を v2(状況JSON ↔ ChatGPT の橋渡しダッシュボード)から
**v3「AIファースト」**(AI=頭脳 / Yosuga Hub=記憶・表示装置)へ転換。今回は方針の確定と
記録に留め、コードは一切変更していない(プロジェクトルール「一度に大規模な変更を行わない」)。

### v3の核心

- **AI=頭脳 / Yosuga Hub=記憶・表示装置**。アプリは考えず、AIが整理した結果を保存・表示・編集・検索する。
- **知識はObsidian(端末内Vault)、制作管理はHub**。会話から知識を適切なノートへ振り分け、リンク/タグも提案。
- **提案 → ユーザーがOK → 保存**(自動更新しない・安全優先)。Android単体でクラウド同期なし。

### 今回の確定事項(ユーザーと合意)

1. **今回の範囲**: 設計記録 + ロードマップ改訂まで。実装は次の合意後。
2. **Obsidian連携**: SAF で実 Vault フォルダを選択し `DocumentFile` で `.md` 読み書き。URI権限は永続化。
3. **AIブリッジ**: 当面は手動JSONブリッジを継続(既存の export/import 機構を再利用)。将来のAPI直結に備え差し替え可能に保つ。

### 実施内容(ドキュメントのみ)

- `yosuga_hub_android_design_v3.md` を新設(v3本文 + 付録「実装方針の確定事項」+ v3ロードマップ)。
- `CLAUDE.md`: 設計書参照を v3 に更新、Current Phase を v3ロードマップへ更新(v2は技術資料として有効と明記)。
- `docs/WORKLOG.md`: 本エントリを追記。

### 現状の実装 → v3 マッピング

- 再利用する土台(実装済み): 5画面 + Room + DataStore + ViewModel/Repository分離 + 状況JSON export/回答JSON import。
- 主な未実装ギャップ:
  - **Task の第一級エンティティ化 + 編集UI**(今は Project に文字列埋め込み、全画面が読み取り専用シード)。
  - **Obsidian連携**(SAFでVault選択・ノート振り分け・リンク/タグ提案)。完全新規。
  - **提案→承認フロー**(回答JSON取り込みを「提案レビュー画面」へ発展)。

### v3ロードマップ(旧 Phase 3「GitHub進捗取得」は後ろへ延期)

- **v3-Step 1**: Task を第一級エンティティ化(TaskEntity/DAO/Repository + タスク/プロジェクト編集の最小UI)。
- **v3-Step 2**: 提案→承認フローの明確化(回答JSONスキーマ拡張: `targetNote`/リンク/タグ、schemaVersion更新)。
- **v3-Step 3**: Obsidian連携(SAF Vault選択・URI権限永続化・ノート追記・`KnowledgeStore`抽象化)。
- **v3-Step 4**: 会話中心の統合表示(ホームをAI秘書視点へ再編)。
- 将来: GitHub Knowledge Repository / Claude Code status統合 / ストレージ抽象化拡張 / AI API直結。

### 次にやること

- v3-Step 1(Task実体化)の着手可否をユーザーと合意 → 合意後に最小実装 + ビルド確認。

---

## 2026-07-22: Phase 2-5 JSONインポート(回答JSON取り込み) — Phase 2 完了

### 目的

Phase 2 の最後の完了条件「ChatGPT回答JSNを取り込める」を満たす。設計書2.3「ChatGPTから
アプリへ」の回答JSNを、設計書15章のルール(schemaVersion検証 / 未知項目は無視 /
不正JSNでクラッシュさせない / 元ファイルを上書きせず履歴を残す)に沿って取り込む。
ホーム・よすが画面の「未実装Toast」だった取り込みボタンを実機能化。

### 新規ライブラリ

- **なし**(kotlinx.serialization は導入済み、ファイル選択は既存の activity-compose を使用)

### 実施内容

- 回答JSNモデル `data/file/model/AssistantResponse`(`@Serializable`)+ 補助モデル
  (`RecommendationImport` / `SuggestedTaskImport` / `SchemaProbe`)を新設
  - 設計書2.3の回答JN構造。必須は `schemaVersion` のみ、他はデフォルト値付き
- `data/file/ResponseImporter`(純粋オブジェクト)を新設
  - `Json { ignoreUnknownKeys = true; isLenient = true }` で未知項目を無視
  - まず `SchemaProbe` で schemaVersion を先読みして対応可否を判定
  - 結果を sealed `ParseResult`(Success / InvalidJson / UnsupportedSchema)で返す
  - `SerializationException` 等を捕捉し、不正JSNでもクラッシュさせない
- `data/repository/ImportRepository` を新設
  - 選択された Uri を `contentResolver` で読み、`ResponseImporter` で検証
  - 成功時のみ `imports/response_<日時>.json` へ履歴保存(上書きしない)し、
    提案を Room の recommendations テーブルへ反映(deleteAll → insertAll で置き換え)
  - 結果を sealed `ImportResult`(Success / InvalidJson / UnsupportedSchema / ReadError)で返す
- `RecommendationDao.deleteAll()` を追加
- `DefaultAppContainer` / `AppContainer` に `importRepository` を追加
- `ui/share/ImportMessage`: `ImportResult` をユーザー向け短文へ変換(設計書8章)
- `HomeViewModel` / `AssistantViewModel` に `importResponse(uri, onResult)` を追加
- ホーム「ChatGPT回答JSNを取り込む」/ よすが「回答JSNを取り込む」を実機能化
  - `ActivityResultContracts.OpenDocument()` でファイル選択 → 取り込み → 結果Toast
  - Room が Flow で提案テーブルを監視しているため、取り込み後に一覧が自動更新される
- 設定画面「JSN保存先」の表示を現状に合わせて更新(内部の exports / imports に保存)
- テスト: `ResponseImporterTest`(正常 / 不明項目無視 / 不正JSN / 非対応バージョン /
  schemaVersion欠落)を追加

### テスト結果

- `assembleDebug` + `testDebugUnitTest` 成功(BUILD SUCCESSFUL)

### Phase 2 完了条件の達成状況

- ✅ アプリが状況JSNを生成できる(Phase 2-4)
- ✅ ChatGPT回答JSNを取り込める(本ステップ)
- ✅ 再起動後もデータが残る(Phase 2-2 Room / 2-3 DataStore)

**→ Phase 2 完了。次は Phase 3(GitHub進捗取得)。**

### 今後の改善候補(任意)

- FileProvider によるファイル添付共有(現状はテキスト共有)
- Storage Access Framework で保存先フォルダを選択(現状は内部固定)
- 取り込み履歴一覧の画面表示、schemaVersion 非対応時の詳細案内

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
