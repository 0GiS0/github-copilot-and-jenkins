(function () {
  const widget = document.getElementById('copilot-chat-widget');
  if (!widget || widget.dataset.ready === 'true') {
    return;
  }
  widget.dataset.ready = 'true';

  const toggle = document.getElementById('copilot-chat-toggle');
  const panel = document.getElementById('copilot-chat-panel');
  const closeButton = document.getElementById('copilot-chat-close');
  const status = document.getElementById('copilot-chat-auth-status');
  const loginButton = document.getElementById('copilot-chat-login-button');
  const logoutButton = document.getElementById('copilot-chat-logout-button');
  const deviceCode = document.getElementById('copilot-chat-device-code');
  const deviceLink = document.getElementById('copilot-chat-device-link');
  const form = document.getElementById('copilot-chat-form');
  const messages = document.getElementById('copilot-chat-messages');
  const prompt = document.getElementById('copilot-chat-prompt');

  const base = (widget.dataset.baseUrl || '/copilot-chat').replace(/\/$/, '');

  async function post(path, body) {
    const response = await fetch(`${base}/${path}`, {
      method: 'POST',
      headers: crumbHeaders({ 'Content-Type': 'application/json' }),
      body: body ? JSON.stringify(body) : undefined
    });
    return response.json();
  }

  function crumbHeaders(headers) {
    const crumb = window.crumb;
    if (crumb && crumb.fieldName && crumb.value) {
      headers[crumb.fieldName] = crumb.value;
    }
    return headers;
  }

  function setAuthenticated(login) {
    status.textContent = `Signed in as ${login}`;
    loginButton.hidden = true;
    logoutButton.hidden = false;
    form.hidden = false;
    loginButton.style.display = 'none';
    logoutButton.style.display = '';
    form.style.display = '';
    deviceCode.textContent = '';
    deviceLink.textContent = '';
    deviceLink.removeAttribute('href');
  }

  function setLoggedOut() {
    status.textContent = 'Not signed in';
    loginButton.hidden = false;
    logoutButton.hidden = true;
    form.hidden = true;
    loginButton.style.display = '';
    logoutButton.style.display = 'none';
    form.style.display = 'none';
  }

  async function refreshStatus() {
    const result = await post('authStatus');
    if (result.authenticated) {
      setAuthenticated(result.login);
    } else {
      setLoggedOut();
    }
  }

  async function startLogin() {
    const start = await post('startLogin');
    deviceCode.textContent = start.userCode;
    deviceLink.href = start.verificationUri;
    deviceLink.textContent = start.verificationUri;
    status.textContent = 'Waiting for GitHub authorization...';
    pollLogin(start.loginId, start.interval || 5);
  }

  function pollLogin(loginId, interval) {
    window.setTimeout(async () => {
      const result = await post(`pollLogin?loginId=${encodeURIComponent(loginId)}`);
      if (result.status === 'authenticated') {
        setAuthenticated(result.login);
      } else if (result.status === 'pending') {
        status.textContent = 'Still waiting for GitHub authorization...';
        pollLogin(loginId, result.interval || interval);
      } else {
        status.textContent = result.message || result.error || 'Login failed';
      }
    }, Math.max(1, interval) * 1000);
  }

  async function logout() {
    await post('logout');
    setLoggedOut();
    addMessage('system', 'Signed out');
  }

  function setOpen(open) {
    panel.hidden = !open;
    toggle.setAttribute('aria-expanded', String(open));
    if (open) {
      prompt.focus();
    }
  }

  function addMessage(kind, text) {
    const message = document.createElement('div');
    message.className = `copilot-chat-message copilot-chat-message--${kind}`;
    message.textContent = text;
    messages.appendChild(message);
    messages.scrollTop = messages.scrollHeight;
    return message;
  }

  async function sendMessage(event) {
    event.preventDefault();
    const text = prompt.value.trim();
    if (!text) {
      return;
    }
    prompt.value = '';
    addMessage('user', text);
    const pending = addMessage('assistant', 'Waiting for Copilot...');
    const result = await post('sendMessage', { prompt: text });
    pending.textContent = result.message || result.error || '';
  }

  toggle.addEventListener('click', () => setOpen(panel.hidden));
  closeButton.addEventListener('click', () => setOpen(false));
  loginButton.addEventListener('click', startLogin);
  logoutButton.addEventListener('click', logout);
  form.addEventListener('submit', sendMessage);
  refreshStatus().catch((error) => {
    status.textContent = error.message || 'Unable to load status';
  });

  if (window.location.pathname.replace(/\/$/, '') === `${new URL(base, window.location.origin).pathname}`) {
    setOpen(true);
  }
}());