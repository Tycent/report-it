// Firebase Configuration (from google-services.json)
const firebaseConfig = {
    apiKey: "AIzaSyC_bUX9kzgHpXEy1IB4W3s6U4VwBuO-KL0",
    authDomain: "report-it-cf60e.firebaseapp.com",
    projectId: "report-it-cf60e",
    storageBucket: "report-it-cf60e.appspot.com",
    messagingSenderId: "1066645824886",
    appId: "1:1066645824886:web:6588640824886"
};

// Initialize Firebase (wrapped safely)
let db = null;
try {
    firebase.initializeApp(firebaseConfig);
    db = firebase.firestore();
} catch (e) {
    console.warn("Firebase initialization skipped or failed. Running in Demo Mode.", e);
}

// Helper to check if online/connected
function isOfflineError(error) {
    const msg = (error && error.message) || '';
    return !db || (error && error.code === 'unavailable') || msg.toLowerCase().includes('offline') || msg.toLowerCase().includes('network');
}

// --- Reveal Animation ---
const revealItems = document.querySelectorAll('.reveal');
if ('IntersectionObserver' in window) {
    const revealObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach((entry) => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
                revealObserver.unobserve(entry.target);
            }
        });
    }, { threshold: 0.14 });
    revealItems.forEach((item) => revealObserver.observe(item));
} else {
    revealItems.forEach((item) => item.classList.add('visible'));
}

// --- Reporting Logic (Connect to Firestore with LocalStorage Demo Fallback) ---
const reportForm = document.querySelector('#report-form');
if (reportForm) {
    reportForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        const submitButton = reportForm.querySelector('button[type="submit"]');
        const formMessage = reportForm.querySelector('.form-message');
        if (!reportForm.checkValidity()) return;

        const formData = new FormData(reportForm);
        const authorityByCategory = {
            'Roads and transport': 'CITY COUNCIL',
            'Water and drainage': 'ZINWA',
            'Power and lighting': 'ZESA',
            'Waste and cleanliness': 'CITY COUNCIL',
            'Safety concern': 'CITY COUNCIL'
        };

        const category = formData.get('category');
        const location = formData.get('location');
        const details = formData.get('details');
        const name = formData.get('name');

        const loggedInUserPhone = localStorage.getItem('userPhone') || "Web User";

        const reportData = {
            description: `${details}\n\nLocation: ${location}`,
            department: authorityByCategory[category] || 'General',
            reporterPhone: loggedInUserPhone,
            timestamp: Date.now(),
            status: "Pending",
            imageUrl: null,
            latitude: null,
            longitude: null
        };

        submitButton.innerHTML = 'Sending report <span aria-hidden="true">&#10003;</span>';
        submitButton.disabled = true;
        formMessage.classList.remove('is-visible');

        try {
            if (!db) throw new Error("Firestore not initialized");
            await db.collection("reports").add(reportData);
            formMessage.textContent = 'Thanks. Your report has been sent to ' + reportData.department + '.';
            formMessage.classList.add('is-visible', 'is-success');
            reportForm.reset();
        } catch (error) {
            console.warn("Firestore unavailable. Saving report locally (Demo Mode).", error);
            const localReports = JSON.parse(localStorage.getItem('demoReports') || '[]');
            localReports.push(reportData);
            localStorage.setItem('demoReports', JSON.stringify(localReports));

            formMessage.textContent = 'Thanks! Your report has been recorded locally (Demo Mode). Department: ' + reportData.department;
            formMessage.classList.add('is-visible', 'is-success');
            reportForm.reset();
        } finally {
            submitButton.innerHTML = 'Send report <span aria-hidden="true">&#8594;</span>';
            submitButton.disabled = false;
        }
    });
}

// --- Login Logic (Connect to Firestore with LocalStorage Demo Fallback) ---
const loginForm = document.querySelector('#login-form');
const loginMessage = document.querySelector('.login-message');

