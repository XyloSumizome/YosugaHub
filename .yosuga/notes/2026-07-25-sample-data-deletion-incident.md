---
type: decision
project_id: yosuga-hub
game: Yosuga Hub
category: data-lifecycle
created_at: 2026-07-25T11:00:00+09:00
updated_at: 2026-07-25T11:00:00+09:00
source: claude-code
tags:
  - Yosuga Hub
  - decision
  - data-lifecycle
  - incident
related_files:
  - app/src/main/java/com/shiro/yosugahub/ui/screen/projectdetail/ProjectEditDialog.kt
  - app/src/main/java/com/shiro/yosugahub/ui/screen/projects/ProjectsScreen.kt
  - app/src/main/java/com/shiro/yosugahub/di/AppContainer.kt
commit:
  hash: 927c412
  branch: master
---

# 変更概要

「サンプルデータを削除」を押したら**全ゲームのノート取り込みが壊れた**。
原因を追って、サンプルデータを機能ごと廃止し、
**プロジェクト追加UI(それまで存在しなかった)**を新設した。

## 変更理由

### 何が起きたか

設定の「サンプルデータを削除」を実行したあと、それまで成功していた
`IMPORT NOTES` が全ゲームで0件になった。エラーも出なかった。

`projects` テーブルを見ると**0件**。GitHub の進捗キャッシュも0件だった。

### なぜ起きたか

`anri` / `paper-armor-frog` / `gengenkyo` は **もともと `SampleSeed` の ID** だった。
シロさんはその仮の行を実プロジェクトとして使い続け、
そこに GitHub のリポジトリ設定(`repoOwner` / `repoName` / `repoBranch`)を足していた。

`SampleDataRepository` は「実データを巻き込まないよう **ID 指定で**消す」設計だった。
**その ID がまさに実運用中の行だった。**
削除は仕様どおり動いて、実際に使っていた器ごと消えた。

`NoteImportRepository` は登録済みプロジェクトを回るだけなので、
回る対象が消えれば何もせず0件を返す。**エラーが出ないのは正常動作**だった。

### さらに悪いこと

調べると **プロジェクトを追加する UI がアプリに存在しなかった**。
プロジェクトは仮データからしか生まれない構造で、つまり
**「仮データを消す」と二度とプロジェクトを作れなくなる**。
シロさんの見立て(「インポートするための器は自動生成されないのでは」)が正しかった。

## 実装内容

**1. プロジェクト追加UIを先に作った**(これが無いと仮データを消せない)

- `ProjectEditDialog` の `original` を nullable にし、null なら新規作成。
- **新規のときだけ ID を入力させる。ID は自動採番しない。**
  サーバー・レコル・分類履歴が参照する正本であり、作り直すときに
  以前と同じ文字列に戻せる必要があるため。編集時は変更不可。
- 検証: 空でない / `^[a-z0-9-]+$` / 既存と重複しない。

**2. サンプルデータを機能ごと撤去した**

設定のセクションと削除ダイアログ、`SampleDataRepository`、
**初回起動時のシード**(残すと新規インストールで復活する)、`seeding_disabled` フラグ。
`SampleSeed.kt` は3つの Repository テストがフィクスチャに使っているので
**main から test へ移し、製品には載せない**形にした。

## 影響範囲

- **3ゲームを手で登録し直した。** ID は以前と同じ文字列にしてある。
- このとき `adb input text` が日本語を打てず英字名で作ったため、
  呼称を `ANRI` / `Kamieru` / `GengeKyou` に統一する判断につながった。
  **表示名は Obsidian の `Games/<名前>/` フォルダ名になる**ので、
  Kamieru は過去2件が旧フォルダに残っている。

## 未解決事項

- **削除ダイアログが「何が消えるか」を名指しで見せていなかった。**
  件数しか出しておらず、対象の実体が見えなかったことが事故を通しやすくした。
  この問題はサンプルデータ固有ではなく、**他の破壊的操作にも当てはまる**。
  今後の削除UIでは対象を列挙する、という方針だけ決めて未実装。
- Obsidian 側の `紙装甲主人公と不死身のカエル` フォルダのリネームが未了。

## 次に行うこと

破壊的操作のUIで「消える対象を名指しで出す」を横断的に見直す。
