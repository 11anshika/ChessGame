const API = '/api/chess';
let gameId = null, gs = null, selected = null, validMoves = [], pendingPromo = null;
let drawOfferedBy = null;

const SYMS = {
  WHITE_KING: '♔', WHITE_QUEEN: '♕', WHITE_ROOK: '♖',
  WHITE_BISHOP: '♗', WHITE_KNIGHT: '♘', WHITE_PAWN: '♙',
  BLACK_KING: '♚', BLACK_QUEEN: '♛', BLACK_ROOK: '♜',
  BLACK_BISHOP: '♝', BLACK_KNIGHT: '♞', BLACK_PAWN: '♟',
};

// ── API helper ────────────────────────────────────────────────────────────────

async function req(method, path, body) {
  const opts = { method, headers: { 'Content-Type': 'application/json' } };
  if (body) opts.body = JSON.stringify(body);
  const r = await fetch(API + path, opts);
  return r.ok ? r.json() : null;
}

// ── Game actions ──────────────────────────────────────────────────────────────

async function newGame() {
  selected = null;
  validMoves = [];
  drawOfferedBy = null;
  document.getElementById('drawBanner').classList.remove('visible');
  gs = await req('POST', '/new');
  if (gs) { gameId = gs.gameId; renderAll(); }
}

async function clickSquare(row, col) {
  if (!gs) return;
  const terminal = ['CHECKMATE','STALEMATE','DRAW_AGREEMENT','DRAW_THREEFOLD',
                    'DRAW_FIFTY_MOVES','DRAW_INSUFFICIENT_MATERIAL','RESIGNED'];
  if (terminal.includes(gs.status)) return;

  // If a piece is already selected, check if this square is a valid destination
  if (selected) {
    const isValid = validMoves.some(m => m[0] === row && m[1] === col);
    if (isValid) {
      const fp = gs.board[selected[0]][selected[1]];
      // Art 3.7e – pawn promotion: must choose piece
      if (fp && fp.type === 'PAWN' && (row === 0 || row === 7)) {
        pendingPromo = { fr: selected[0], fc: selected[1], tr: row, tc: col };
        showPromo(fp.color);
        return;
      }
      await doMove(selected[0], selected[1], row, col, null);
      return;
    }
  }

  // Select a new piece belonging to the current player
  const p = gs.board[row][col];
  if (p && p.color === gs.currentTurn) {
    selected = [row, col];
    const data = await req('GET', `/${gameId}/moves/${row}/${col}`);
    if (data) { validMoves = data.validMovesForSelected || []; gs = data; renderBoard(); }
  } else {
    selected = null;
    validMoves = [];
    renderBoard();
  }
}

async function doMove(fr, fc, tr, tc, promo) {
  const body = { fromRow: fr, fromCol: fc, toRow: tr, toCol: tc };
  if (promo) body.promotion = promo;
  const data = await req('POST', `/${gameId}/move`, body);
  if (data) { gs = data; selected = null; validMoves = []; renderAll(); }
}

// Art 3.7e – show promotion modal
function showPromo(color) {
  const pieces = ['QUEEN', 'ROOK', 'BISHOP', 'KNIGHT'];
  const ws = { QUEEN: '♕', ROOK: '♖', BISHOP: '♗', KNIGHT: '♘' };
  const bs = { QUEEN: '♛', ROOK: '♜', BISHOP: '♝', KNIGHT: '♞' };
  const syms = color === 'WHITE' ? ws : bs;
  const row = document.getElementById('promoRow');
  row.innerHTML = '';
  pieces.forEach(p => {
    const btn = document.createElement('button');
    btn.className = 'promo-btn';
    btn.textContent = syms[p];
    btn.title = p;
    btn.onclick = async () => {
      document.getElementById('promoModal').classList.remove('visible');
      const pd = pendingPromo;
      pendingPromo = null;
      await doMove(pd.fr, pd.fc, pd.tr, pd.tc, p);
    };
    row.appendChild(btn);
  });
  document.getElementById('promoModal').classList.add('visible');
}