if (loginForm) {
    loginForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        const phone = loginForm.phone.value.trim();
        const password = loginForm.password.value;
        const submitButton = loginForm.querySelector('button[type="submit"]');

        submitButton.disabled = true;
        submitButton.textContent = 'Verifying...';
        loginMessage.classList.remove('is-visible');

        try {
            if (!db) throw new Error("Firestore not initialized");
            const doc = await db.collection("users").doc(phone).get();

            if (doc.exists && doc.data().password === password) {
                const userData = doc.data();
                loginMessage.className = 'login-message is-visible is-success';
                loginMessage.textContent = `Welcome back, ${userData.name}.`;

                localStorage.setItem('userPhone', phone);
                localStorage.setItem('userName', userData.name);

                window.setTimeout(() => { window.location.href = 'app.html'; }, 800);
            } else {
                throw new Error('Invalid phone number or password.');
            }
        } catch (error) {
            console.warn("Firestore login fallback to LocalStorage (Demo Mode).", error);
            const localUsers = JSON.parse(localStorage.getItem('demoUsers') || '{}');
            const localUser = localUsers[phone];

            // Allow pre-seeded test admin/user credentials or registered local users
            if ((phone === '0777777777' && password === '123456789') || (localUser && localUser.password === password)) {
                const userName = localUser ? localUser.name : 'Tafadzwa';
                loginMessage.className = 'login-message is-visible is-success';
                loginMessage.textContent = `Welcome back, ${userName} (Demo Mode).`;

                localStorage.setItem('userPhone', phone);
                localStorage.setItem('userName', userName);

                window.setTimeout(() => { window.location.href = 'app.html'; }, 800);
            } else {
                loginMessage.className = 'login-message is-visible';
                loginMessage.textContent = 'Invalid credentials. (Demo Mode: try phone 0777777777, password 123456789)';
            }
        } finally {
            submitButton.disabled = false;
            submitButton.innerHTML = 'Log in <span aria-hidden="true">&#8594;</span>';
        }
    });
}

// --- Registration Logic (Connect to Firestore with LocalStorage Demo Fallback) ---
const registerForm = document.querySelector('#register-form');
if (registerForm) {
    registerForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        const name = registerForm.name.value.trim();
        const address = registerForm.address.value.trim();
        const phone = registerForm.phone.value.trim();
        const password = registerForm.password.value;
        const confirmPassword = registerForm.confirm_password.value;
        const registerMessage = registerForm.querySelector('.login-message');

        if (password !== confirmPassword) {
            registerMessage.textContent = 'Passwords must match.';
            registerMessage.className = 'login-message is-visible';
            return;
        }

        const submitButton = registerForm.querySelector('button[type="submit"]');
        submitButton.disabled = true;
        submitButton.textContent = 'Creating...';

        try {
            if (!db) throw new Error("Firestore not initialized");
            const existing = await db.collection("users").doc(phone).get();
            if (existing.exists) throw new Error('An account with this phone number already exists.');

            await db.collection("users").doc(phone).set({
                name: name,
                address: address,
                phoneNumber: phone,
                password: password
            });

            registerMessage.className = 'login-message is-visible is-success';
            registerMessage.textContent = 'Account created successfully!';
            window.setTimeout(() => { window.location.href = 'index.html'; }, 1000);
        } catch (error) {
            console.warn("Firestore register fallback to LocalStorage (Demo Mode).", error);
            const localUsers = JSON.parse(localStorage.getItem('demoUsers') || '{}');
            if (localUsers[phone]) {
                registerMessage.className = 'login-message is-visible';
                registerMessage.textContent = 'An account with this phone number already exists locally.';
                return;
            }

            localUsers[phone] = { name, address, phoneNumber: phone, password };
            localStorage.setItem('demoUsers', JSON.stringify(localUsers));

            registerMessage.className = 'login-message is-visible is-success';
            registerMessage.textContent = 'Account created successfully (Demo Mode)!';
            window.setTimeout(() => { window.location.href = 'index.html'; }, 1000);
        } finally {
            submitButton.disabled = false;
            submitButton.innerHTML = 'Create account <span aria-hidden="true">&#8594;</span>';
        }
    });
}

// --- Check Session for app.html ---
if (window.location.pathname.includes('app.html')) {
    if (!localStorage.getItem('userPhone')) {
        window.location.href = 'index.html';
    }
}
