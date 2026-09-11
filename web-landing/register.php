<?php
header('Content-Type: application/json; charset=utf-8');

if ($_SERVER['REQUEST_METHOD'] !== 'POST') {
    http_response_code(405);
    echo json_encode(['success' => false, 'message' => 'POST requests only.']);
    exit;
}

$name = trim($_POST['name'] ?? '');
$address = trim($_POST['address'] ?? '');
$phone = trim($_POST['phone'] ?? '');
$password = (string) ($_POST['password'] ?? '');
$confirmPassword = (string) ($_POST['confirm_password'] ?? '');

if ($name === '' || $address === '' || $phone === '' || $password === '') {
    http_response_code(422);
    echo json_encode(['success' => false, 'message' => 'Complete all fields to create your account.']);
    exit;
}

if (strlen($password) < 6 || $password !== $confirmPassword) {
    http_response_code(422);
    echo json_encode(['success' => false, 'message' => 'Passwords must match and contain at least 6 characters.']);
    exit;
}

$projectId = 'report-it-cf60e';
$apiKey = 'AIzaSyC_bUX9kzgHpXEy1IB4W3s6U4VwBuO-KL0';
$documentUrl = 'https://firestore.googleapis.com/v1/projects/' . $projectId . '/databases/(default)/documents/users/' . rawurlencode($phone) . '?key=' . rawurlencode($apiKey);
$context = stream_context_create(['http' => ['ignore_errors' => true, 'timeout' => 10]]);
$existingResponse = file_get_contents($documentUrl, false, $context);
$existingDocument = $existingResponse === false ? null : json_decode($existingResponse, true);

if (is_array($existingDocument) && !isset($existingDocument['error'])) {
    http_response_code(409);
    echo json_encode(['success' => false, 'message' => 'An account with that phone number already exists.']);
    exit;
}

$document = [
    'fields' => [
        'name' => ['stringValue' => $name],
        'address' => ['stringValue' => $address],
        'phoneNumber' => ['stringValue' => $phone],
        'password' => ['stringValue' => $password]
    ]
];
$options = [
    'http' => [
        'method' => 'PATCH',
        'header' => "Content-Type: application/json\r\n",
        'content' => json_encode($document),
        'ignore_errors' => true,
        'timeout' => 10
    ]
];
$response = file_get_contents($documentUrl, false, stream_context_create($options));
$createdDocument = $response === false ? null : json_decode($response, true);

if (!is_array($createdDocument) || isset($createdDocument['error'])) {
    http_response_code(502);
    echo json_encode(['success' => false, 'message' => 'The account could not be saved. Check the Firestore connection and try again.']);
    exit;
}

echo json_encode(['success' => true, 'message' => 'Account created.']);
