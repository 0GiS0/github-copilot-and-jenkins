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
  const deviceInfo = document.getElementById('copilot-chat-device-info');
  const copyButton = document.getElementById('copilot-chat-copy-button');
  const form = document.getElementById('copilot-chat-form');
  const messages = document.getElementById('copilot-chat-messages');
  const prompt = document.getElementById('copilot-chat-prompt');
  const loginContainer = document.getElementById('copilot-chat-login');
  const resizeHandle = document.getElementById('copilot-chat-resize-handle');
  const sendButton = document.getElementById('copilot-chat-send');
  const fullscreenButton = document.getElementById('copilot-chat-fullscreen');
  const fullscreenExpandIcon = document.getElementById('copilot-chat-fullscreen-expand');
  const fullscreenCollapseIcon = document.getElementById('copilot-chat-fullscreen-collapse');
  let isFullscreen = false;

  // Load markdown library
  const script = document.createElement('script');
  script.src = 'https://cdn.jsdelivr.net/npm/marked@11.1.1/+esm';
  script.type = 'module';
  script.onload = () => {
    window.markdownReady = true;
  };
  document.head.appendChild(script);

  function autoResize() {
    prompt.style.height = 'auto';
    prompt.style.height = `${Math.min(prompt.scrollHeight, 180)}px`;
  }

  function updateSendState() {
    if (sendButton) {
      sendButton.disabled = prompt.value.trim().length === 0;
    }
  }

  const base = (widget.dataset.baseUrl || '/copilot-chat').replace(/\/$/, '');
  const pluginBase = `${document.head.dataset.rooturl || ''}/plugin/copilot-chat`;
  const copilotLogoUrl = `${pluginBase}/images/github-copilot-jenkins-logo.png`;
  let currentUserAvatar = '';
  let currentUserLogin = '';
  let selectedModel = localStorage.getItem('copilot-chat-model') || '';
  let availableModels = [];
  const CHAT_STORAGE_KEY = 'copilot-chat-history';
  const newConversationButton = document.getElementById('copilot-chat-new-conversation');

  // Get model selector from DOM
  const modelSelector = document.getElementById('copilot-chat-model-selector');

  if (modelSelector) {
    modelSelector.addEventListener('change', () => {
      selectedModel = modelSelector.value;
      localStorage.setItem('copilot-chat-model', selectedModel);
    });
  }

  async function loadModels() {
    if (!modelSelector) return;
    try {
      const result = await post('models');
      if (result.error) {
        console.error('Failed to load models:', result.error);
        modelSelector.innerHTML = '<option value="">Default model</option>';
        modelSelector.hidden = false;
        return;
      }
      availableModels = result.models || [];
      const defaultModel = result.defaultModel || '';
      
      modelSelector.innerHTML = '';
      availableModels.forEach(model => {
        const option = document.createElement('option');
        option.value = model.id;
        option.textContent = model.name || model.id;
        if (selectedModel === model.id || (!selectedModel && model.id === defaultModel)) {
          option.selected = true;
          selectedModel = model.id;
        }
        modelSelector.appendChild(option);
      });
      
      if (availableModels.length === 0) {
        modelSelector.innerHTML = '<option value="">Default model</option>';
      }
      
      modelSelector.hidden = false;
    } catch (err) {
      console.error('Error loading models:', err);
      modelSelector.innerHTML = '<option value="">Default model</option>';
      modelSelector.hidden = false;
    }
  }

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

  function setAuthenticated(login, githubId) {
    currentUserLogin = login;
    currentUserAvatar = githubId ? `https://avatars.githubusercontent.com/u/${githubId}?s=56` : '';
    status.textContent = `Signed in as ${login}`;
    loginButton.hidden = true;
    logoutButton.hidden = false;
    form.hidden = false;
    if (loginContainer) {
      loginContainer.hidden = true;
    }
    if (deviceInfo) {
      deviceInfo.hidden = true;
    }
    deviceCode.textContent = '';
    deviceLink.textContent = '';
    deviceLink.removeAttribute('href');
    loadModels();
  }

  function setLoggedOut() {
    status.textContent = 'Not signed in';
    loginButton.hidden = false;
    logoutButton.hidden = true;
    form.hidden = true;
    modelSelector.hidden = true;
    if (loginContainer) {
      loginContainer.hidden = false;
    }
  }

  async function refreshStatus() {
    const result = await post('authStatus');
    if (result.authenticated) {
      setAuthenticated(result.login, result.id);
    } else {
      setLoggedOut();
    }
  }

  async function startLogin() {
    try {
      const start = await post('startLogin');
      if (start.error) {
        status.textContent = `❌ ${start.error}`;
        return;
      }
      if (!start.userCode || !start.verificationUri) {
        status.textContent = '❌ Invalid response from server';
        console.error('Invalid startLogin response:', start);
        return;
      }
      deviceCode.textContent = start.userCode;
      deviceLink.href = start.verificationUri;
      deviceLink.textContent = start.verificationUri;
      if (deviceInfo) {
        deviceInfo.hidden = false;
      }
      status.textContent = 'Waiting for GitHub authorization...';
      pollLogin(start.loginId, start.interval || 5);
    } catch (err) {
      status.textContent = `❌ ${err.message || 'Login failed'}`;
      console.error('startLogin error:', err);
    }
  }

  function pollLogin(loginId, interval) {
    // Reduce the minimum interval to 2 seconds for faster feedback
    const pollInterval = Math.max(2, interval) * 1000;
    window.setTimeout(async () => {
      try {
        const result = await post(`pollLogin?loginId=${encodeURIComponent(loginId)}`);
        if (result.status === 'authenticated') {
          setAuthenticated(result.login, result.id);
        } else if (result.status === 'pending') {
          status.textContent = '⏳ Waiting for GitHub authorization...';
          pollLogin(loginId, result.interval || interval);
        } else if (result.error) {
          status.textContent = `❌ ${result.error}${result.message ? ': ' + result.message : ''}`;
        } else {
          status.textContent = `❌ ${result.message || 'Login failed'}`;
        }
      } catch (err) {
        status.textContent = `❌ ${err.message || 'Network error'}`;
        console.error('pollLogin error:', err);
      }
    }, pollInterval);
  }

  async function logout() {
    await post('logout');
    clearHistory();
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

  function saveHistory() {
    try {
      const entries = [];
      messages.querySelectorAll('.copilot-chat-message-row').forEach(row => {
        const kind = row.classList.contains('copilot-chat-message-row--user') ? 'user'
          : row.classList.contains('copilot-chat-message-row--assistant') ? 'assistant'
          : 'system';
        const msg = row.querySelector('.copilot-chat-message');
        if (!msg) return;
        // Skip loading placeholders
        if (msg.querySelector('.copilot-chat-loading__text')) return;
        const html = msg.innerHTML;
        entries.push({ kind, html });
      });
      sessionStorage.setItem(CHAT_STORAGE_KEY, JSON.stringify(entries));
    } catch (e) {
      // sessionStorage may be full or disabled
    }
  }

  function restoreHistory() {
    try {
      const data = sessionStorage.getItem(CHAT_STORAGE_KEY);
      if (!data) return;
      const entries = JSON.parse(data);
      entries.forEach(entry => {
        const wrapper = document.createElement('div');
        wrapper.className = `copilot-chat-message-row copilot-chat-message-row--${entry.kind}`;
        if (entry.kind === 'user' || entry.kind === 'assistant') {
          const avatar = document.createElement('div');
          avatar.className = 'copilot-chat-avatar';
          if (entry.kind === 'assistant') {
            const img = document.createElement('img');
            img.src = copilotLogoUrl;
            img.alt = 'Copilot';
            img.className = 'copilot-chat-avatar__img';
            avatar.appendChild(img);
          } else if (currentUserAvatar) {
            const img = document.createElement('img');
            img.src = currentUserAvatar;
            img.alt = currentUserLogin;
            img.className = 'copilot-chat-avatar__img';
            avatar.appendChild(img);
          } else {
            avatar.innerHTML = `<span class="copilot-chat-avatar__initials">${(currentUserLogin || 'U').charAt(0).toUpperCase()}</span>`;
          }
          wrapper.appendChild(avatar);
        }
        const message = document.createElement('div');
        message.className = `copilot-chat-message copilot-chat-message--${entry.kind}`;
        if (entry.kind === 'assistant') {
          message.classList.add('copilot-chat-message--markdown', 'copilot-chat-message--complete');
        }
        message.innerHTML = entry.html;
        wrapper.appendChild(message);
        messages.appendChild(wrapper);
      });
      messages.scrollTop = messages.scrollHeight;
    } catch (e) {
      // corrupted data – ignore
    }
  }

  function clearHistory() {
    sessionStorage.removeItem(CHAT_STORAGE_KEY);
    messages.innerHTML = '';
  }

  function addMessage(kind, text, isLoading = false) {
    const wrapper = document.createElement('div');
    wrapper.className = `copilot-chat-message-row copilot-chat-message-row--${kind}`;

    // Avatar
    if (kind === 'user' || kind === 'assistant') {
      const avatar = document.createElement('div');
      avatar.className = 'copilot-chat-avatar';
      if (kind === 'assistant') {
        const img = document.createElement('img');
        img.src = copilotLogoUrl;
        img.alt = 'Copilot';
        img.className = 'copilot-chat-avatar__img';
        if (isLoading) img.classList.add('copilot-chat-avatar__img--pulse');
        avatar.appendChild(img);
      } else if (currentUserAvatar) {
        const img = document.createElement('img');
        img.src = currentUserAvatar;
        img.alt = currentUserLogin;
        img.className = 'copilot-chat-avatar__img';
        avatar.appendChild(img);
      } else {
        avatar.innerHTML = `<span class="copilot-chat-avatar__initials">${(currentUserLogin || 'U').charAt(0).toUpperCase()}</span>`;
      }
      wrapper.appendChild(avatar);
    }

    const message = document.createElement('div');
    message.className = `copilot-chat-message copilot-chat-message--${kind}`;
    
    if (isLoading) {
      message.innerHTML = `<span class="copilot-chat-loading__text">Thinking...</span>`;
    } else if (kind === 'assistant') {
      message.className += ' copilot-chat-message--markdown';
      message.innerHTML = renderMarkdown(text || '(empty response)');
    } else {
      message.textContent = text;
    }
    
    wrapper.appendChild(message);
    messages.appendChild(wrapper);
    messages.scrollTop = messages.scrollHeight;
    if (!isLoading) saveHistory();
    return message;
  }

  function sanitizeHtml(html) {
    const div = document.createElement('div');
    div.textContent = html;
    return div.innerHTML;
  }

  function renderMarkdown(text) {
    // Escape HTML entities but preserve UTF-8 characters
    let html = text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
    
    // Code blocks (must go before inline code)
    html = html.replace(/```(\w*)\n?([\s\S]*?)```/g, '<pre><code>$2</code></pre>');
    
    // Bold
    html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/__(.+?)__/g, '<strong>$1</strong>');
    
    // Italic
    html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');
    html = html.replace(/_(.+?)_/g, '<em>$1</em>');
    
    // Inline code
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
    
    // Unordered lists
    html = html.replace(/^[\s]*[-*]\s+(.+)$/gm, '<li>$1</li>');
    html = html.replace(/(<li>.*<\/li>\n?)+/g, '<ul>$&</ul>');

    // Links
    html = html.replace(/\[(.+?)\]\((.+?)\)/g, '<a href="$2" target="_blank">$1</a>');
    
    // Headings
    html = html.replace(/^### (.+)$/gm, '<h3>$1</h3>');
    html = html.replace(/^## (.+)$/gm, '<h2>$1</h2>');
    html = html.replace(/^# (.+)$/gm, '<h1>$1</h1>');
    
    // Line breaks
    html = html.replace(/\n/g, '<br>');
    
    return html;
  }

  async function sendMessage(event) {
    event.preventDefault();
    const text = prompt.value.trim();
    if (!text) {
      return;
    }
    prompt.value = '';
    autoResize();
    prompt.disabled = true;
    sendButton.disabled = true;

    addMessage('user', text);
    const pending = addMessage('assistant', '', true);  // Show loading animation

    try {
      const response = await fetch(`${base}/sendMessage`, {
        method: 'POST',
        headers: crumbHeaders({ 'Content-Type': 'application/json' }),
        body: JSON.stringify({ prompt: text, pagePath: window.location.pathname, model: selectedModel || null })
      });

      if (!response.ok) {
        const error = await response.json();
        pending.innerHTML = `<span class="copilot-chat-message--error">⚠ ${error.error || 'Unknown error'}</span>`;
        pending.classList.add('copilot-chat-message--error');
        return;
      }

      let fullContent = '';
      let reasoningContent = '';
      let reasoningBlock = null;
      let hasStartedResponse = false;
      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = '';

      // Clear loading animation and stop avatar pulse
      pending.innerHTML = '';
      pending.classList.remove('copilot-chat-message--error');
      pending.classList.add('copilot-chat-message--markdown');
      const pulsingAvatar = pending.parentElement.querySelector('.copilot-chat-avatar__img--pulse');
      if (pulsingAvatar) pulsingAvatar.classList.remove('copilot-chat-avatar__img--pulse');

      const cursor = document.createElement('span');
      cursor.className = 'copilot-chat-cursor';
      pending.appendChild(cursor);

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n\n');
        buffer = lines.pop() || '';

        for (const line of lines) {
          if (line.startsWith('data: ')) {
            try {
              const data = JSON.parse(line.slice(6));
              if (data.type === 'reasoning') {
                // Create reasoning block if it doesn't exist
                if (!reasoningBlock) {
                  reasoningBlock = document.createElement('details');
                  reasoningBlock.className = 'copilot-chat-reasoning';
                  reasoningBlock.open = true;
                  const summary = document.createElement('summary');
                  summary.textContent = '💭 Thinking...';
                  reasoningBlock.appendChild(summary);
                  const content = document.createElement('div');
                  content.className = 'copilot-chat-reasoning__content';
                  reasoningBlock.appendChild(content);
                  pending.insertBefore(reasoningBlock, cursor);
                }
                reasoningContent += data.content;
                const content = reasoningBlock.querySelector('.copilot-chat-reasoning__content');
                content.textContent = reasoningContent;
                messages.scrollTop = messages.scrollHeight;
              } else if (data.type === 'delta') {
                // Collapse reasoning block when response starts
                if (reasoningBlock && !hasStartedResponse) {
                  reasoningBlock.open = false;
                  const summary = reasoningBlock.querySelector('summary');
                  if (summary) summary.textContent = '💭 Show thinking';
                  hasStartedResponse = true;
                }
                fullContent += data.content;
                // Rebuild pending content with reasoning block + response
                if (reasoningBlock) {
                  pending.innerHTML = '';
                  pending.appendChild(reasoningBlock);
                  const responseDiv = document.createElement('div');
                  responseDiv.innerHTML = renderMarkdown(fullContent);
                  pending.appendChild(responseDiv);
                } else {
                  pending.innerHTML = renderMarkdown(fullContent);
                }
                pending.appendChild(cursor);
                messages.scrollTop = messages.scrollHeight;
              } else if (data.type === 'complete') {
                cursor.remove();
                pending.classList.add('copilot-chat-message--complete');
                saveHistory();
              } else if (data.type === 'error') {
                cursor.remove();
                pending.innerHTML = `<span class="copilot-chat-message--error">⚠ ${data.message}</span>`;
                pending.classList.add('copilot-chat-message--error');
              }
            } catch (e) {
              console.error('Failed to parse SSE message:', line, e);
            }
          }
        }
      }
    } catch (err) {
      pending.innerHTML = `<span class="copilot-chat-message--error">⚠ ${err.message || 'Network error'}</span>`;
      pending.classList.add('copilot-chat-message--error');
    } finally {
      prompt.disabled = false;
      sendButton.disabled = false;
      updateSendState();
      prompt.focus();
    }
  }

  function toggleFullscreen(enable) {
    isFullscreen = enable !== undefined ? enable : !isFullscreen;
    panel.classList.toggle('copilot-chat-panel--fullscreen', isFullscreen);
    if (fullscreenExpandIcon) fullscreenExpandIcon.hidden = isFullscreen;
    if (fullscreenCollapseIcon) fullscreenCollapseIcon.hidden = !isFullscreen;
    fullscreenButton.title = isFullscreen ? 'Exit fullscreen' : 'Toggle fullscreen';
    fullscreenButton.setAttribute('aria-label', fullscreenButton.title);
  }

  toggle.addEventListener('click', () => setOpen(panel.hidden));
  closeButton.addEventListener('click', () => {
    if (isFullscreen) toggleFullscreen(false);
    setOpen(false);
  });
  if (fullscreenButton) {
    fullscreenButton.addEventListener('click', () => toggleFullscreen());
  }
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && isFullscreen && !panel.hidden) {
      toggleFullscreen(false);
    }
  });
  loginButton.addEventListener('click', startLogin);
  logoutButton.addEventListener('click', logout);
  form.addEventListener('submit', sendMessage);
  if (newConversationButton) {
    newConversationButton.addEventListener('click', clearHistory);
  }

  if (copyButton) {
    copyButton.addEventListener('click', async () => {
      const codeText = deviceCode.textContent.trim();
      if (codeText) {
        try {
          await navigator.clipboard.writeText(codeText);
          copyButton.classList.add('copied');
          copyButton.setAttribute('title', 'Copied!');
          setTimeout(() => {
            copyButton.classList.remove('copied');
            copyButton.setAttribute('title', 'Copy code to clipboard');
          }, 2000);
        } catch (err) {
          console.error('Failed to copy:', err);
        }
      }
    });
  }

  prompt.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) {
      event.preventDefault();
      if (typeof form.requestSubmit === 'function') {
        form.requestSubmit();
      } else {
        sendMessage(event);
      }
    }
  });

  prompt.addEventListener('input', () => {
    autoResize();
    updateSendState();
  });

  updateSendState();

  if (resizeHandle) {
    let resizeState = null;

    const onPointerMove = (event) => {
      if (!resizeState) {
        return;
      }
      const dx = resizeState.startX - event.clientX;
      const dy = resizeState.startY - event.clientY;
      const newWidth = Math.max(320, Math.min(window.innerWidth - 32, resizeState.startWidth + dx));
      const newHeight = Math.max(320, Math.min(window.innerHeight - 120, resizeState.startHeight + dy));
      panel.style.width = `${newWidth}px`;
      panel.style.height = `${newHeight}px`;
    };

    const stopResize = () => {
      resizeState = null;
      document.removeEventListener('pointermove', onPointerMove);
      document.removeEventListener('pointerup', stopResize);
      document.body.style.userSelect = '';
    };

    resizeHandle.addEventListener('pointerdown', (event) => {
      event.preventDefault();
      resizeState = {
        startX: event.clientX,
        startY: event.clientY,
        startWidth: panel.offsetWidth,
        startHeight: panel.offsetHeight
      };
      document.body.style.userSelect = 'none';
      document.addEventListener('pointermove', onPointerMove);
      document.addEventListener('pointerup', stopResize);
    });
  }

  restoreHistory();

  refreshStatus().catch((error) => {
    status.textContent = error.message || 'Unable to load status';
  });

  if (window.location.pathname.replace(/\/$/, '') === `${new URL(base, window.location.origin).pathname}`) {
    setOpen(true);
  }
}());