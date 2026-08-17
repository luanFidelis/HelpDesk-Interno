/* ============================================================
   CHAMADOS · HQ — frontend client (editorial zine build)
   ============================================================ */
(() => {
  const API = '/api/v1/chamados';

  const TIPO_LABEL = {
    PERMISSOES: 'Permissões',
    EMAIL: 'E-mail',
    HARDWARE: 'Hardware',
    IMPRESSORA: 'Impressora',
    SISTEMA: 'Sistema',
    OUTROS: 'Outros',
  };

  const STATUS_LABEL = {
    ABERTO: 'Aberto',
    EM_ANDAMENTO: 'Em andamento',
    RESOLVIDO: 'Resolvido',
    FECHADO: 'Fechado',
    REABERTO: 'Reaberto',
  };

  // REABERTO não é uma transição manual: somente o botão de reabertura pode aplicá-lo.
  const STATUS_LIST = ['ABERTO', 'EM_ANDAMENTO', 'RESOLVIDO', 'FECHADO'];

  const PRIORIDADE_LABEL = {
    BAIXA: 'Baixa',
    MEDIA: 'Média',
    ALTA: 'Alta',
    CRITICA: 'Crítica',
  };
  function prioridadeToken(p) {
    return ({ BAIXA: 'baixa', MEDIA: 'media', ALTA: 'alta', CRITICA: 'critica' })[p] || 'baixa';
  }

  /* ---------------------------------------------------------- API */

  /**
   * O backend responde erro sempre no mesmo formato (ErroResposta): status,
   * mensagem e, quando é validação, o erro de cada campo. Aqui isso vira a
   * mensagem que aparece no toast, em vez de um "(400)" solto na tela.
   */
  async function erroDaResposta(r, fallback) {
    try {
      const corpo = await r.json();
      const porCampo = corpo.campos ? Object.values(corpo.campos).filter(Boolean) : [];
      if (porCampo.length) return new Error(porCampo.join(' '));
      if (corpo.mensagem) return new Error(corpo.mensagem);
    } catch { /* resposta sem corpo JSON: cai no texto padrão */ }
    return new Error(`${fallback} (${r.status})`);
  }

  async function pedir(url, opcoes, fallback) {
    const r = await fetch(url, opcoes);
    if (!r.ok) throw await erroDaResposta(r, fallback);
    return r.status === 204 ? null : r.json();
  }

  const jsonBody = (corpo) => ({
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(corpo),
  });

  const api = {
    // GET /api/v1/chamados → todas as versões gravadas; groupByNC dobra em 1 por chamado
    list() {
      return pedir(API, undefined, 'Falha ao listar');
    },
    listUser(numeroChamado) {
      return pedir(`${API}/${encodeURIComponent(numeroChamado)}`, undefined,
        'Falha ao buscar histórico');
    },
    // 201 Created devolvendo o chamado gravado — é daí que sai o protocolo
    create(dto) {
      return pedir(API, { method: 'POST', ...jsonBody(dto) }, 'Falha ao registrar');
    },
    changeStatus(numeroChamado, status) {
      return pedir(`${API}/${encodeURIComponent(numeroChamado)}/status`,
        { method: 'PATCH', ...jsonBody({ status }) }, 'Falha ao atualizar status');
    },
    async show(numeroChamado) {
      const registros = await pedir(`${API}/${encodeURIComponent(numeroChamado)}`, undefined,
        'Falha ao carregar chamado');
      if (!Array.isArray(registros)) throw new Error('Resposta inválida ao carregar chamado');
      return registros;
    },
    reabrir(numeroChamado) {
      return pedir(`${API}/${encodeURIComponent(numeroChamado)}/reaberturas`,
        { method: 'POST' }, 'Falha ao reabrir chamado');
    },
    alterarDescricao(numeroChamado, descricao) {
      return pedir(`${API}/${encodeURIComponent(numeroChamado)}/descricao`,
        { method: 'PATCH', ...jsonBody({ descricao }) }, 'Falha ao alterar descrição');
    },
  };

  /* ---------------------------------------------------------- utils */
  function parseDate(v) {
    if (!v) return null;
    if (Array.isArray(v)) {
      const [y, m, d, h = 0, mi = 0, s = 0] = v;
      return new Date(y, m - 1, d, h, mi, s);
    }
    return new Date(v);
  }
  function fmtDate(v) {
    const d = parseDate(v);
    if (!d || isNaN(d)) return '—';
    return d.toLocaleString('pt-BR', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit',
    });
  }
  const $  = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => Array.from(r.querySelectorAll(s));

  /* ---------------------------------------------------------- toast */
  const toastEl = $('#toast');
  let toastTimer = null;
  function toast(message, tone = 'ok') {
    toastEl.querySelector('.toast__msg').textContent = message;
    toastEl.dataset.tone = tone;
    toastEl.hidden = false;
    requestAnimationFrame(() => toastEl.dataset.show = 'true');
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
      toastEl.dataset.show = 'false';
      setTimeout(() => { toastEl.hidden = true; }, 320);
    }, 3400);
  }

  /* ---------------------------------------------------------- routing */
  const tabs = $$('.tab');
  const views = $$('.view');

  function activate(target) {
    tabs.forEach(b => b.classList.toggle('tab--active', b.dataset.target === target));
    views.forEach(v => v.classList.toggle('view--active', v.dataset.view === target));
    if (target === 'admin') loadAdmin();
    if (history.replaceState) history.replaceState(null, '', `#${target}`);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
  tabs.forEach(b => b.addEventListener('click', () => activate(b.dataset.target)));

  const initial = (location.hash || '').replace('#', '');
  if (['abrir', 'meus', 'admin'].includes(initial)) activate(initial);

  /* ---------------------------------------------------------- ABRIR */
  const formAbrir = $('#formAbrir');
  const ack = $('#ackAbrir');

  formAbrir.addEventListener('submit', async (e) => {
    e.preventDefault();
    const fd = new FormData(formAbrir);
    const dto = {
      nome: fd.get('nome').trim(),
      email: fd.get('email').trim(),
      tipoChamado: fd.get('tipoChamado'),
      prioridade: fd.get('prioridade'),
      descricao: fd.get('descricao').trim(),
    };

    const submitBtn = formAbrir.querySelector('button[type="submit"]');
    const topEl = submitBtn.querySelector('.stamp__top');
    const originalText = topEl ? topEl.textContent : '';
    submitBtn.disabled = true;
    if (topEl) topEl.textContent = 'REGISTRANDO…';

    try {
      // O POST devolve o chamado gravado, então o protocolo vem na resposta —
      // não é preciso listar tudo e tentar reconhecer o próprio chamado.
      const criado = await api.create(dto);

      $('#ackNumero').textContent = criado.numeroChamado || '—';
      $('#ackNome').textContent   = criado.nome || dto.nome;
      $('#ackTipo').textContent   = TIPO_LABEL[criado.tipoChamado] || criado.tipoChamado;
      const ackPri = $('#ackPrioridade');
      ackPri.textContent = PRIORIDADE_LABEL[criado.prioridade] || criado.prioridade || '—';
      ackPri.className = `pri-badge pri-${prioridadeToken(criado.prioridade)}`;
      $('#ackData').textContent   = criado.dataAbertura ? fmtDate(criado.dataAbertura) : '—';
      ack.hidden = false;
      ack.scrollIntoView({ behavior: 'smooth', block: 'center' });

      formAbrir.reset();
      toast('Chamado registrado — recibo emitido.', 'ok');
    } catch (err) {
      toast(err.message || 'Erro ao registrar chamado.', 'err');
    } finally {
      submitBtn.disabled = false;
      if (topEl) topEl.textContent = originalText;
    }
  });

  $('#ackClose').addEventListener('click', () => {
    ack.hidden = true;
    formAbrir.querySelector('input[name="nome"]').focus();
  });

  /* ---------------------------------------------------------- MEUS */
  const formMeus = $('#formMeus');
  const meusResults = $('#meusResults');

  // normaliza o que o usuário digita: tira espaços (inclusive invisíveis),
  // maiúsculas, e auto-prefixa "NC-" quando vierem só os números
  // Protocolo é NC-ANO-SEQUENCIA (ex.: NC-2026-0007). Aceita o número digitado
  // sem o prefixo ("2026-0007") ou sem o hífen ("NC2026-0007").
  function normalizarNC(raw) {
    let v = (raw || '').replace(/\s+/g, '').toUpperCase();
    if (/^[\d-]+$/.test(v)) v = 'NC-' + v;                   // "2026-0007" → "NC-2026-0007"
    else if (/^NC[\d-]+$/.test(v)) v = v.replace(/^NC-?/, 'NC-'); // "NC2026-0007" → "NC-2026-0007"
    return v;
  }

  formMeus.addEventListener('submit', async (e) => {
    e.preventDefault();
    const inputEl = $('#numeroChamado');
    const numeroChamado = normalizarNC(inputEl.value);
    inputEl.value = numeroChamado; // reflete o valor normalizado pro usuário
    if (!numeroChamado || numeroChamado === 'NC-') {
      toast('Informe o número do chamado.', 'err');
      return;
    }
    meusResults.innerHTML = `<div class="empty">
      <span class="empty__big">··</span>
      <div class="empty__txt">
        <p class="empty__title">Consultando…</p>
        <p class="empty__sub">Recuperando o arquivo do solicitante.</p>
      </div>
    </div>`;
    try {
      const items = await api.listUser(numeroChamado);
      renderMeus(items);
    } catch (err) {
      renderMeusEmpty('Não foi possível buscar', err.message);
      toast(err.message || 'Erro na consulta.', 'err');
    }
  });

  function renderMeus(items) {
    const groups = groupByNC(items);
    if (groups.length === 0) {
      state.meus = [];
      renderMeusEmpty('Nenhum chamado encontrado', 'Não existem registros para este número de chamado.');
      return;
    }
    state.meus = groups;
    const tpl = $('#ticketCardTpl');
    const grid = document.createElement('div');
    grid.className = 'tickets';
    groups.forEach(c => {
      const node = tpl.content.firstElementChild.cloneNode(true);
      const status = c.status || 'ABERTO';
      node.classList.add(`st-${status}`);
      node.dataset.num = c.numeroChamado || '';
      node.querySelector('.ticket__num').textContent = c.numeroChamado || '—';
      node.querySelector('.ticket__status').textContent = STATUS_LABEL[status] || status;
      node.querySelector('.ticket__tipo').textContent = TIPO_LABEL[c.tipoChamado] || c.tipoChamado || 'sem tipo';
      const priBadge = node.querySelector('.ticket__pri');
      if (c.prioridade) {
        priBadge.textContent = PRIORIDADE_LABEL[c.prioridade] || c.prioridade;
        priBadge.classList.add(`pri-${prioridadeToken(c.prioridade)}`);
        priBadge.hidden = false;
      }
      node.querySelector('.ticket__desc').textContent = c.descricao || '';
      node.querySelector('.ticket__abertura').textContent = fmtDate(c.dataAbertura);
      node.querySelector('.ticket__fechamento').textContent = c.dataFechamento ? fmtDate(c.dataFechamento) : '—';
      grid.appendChild(node);
    });
    meusResults.innerHTML = '';
    meusResults.appendChild(grid);
  }

  function renderMeusEmpty(title, sub) {
    meusResults.innerHTML = `<div class="empty">
      <span class="empty__big">!?</span>
      <div class="empty__txt">
        <p class="empty__title">${escapeHtml(title)}</p>
        <p class="empty__sub">${escapeHtml(sub)}</p>
      </div>
    </div>`;
  }

  /* ---------------------------------------------------------- ADMIN */
  const admBody = $('#admBody');
  const refreshBtn = $('#refreshAdm');
  const filterChips = {
    status: $$('.chips[data-filter="status"] .chip'),
    tipo:   $$('.chips[data-filter="tipo"] .chip'),
  };
  const state = { all: [], filters: { status: '', tipo: '' }, loaded: false };

  Object.entries(filterChips).forEach(([key, chips]) => {
    chips.forEach(chip => chip.addEventListener('click', () => {
      chips.forEach(c => c.classList.remove('chip--active'));
      chip.classList.add('chip--active');
      state.filters[key] = chip.dataset.value;
      renderAdmin();
    }));
  });

  refreshBtn.addEventListener('click', () => loadAdmin(true));

  /**
   * Agrupa todas as linhas que compartilham o mesmo numeroChamado.
   * Cada update do backend grava uma linha nova → aqui viram o HISTÓRICO
   * de um único chamado, ordenado por data.
   */
  function groupByNC(rows) {
    const map = new Map();
    for (const c of (rows || [])) {
      const key = c.numeroChamado || c.id || '';
      if (!map.has(key)) map.set(key, []);
      map.get(key).push(c);
    }
    const groups = [];
    for (const [nc, items] of map) {
      // ordem cronológica (mais antigo → mais recente)
      items.sort((a, b) => (parseDate(a.dataAbertura) || 0) - (parseDate(b.dataAbertura) || 0));
      const first = items[0];
      const last = items[items.length - 1];
      const fechado = last.status === 'RESOLVIDO' || last.status === 'FECHADO';
      groups.push({
        numeroChamado: nc,
        nome: first.nome,
        email: first.email,
        tipoChamado: first.tipoChamado,
        prioridade: (items.find(i => i.prioridade) || first).prioridade || null,
        descricao: last.descricao,                 // descrição mais recente (reflete edições)
        dataAbertura: first.dataAbertura,          // abertura original
        ultimaAtualizacao: last.dataAbertura,      // data do último registro
        status: last.status,                       // status atual (último)
        currentId: last.id,                        // id sobre o qual agimos
        dataFechamento: last.dataFechamento || (fechado ? last.dataAbertura : null),
        history: items,                            // trilha completa
      });
    }
    // mais recentemente atualizados primeiro
    groups.sort((a, b) => (parseDate(b.ultimaAtualizacao) || 0) - (parseDate(a.ultimaAtualizacao) || 0));
    return groups;
  }

  async function loadAdmin(force = false) {
    if (state.loaded && !force) return;
    admBody.innerHTML = `<div class="ledger__empty">Carregando registros…</div>`;
    try {
      state.all = groupByNC(await api.list());
      state.loaded = true;
      renderAdmin();
      pingApi(true);
    } catch (err) {
      admBody.innerHTML = `<div class="ledger__empty">Falha ao carregar — ${escapeHtml(err.message)}</div>`;
      pingApi(false);
    }
  }

  function renderAdmin() {
    // REABERTO entra na conta: sem ele a soma dos cartões não fecha com o total.
    const counts = {
      total: state.all.length,
      ABERTO: 0, EM_ANDAMENTO: 0, RESOLVIDO: 0, FECHADO: 0, REABERTO: 0,
    };
    state.all.forEach(c => { if (counts[c.status] != null) counts[c.status] += 1; });
    Object.entries(counts).forEach(([k, n]) => {
      const stat = $(`.stat[data-key="${k}"] .stat__num`);
      if (stat) animateNumber(stat, n);
    });

    const list = state.all
      .filter(c => !state.filters.status || c.status === state.filters.status)
      .filter(c => !state.filters.tipo   || c.tipoChamado === state.filters.tipo)
      .sort((a, b) => (parseDate(b.ultimaAtualizacao) || 0) - (parseDate(a.ultimaAtualizacao) || 0));

    if (list.length === 0) {
      admBody.innerHTML = `<div class="ledger__empty">Nenhum chamado corresponde aos filtros aplicados.</div>`;
      return;
    }

    admBody.innerHTML = list.map((c, i) => row(c, i + 1)).join('');

    $$('.status-select select', admBody).forEach(sel => {
      sel.addEventListener('change', async () => {
        const numeroChamado = sel.dataset.num;   // protocolo identifica o chamado
        const next = sel.value;
        const prev = sel.dataset.prev;
        sel.disabled = true;
        try {
          await api.changeStatus(numeroChamado, next);
          toast(`Status → ${STATUS_LABEL[next]}.`, 'ok');
          // o backend gravou uma linha de histórico nova → re-agrupa tudo
          await loadAdmin(true);
        } catch (err) {
          sel.value = prev;
          sel.disabled = false;
          toast(err.message || 'Erro ao atualizar status.', 'err');
        }
      });
    });
  }

  function row(c, idx) {
    const status = c.status || 'ABERTO';
    const tipo = TIPO_LABEL[c.tipoChamado] || c.tipoChamado || '—';
    const locked = status === 'FECHADO';
    const updates = buildTimelineEvents(c.history || [c]).length;
    const histHint = updates > 1
      ? `<span class="c-prot__hist">${updates} registros · últ. ${escapeHtml(fmtDate(c.ultimaAtualizacao))}</span>`
      : '';
    const statusCell = locked
      ? `
        <div class="status-locked st-${status}">
          <span class="status-locked__badge">
            <span class="pip"></span>${STATUS_LABEL[status]}
          </span>
          <button class="reopen-btn" type="button"
                  data-num="${escapeHtml(c.numeroChamado || '')}"
                  title="Reabrir chamado">
            <svg width="13" height="13" viewBox="0 0 13 13" aria-hidden="true">
              <path d="M11 6.5a4.5 4.5 0 1 1-1.3-3.15M11 1.5v3H8" stroke="currentColor" stroke-width="1.6" fill="none" stroke-linecap="square"/>
            </svg>
            <span>reabrir</span>
          </button>
        </div>`
      : `
        <div class="status-select st-${status}">
          <select data-num="${escapeHtml(c.numeroChamado || '')}" data-prev="${status}" aria-label="Alterar status do chamado">
            ${status === 'REABERTO' ? `<option value="REABERTO" selected hidden>${STATUS_LABEL.REABERTO}</option>` : ''}
            ${STATUS_LIST.map(s => `<option value="${s}" ${s === status ? 'selected' : ''}>${STATUS_LABEL[s]}</option>`).join('')}
          </select>
        </div>`;

    return `
      <div class="row" data-num="${escapeHtml(c.numeroChamado || '')}">
        <div class="c-no">${String(idx).padStart(2, '0')}</div>
        <div class="c-prot">${escapeHtml(c.numeroChamado || '—')}${histHint}</div>
        <div class="c-sol">
          <span class="name">${escapeHtml(c.nome || '—')}</span>
          <span class="email">${escapeHtml(c.email || '')}</span>
        </div>
        <div class="c-tipo">
          ${escapeHtml(tipo)}
          ${c.prioridade ? `<span class="pri-badge pri-${prioridadeToken(c.prioridade)}">${PRIORIDADE_LABEL[c.prioridade] || c.prioridade}</span>` : ''}
        </div>
        <div class="c-data">${escapeHtml(fmtDate(c.dataAbertura))}</div>
        <div class="c-st">${statusCell}</div>
      </div>
    `;
  }

  /* ---------------------------------------------------------- MODAL */
  const modal = $('#modal');
  let currentModalNC = null;
  let currentModalDesc = '';
  let modalOrigin = 'meus';   // 'meus' | 'admin'
  let canEditDesc = false;    // editar descrição só em "Meus Chamados"

  function openModal(c, origin = modalOrigin) {
    const status = c.status || 'ABERTO';
    const statusLabel = STATUS_LABEL[status] || status;
    modalOrigin = origin;
    canEditDesc = origin === 'meus';
    currentModalNC = c.numeroChamado || null;
    currentModalDesc = c.descricao || '';
    resetDescEditor();

    $('#modalProtocolBand').textContent = c.numeroChamado || 'NC-—';
    $('#modalTitle').textContent = c.numeroChamado || 'NC-—';
    $('#modalNome').textContent = c.nome || '—';
    $('#modalEmail').textContent = c.email || '—';
    $('#modalTipo').textContent = TIPO_LABEL[c.tipoChamado] || c.tipoChamado || '—';
    const priEl = $('#modalPrioridade');
    if (c.prioridade) {
      priEl.textContent = PRIORIDADE_LABEL[c.prioridade] || c.prioridade;
      priEl.className = `pri-badge pri-${prioridadeToken(c.prioridade)}`;
    } else {
      priEl.textContent = '—';
      priEl.className = 'pri-badge pri-none';
    }
    $('#modalId').textContent = c.ultimaAtualizacao ? fmtDate(c.ultimaAtualizacao) : fmtDate(c.dataAbertura);
    $('#modalAbertura').textContent = fmtDate(c.dataAbertura);
    $('#modalFechamento').textContent = c.dataFechamento ? fmtDate(c.dataFechamento) : '— em aberto';
    $('#modalDuracao').textContent = computeDuration(c);
    $('#modalResolvido').textContent = resolutionLabel(status, c.dataFechamento);
    $('#modalDescricao').textContent = c.descricao || '— sem descrição —';

    const stamp = $('#modalStamp');
    stamp.style.setProperty('--st', `var(--st-${statusToken(status)})`);
    $('#modalStatus').textContent = statusLabel;

    renderTimeline(c);

    // Reopen action — only when chamado is FECHADO
    const actions = $('#modalActions');
    const reopenBtn = $('#modalReopenBtn');
    if (status === 'FECHADO') {
      actions.hidden = false;
      reopenBtn.dataset.num = c.numeroChamado || '';
    } else {
      actions.hidden = true;
      reopenBtn.dataset.num = '';
    }

    modal.hidden = false;
    document.body.style.overflow = 'hidden';
    modal.querySelector('.modal__close').focus();
  }

  function closeModal() {
    modal.hidden = true;
    document.body.style.overflow = '';
  }

  modal.addEventListener('click', (e) => {
    if (e.target.closest('#modalReopenBtn')) {
      const btn = e.target.closest('#modalReopenBtn');
      handleReabrir(btn.dataset.num, btn);
      return;
    }
    if (e.target.closest('[data-close="1"]')) closeModal();
  });
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && !modal.hidden) closeModal();
  });

  /* ---- editar descrição ---- */
  const descEditBtn = $('#descEditBtn');
  const descEdit    = $('#descEdit');
  const descInput   = $('#descInput');
  const descView    = $('#modalDescricao');

  function resetDescEditor() {
    descEdit.hidden = true;
    descView.hidden = false;
    descEditBtn.hidden = !canEditDesc;   // escondido na aba Admin
  }
  function openDescEditor() {
    descInput.value = currentModalDesc || '';
    descEdit.hidden = false;
    descView.hidden = true;
    descEditBtn.hidden = true;
    descInput.focus();
  }

  descEditBtn.addEventListener('click', openDescEditor);
  $('#descCancel').addEventListener('click', resetDescEditor);

  $('#descSave').addEventListener('click', async () => {
    const nc = currentModalNC;
    const nova = descInput.value.trim();
    if (!nc) return;
    if (!nova) { toast('A descrição não pode ficar vazia.', 'err'); return; }
    if (nova === (currentModalDesc || '').trim()) { resetDescEditor(); return; }

    const saveBtn = $('#descSave');
    const topEl = saveBtn.querySelector('.stamp__top');
    const original = topEl ? topEl.textContent : '';
    saveBtn.disabled = true;
    if (topEl) topEl.textContent = 'SALVANDO…';
    try {
      await api.alterarDescricao(nc, nova);
      toast('Descrição atualizada.', 'ok');
      await loadAdmin(true);            // o backend gravou um registro novo → re-agrupa
      await loadAndShow(nc, modalOrigin); // reabre o modal com o histórico atualizado
    } catch (err) {
      toast(err.message || 'Erro ao alterar descrição.', 'err');
    } finally {
      saveBtn.disabled = false;
      if (topEl) topEl.textContent = original;
    }
  });

  function statusToken(s) {
    return ({ ABERTO: 'aberto', REABERTO: 'reaberto', EM_ANDAMENTO: 'andamento', RESOLVIDO: 'resolvido', FECHADO: 'fechado' })[s] || 'fechado';
  }

  function resolutionLabel(status, dataFechamento) {
    if (status === 'RESOLVIDO') return 'sim — chamado resolvido';
    if (status === 'FECHADO')   return dataFechamento ? 'sim — finalizado' : 'fechado sem conclusão';
    if (status === 'EM_ANDAMENTO') return 'em atendimento';
    if (status === 'REABERTO') return 'reaberto — novo ciclo';
    return 'não — aguardando';
  }

  function computeDuration(c) {
    const start = parseDate(c.dataAbertura);
    const end = parseDate(c.dataFechamento) || new Date();
    if (!start || isNaN(start)) return '—';
    const ms = end - start;
    if (ms < 0) return '—';
    const mins = Math.floor(ms / 60000);
    if (mins < 60) return `${mins} min`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ${mins % 60}min`;
    const days = Math.floor(hrs / 24);
    return `${days}d ${hrs % 24}h`;
  }

  function renderTimeline(c) {
    const tl = $('#modalTimeline');
    const hist = (c.history && c.history.length) ? c.history : [c];
    const events = buildTimelineEvents(hist);

    tl.innerHTML = events.map(e => `
      <li>
        <span class="tl-pip${e.kind === 'desc' ? ' tl-pip--desc' : ''}" style="--tl: var(--st-${e.tok})"></span>
        <div class="tl-body">
          <span class="tl-when">${escapeHtml(fmtDate(e.when))}</span>
          <span class="tl-what">${e.what}</span>
        </div>
      </li>
    `).join('');
  }

  /**
   * Constrói os eventos da linha do tempo comparando cada registro com o anterior:
   *  - status diferente  → evento de mudança de status (ou reabertura)
   *  - mesmo status, descrição diferente → evento "Descrição alterada"
   *  - nada mudou        → registro duplicado, ignorado
   */
  function buildTimelineEvents(history) {
    const items = history || [];
    const events = [];
    let prev = null;
    items.forEach((h, i) => {
      const st = h.status || 'ABERTO';
      const desc = h.descricao || '';
      if (i === 0) {
        events.push({ tok: statusToken(st), when: h.dataAbertura, kind: 'status',
          what: 'Chamado <em>aberto</em> pelo solicitante.' });
      } else {
        const statusMudou = st !== (prev.status || 'ABERTO');
        const descMudou = desc !== (prev.descricao || '');
        if (statusMudou) {
          const what = st === 'REABERTO'
            ? 'Chamado <em>reaberto</em> — novo ciclo.'
            : `Status alterado para <em>${STATUS_LABEL[st] || st}</em>.`;
          events.push({ tok: statusToken(st), when: h.dataAbertura, kind: 'status', what });
        } else if (descMudou) {
          events.push({ tok: statusToken(st), when: h.dataAbertura, kind: 'desc',
            what: 'Descrição <em>alterada</em>.' });
        }
      }
      prev = h;
    });
    return events;
  }

  async function loadAndShow(numeroChamado, origin = 'meus') {
    if (!numeroChamado) return;
    try {
      const registros = await api.show(numeroChamado);
      const group = groupByNC(registros)
        .find(g => g.numeroChamado === numeroChamado);

      if (!group) throw new Error(`Nenhum registro encontrado para ${numeroChamado}`);
      openModal(group, origin);
    } catch (err) {
      toast(err.message || 'Erro ao carregar os detalhes do chamado.', 'err');
    }
  }

  /* row click → abre o modal com o grupo (excluindo a coluna de status) */
  admBody.addEventListener('click', (e) => {
    if (e.target.closest('.reopen-btn')) {
      const btn = e.target.closest('.reopen-btn');
      handleReabrir(btn.dataset.num, btn);
      return;
    }
    const row = e.target.closest('.row');
    if (!row) return;
    if (e.target.closest('.c-st')) return;
    loadAndShow(row.dataset.num, 'admin');   // Admin: sem botão de editar descrição
  });

  /* ticket card click (Meus Chamados) */
  meusResults.addEventListener('click', (e) => {
    const card = e.target.closest('.ticket');
    if (!card) return;
    loadAndShow(card.dataset.num, 'meus');   // Meus Chamados: pode editar descrição
  });

  /* reabrir handler — usado pelo botão na linha e no modal */
  async function handleReabrir(numeroChamado, sourceBtn) {
    if (!numeroChamado) return;
    if (!confirm(`Reabrir o chamado ${numeroChamado}?`)) return;
    if (sourceBtn) sourceBtn.disabled = true;
    try {
      await api.reabrir(numeroChamado);
      toast(`Chamado ${numeroChamado} reaberto.`, 'ok');
      await loadAdmin(true);            // re-agrupa a partir do servidor
      if (!modal.hidden) {
        await loadAndShow(numeroChamado, modalOrigin); // recarrega preservando a origem
      }
    } catch (err) {
      toast(err.message || 'Erro ao reabrir chamado.', 'err');
    } finally {
      if (sourceBtn) sourceBtn.disabled = false;
    }
  }

  /* ---------------------------------------------------------- helpers */
  function escapeHtml(v) {
    return String(v ?? '')
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#39;');
  }

  function animateNumber(el, target) {
    const start = parseInt(el.textContent.replace(/\D/g, ''), 10) || 0;
    const delta = target - start;
    if (delta === 0) { el.textContent = target; return; }
    const dur = 480;
    const t0 = performance.now();
    function step(t) {
      const k = Math.min(1, (t - t0) / dur);
      const eased = 1 - Math.pow(1 - k, 3);
      el.textContent = Math.round(start + delta * eased);
      if (k < 1) requestAnimationFrame(step);
    }
    requestAnimationFrame(step);
  }

  /* ---------------------------------------------------------- health */
  const apiStatus = $('#apiStatus');
  async function pingApi(known) {
    if (known === true) { apiStatus.textContent = 'online'; return; }
    if (known === false) { apiStatus.textContent = 'offline'; return; }
    try {
      const r = await fetch(API, { method: 'GET' });
      apiStatus.textContent = r.ok ? 'online' : 'degradado';
    } catch {
      apiStatus.textContent = 'offline';
    }
  }
  pingApi();

})();
