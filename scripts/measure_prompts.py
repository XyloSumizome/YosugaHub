#!/usr/bin/env python3
"""ヨスガへ渡す指示文の寸法を測る。

**2026-07-27 に測る対象が変わった。** それまではプロンプトを人が貼る前提で、
「カスタムGPT の Instructions 8000 文字」「カスタム指示 1500 文字」という
*貼り先の欄*の上限を見ていた。レコル廃止と、指示文を Hub が同梱する方式への
変更で、その欄はどちらも使わなくなった。

いま効いている制約は1つだけ:

- **URL の `?q=` に載るか**(`ChatGptLink.MAX_URL_LENGTH` = 2000)。
  超えるとエラーではなく**黙って切れる**。切れた指示文は一見それらしいので
  事故に気づけない。載らない場合はクリップボード経由になる(それが既定)。

参考として素の文字数も出す。共有インテント(`EXTRA_TEXT`)は Binder の
約1MB まで通るので、そちらは実質の上限にならない。
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


def main() -> int:
    if not SOURCE.exists():
        print(f"❌ {SOURCE.relative_to(ROOT)} が見つからない")
        return 1

    blocks = re.findall(r'"""(.*?)"""', SOURCE.read_text(encoding="utf-8"), re.S)
    if len(blocks) < 3:
        print("❌ 指示文を3つ(朝 / 観測日記 / セッション記録)取り出せなかった")
        return 1

    print(f"{SOURCE.relative_to(ROOT)}\n")
    for name, body in zip(("朝の前置き", "観測日記", "セッション記録"), blocks):
        encoded = len(urllib.parse.quote(body, safe="")) + URL_OVERHEAD
        fits = encoded <= MAX_URL_LENGTH
        mark = "✅" if fits else "📋"
        how = "?q= で入力欄に入る" if fits else "クリップボード経由(?q= には載らない)"
        print(f"{mark} {name:8} 素 {len(body):5} 字 / URL化 {encoded:6} 字 — {how}")

    print(
        "\n📋 は異常ではない。日本語は URL エンコードで 1文字 9 バイトになるため、"
        "\n   200 字を超える指示文は載らない。コンソールのボタンは必ず"
        "\n   クリップボードへ入れてから開くので、貼り付ければ済む。"
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
