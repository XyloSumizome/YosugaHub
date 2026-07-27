#!/usr/bin/env python3
"""ヨスガへ渡す指示文の寸法を測る。

**2026-07-27 に測る対象が2度変わった。**

1. もとは「カスタムGPT の Instructions 8000 文字」「カスタム指示 1500 文字」という
   *貼り先の欄*の上限を見ていた。レコル廃止と指示文の Hub 同梱で、その欄は使わなくなった。
2. 次に「URL の `?q=` に載るか」を見た。だが日本語は URL エンコードで1文字9バイトになり、
   **どの指示文も載らないことが確定した**。そこで夜のボタンも朝と同じ
   **共有インテント**(`EXTRA_TEXT` / Binder の約1MBまで)に変えた。

→ **いま長さは実質の制約ではない。** それでも測るのは、指示文が**読まれる量**として
   妥当かを見るため。長すぎる指示は守られない。

⚠ **ブロックは名前で取り出す。** 以前は `\"\"\"` を出てくる順に決め打ちしていたため、
定数を1つ足しただけで**全部ずれて**別の指示文の字数を表示していた(気づきにくい壊れ方)。
"""
import pathlib
import re
import sys
import urllib.parse

ROOT = pathlib.Path(__file__).resolve().parent.parent
SOURCE = ROOT / "app/src/main/java/com/shiro/yosugahub/data/prompt/YosugaPrompt.kt"

# ChatGptLink.MAX_URL_LENGTH と揃える。片方だけ変えると案内文が嘘になる。
MAX_URL_LENGTH = 2000
# `https://chatgpt.com/c/<id>?q=` 相当。URL 本体が食う分の見積もり。
URL_OVERHEAD = 40

# (表示名, その宣言を見つける目印)。目印の**後ろで最初に現れる** `"""…"""` を取る。
PARTS = (
    ("朝の前置き", "MORNING_HEADER"),
    ("夜の前置き", "NIGHT_HEADER"),
    ("観察日誌の本文", "fun diary"),
    ("セッション記録の本文", "fun session"),
)


def block_after(text: str, anchor: str) -> str | None:
    """`anchor` より後ろで最初の三重引用符ブロックを返す。"""
    start = text.find(anchor)
    if start < 0:
        return None
    m = re.search(r'"""(.*?)"""', text[start:], re.S)
    return m.group(1) if m else None


def main() -> int:
    if not SOURCE.exists():
        print(f"❌ {SOURCE.relative_to(ROOT)} が見つからない")
        return 1

    text = SOURCE.read_text(encoding="utf-8")
    found = {}
    for name, anchor in PARTS:
        body = block_after(text, anchor)
        if body is None:
            print(f"❌ 「{name}」({anchor})を取り出せなかった。目印が変わっていないか確認する")
            return 1
        found[name] = body

    print(f"{SOURCE.relative_to(ROOT)}\n")
    for name, body in found.items():
        print(f"   {name:12} {len(body):5} 字")

    # 実際に渡されるのは 前置き + 本文。夜は毎回まっさらな会話が開くので
    # NIGHT_HEADER が必ず付く。
    night = len(found["夜の前置き"])
    print("\n実際に渡る量(前置き込み):\n")
    totals = (
        ("朝 ヨスガへ現状報告", len(found["朝の前置き"]), "+ 現況JSON(約31KB)"),
        ("夜 観察日誌を要求", night + len(found["観察日誌の本文"]), ""),
        ("夜 ヨスガへ総括を要求", night + len(found["セッション記録の本文"]), ""),
    )
    for name, size, note in totals:
        encoded = len(urllib.parse.quote("あ" * size, safe="")) + URL_OVERHEAD
        fits = "✅ URLにも載る" if encoded <= MAX_URL_LENGTH else "— URLには載らない"
        print(f"   {name:20} 約 {size:5} 字  {fits} {note}")

    print(
        "\n⚠ どれも共有インテント(EXTRA_TEXT)で渡すので、長さは制約にならない。"
        "\n  「URLには載らない」は退避経路(ChatGPT アプリが無い端末)の話でしかない。"
        "\n  日本語は URL エンコードで1文字9バイトになるため、載るのは約200字まで。"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
