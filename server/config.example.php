<?php
// Yosuga Hub サーバー設定のひな形。
//
// このファイルを config.php へコピーしてから編集する。
//   cp config.example.php config.php
//
// config.php は .gitignore で追跡から外してある。
// **本物のトークンを config.example.php 側へ書かないこと**(Git に載って公開される)。

// アプリの 設定 → サーバー同期 → 「トークン生成」で作った値をここへ貼る。
// この初期値のままだと upload.php / api.php は全リクエストを 403 で拒否する。
// ⚠ この文字列は api.php / upload.php が「未設定」の目印として見ている。**変えないこと**
//   (別の語に書き換えると selftest の tokenConfigured が嘘をつく)。
$YOSUGA_TOKEN = 'CHANGE_ME_TO_LONG_RANDOM_STRING';

// JSONの保存先(このディレクトリ直下の data/)。
$YOSUGA_DATA_DIR = __DIR__ . '/data';
