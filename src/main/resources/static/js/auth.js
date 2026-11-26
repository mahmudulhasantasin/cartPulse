import { CartPulseAPI } from './api.js';
import { CartPulseUI } from './ui.js';

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

async function init() {
  const user = await CartPulseUI.initPage('auth');
  if (user) {
    redirectByRole(user.role);
    return;
  }

  bindLogin();
  bindRegister();
  bindDemoPrefill();
}

function redirectByRole(role) {
  const params = new URLSearchParams(window.location.search);
  const next = params.get('redirect');
  if (next && next.startsWith('/')) {
    window.location.href = next;
    return;
  }
  if (role === 'SELLER') {
    window.location.href = 'seller-dashboard.html';
  } else if (role === 'ADMIN') {
    window.location.href = 'admin-dashboard.html';
  } else {
    window.location.href = 'index.html';
  }
}

function bindLogin() {
  const form = document.getElementById('login-form');
  if (!form) return;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    CartPulseUI.clearFormErrors(form);

    const emailEl = document.getElementById('login-email');
    const passEl = document.getElementById('login-password');
    let valid = true;

    if (!EMAIL_RE.test(emailEl.value.trim())) {
      CartPulseUI.setFieldError(emailEl, 'Enter a valid email address.');
      valid = false;
    }
    if (passEl.value.length < 6) {
      CartPulseUI.setFieldError(passEl, 'Password must be at least 6 characters.');
      valid = false;
    }
    if (!valid) return;

    const btn = document.getElementById('login-submit-btn');
    CartPulseUI.setButtonLoading(btn, true, 'Signing in...');

    try {
      const user = await CartPulseAPI.auth.login({
        email: emailEl.value.trim(),
        password: passEl.value
      });
      CartPulseUI.showToast(`Welcome back, ${user.fullName}`);
      setTimeout(() => redirectByRole(user.role), 300);
    } catch (err) {
      CartPulseUI.applyFieldErrors(err.fieldErrors, { email: 'login-email', password: 'login-password' });
      CartPulseUI.showToast(err.message || 'Invalid credentials', 'error');
      CartPulseUI.setButtonLoading(btn, false);
    }
  });
}

function bindRegister() {
  const form = document.getElementById('register-form');
  if (!form) return;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    CartPulseUI.clearFormErrors(form);

    const nameEl = document.getElementById('reg-name');
    const emailEl = document.getElementById('reg-email');
    const roleEl = document.getElementById('reg-role');
    const passEl = document.getElementById('reg-password');
    const confirmEl = document.getElementById('reg-confirm');
    let valid = true;

    if (nameEl.value.trim().length < 2) {
      CartPulseUI.setFieldError(nameEl, 'Enter your full name.');
      valid = false;
    }
    if (!EMAIL_RE.test(emailEl.value.trim())) {
      CartPulseUI.setFieldError(emailEl, 'Enter a valid email address.');
      valid = false;
    }
    if (passEl.value.length < 6) {
      CartPulseUI.setFieldError(passEl, 'Password must be at least 6 characters.');
      valid = false;
    }
    if (confirmEl.value !== passEl.value) {
      CartPulseUI.setFieldError(confirmEl, 'Passwords do not match.');
      valid = false;
    }
    if (!valid) return;

    const btn = document.getElementById('reg-submit-btn');
    CartPulseUI.setButtonLoading(btn, true, 'Creating account...');

    try {
      const user = await CartPulseAPI.auth.register({
        fullName: nameEl.value.trim(),
        email: emailEl.value.trim(),
        password: passEl.value,
        role: roleEl.value
      });
      CartPulseUI.showToast('Account created!');
      setTimeout(() => redirectByRole(user.role), 350);
    } catch (err) {
      CartPulseUI.applyFieldErrors(err.fieldErrors, {
        fullName: 'reg-name',
        email: 'reg-email',
        password: 'reg-password'
      });
      CartPulseUI.showToast(err.message || 'Registration failed', 'error');
      CartPulseUI.setButtonLoading(btn, false);
    }
  });
}

function bindDemoPrefill() {
  document.querySelectorAll('[data-demo-email]').forEach((btn) => {
    btn.addEventListener('click', () => {
      document.getElementById('login-email').value = btn.dataset.demoEmail;
      document.getElementById('login-password').value = btn.dataset.demoPass;
      document.getElementById('login-form').requestSubmit();
    });
  });
}

init();
