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

        const submitButton = registerForm.querySelector('button[type="submit"]');
        submitButton.disabled = true;
        submitButton.textContent = 'Creating...';

        try {
            if (password !== confirmPassword) throw new Error('Passwords must match.');
            const response = await fetch('local-auth.php?action=register', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'same-origin',
                body: JSON.stringify({ name, address, phone, password })
            });
            const result = await response.json();
            if (!response.ok || !result.success) throw new Error(result.message || 'Account creation failed.');
            registerMessage.className = 'login-message is-visible is-success';
            registerMessage.textContent = 'Account created on this computer.';
            window.setTimeout(() => { window.location.href = 'app.html'; }, 800);
        } catch (error) {
            registerMessage.className = 'login-message is-visible';
            registerMessage.textContent = error.message || 'Unable to create the local account.';
        } finally {
            submitButton.disabled = false;
            submitButton.innerHTML = 'Create account <span aria-hidden="true">&#8594;</span>';
        }
    });
}