// Art 9.1 – offer draw
async function offerDraw(color) {
  if (!gameId) return;
  gs = await req('POST', `/${gameId}/draw/offer`, { color });
  if (gs) { drawOfferedBy = color; renderAll(); }
}

// Art 9.1 – accept draw
async function acceptDraw() {
  if (!gameId || !drawOfferedBy) return;
  const accepting = drawOfferedBy === 'WHITE' ? 'BLACK' : 'WHITE';
  gs = await req('POST', `/${gameId}/draw/accept`, { color: accepting });
  if (gs) {
    drawOfferedBy = null;
    document.getElementById('drawBanner').classList.remove('visible');
    renderAll();
  }
}

function hideDraw() {
  drawOfferedBy = null;
  document.getElementById('drawBanner').classList.remove('visible');
}

// Art 5.1b – resign
async function resign(color) {
  if (!gameId) return;
  gs = await req('POST', `/${gameId}/resign`, { color });
  if (gs) renderAll();
}

// ── Render ────────────────────────────────────────────────────────────────────

function renderAll() {
  renderBoard();
  renderSidebar();
  renderGameOver();
}

function renderBoard() {
  const boardEl = document.getElementById('board');
  boardEl.innerHTML = '';
  if (!gs) return;

  // Find king in check for highlight
  const checkColor = (gs.status === 'CHECK' || gs.status === 'CHECKMATE') ? gs.currentTurn : null;
  let kingCheckSq = null;
  if (checkColor) {
    for (let r = 0; r < 8; r++) for (let c = 0; c < 8; c++) {
      const p = gs.board[r][c];
      if (p && p.type === 'KING' && p.color === checkColor) kingCheckSq = [r, c];
    }
  }

  for (let r = 0; r < 8; r++) {
    for (let c = 0; c < 8; c++) {
      const sq = document.createElement('div');
      sq.className = `sq ${(r + c) % 2 === 0 ? 'light' : 'dark'}`;

      if (selected && selected[0] === r && selected[1] === c) sq.classList.add('selected');
      if (kingCheckSq && kingCheckSq[0] === r && kingCheckSq[1] === c) sq.classList.add('in-check');

      const isValid = validMoves.some(m => m[0] === r && m[1] === c);
      if (isValid) sq.classList.add(gs.board[r][c] ? 'valid-cap' : 'valid');

      const p = gs.board[r][c];
      if (p) {
        const pe = document.createElement('span');
        pe.className = 'piece';
        pe.style.color = p.color === 'WHITE' ? 'var(--white-piece)' : 'var(--black-piece)';
        if (p.color === 'BLACK') pe.style.webkitTextStroke = '0.5px rgba(255,255,255,.12)';
        pe.textContent = SYMS[p.color + '_' + p.type] || '?';
        sq.appendChild(pe);
      }

      sq.addEventListener('click', () => clickSquare(r, c));
      boardEl.appendChild(sq);
    }
  }

  // Coordinates
  const files = ['a','b','c','d','e','f','g','h'];
  const ranks = ['8','7','6','5','4','3','2','1'];
  document.getElementById('cFiles').innerHTML = files.map(f => `<span class="coord-label">${f}</span>`).join('');
  document.getElementById('cRanks').innerHTML = ranks.map(r => `<span class="coord-label">${r}</span>`).join('');
}

