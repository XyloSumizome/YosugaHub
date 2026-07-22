# Yosuga Hub

個人用のAndroid制作管理アプリ。

Googleカレンダー、3つのゲーム制作プロジェクト、ChatGPT(よすが)との情報交換を
1台のAndroid端末で一元管理するダッシュボード。

詳細は設計書 [`yosuga_hub_android_design_v2.md`](yosuga_hub_android_design_v2.md) を参照。

## 技術構成

- Kotlin / Jetpack Compose / Material 3
- Gradle Kotlin DSL
- minSdk 26 / targetSdk 35

## ビルド方法

1. Android Studio(最新安定版)でこのフォルダを開く
2. Gradle Syncを実行する
3. エミュレーターまたは実機で `app` を実行する

コマンドライン:

```
./gradlew assembleDebug
./gradlew testDebugUnitTest
```

## 進捗

- [x] Phase 0: 環境構築(プロジェクト骨組み)
- [x] Phase 1: 5画面の骨組み + ダミーデータ
- [ ] Phase 2: ローカルデータ(Room / DataStore / JSON入出力)
- [ ] Phase 3: GitHub進捗取得
- [ ] Phase 4: Googleカレンダー読み取り
- [ ] Phase 5: ホーム統合
- [ ] Phase 6: 品質改善
- [ ] Phase 7: OpenAI API連携(検討)

作業記録: [`docs/WORKLOG.md`](docs/WORKLOG.md)
