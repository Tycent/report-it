<?php
declare(strict_types=1);

header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');

$action = $_GET['action'] ?? '';
$isSecure = !empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off';
session_set_cookie_params([
    'httponly' => true,
    'secure' => $isSecure,
    'samesite' => 'Strict',
    'path' => '/',
]);
session_start();

function respond(int $status, array $body): never
{
    http_response_code($status);
    echo json_encode($body, JSON_UNESCAPED_SLASHES);
    exit;
}

function readRequest(): array
{
    $body = file_get_contents('php://input');
    $data = json_decode($body === false ? '' : $body, true);
    if (!is_array($data)) {
        respond(400, ['success' => false, 'message' => 'Invalid request.']);
    }
    return $data;
}

function normalizePhone(string $value): string
{
    $value = trim($value);
    if ($value === '' || !preg_match('/^\+?[\d\s().-]+$/', $value)) {
        respond(422, ['success' => false, 'message' => 'Enter a valid phone number.']);
    }

    $digits = preg_replace('/\D/', '', $value);
    if (str_starts_with($digits, '0')) {
        $digits = '263' . substr($digits, 1);
    }
    if (strlen($digits) < 7 || strlen($digits) > 15) {
        respond(422, ['success' => false, 'message' => 'Enter a valid phone number.']);
    }
    return '+' . $digits;
}

function accountsPath(): string
{
    $base = getenv('LOCALAPPDATA');
    if ($base === false || $base === '') {
        $base = getenv('HOME') ?: sys_get_temp_dir();
    }
    $directory = $base . DIRECTORY_SEPARATOR . 'ReportItLocal';
    if (!is_dir($directory) && !mkdir($directory, 0700, true) && !is_dir($directory)) {
        throw new RuntimeException('Unable to create the local account directory.');
    }
    return $directory . DIRECTORY_SEPARATOR . 'accounts.json';
}

function withAccounts(callable $callback): mixed
{
    $path = accountsPath();
    $file = fopen($path, 'c+');
    if ($file === false) {
        throw new RuntimeException('Unable to open the local account store.');
    }

    try {
        if (!flock($file, LOCK_EX)) {
            throw new RuntimeException('Unable to lock the local account store.');
        }
        rewind($file);
        $contents = stream_get_contents($file);
        $accounts = $contents === '' ? [] : json_decode($contents, true, 512, JSON_THROW_ON_ERROR);
        if (!is_array($accounts)) {
            throw new RuntimeException('The local account store is invalid.');
        }

        $result = $callback($accounts);
        rewind($file);
        if (!ftruncate($file, 0) || fwrite($file, json_encode($accounts, JSON_THROW_ON_ERROR)) === false || !fflush($file)) {
            throw new RuntimeException('Unable to save the local account store.');
        }
        @chmod($path, 0600);
        flock($file, LOCK_UN);
        return $result;
    } finally {
        fclose($file);
    }
}

if ($action === 'me' && $_SERVER['REQUEST_METHOD'] === 'GET') {
    respond(200, ['success' => true, 'user' => $_SESSION['local_user'] ?? null]);
}

if ($action === 'logout' && $_SERVER['REQUEST_METHOD'] === 'POST') {
    $_SESSION = [];
    if (ini_get('session.use_cookies')) {
        $params = session_get_cookie_params();
        setcookie(session_name(), '', [
            'expires' => time() - 42000,
            'path' => $params['path'],
            'secure' => $params['secure'],
            'httponly' => $params['httponly'],
            'samesite' => $params['samesite'],
        ]);
    }
    session_destroy();
    respond(200, ['success' => true]);
}

if (($action !== 'login' && $action !== 'register') || $_SERVER['REQUEST_METHOD'] !== 'POST') {
    header('Allow: GET, POST');
    respond(405, ['success' => false, 'message' => 'Unsupported authentication request.']);
}

$data = readRequest();
$phone = normalizePhone((string) ($data['phone'] ?? ''));
$phoneKey = substr($phone, 1);
$password = (string) ($data['password'] ?? '');
if ($password === '' || strlen($password) > 4096) {
    respond(422, ['success' => false, 'message' => 'Enter a valid password.']);
}

try {
    if ($action === 'register') {
        if (strlen($password) < 6) {
            respond(422, ['success' => false, 'message' => 'Password must contain at least 6 characters.']);
        }
        $name = trim((string) ($data['name'] ?? ''));
        $address = trim((string) ($data['address'] ?? ''));
        withAccounts(static function (array &$accounts) use ($phoneKey, $phone, $password, $name, $address): void {
            if (isset($accounts[$phoneKey])) {
                respond(409, ['success' => false, 'message' => 'An account already exists for that phone number.']);
            }
            $accounts[$phoneKey] = [
                'phone' => $phone,
                'name' => $name,
                'address' => $address,
                'passwordHash' => password_hash($password, PASSWORD_DEFAULT),
            ];
        });
    }

    $user = withAccounts(static function (array &$accounts) use ($phoneKey, $password): ?array {
        $account = $accounts[$phoneKey] ?? null;
        if (!is_array($account) || !password_verify($password, (string) ($account['passwordHash'] ?? ''))) {
            return null;
        }
        return [
            'phone' => $account['phone'],
            'name' => $account['name'] ?? '',
            'address' => $account['address'] ?? '',
        ];
    });

    if ($action === 'login' && $user === null) {
        respond(401, ['success' => false, 'message' => 'Phone number or password is incorrect.']);
    }
    if ($action === 'register') {
        $user = [
            'phone' => $phone,
            'name' => trim((string) ($data['name'] ?? '')),
            'address' => trim((string) ($data['address'] ?? '')),
        ];
    }

    session_regenerate_id(true);
    $_SESSION['local_user'] = $user;
    respond(200, ['success' => true, 'user' => $user]);
} catch (Throwable $error) {
    error_log('Local authentication error: ' . $error->getMessage());
    respond(500, ['success' => false, 'message' => 'Local authentication could not be completed. Check the local server and try again.']);
}
