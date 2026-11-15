(function () {
  const root = document.documentElement;
  const savedTheme = localStorage.getItem("ims-theme");
  if (savedTheme === "light" || savedTheme === "dark") root.setAttribute("data-theme", savedTheme);
  else root.setAttribute("data-theme", window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light");

  window.toggleLoginTheme = function () {
    const next = root.getAttribute("data-theme") === "dark" ? "light" : "dark";
    root.setAttribute("data-theme", next);
    localStorage.setItem("ims-theme", next);
    const button = document.getElementById("themeToggle");
    if (button) button.textContent = next === "dark" ? "☀ Light mode" : "☾ Dark mode";
  };

  window.togglePasswordVisibility = function () {
    const input = document.getElementById("password");
    const button = document.getElementById("togglePassword");
    if (!input || !button) return;
    const visible = input.type === "password";
    input.type = visible ? "text" : "password";
    button.textContent = visible ? "Hide" : "Show";
    button.setAttribute("aria-label", visible ? "Hide password" : "Show password");
  };

  document.addEventListener("DOMContentLoaded", function () {
    const visual = document.getElementById("loginVisual");
    const params = new URLSearchParams(window.location.search);
    if (visual && params.has("error")) visual.classList.add("is-error");
    // A success state must only be activated by a server-confirmed authentication flow.
    if (visual && params.has("loginSuccess")) visual.classList.add("is-success");
    const themeButton = document.getElementById("themeToggle");
    if (themeButton) themeButton.textContent = root.getAttribute("data-theme") === "dark" ? "☀ Light mode" : "☾ Dark mode";
  });
})();
