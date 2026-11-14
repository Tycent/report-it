# Local website authentication

1. Install PHP 8.1 or later and make sure `php` is available on your PATH.
2. Double-click `start-local.bat`.
3. Open `http://127.0.0.1:8000/app.html` in the same computer's browser.

Create accounts and sign in while the local server is running. Account records are stored outside the website folder under `%LOCALAPPDATA%\ReportItLocal\accounts.json`; passwords are stored as hashes. The local PHP server listens only on this computer (`127.0.0.1`).

Local authentication does not upload account passwords to Firebase. Report submissions and report photos still go to the configured Firebase project, as requested. Firebase Anonymous Authentication must be enabled for the website's Firebase project for report submission and report tracking to work.
