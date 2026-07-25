#!/usr/bin/env python3
"""AI へ渡すプロンプトの文字数を測る。

**どちらも「静かに壊れる」種類の制約**なので、編集したら必ず測ること。

- レコル(カスタムGPT): Instructions は 8000 文字が上限。
  超えた分は**黙って切り捨てられ**、切れた部分の指示は効かない。
- ヨスガ(通常の ChatGPT): Instructions 欄が無い。カスタム指示は1枠 1500 文字程度が
  上限なので、超えるならメモリに覚えさせる運用になる(超過は即エラーではない)。
"""
import pathlib
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent

# (表示名, ファイル, 本文の開始行の目印, 上限, 上限超過を失敗として扱うか)
TARGETS = [
    ("レコル", "docs/recoru_prompt.md", "あなたは「レコル」", 8000, True),
    ("ヨスガ", "docs/yosuga_prompt.md", "あなたは「ヨスガ」", 1500, False),
]

failed = False
for name, rel, marker, limit, hard in TARGETS:
    path = ROOT / rel
    lines = path.read_text(encoding="utf-8").split("\n")
    try:
        body = next(i for i, l in enumerate(lines) if l.startswith(marker))
    except StopIteration:
        print(f"❌ {name}: 開始行 '{marker}' が見つからない({rel})")
        failed = True
        continue

    start = max(i for i, l in enumerate(lines[:body]) if l.strip() == "```")
    end = next(i for i, l in enumerate(lines) if i > start and l.strip() == "```")
    size = len("\n".join(lines[start + 1:end]))

    if size <= limit:
        print(f"✅ {name}: {size} 文字 / {limit}(残り {limit - size})")
    elif hard:
        print(f"❌ {name}: {size} 文字 / {limit} — {size - limit} 文字オーバー。貼ると末尾が切れる。")
        failed = True
    else:
        print(f"⚠ {name}: {size} 文字 / {limit} — カスタム指示には入らない。メモリに覚えさせること。")

sys.exit(1 if failed else 0)
