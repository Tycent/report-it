<?php
session_start();
header('Content-Type: application/json; charset=utf-8');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'POST requests only.']);
    exit;
}

$phone = trim($_POST['phone'] ?? '');
$password = (string) ($_POST['password'] ?? '');

if ($phone === '' || $password === '') {
    http_response_code(422);
    echo json_encode(['success' => false, 'message' => 'Enter your phone number and password.']);
    exit;
}

$projectId = 'report-it-cf60e';
$apiKey = 'AIzaSyC_bUX9kzgHpXEy1IB4W3s6U4VwBuO-KL0';
$url = 'https://firestore.googleapis.com/v1/projects/' . $projectId . '/databases/(default)/documents/users/' . rawurlencode($phone) . '?key=' . rawurlencode($apiKey);
$context = stream_context_create(['http' => ['ignore_errors' => true, 'timeout' => 10]]);
$response = file_get_contents($url, false, $context);
$document = $response === false ? null : json_decode($response, true);

if (!is_array($document) || isset($document['error'])) {
    http_response_code(401);
    echo json_encode(['success' => false, 'message' => 'Account not found or the login service is unavailable.']);
    exit;
}

$storedPassword = $document['fields']['password']['stringValue'] ?? '';
if (!hash_equals($storedPassword, $password)) {
    http_response_code(401);
    echo json_encode(['success' => false, 'message' => 'Incorrect phone number or password.']);
    exit;
}

$name = $document['fields']['name']['stringValue'] ?? '';
$_SESSION['report_it_phone'] = $phone;
$_SESSION['report_it_name'] = $name;
echo json_encode(['success' => true, 'name' => $name]);