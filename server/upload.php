<?php
// Yosuga Hub: Androidアプリからの JSON 受け口。
// トークン(X-Yosuga-Token ヘッダー)を検証し、data/ 配下へ保存する。
require __DIR__ . '/config.php';
header('Content-Type: application/json; charset=utf-8');

$token = $_SERVER['HTTP_X_YOSUGA_TOKEN'] ?? '';
if ($YOSUGA_TOKEN === 'CHANGE_ME_TO_LONG_RANDOM_STRING' || !hash_equals($YOSUGA_TOKEN, $token)) {
    http_response_code(403);
    echo json_encode(['ok' => false, 'error' => 'forbidden']);
    exit;
}

if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') {
    http_response_code(405);
    echo json_encode(['ok' => false, 'error' => 'method_not_allowed']);
    exit;
}

$body = json_decode(file_get_contents('php://input'), true);
if (!is_array($body) || !isset($body['files']) || !is_array($body['files'])) {
    http_response_code(400);
    echo json_encode(['ok' => false, 'error' => 'bad_request']);
    exit;
}

if (!is_dir($YOSUGA_DATA_DIR)) {
    mkdir($YOSUGA_DATA_DIR, 0755, true);
}

$saved = 0;
foreach ($body['files'] as $file) {
    $name = $file['name'] ?? '';
    $content = $file['content'] ?? null;
    // ファイル名は英小文字・数字・アンダースコアの .json のみ(パス注入を防ぐ)。
    if (!preg_match('/^[a-z0-9_]+\.json$/', $name) || !is_string($content)) {
        continue;
    }
    // 中身がJSONとして妥当なものだけ保存する。
    json_decode($content);
    if (json_last_error() !== JSON_ERROR_NONE) {
        continue;
    }
    file_put_contents($YOSUGA_DATA_DIR . '/' . $name, $content);
    $saved++;
}

echo json_encode(['ok' => true, 'saved' => $saved]);
