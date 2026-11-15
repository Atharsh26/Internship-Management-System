(function () {
  function setupLoginPage() {
    const password = document.getElementById("password");
    const eyeButton = document.getElementById("togglePassword");
    const form = document.getElementById("loginForm");
    const submit = document.getElementById("loginSubmit");

    if (eyeButton && password) {
      eyeButton.addEventListener("click", function () {
        const showing = password.type === "password";
        password.type = showing ? "text" : "password";
        eyeButton.classList.toggle("is-visible", showing);
        eyeButton.setAttribute("aria-label", showing ? "Hide password" : "Show password");
        eyeButton.setAttribute("title", showing ? "Hide password" : "Show password");
      });
    }

    // Spring Security normally redirects failed login attempts to /login?error.
    const params = new URLSearchParams(window.location.search);
    const errorState = document.getElementById("loginErrorState");
    if (params.has("error") && errorState) errorState.classList.add("visible");

    // Do not fake a successful login in the browser. A success message should
    // only be shown after a server-side AuthenticationSuccessHandler confirms it.
    if (form && submit) {
      form.addEventListener("submit", function () {
        submit.disabled = true;
        submit.textContent = "Signing in…";
      });
    }
  }
  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", setupLoginPage);
  else setupLoginPage();
})();
