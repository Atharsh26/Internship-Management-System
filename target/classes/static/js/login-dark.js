document.addEventListener('DOMContentLoaded', () => {
    const toggleBtn = document.getElementById('togglePassword');
    const passwordInput = document.getElementById('password');
    const form = document.getElementById('loginForm');
    const submitBtn = document.getElementById('loginSubmit');

    if (toggleBtn && passwordInput) {
        toggleBtn.addEventListener('click', () => {
            const isPassword = passwordInput.getAttribute('type') === 'password';
            passwordInput.setAttribute('type', isPassword ? 'text' : 'password');
            toggleBtn.textContent = isPassword ? 'Hide' : 'Show';
            toggleBtn.setAttribute('aria-label', isPassword ? 'Hide password' : 'Show password');
        });
    }

    if (form && submitBtn) {
        form.addEventListener('submit', () => {
            submitBtn.disabled = true;
            submitBtn.textContent = 'Signing in...';
        });
    }
});
