const revealItems = document.querySelectorAll('.reveal');

if ('IntersectionObserver' in window) {
    const revealObserver = new IntersectionObserver((entries, observer) => {
        entries.forEach((entry) => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
                observer.unobserve(entry.target);
            }
        });
    }, { threshold: 0.14 });

    revealItems.forEach((item) => revealObserver.observe(item));
} else {
    revealItems.forEach((item) => item.classList.add('visible'));
}

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
        const payload = new URLSearchParams({
            authority: authorityByCategory[category],
            description: `${details}\n\nLocation: ${location}\nReported by: ${name}`
        });

        submitButton.innerHTML = 'Sending report <span aria-hidden="true">&#10003;</span>';
        submitButton.disabled = true;
        formMessage.classList.remove('is-visible');

        try {
            const response = await fetch(reportForm.dataset.endpoint, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: payload
            });
            const result = await response.json();
            if (!response.ok || !result.success) throw new Error(result.message || 'Unable to send report.');
            formMessage.textContent = 'Thanks. Your report has been sent for review.';
            formMessage.classList.add('is-visible');
            reportForm.reset();
        } catch (error) {
            formMessage.textContent = 'We could not send your report right now. Please try again.';
            formMessage.classList.add('is-visible');
        } finally {
            submitButton.innerHTML = 'Send report <span aria-hidden="true">&#8594;</span>';
            submitButton.disabled = false;
        }
    });
}

const loginModal = document.querySelector('#login-modal');
const loginForm = document.querySelector('#login-form');
const loginMessage = document.querySelector('.login-message');

document.querySelectorAll('[data-open-login]').forEach((button) => {
    button.addEventListener('click', () => {
        if (!loginModal) return;
        loginModal.hidden = false;
        loginForm.querySelector('input').focus();
    });
});

document.querySelectorAll('[data-close-login]').forEach((button) => {
    button.addEventListener('click', () => {
        if (!loginModal) return;
        loginModal.hidden = true;
        loginMessage.classList.remove('is-visible');
    });
});

if (loginForm) {
    loginForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!loginForm.checkValidity()) return;

        const submitButton = loginForm.querySelector('button[type="submit"]');
        submitButton.disabled = true;
        submitButton.textContent = 'Checking...';
        loginMessage.classList.remove('is-visible');

        try {
            const response = await fetch(loginForm.dataset.endpoint, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams(new FormData(loginForm))
            });
            const result = await response.json();
            if (!response.ok || !result.success) throw new Error(result.message || 'Login failed.');
            loginMessage.className = 'login-message is-visible is-success';
            loginMessage.textContent = `Welcome back${result.name ? `, ${result.name}` : ''}.`;
            loginForm.reset();
            window.location.href = 'app.html';
        } catch (error) {
            loginMessage.className = 'login-message is-visible';
            loginMessage.textContent = error.message || 'We could not log you in right now.';
        } finally {
            submitButton.disabled = false;
            submitButton.innerHTML = 'Log in <span aria-hidden="true">&#8594;</span>';
        }
    });
}

const registerForm = document.querySelector('#register-form');
if (registerForm) {
    registerForm.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (!registerForm.checkValidity()) return;

        const submitButton = registerForm.querySelector('button[type="submit"]');
        const registerMessage = registerForm.querySelector('.login-message');
        const password = registerForm.querySelector('[name="password"]');
        const confirmPassword = registerForm.querySelector('[name="confirm_password"]');
        if (password.value !== confirmPassword.value) {
            registerMessage.textContent = 'Passwords must match.';
            registerMessage.className = 'login-message is-visible';
            return;
        }

        submitButton.disabled = true;
        submitButton.textContent = 'Creating account...';
        registerMessage.classList.remove('is-visible');

        try {
            const response = await fetch(registerForm.dataset.endpoint, {
                method: 'POST',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams(new FormData(registerForm))
            });
            const result = await response.json();
            if (!response.ok || !result.success) throw new Error(result.message || 'Registration failed.');
            registerMessage.className = 'login-message is-visible is-success';
            registerMessage.textContent = 'Account created. Redirecting to login...';
            registerForm.reset();
            window.setTimeout(() => { window.location.href = 'index.html'; }, 900);
        } catch (error) {
            registerMessage.className = 'login-message is-visible';
            registerMessage.textContent = error.message || 'We could not create your account right now.';
        } finally {
            submitButton.disabled = false;
            submitButton.innerHTML = 'Create account <span aria-hidden="true">&#8594;</span>';
        }
    });
}
