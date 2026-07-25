#!/usr/bin/env python3
"""レコル用システムプロンプトの文字数を測る。

カスタムGPT の Instructions は 8000 文字が上限で、超えると**末尾が黙って
切り捨てられる**。切れた部分の指示は効かないため、気づきにくい形で壊れる。
docs/recoru_prompt.md の該当ブロックを編集したら、これを実行して確かめること。
"""
import pathlib
import sys

LIMIT = 8000
DOC = pathlib.Path(__file__).resolve().parent.parent / "docs" / "recoru_prompt.md"

lines = DOC.read_text(encoding="utf-8").split("\n")
# システムプロンプト本体は「あなたは「レコル」」で始まるフェンス内。
try:
    body = next(i for i, l in enumerate(lines) if l.startswith("あなたは「レコル」"))
except StopIteration:
    sys.exit("システムプロンプトの開始行が見つからない(docs/recoru_prompt.md)")

start = max(i for i, l in enumerate(lines[:body]) if l.strip() == "```")
end = next(i for i, l in enumerate(lines) if i > start and l.strip() == "```")
size = len("\n".join(lines[start + 1:end]))

print(f"システムプロンプト: {size} 文字 / 上限 {LIMIT}")
if size > LIMIT:
    sys.exit(f"❌ {size - LIMIT} 文字オーバー。貼ると末尾が切れる。")
print(f"✅ 収まる(残り {LIMIT - size} 文字)")
