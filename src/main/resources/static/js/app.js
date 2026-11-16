// Confirm destructive actions (any form with data-confirm="message")
document.addEventListener('submit', function (e) {
  var msg = e.target.getAttribute('data-confirm');
  if (msg && !window.confirm(msg)) e.preventDefault();
});

// Theme switcher: dark is the default; choice is remembered in this browser.
(function () {
  function mark(theme) {
    document.querySelectorAll('[data-theme-set]').forEach(function (b) {
      b.classList.toggle('active', b.getAttribute('data-theme-set') === theme);
    });
  }
  function apply(theme) {
    document.documentElement.setAttribute('data-theme', theme);
    try { localStorage.setItem('theme', theme); } catch (e) {}
    mark(theme);
  }
  document.addEventListener('click', function (e) {
    var b = e.target.closest('[data-theme-set]');
    if (b) apply(b.getAttribute('data-theme-set'));
  });
  document.addEventListener('DOMContentLoaded', function () {
    mark(document.documentElement.getAttribute('data-theme') || 'dark');
  });
})();

document.addEventListener('DOMContentLoaded', function () {
  if (window.lucide) { window.lucide.createIcons(); }
});
