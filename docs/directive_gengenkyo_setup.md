# 指示書: GengeKyou(げんげきょう)の Yosuga Hub 連携をはじめる(2026-07-25)

宛先: **GengeKyou(げんげきょう)** の Claude Code
種別: 導入(初回のみ)。以後は各セッションで自動的に更新される

## 渡し方(シロさんの手順)

GengeKyou の Claude Code に、**次の2つをこの順で貼る**。

1. **この指示書の「## 目的」以降**
2. **`docs/claude_code_onboarding.md` の「導入指示(ここから下を各ゲームの Claude Code へ貼る)」
   のコードブロック全体**(仕様の本体。長いが、これだけで自己完結している)

2 を貼り忘れると仕様が分からず、推測でファイルを作られてしまう。**必ず両方貼る。**

貼ったあとは、GengeKyou 側で **コミットして GitHub へ push** されるまで Hub には見えない。
push を確認してから、Hub の `PROJECTS` →「GitHubから更新」を押す。

---

## 目的

このプロジェクト(GengeKyou / げんげきょう)を、Androidアプリ **Yosuga Hub** から進捗確認できるようにする。
ANRI と Kamieru は導入済みで、GengeKyou だけが未対応。

Hub は GitHub 上の `.yosuga/status.json` を読んで、進捗・ブロッカー・修正のログを表示する。
**Hub は判断も要約もしない。** 意味付けは情報を作る側、つまりあなたが行う。

## やること

1. 併せて渡される「導入指示」に従い、`.yosuga/` の4ファイルを作る。
   - `.yosuga/project.json` / `.yosuga/status.json` / `.yosuga/status.md` /
     `.yosuga/schema-version.txt`
2. **`projectId` は `gengenkyo` にする。変更しない。**
   Hub 側は既にこのIDでプロジェクトを持っている。違うIDだと結びつかない。
3. 導入指示の「CLAUDE.md へ追加する内容」を、このプロジェクトの `CLAUDE.md` へ追加する。
   **これを入れないと以後の自動更新が働かない**ので、飛ばさないこと。
4. `.yosuga/notes/`(知識ノート)も導入指示の v5 節に従って用意する。
5. コミットして **GitHub へ push** する。ローカルに置いただけでは Hub から見えない。

## 完了条件

- `.yosuga/status.json` が JSON として妥当で、`schemaVersion` と
  `projectId: "gengenkyo"` を持つ
- `summary` が空でなく、`nextTasks` が存在する
- `CLAUDE.md` に「Yosuga Hub Data Provider Rules」が入っている
- **push 済み**(GitHub の既定ブランチで `.yosuga/status.json` が見える)
- 報告に「未実装・未検証のもの」が正直に書かれている

## 注意

- **推測で埋めない。** 実際のコード・テスト結果・Git履歴に基づいて書く。
  分からないことは `dataQuality.warnings` か `questionsForYosuga` に残す。
  空欄を埋めるためにそれらしい話を作らない方が、Hub にとって価値が高い。
- **`recentChanges` の各件に `date`(`"yyyy-MM-dd"`)を必ず書く**(2026-07-25 追加)。
  Hub は近況報告に**直近2週間分だけ**を載せるので、日付が無いと期間で絞れず、
  古い変更が載り続ける。**古いものを消すのは Hub の仕事なので、あなたは積んでよい。**
- 秘密情報・アクセストークン・APIキー・個人情報・ローカル絶対パスを `.yosuga/` に含めない。
- 初回なので `recentChanges` は Git履歴から遡って書いてよい。ただし**日付は履歴の実際の日付**を使う。

## 参考: なぜ2週間か

Hub は毎朝「Morning Brief」を作り、シロさんの会話相手(ヨスガ)へ渡す。
そこに「昨日の成果」の節があり、**あなたが書いた `recentChanges` がその材料**になる。
日付が無いとどれが昨日の作業か分からず、その節が書けない。