function renderSidebar() {
  if (!gs) return;
  const { status, currentTurn: turn } = gs;

  // Status text
  const st = document.getElementById('statusText');
  st.className = 'status-text';
  const statusClass = {
    CHECKMATE: 'checkmate', STALEMATE: 'stalemate', CHECK: 'check',
    DRAW_AGREEMENT: 'draw', DRAW_THREEFOLD: 'draw',
    DRAW_FIFTY_MOVES: 'draw', DRAW_INSUFFICIENT_MATERIAL: 'draw',
    RESIGNED: 'checkmate'
  };
  st.classList.add(statusClass[status] || 'active');

  const statusLabel = {
    ACTIVE: `${cap(turn)} to move`,
    CHECK: `${cap(turn)} is in Check! ⚠`,
    CHECKMATE: `Checkmate — ${cap(gs.winner)} wins`,
    STALEMATE: 'Stalemate — Draw',
    DRAW_AGREEMENT: 'Draw by agreement',
    DRAW_THREEFOLD: 'Draw — threefold repetition',
    DRAW_FIFTY_MOVES: 'Draw — fifty-move rule',
    DRAW_INSUFFICIENT_MATERIAL: 'Draw — insufficient material',
    RESIGNED: `${cap(gs.winner)} wins by resignation`
  };
  st.textContent = statusLabel[status] || `${cap(turn)} to move`;

  // Turn indicator
  document.getElementById('turnDot').className = `turn-dot ${turn === 'WHITE' ? 'white' : 'black'}`;
  document.getElementById('turnLabel').textContent = cap(turn);

  // Info counters
  document.getElementById('moveNum').textContent = gs.fullMoveNumber || 1;
  document.getElementById('halfClock').textContent = gs.halfMoveClock || 0;

  // Move history (SAN – Appendix C)
  const moves = gs.moveHistory || [];
  const list = document.getElementById('moveList');
  list.innerHTML = '';
  for (let i = 0; i < moves.length; i += 2) {
    const div = document.createElement('div');
    div.className = 'move-pair' + (i >= moves.length - 2 ? ' latest' : '');
    div.innerHTML = `
      <span class="move-num">${Math.floor(i / 2) + 1}.</span>
      <span class="move-w">${moves[i] || ''}</span>
      <span class="move-b">${moves[i + 1] || ''}</span>`;
    list.appendChild(div);
  }
  list.scrollTop = list.scrollHeight;

  // Draw offer banner (Art 9.1)
  const banner = document.getElementById('drawBanner');
  if (gs.drawOfferedByWhite || gs.drawOfferedByBlack) {
    const offerer = gs.drawOfferedByWhite ? 'White' : 'Black';
    document.getElementById('drawBannerText').textContent = `${offerer} offers a draw`;
    banner.classList.add('visible');
  } else {
    banner.classList.remove('visible');
  }
}

function renderGameOver() {
  if (!gs) return;
  const terminal = ['CHECKMATE','STALEMATE','DRAW_AGREEMENT','DRAW_THREEFOLD',
                    'DRAW_FIFTY_MOVES','DRAW_INSUFFICIENT_MATERIAL','RESIGNED'];
  const el = document.getElementById('gameOver');
  if (!terminal.includes(gs.status)) { el.classList.remove('visible'); return; }

  el.classList.add('visible');

  const titleMap = {
    CHECKMATE: `♔ ${cap(gs.winner)} Wins by Checkmate`,
    STALEMATE: '⚖ Draw — Stalemate',
    DRAW_AGREEMENT: '⚖ Draw by Agreement',
    DRAW_THREEFOLD: '⚖ Draw — Threefold Repetition',
    DRAW_FIFTY_MOVES: '⚖ Draw — Fifty-Move Rule',
    DRAW_INSUFFICIENT_MATERIAL: '⚖ Draw — Insufficient Material',
    RESIGNED: `♔ ${cap(gs.winner)} Wins — Opponent Resigned`
  };
  const subMap = {
    CHECKMATE: 'The king is under attack with no legal escape. Art 5.1',
    STALEMATE: 'No legal moves remain; king is not in check. Art 5.2a',
    DRAW_AGREEMENT: 'Both players agreed to a draw. Art 9.1',
    DRAW_THREEFOLD: 'The same position occurred three times. Art 9.2',
    DRAW_FIFTY_MOVES: '50 consecutive moves without a pawn move or capture. Art 9.3',
    DRAW_INSUFFICIENT_MATERIAL: 'Neither side can force checkmate. Art 5.2b / 9.6',
    RESIGNED: 'A player declared they resign. Art 5.1b'
  };

  document.getElementById('goTitle').textContent = titleMap[gs.status] || 'Game Over';
  document.getElementById('goSub').textContent   = subMap[gs.status]   || '';
}

// ── Utility ───────────────────────────────────────────────────────────────────

function cap(s) { return s ? s.charAt(0) + s.slice(1).toLowerCase() : ''; }

// ── Start ─────────────────────────────────────────────────────────────────────

newGame();
