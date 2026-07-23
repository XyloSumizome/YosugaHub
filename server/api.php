<?php
// Yosuga Hub: AI(ChatGPT)向けの配信口。
// 例: api.php?file=projects&token=xxxx
//     api.php?file=index&token=xxxx  → ファイル一覧と更新時刻
require __DIR__ . '/config.php';
header('Content-Type: application/json; charset=utf-8');

$token = $_GET['token'] ?? ($_SERVER['HTTP_X_YOSUGA_TOKEN'] ?? '');
if ($YOSUGA_TOKEN === 'CHANGE_ME_TO_LONG_RANDOM_STRING' || !hash_equals($YOSUGA_TOKEN, $token)) {
    http_response_code(403);
    echo json_encode(['ok' => false, 'error' => 'forbidden']);
    exit;
}

$file = $_GET['file'] ?? '';

if ($file === 'index') {
    $list = [];
    foreach (glob($YOSUGA_DATA_DIR . '/*.json') ?: [] as $path) {
        $list[] = [
            'file' => basename($path, '.json'),
            'updatedAt' => date('c', filemtime($path)),
        ];
    }
    echo json_encode(['ok' => true, 'files' => $list]);
    exit;
}

if (!preg_match('/^[a-z0-9_]+$/', $file)) {
    http_response_code(400);
    echo json_encode(['ok' => false, 'error' => 'bad_request']);
    exit;
}

$path = $YOSUGA_DATA_DIR . '/' . $file . '.json';
if (!is_file($path)) {
    http_response_code(404);
    echo json_encode(['ok' => false, 'error' => 'not_found']);
    exit;
}

readfile($path);
