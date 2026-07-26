---
type: development-log
project_id: yosuga-hub
game: Yosuga Hub
category: calendar
created_at: 2026-07-26T11:40:00+09:00
updated_at: 2026-07-26T11:40:00+09:00
source: claude-code
tags:
  - Yosuga Hub
  - development-log
  - calendar
  - android
related_files:
  - app/src/main/java/com/shiro/yosugahub/data/calendar/DeviceCalendarDataSource.kt
  - app/src/main/java/com/shiro/yosugahub/ui/screen/settings/SettingsScreen.kt
commit:
  branch: master
---

# 変更概要

**カレンダーが0件だった原因は Hub ではなく、端末の同期設定だった。**
Android には**カレンダーの同期先が2つ**あり、Hub が読む方だけが OFF になっていた。
あわせて、設定画面に残っていた「Googleアカウント: 未接続(Phase 4 で実装予定)」を削除した。
この文言が誤診を誘発したため。

## 変更理由

### 症状

Google カレンダーアプリでは予定が普通に見えるのに、Hub は「予定はありませんでした」。
`adb` から `content://com.android.calendar/calendars` を引いても `No result found`。
**アプリでは見えるのにプロバイダが空**という矛盾。

### 原因: 同期先が2つある

`設定 → アカウントの同期` に、紛らわしい2項目が並んでいる。

| 表示名 | authority | 実体 | 誰が読むか |
|---|---|---|---|
| **Google カレンダー** | `com.google.android.calendar` | Google アプリ専用のストア | Google カレンダーアプリ |
| **カレンダー** | `com.android.calendar` | **端末の共有 CalendarProvider** | **Hub を含む全アプリ** |

前者だけ ON、後者が **同期OFF** だった。
だから**アプリには表示され、共有プロバイダには何も降りてこない**。

`dumpsys content` にも同じことが出ていた。
`com.google.android.calendar` は SUCCESS、`com.android.calendar` は行がゼロ。

### なぜ誤診しかけたか

設定画面に **「Googleアカウント: 未接続(Phase 4 で実装予定)」** が残っていた。
これは**設計書v2の旧 Phase 4(OAuth で Google カレンダーAPIへ繋ぐ)**の名残で、
実際には `CalendarContract` を読む方式へ差し替え済み(`READ_CALENDAR` のみ)。
**Hub は Google アカウントへ接続する必要がない。**

にもかかわらずこの表示があるため、「アカウント未接続だから0件なのだ」と読めてしまう。
実際そう疑った。**古い設計の消し忘れが、無関係な不具合の原因に見えた。**

## 実装内容

1. 設定画面の「Googleアカウント」セクションを**削除**。
2. 「カレンダー」セクションに、**Google アカウントへの接続は不要**であることと、
   予定が出ないときは端末の同期設定を見る、という案内を追加した。

`DeviceCalendarDataSource` は変更していない。**最初から正しかった。**

## 影響範囲

実機(Pixel 10a / Android 17)で確認。「カレンダー」の同期を ON にしたところ、
共有プロバイダにカレンダー2つ(祝日 + 個人)が降り、Hub の Room に **7件**
(`past` 5 / `upcoming` 2)が入った。終日予定は日付のみ、時刻付きは時刻ありで整形された。

## 未解決事項

なし。2つ目のアカウント(`xylophone0316@gmail.com`)は同じ設定を持つが、
そちらに予定が無いため ON にしていない。必要になったら同じ操作でよい。

## 次に行うこと

**「動かない」の切り分けは、まずアプリの外を疑う。**

- 端末の設定・同期・権限は、アプリのコードをいくら読んでも分からない。
  `adb shell content query` と `dumpsys content` で**プロバイダの中身を直接見る**のが速い。
- **UI に古い設計の残骸を残さない。** 実装済みの機能に「未実装」と書いてあると、
  無関係な不具合の犯人に見える。方式を変えたら、その痕跡を画面から消す。
