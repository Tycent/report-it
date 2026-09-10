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
    reportForm.addEventListener('submit', (event) => {
        const submitButton = reportForm.querySelector('button[type="submit"]');
        if (!reportForm.checkValidity()) return;
        submitButton.innerHTML = 'Sending report <span aria-hidden="true">&#10003;</span>';
        submitButton.disabled = true;
    });
}
