/* ============================================================
   khajabad — interactive preview logic
   ============================================================ */

const $ = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

/* ------------------------------------------------------------ icons */

const I = {
  book: '<path d="M3 6.2c3.5-1.6 6.5-1.2 9 .6 2.5-1.8 5.5-2.2 9-.6v12.4c-3.5-1.6-6.5-1.2-9 .6-2.5-1.8-5.5-2.2-9-.6Z"/><path d="M12 6.8v12.4"/>',
  beads: '<circle cx="12" cy="3.8" r="1.4"/><circle cx="19.2" cy="7.5" r="1.4"/><circle cx="20.2" cy="12" r="1.4"/><circle cx="17.6" cy="17.9" r="1.4"/><circle cx="12" cy="20.2" r="1.4"/><circle cx="6.4" cy="17.9" r="1.4"/><circle cx="3.8" cy="12" r="1.4"/><circle cx="4.8" cy="7.5" r="1.4"/><circle cx="12" cy="12" r="1.5"/>',
  hands: '<path d="M4.2 20.6v-10c0-1.5 1.4-2.5 2.8-1.8l2.5 1.5v10.3"/><path d="M19.8 20.6v-10c0-1.5-1.4-2.5-2.8-1.8l-2.5 1.5v10.3"/><path d="M4.2 14.4h5.3"/><path d="M19.8 14.4h-5.3"/>',
  dome: '<path d="M12 2.6v2"/><path d="M4.6 20.4h14.8"/><path d="M6.4 20.4v-6.8c0-4.2 2.8-7 5.6-7s5.6 2.8 5.6 7v6.8"/><path d="M10 20.4v-3.8c0-1.4.9-2.4 2-2.4s2 1 2 2.4v3.8"/>',
  bait: '<path d="M11.2 3c1 5.2 3.8 8 9 9-5.2 1-8 3.8-9 9-1-5.2-3.8-8-9-9 5.2-1 8-3.8 9-9Z"/>',
  scroll: '<path d="M6.8 3.6h8.8l4.2 4.2v12.6H6.8Z"/><path d="M15.2 3.8v4.4h4.4"/><path d="M9.6 12.4h6.8M9.6 16h6.8"/>',
  mihrab: '<path d="M5.4 20.6V11.2C5.4 7.4 8.3 4.6 12 4.6s6.6 2.8 6.6 6.6v9.4"/><path d="M8.6 20.6v-9c0-2.3 1.5-3.9 3.4-3.9s3.4 1.6 3.4 3.9v9"/><path d="M3.4 20.6h17.2"/>',
  lantern: '<path d="M12 2v1.4"/><path d="M9 3.6h6l-.8 2.4H9.8Z"/><path d="M8 8.4h8l1.2 9.6H6.8Z"/><path d="M9.6 20.6h4.8"/><path d="M9.8 11.2v4M12 11.2v4M14.2 11.2v4"/>',
  dots: '<circle cx="5.6" cy="12" r="1.9" fill="currentColor" stroke="none"/><circle cx="12" cy="12" r="1.9" fill="currentColor" stroke="none"/><circle cx="18.4" cy="12" r="1.9" fill="currentColor" stroke="none"/>',
  search: '<circle cx="11" cy="11" r="6.6"/><path d="M15.9 15.9 20.6 20.6"/>',
  bookmark: '<path d="M6.6 3.8h10.8v16.6L12 16.2l-5.4 4.2Z"/>',
  chev: '<path d="M9.4 5.4 16 12l-6.6 6.6"/>',
  check: '<path d="M5 12.6 9.6 17.2 19.2 7.2"/>'
};

const svg = (inner, cls = '') => `<svg viewBox="0 0 24 24" class="${cls}">${inner}</svg>`;

/* ------------------------------------------------------------ data */

const BOOKS = [
  {
    id: 'shajarath', slug: 'shajarath',
    title: 'ശജരത്തുൽ മുബാറക്ക',
    latin: 'Shajarathul Mubarak',
    pages: 12,
    cover: 'pages/shajarath-01.webp',
    category: 'mawlid'
  },
  {
    id: 'swalath', slug: 'swalath',
    title: 'സ്വലാത്ത് ജൽസ',
    latin: 'Swalath Jalsah',
    pages: 10,
    cover: 'pages/swalath-01.webp',
    category: 'swalath'
  }
];

const CATEGORIES = [
  { label: 'ദിക്ർ', icon: 'beads', book: null },
  { label: 'ദുആ', icon: 'hands', book: null },
  { label: 'സ്വലാത്ത്', icon: 'dome', book: 'swalath' },
  { label: 'മൗലിദ്', icon: 'book', book: 'shajarath' },
  { label: 'ബൈത്തുകൾ', icon: 'bait', book: null },
  { label: 'മാലപ്പാട്ട്', icon: 'scroll', book: null },
  { label: 'രാത്തീബ്', icon: 'mihrab', book: 'shajarath' },
  { label: 'മജ്ലിസുന്നൂർ', icon: 'lantern', book: null },
  { label: 'മറ്റുള്ളവ', icon: 'dots', book: null }
];

const pageSrc = (book, n) => `pages/${book.slug}-${String(n + 1).padStart(2, '0')}.webp`;
const bookById = id => BOOKS.find(b => b.id === id) || BOOKS[0];

/* ------------------------------------------------------------ state */

const saved = {
  int: (k, d) => Number(localStorage.getItem(k) ?? d) || 0,
  set: (k, v) => localStorage.setItem(k, v),
  json: (k, d) => { try { return JSON.parse(localStorage.getItem(k) ?? d); } catch (e) { return JSON.parse(d); } }
};

/* ------------------------------------------------------------ events feed
   Live data comes from the website's Notte function:
     POST https://us-prod.notte.cc/functions/<id>/runs/start
   The preview runs offline, so it renders a sample payload of the same shape
   the app's tolerant parser expects (title/start/location/description...).
   Set window.KB_LIVE = true and serve through a proxy to use the live feed.
*/
const FEED_ENDPOINT = 'https://us-prod.notte.cc/functions/635cfdc6-667e-453c-bc57-bce54f35724b/runs/start';

const SAMPLE_EVENTS = [
  {
    id: 'e1',
    title: 'മാസാനത്ത് സ്വലാത്ത് ജൽസ',
    start: Date.now() + 1000 * 60 * 60 * 27,
    location: 'മുഹിയുദ്ദീൻ ജുമാ മസ്ജിദ്, കോഴിക്കോട്',
    description: 'വാർഷിക സ്വലാത്ത് ജൽസ. മഗ്‌രിബ് നമസ്കാരത്തിന് ശേഷം ആരംഭിക്കും. എല്ലാവർക്കും സ്വാഗതം.',
    highlighted: true,
  },
  {
    id: 'e2',
    title: 'ശജരത്തുൽ മുബാറക്ക ദിക്ർ മജ്ലിസ്',
    start: Date.now() + 1000 * 60 * 60 * 24 * 6,
    location: 'മലപ്പുറം ടൗൺ ജുമാ മസ്ജിദ്',
    description: 'രാത്രി 8 മണിക്ക് ആരംഭം. മൗലിദ്, ദുആ, ആശീർവാദം.',
  },
  {
    id: 'e3',
    title: 'മജ്ലിസുന്നൂർ യൂത്ത് സംഗമം',
    start: Date.now() + 1000 * 60 * 60 * 24 * 13,
    location: 'സ്വലാത്ത് നഗർ, തിരൂരങ്ങാടി',
    description: 'യുവജന സംഗമവും പുരസ്കാര സമർപ്പണവും.',
  },
  {
    id: 'e4',
    title: 'വാർഷിക മാലപ്പാട്ട് രാത്രി',
    start: Date.now() + 1000 * 60 * 60 * 24 * 26,
    location: 'വലിയ ജുമാ മസ്ജിദ്, കണ്ണൂർ',
    description: 'മാലപ്പാട്ടും ബൈത്തുകളും അണിയിച്ചൊരുക്കി.',
  },
];

let EVENTS = SAMPLE_EVENTS.slice();
let EVENTS_FETCHED_AT = Date.now();
let EVENTS_LIVE = false;

function sortEvents(list) {
  return list.slice().sort((a, b) => (a.start || Infinity) - (b.start || Infinity));
}

function nextEvent() {
  const now = Date.now();
  return sortEvents(EVENTS).find(e => !e.start || e.start >= now) || EVENTS[0] || null;
}

const MONTHS = ['JAN', 'FEB', 'MAR', 'APR', 'MAY', 'JUN', 'JUL', 'AUG', 'SEP', 'OCT', 'NOV', 'DEC'];
const MONTH_LONG = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];

function dateBits(start) {
  if (!start) return { day: '--', month: '' };
  const d = new Date(start);
  return { day: String(d.getDate()).padStart(2, '0'), month: MONTHS[d.getMonth()] };
}

function timeLabel(start) {
  if (!start) return 'തീയതി ഉടൻ';
  return new Date(start).toLocaleTimeString('en-US', { hour: 'numeric', minute: '2-digit' });
}

function readable(start) {
  if (!start) return 'തീയതി പ്രഖ്യാപിച്ചിട്ടില്ല';
  const d = new Date(start);
  return d.toLocaleDateString('en-GB', { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' }) +
    ' · ' + timeLabel(start);
}

function sameDay(a, b) {
  const x = new Date(a), y = new Date(b);
  return x.getFullYear() === y.getFullYear() && x.getMonth() === y.getMonth() && x.getDate() === y.getDate();
}

function relativeTime(ts) {
  const mins = Math.max(0, Math.round((Date.now() - ts) / 60000));
  if (mins < 1) return 'ഇപ്പോൾ';
  if (mins < 60) return mins + ' മിനിറ്റ് മുൻപ്';
  if (mins < 1440) return Math.round(mins / 60) + ' മണിക്കൂർ മുൻപ്';
  return Math.round(mins / 1440) + ' ദിവസം മുൻപ്';
}

function countdown(start) {
  if (!start) return 'തീയതി ഉടൻ';
  const diff = start - Date.now();
  if (diff <= 0) return 'ഇപ്പോൾ നടക്കുന്നു';
  const days = Math.floor(diff / 86400000);
  const hours = Math.floor(diff / 3600000) % 24;
  const mins = Math.floor(diff / 60000) % 60;
  const secs = Math.floor(diff / 1000) % 60;
  if (days >= 2) return 'ഇനി ' + days + ' ദിവസം';
  if (days >= 1) return 'ഇനി 1 ദിവസം ' + hours + ' മണിക്കൂർ';
  if (hours >= 1) return 'ഇനി ' + hours + 'h ' + mins + 'm';
  return 'ഇനി ' + mins + 'm ' + secs + 's';
}

const state = {
  screen: 'splash',
  lastTab: 'home',
  query: '',
  book: 'shajarath',
  page: 2,
  counter: saved.int('kb_counter', 0),
  target: saved.int('kb_target', 33),
  bookmarks: new Set(saved.json('kb_bookmarks', '[]')),
  progress: saved.json('kb_progress', '{"shajarath":2,"swalath":0}'),
  lastBook: localStorage.getItem('kb_last') || 'shajarath',
  zoom: false,
  night: true,
  chrome: true
};

const saveBookmarks = () => saved.set('kb_bookmarks', JSON.stringify([...state.bookmarks]));
const bmKey = (book, page) => `${book}:${page}`;

/* ------------------------------------------------------------ toast */

let toastTimer;
function toast(text) {
  const el = $('#toast');
  el.innerHTML = svg(I.check) + `<span>${text}</span>`;
  el.classList.add('on');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => el.classList.remove('on'), 2300);
}

/* ------------------------------------------------------------ theme */

function applyTheme(mode) {
  document.documentElement.classList.toggle('dark', mode === 'dark');
  saved.set('kb_theme', mode);
  const sw = $('#drawerSwitch');
  if (sw) sw.classList.toggle('on', mode === 'dark');
  $('#themeBtn').querySelector('span').textContent = mode === 'dark' ? 'Light' : 'Night';
  updateStatusbarColor();
}
function toggleTheme() {
  applyTheme(document.documentElement.classList.contains('dark') ? 'light' : 'dark');
}
function updateStatusbarColor() {
  const darkScreen = state.screen === 'reader' && state.night;
  $('#viewport').classList.toggle('dark-status', darkScreen);
}

/* ------------------------------------------------------------ splash */

function buildSplash() {
  const words = 'khajabad'.split('');
  $('#wordmark').innerHTML = words
    .map((c, i) => `<span style="animation-delay:${0.28 + i * 0.048}s">${c}</span>`)
    .join('');
  const embers = $('#embers');
  embers.innerHTML = '';
  for (let i = 0; i < 16; i++) {
    const dot = document.createElement('i');
    dot.style.left = `${6 + Math.random() * 88}%`;
    dot.style.bottom = `${-6 + Math.random() * 40}%`;
    dot.style.animationDelay = `${-Math.random() * 9}s`;
    dot.style.animationDuration = `${7 + Math.random() * 5}s`;
    embers.appendChild(dot);
  }
}

function playSplash() {
  if (playSplash.count === undefined) playSplash.count = 0;
  if (playSplash.count++ > 0) {
    const inner = $('.splash-inner');
    const clone = inner.cloneNode(true);
    inner.replaceWith(clone);
    buildSplash();
  }
  const screen = $('#screen-splash');
  screen.classList.remove('on');
  const mark = $('.mark');
  mark.classList.remove('sheen');
  void screen.offsetWidth;
  screen.classList.add('on');
  mark.classList.add('sheen');
  const splashEl = $('.splash');
  splashEl.classList.remove('run');
  void splashEl.offsetWidth;
  splashEl.classList.add('run');
  clearTimeout(playSplash.timer);
  playSplash.timer = setTimeout(() => {
    if (state.screen === 'splash') showScreen('home');
  }, 3000);
}

/* ------------------------------------------------------------ navigation */

const SCREEN_TABS = { home: 0, library: 1, counter: 2, search: 3 };
const NO_TABBAR = new Set(['splash', 'reader', 'bookmarks', 'events']);
const NO_STATUSBAR_HIDE = new Set(['splash']);

function showScreen(name) {
  state.screen = name;
  $$('.screen').forEach(el => el.classList.toggle('on', el.id === `screen-${name}`));
  $$('#screenNav button').forEach(b => b.classList.toggle('on', b.dataset.screen === name));

  const tabbar = $('#tabbar');
  tabbar.classList.toggle('hidden', NO_TABBAR.has(name));
  if (SCREEN_TABS[name] !== undefined) {
    $$('.tab').forEach((t, i) => t.classList.toggle('on', i === SCREEN_TABS[name]));
    $('#tabIndicator').style.transform = `translateX(${SCREEN_TABS[name] * 100}%)`;
  }
  $$('.drawer-item').forEach(item => {
    const go = item.dataset.go;
    item.classList.toggle('on', go === name || (name === 'bookmarks' && go === 'bookmarks'));
  });

  if (name === 'home') renderHome();
  if (name === 'library') renderLibrary();
  if (name === 'counter') renderCounter();
  if (name === 'search') { renderSearch(); setTimeout(() => $('#searchInput').focus(), 220); }
  if (name === 'bookmarks') renderBookmarks();
  if (name === 'events') renderEvents();
  if (name === 'reader') { updateReader(); }
  if (name === 'splash') playSplash();

  updateStatusbarColor();
}

function openDrawer(open) {
  $('#drawer').classList.toggle('on', open);
  $('#drawerScrim').classList.toggle('on', open);
}

/* ------------------------------------------------------------ home */

function renderHome() {
  const recentIds = [state.lastBook, ...BOOKS.map(b => b.id).filter(id => id !== state.lastBook)];
  const recent = recentIds.map(bookById);
  const chips = $('#recentChips');
  chips.innerHTML = recent.map(b => `<button class="chip" data-book="${b.id}">${b.title}</button>`).join('')
    + `<button class="result-ico" style="background:transparent;border:1.2px solid var(--line);color:var(--ink-soft);align-self:center;width:48px;height:48px;border-radius:50%" data-go="library">${svg(I.chev)}</button>`;

  const featured = bookById(state.lastBook);
  const page = state.progress[featured.id] ?? 0;
  $('#featuredTitle').textContent = featured.title;
  $('#featuredMeta').textContent = page > 0
    ? `വായന തുടരുക · പേജ് ${page + 1} / ${featured.pages}`
    : `${featured.pages} പേജ് · ${featured.latin}`;
  $('#featuredCard').dataset.book = featured.id;
  $('#featuredProgress').style.width = `${Math.max(2, ((page + 1) / featured.pages) * 100)}%`;

  const grid = $('#categoryGrid');
  grid.innerHTML = CATEGORIES.map((c, i) => `
    <button class="tile" data-cat="${i}" style="animation-delay:${0.06 * i + 0.1}s">
      <span class="tile-btn">${svg(I[c.icon])}</span>
      <span class="tile-label">${c.label}</span>
    </button>`).join('');
}

function bindHomeParallax() {
  const scroller = $('#homeScroll');
  const hero = $('.hero');
  scroller.addEventListener('scroll', () => {
    const y = Math.min(scroller.scrollTop, 420);
    hero.style.transform = `translateY(${y * 0.28}px) scale(${1 - (y / 420) * 0.06})`;
    hero.style.opacity = String(Math.max(0.15, 1 - y / 520));
  }, { passive: true });
}

/* ------------------------------------------------------------ library */

function renderLibrary() {
  const list = $('#libList');
  list.innerHTML = BOOKS.map(b => {
    const page = state.progress[b.id] ?? 0;
    const pct = Math.round(((page + 1) / b.pages) * 100);
    return `<button class="book-row" data-book="${b.id}">
      <span class="cover"><img src="${b.cover}" alt="${b.latin} cover" loading="lazy"></span>
      <span class="book-body">
        <span class="book-title mal">${b.title}</span>
        <span class="book-latin">${b.latin}</span>
        <span class="bar"><i style="width:${Math.max(3, pct)}%"></i></span>
        <span class="book-meta">${b.pages} പേജ് · ${pct}%</span>
      </span>
      <span class="row-go">${svg(I.chev)}</span>
    </button>`;
  }).join('');

  const count = state.bookmarks.size;
  $('#savedCount').textContent = count === 0
    ? 'ഇനിയും ഒന്നും സേവ് ചെയ്തിട്ടില്ല'
    : `${count} പേജ് ബുക്ക്മാർക്ക് ചെയ്തിരിക്കുന്നു`;
}

/* ------------------------------------------------------------ reader */

function openReader(bookId, page) {
  const book = bookById(bookId);
  state.book = book.id;
  state.lastBook = book.id;
  saved.set('kb_last', book.id);
  state.page = page ?? (state.progress[book.id] ?? 0);
  state.page = Math.max(0, Math.min(book.pages - 1, state.page));
  state.zoom = false;
  state.chrome = true;
  $('#pageCard').classList.remove('zoomed');
  $('#screen-reader').classList.remove('chrome-off');
  showScreen('reader');
  updateReader(true);
}

function updateReader(rebuildSheet) {
  const book = bookById(state.book);
  const page = state.page;
  $('#readerBookTitle').textContent = book.title;
  $('#readerPageLabel').textContent = `പേജ് ${page + 1} / ${book.pages}`;
  $('#readerFoot').textContent = `${book.latin.toUpperCase()} · പേജ് ${page + 1} / ${book.pages}`;
  const pct = ((page + 1) / book.pages) * 100;
  $('#readerProgress').style.width = `${pct}%`;
  $('#scrubFill').style.width = `${((page) / (book.pages - 1)) * 100}%`;
  $('#scrubThumb').style.left = `calc(${((page) / (book.pages - 1)) * 100}% - 7px)`;
  $('#pageImg').src = pageSrc(book, page);
  $('#pageCard').style.aspectRatio = book.slug === 'shajarath' ? '298 / 417' : '760 / 909';

  const marked = state.bookmarks.has(bmKey(book.id, page));
  $('#bookmarkBtn').classList.toggle('on', marked);

  state.progress[book.id] = page;
  saved.set('kb_progress', JSON.stringify(state.progress));

  if (rebuildSheet) {
    $('#sheetCount').textContent = book.pages;
    $('#thumbs').innerHTML = Array.from({ length: book.pages }).map((_, i) => `
      <button class="thumb ${i === page ? 'on' : ''}" data-page="${i}">
        <span class="thumb-img"><img src="${pageSrc(book, i)}" alt="page ${i + 1}" loading="lazy"></span>
        <span>${i + 1}</span>
      </button>`).join('');
  } else {
    $$('#thumbs .thumb').forEach((t, i) => t.classList.toggle('on', i === page));
  }
}

function turnPage(direction) {
  const book = bookById(state.book);
  const next = state.page + direction;
  if (next < 0 || next > book.pages - 1) return;
  const card = $('#pageCard');
  card.style.transition = '';
  card.classList.add(direction > 0 ? 'swipe-left' : 'swipe-right');
  card.classList.add('turning');
  vib(8);
  setTimeout(() => {
    state.page = next;
    card.classList.remove('swipe-left', 'swipe-right');
    card.classList.add('enter', direction > 0 ? 'enter-next' : 'enter-prev');
    updateReader();
    requestAnimationFrame(() => {
      requestAnimationFrame(() => {
        card.classList.remove('enter', 'enter-next', 'enter-prev');
      });
    });
    setTimeout(() => card.classList.remove('turning'), 400);
  }, 160);
}

function bindReaderGestures() {
  const card = $('#pageCard');
  const stage = $('#pageStage');
  let drag = null;
  let lastTap = 0;
  let tapTimer = null;

  card.addEventListener('pointerdown', e => {
    if (state.zoom) return;
    drag = { x: e.clientX, y: e.clientY, dx: 0 };
    card.style.transition = 'none';
    try { card.setPointerCapture(e.pointerId); } catch (err) { /* noop */ }
  });

  card.addEventListener('pointermove', e => {
    if (!drag || state.zoom) return;
    drag.dx = e.clientX - drag.x;
    if (Math.abs(drag.dx) > 6) {
      card.classList.add('turning');
      card.style.transform = `perspective(1600px) rotateY(${drag.dx * 0.045}deg) translateX(${drag.dx * 0.14}px) scale(.985)`;
    }
  });

  const endDrag = () => {
    if (!drag || state.zoom) { drag = null; return; }
    const dx = drag.dx;
    drag = null;
    card.style.transition = '';
    card.style.transform = '';
    card.classList.remove('turning');
    if (dx < -56) turnPage(1);
    else if (dx > 56) turnPage(-1);
  };
  card.addEventListener('pointerup', endDrag);
  card.addEventListener('pointercancel', endDrag);
  card.addEventListener('pointerleave', endDrag);

  card.addEventListener('dblclick', e => {
    e.preventDefault();
    state.zoom = !state.zoom;
    card.classList.toggle('zoomed', state.zoom);
    vib(10);
  });

  stage.addEventListener('click', () => {
    const now = Date.now();
    if (now - lastTap < 300) {
      clearTimeout(tapTimer);
      lastTap = 0;
      return;
    }
    lastTap = now;
    clearTimeout(tapTimer);
    tapTimer = setTimeout(() => {
      state.chrome = !state.chrome;
      $('#screen-reader').classList.toggle('chrome-off', !state.chrome);
    }, 280);
  });

  // scrubber
  const scrub = $('#scrub');
  const seek = clientX => {
    const rect = scrub.getBoundingClientRect();
    const f = Math.max(0, Math.min(1, (clientX - rect.left) / rect.width));
    const book = bookById(state.book);
    state.page = Math.round(f * (book.pages - 1));
    updateReader();
  };
  let scrubbing = false;
  scrub.addEventListener('pointerdown', e => { scrubbing = true; seek(e.clientX); scrub.setPointerCapture(e.pointerId); });
  scrub.addEventListener('pointermove', e => { if (scrubbing) seek(e.clientX); });
  scrub.addEventListener('pointerup', () => { scrubbing = false; });

  $('#prevBtn').onclick = () => turnPage(-1);
  $('#nextBtn').onclick = () => turnPage(1);
  $('#readerBack').onclick = () => showScreen(state.lastTab || 'home');

  $('#bookmarkBtn').onclick = () => {
    const key = bmKey(state.book, state.page);
    if (state.bookmarks.has(key)) {
      state.bookmarks.delete(key);
      toast('ബുക്ക്മാർക്ക് നീക്കി');
    } else {
      state.bookmarks.add(key);
      toast('ബുക്ക്മാർക്ക് ചെയ്തു');
    }
    saveBookmarks();
    updateReader();
    vib(12);
  };

  $('#nightBtn').onclick = () => {
    state.night = !state.night;
    $('#screen-reader').classList.toggle('day', !state.night);
    updateStatusbarColor();
  };

  $('#thumbsBtn').onclick = () => {
    $('#sheet').classList.add('on');
    $('#sheetScrim').classList.add('on');
  };
  $('#sheetScrim').onclick = () => {
    $('#sheet').classList.remove('on');
    $('#sheetScrim').classList.remove('on');
  };
  $('#thumbs').addEventListener('click', e => {
    const btn = e.target.closest('.thumb');
    if (!btn) return;
    state.page = Number(btn.dataset.page);
    updateReader();
    $('#sheet').classList.remove('on');
    $('#sheetScrim').classList.remove('on');
  });
}

function vib(ms) { if (navigator.vibrate) navigator.vibrate(ms); }

/* ------------------------------------------------------------ counter */

const RING_LEN = 2 * Math.PI * 132;

function renderCounter() {
  const target = state.target;
  const inCycle = target > 0 ? state.counter % target : state.counter;
  const cycles = target > 0 ? Math.floor(state.counter / target) : 0;
  $('#countNum').textContent = state.counter;
  $('#countSub').textContent = target > 0 ? `${inCycle} / ${target}` : '∞';
  $('#cycleLabel').textContent = `സൈക്കിൾ ${cycles + 1} · ${target > 0 ? target : '∞'} എണ്ണം`;
  const fraction = target > 0 ? inCycle / target : 0;
  $('#ringFill').style.strokeDashoffset = String(RING_LEN * (1 - fraction));
  $$('#presets button').forEach(b => b.classList.toggle('on', Number(b.dataset.target) === target));
}

function bindCounter() {
  $('#tapSurface').addEventListener('click', () => {
    state.counter += 1;
    saved.set('kb_counter', state.counter);
    renderCounter();
    const ripple = document.createElement('i');
    $('#ripples').appendChild(ripple);
    setTimeout(() => ripple.remove(), 780);
    vib(14);
  });

  $('#undoBtn').onclick = () => {
    if (state.counter === 0) return;
    state.counter -= 1;
    saved.set('kb_counter', state.counter);
    renderCounter();
  };

  const reset = () => {
    state.counter = 0;
    saved.set('kb_counter', 0);
    renderCounter();
    toast('കൗണ്ടർ പുനഃസജ്ജമാക്കി');
  };
  $('#resetBtn').onclick = reset;
  $('#resetBtn2').onclick = reset;

  $('#presets').addEventListener('click', e => {
    const btn = e.target.closest('button');
    if (!btn) return;
    state.target = Number(btn.dataset.target);
    state.counter = 0;
    saved.set('kb_target', state.target);
    saved.set('kb_counter', 0);
    renderCounter();
  });
}

/* ------------------------------------------------------------ search */

function renderSearch() {
  const q = state.query.trim();
  const body = $('#searchBody');
  const clear = $('#clearSearch');
  clear.classList.toggle('show', q.length > 0);

  if (!q) {
    const pairs = [...state.bookmarks].map(k => {
      const [id, p] = k.split(':');
      return { book: bookById(id), page: Number(p) };
    });
    body.innerHTML = `
      <div class="kicker-row">SUGGESTIONS</div>
      <div class="suggest-row">
        ${CATEGORIES.slice(0, 6).map(c => `<button class="suggest" data-suggest="${c.label}">${c.label}</button>`).join('')}
      </div>
      <div class="kicker-row">ALL BOOKS</div>
      ${BOOKS.map(b => resultRow(I.book, b.title, `${b.pages} പേജ് · ${b.latin}`, `data-book="${b.id}"`)).join('')}
      ${pairs.length ? `<div class="kicker-row">SAVED PAGES</div>` + pairs.map(p =>
        resultRow(I.bookmark, `${p.book.title} · പേജ് ${p.page + 1}`, p.book.latin, `data-book="${p.book.id}" data-page="${p.page}"`)
      ).join('') : ''}
    `;
    return;
  }

  const matched = BOOKS.filter(b => b.title.includes(q) || b.latin.toLowerCase().includes(q.toLowerCase()));
  const numeric = /^\d+$/.test(q) ? Number(q) : null;
  const savedMatches = [...state.bookmarks].map(k => {
    const [id, p] = k.split(':');
    return { book: bookById(id), page: Number(p) };
  }).filter(p => p.book.title.includes(q));

  let html = '';
  if (matched.length) {
    html += `<div class="kicker-row">${matched.length} പുസ്തകം</div>`;
    html += matched.map(b => resultRow(I.book, b.title, `${b.pages} പേജ് · ${b.latin}`, `data-book="${b.id}"`)).join('');
  }
  if (numeric !== null) {
    html += `<div class="kicker-row">പേജിലേക്ക് പോകുക</div>`;
    html += BOOKS.map(b => {
      const page = Math.max(0, Math.min(b.pages - 1, numeric - 1));
      return resultRow(I.chev, `${b.title} · പേജ് ${page + 1}`, 'ഈ പേജിലേക്ക് തുറക്കുക', `data-book="${b.id}" data-page="${page}"`);
    }).join('');
  }
  if (savedMatches.length) {
    html += `<div class="kicker-row">SAVED</div>`;
    html += savedMatches.map(p => resultRow(I.bookmark, `${p.book.title} · പേജ് ${p.page + 1}`, p.book.latin, `data-book="${p.book.id}" data-page="${p.page}"`)).join('');
  }
  if (!html) {
    html = `<div class="empty-state">
      ${svg(I.search)}
      <b>“${q}” കണ്ടെത്തിയില്ല</b>
      <p>മറ്റൊരു വാക്ക് ശ്രമിക്കുക അല്ലെങ്കിൽ സേവ് ചെയ്ത പേജുകൾ നോക്കുക</p>
      <button class="chip" data-go="bookmarks">ബുക്ക്മാർക്കുകൾ</button>
    </div>`;
  }
  body.innerHTML = html;
}

function resultRow(icon, title, sub, attrs) {
  return `<button class="result" ${attrs}>
    <span class="result-ico">${svg(icon)}</span>
    <span class="result-body">
      <span class="result-title">${title}</span>
      <span class="result-sub">${sub}</span>
    </span>
    ${svg(I.chev)}
  </button>`;
}

function bindSearch() {
  const input = $('#searchInput');
  input.addEventListener('input', () => {
    state.query = input.value;
    $('#searchField').classList.toggle('focus', input.value.length > 0);
    renderSearch();
  });
  input.addEventListener('focus', () => $('#searchField').classList.add('focus'));
  input.addEventListener('blur', () => { if (!input.value) $('#searchField').classList.remove('focus'); });
  $('#clearSearch').onclick = () => {
    input.value = '';
    state.query = '';
    renderSearch();
    input.focus();
  };
  $('#searchBody').addEventListener('click', e => {
    const sug = e.target.closest('[data-suggest]');
    if (sug) {
      input.value = sug.dataset.suggest;
      state.query = sug.dataset.suggest;
      renderSearch();
    }
  });
}

/* ------------------------------------------------------------ bookmarks */

function renderBookmarks() {
  const pairs = [...state.bookmarks].map(k => {
    const [id, p] = k.split(':');
    return { book: bookById(id), page: Number(p) };
  }).sort((a, b) => a.book.id.localeCompare(b.book.id) || a.page - b.page);

  $('#bmCount').textContent = `${pairs.length} ബുക്ക്മാർക്കുകൾ`;
  const list = $('#bookmarkList');
  if (!pairs.length) {
    list.innerHTML = `<div class="empty-state">
      ${svg(I.bookmark)}
      <b>സേവ് ചെയ്ത പേജുകൾ ഇല്ല</b>
      <p>വായിക്കുമ്പോൾ മുകളിലെ ബുക്ക്മാർക്ക് ഐക്കൺ ടാപ്പ് ചെയ്യുക</p>
    </div>`;
    return;
  }
  list.innerHTML = pairs.map(p => `
    <div class="bm-row">
      <button class="bm-cover" data-book="${p.book.id}" data-page="${p.page}">
        <img src="${pageSrc(p.book, p.page)}" alt="" loading="lazy">
      </button>
      <button class="bm-body" data-book="${p.book.id}" data-page="${p.page}" style="text-align:left;background:none">
        <b>${p.book.title}</b>
        <em>പേജ് ${p.page + 1} / ${p.book.pages}</em>
      </button>
      <button class="bm-remove" data-remove="${p.book.id}:${p.page}" aria-label="Remove">
        ${svg(I.bookmark)}
      </button>
    </div>`).join('');
}

/* ------------------------------------------------------------ events UI */

function renderEventHero() {
  const host = document.getElementById('eventHero');
  const ev = nextEvent();
  if (!host) return;
  if (!ev) {
    host.innerHTML = '<div class="eh-top"><span class="eh-live">വരാനിരിക്കുന്ന പരിപാടികൾ ഒന്നുമില്ല</span></div>';
    return;
  }
  const { day, month } = dateBits(ev.start);
  const meta = [timeLabel(ev.start), ev.location].filter(Boolean).join(' · ');
  host.innerHTML = `
    <div class="eh-top">
      <span class="eh-live"><i class="eh-dot"></i>${EVENTS_LIVE ? 'NEXT EVENT' : 'DEMO FEED'}</span>
      ${ev.highlighted ? `<span class="eh-flag">${svg('<path d="M12 2.6c.9 5.5 3.9 8.5 9.4 9.4-5.5.9-8.5 3.9-9.4 9.4-.9-5.5-3.9-8.5-9.4-9.4 5.5-.9 8.5-3.9 9.4-9.4Z"/>')}FEATURED</span>` : ''}
    </div>
    <div class="eh-main">
      <span class="date-block"><b>${day}</b><span>${month}</span></span>
      <span>
        <span class="eh-title">${ev.title}</span>
        <span class="eh-meta">${meta}</span>
      </span>
    </div>
    <div class="eh-foot">
      <span class="eh-count">${svg('<circle cx="12" cy="12" r="8.4"/><path d="M12 11v5.4"/><path d="M12 7.9v.2"/>')}<span id="heroCountdown">${countdown(ev.start)}</span></span>
      <span class="eh-cta">കാണുക ${svg('<path d="M9.4 5.4 16 12l-6.6 6.6"/>')}</span>
    </div>`;
}

function renderEvents() {
  const list = document.getElementById('eventsList');
  const sub = document.getElementById('eventsSub');
  if (!list) return;
  const sorted = sortEvents(EVENTS);
  sub.textContent = `${sorted.length} പരിപാടികൾ · ${relativeTime(EVENTS_FETCHED_AT)} അപ്ഡേറ്റ്`;
  const note = document.getElementById('feedNote');
  if (note) {
    note.querySelector('span').textContent = EVENTS_LIVE
      ? 'ലൈവ് ഫീഡ് · Notte ഫങ്ക്ഷൻ വഴി'
      : 'ഡെമോ ഡാറ്റ · ആപ്പിൽ Notte ഫങ്ക്ഷനിൽ നിന്ന് ലൈവ് ആയി വരും';
  }

  let html = '';
  let lastMonth = '';
  sorted.forEach(ev => {
    const d = ev.start ? new Date(ev.start) : null;
    const monthKey = d ? `${MONTH_LONG[d.getMonth()]} ${d.getFullYear()}` : 'തീയതി പ്രഖ്യാപിക്കാത്തത്';
    if (monthKey !== lastMonth) {
      html += `<div class="month-label">${monthKey.toUpperCase()}</div>`;
      lastMonth = monthKey;
    }
    const { day, month } = dateBits(ev.start);
    const today = ev.start && sameDay(ev.start, Date.now());
    const meta = [timeLabel(ev.start), ev.location].filter(Boolean).join(' · ');
    html += `
      <button class="ev-row ${today ? 'today' : ''}" data-event="${ev.id}">
        <span class="date-block ${today ? '' : 'soft'}"><b>${day}</b><span>${month}</span></span>
        <span class="ev-body">
          <span class="ev-title">${ev.title}</span>
          <span class="ev-meta">${today ? '<span class="ev-today">ഇന്ന്</span>' : ''}${meta}</span>
        </span>
        ${svg(I.chev)}
      </button>`;
  });
  list.innerHTML = html;

  const drawerSub = document.getElementById('drawerEventSub');
  if (drawerSub) drawerSub.textContent = sorted.length ? `${sorted.length} പരിപാടികൾ` : 'അടുത്ത പരിപാടികൾ';
}

function openEventSheet(id) {
  const ev = EVENTS.find(e => e.id === id);
  if (!ev) return;
  const { day, month } = dateBits(ev.start);
  document.getElementById('eventSheetBody').innerHTML = `
    <div class="es-head">
      <span class="date-block"><b>${day}</b><span>${month}</span></span>
      <span>
        <span class="es-title">${ev.title}</span>
        <span class="es-when">${readable(ev.start)}</span>
      </span>
      <button class="es-close" id="esClose">${svg('<path d="M5.6 5.6 18.4 18.4"/><path d="M18.4 5.6 5.6 18.4"/>')}</button>
    </div>
    ${ev.location ? `<div class="es-info"><span class="ico">${svg('<path d="M5.4 20.6V11.2C5.4 7.4 8.3 4.6 12 4.6s6.6 2.8 6.6 6.6v9.4"/><path d="M8.6 20.6v-9c0-2.3 1.5-3.9 3.4-3.9s3.4 1.6 3.4 3.9v9"/><path d="M3.4 20.6h17.2"/>')}</span><span><b>സ്ഥലം</b><span>${ev.location}</span></span></div>` : ''}
    ${ev.description ? `<p class="es-desc">${ev.description}</p>` : ''}
    <div class="es-actions">
      <button class="es-btn filled" id="esCalendar">${svg('<path d="M12 5.4v13.2"/><path d="M5.4 12h13.2"/>')}കലണ്ടറിൽ ചേർക്കുക</button>
      <button class="es-btn ghost" id="esShare">${svg('<path d="M12 15.4V3.8"/><path d="M8.2 7.4 12 3.6l3.8 3.8"/><path d="M5.6 12.4v19.6h12.8V12.4"/>')}പങ്കിടുക</button>
    </div>`;
  document.getElementById('eventSheet').classList.add('on');
  document.getElementById('eventScrim').classList.add('on');

  document.getElementById('esClose').onclick = closeEventSheet;
  document.getElementById('esCalendar').onclick = () => toast('APK-യിൽ കലണ്ടർ തുറക്കും');
  document.getElementById('esShare').onclick = () => toast('പങ്കിട്ടു · ' + ev.title);
  document.getElementById('esCalendar').addEventListener('click', closeEventSheet);
  document.getElementById('esShare').addEventListener('click', closeEventSheet);
}

function closeEventSheet() {
  document.getElementById('eventSheet').classList.remove('on');
  document.getElementById('eventScrim').classList.remove('on');
}

/** Optional live fetch — only works when served behind a proxy that adds auth. */
async function tryLiveFeed() {
  if (!window.KB_LIVE) return;
  try {
    const res = await fetch('/api/events', { method: 'POST' });
    if (!res.ok) throw new Error('HTTP ' + res.status);
    const data = await res.json();
    const list = (Array.isArray(data) ? data : data.events || data.data || [])
      .map((e, i) => ({
        id: e.id || 'live' + i,
        title: e.title || e.name || 'Event',
        start: e.start ? new Date(e.start).getTime() : null,
        location: e.location || e.venue || '',
        description: e.description || '',
        highlighted: !!(e.highlighted || e.featured),
      }));
    if (list.length) {
      EVENTS = list;
      EVENTS_FETCHED_AT = Date.now();
      EVENTS_LIVE = true;
      renderEvents();
      renderEventHero();
    }
  } catch (e) {
    console.warn('live feed unavailable, keeping sample data', e);
  }
}

/* ------------------------------------------------------------ global bindings */

function bindGlobal() {
  // top nav
  $$('#screenNav button').forEach(btn => {
    btn.onclick = () => {
      const target = btn.dataset.screen;
      if (target === 'reader') openReader(state.lastBook, state.progress[state.lastBook] ?? 0);
      else showScreen(target);
    };
  });
  $('#themeBtn').onclick = toggleTheme;

  // tabbar
  $$('.tab').forEach(tab => {
    tab.onclick = () => {
      state.lastTab = tab.dataset.tab;
      showScreen(tab.dataset.tab);
      if (tab.dataset.tab === 'counter') renderCounter();
    };
  });

  // drawer
  $('#menuBtn').onclick = () => openDrawer(true);
  $('#drawerScrim').onclick = () => openDrawer(false);
  $('#heartBtn').onclick = () => showScreen('bookmarks');
  $('#shareBtn').onclick = () => toast('ലിങ്ക് പങ്കിട്ടു · khajabad');
  $('#drawerNight').onclick = toggleTheme;

  $('#drawer').addEventListener('click', e => {
    const item = e.target.closest('[data-go]');
    if (!item) return;
    openDrawer(false);
    showScreen(item.dataset.go);
  });

  // generic data-go (links inside screens)
  $$('.screen').forEach(screen => {
    screen.addEventListener('click', e => {
      const go = e.target.closest('[data-go]');
      if (go && go.closest('.screen')) {
        showScreen(go.dataset.go);
        return;
      }
      const bookBtn = e.target.closest('[data-book]');
      if (bookBtn && !bookBtn.closest('#featuredCard') && !bookBtn.closest('.tab')) {
        openReader(bookBtn.dataset.book, bookBtn.dataset.page !== undefined ? Number(bookBtn.dataset.page) : (state.progress[bookBtn.dataset.book] ?? 0));
        return;
      }
      const cat = e.target.closest('[data-cat]');
      if (cat) {
        const category = CATEGORIES[Number(cat.dataset.cat)];
        if (category.book) openReader(category.book, 0);
        else toast(`“${category.label}” ഉടൻ വരുന്നു`);
        return;
      }
      const remove = e.target.closest('[data-remove]');
      if (remove) {
        state.bookmarks.delete(remove.dataset.remove);
        saveBookmarks();
        renderBookmarks();
        toast('ബുക്ക്മാർക്ക് നീക്കി');
      }
    });
  });

  $('#featuredCard').onclick = () => openReader($('#featuredCard').dataset.book, state.progress[$('#featuredCard').dataset.book] ?? 0);

  // library import (preview only)
  const importMsg = () => toast('APK-യിൽ സിസ്റ്റം ഫയൽ പിക്കർ തുറക്കും');
  $('#importBtn').onclick = importMsg;
  $('#importCard').onclick = importMsg;

  bindSearch();
  bindCounter();
  bindReaderGestures();
  bindHomeParallax();

  // keyboard shortcuts
  window.addEventListener('keydown', e => {
    if (e.target.matches('input, textarea') || state.screen !== 'reader') return;
    if (e.key === 'ArrowRight') turnPage(1);
    if (e.key === 'ArrowLeft') turnPage(-1);
  });

  // events
  document.getElementById('eventsRefresh').onclick = () => {
    EVENTS_FETCHED_AT = Date.now();
    renderEvents();
    renderEventHero();
    toast(EVENTS_LIVE ? 'അപ്ഡേറ്റ് ചെയ്തു' : 'ഡെമോ ഡാറ്റ പുതുക്കി');
  };
  document.getElementById('eventsList').addEventListener('click', e => {
    const row = e.target.closest('[data-event]');
    if (row) openEventSheet(row.dataset.event);
  });
  document.getElementById('eventScrim').onclick = closeEventSheet;

  // hero + rows countdown ticker
  setInterval(() => {
    const el = document.getElementById('heroCountdown');
    const ev = nextEvent();
    if (el && ev) el.textContent = countdown(ev.start);
  }, 1000);

  // fit device to viewport
  fitDevice();
  window.addEventListener('resize', fitDevice);
}

function fitDevice() {
  const device = $('#device');
  const wrap = $('.device-wrap');
  const isMobile = window.innerWidth < 980;
  const availH = window.innerHeight - (isMobile ? 150 : 190);
  const availW = Math.min(window.innerWidth - (isMobile ? 24 : 60), 390);
  const scale = Math.max(0.42, Math.min(1, availH / 844, availW / 390));
  device.style.transform = `scale(${scale})`;
  wrap.style.width = `${390 * scale}px`;
  wrap.style.height = `${844 * scale + 34}px`;
  wrap.style.margin = '0 auto';
}

/* ------------------------------------------------------------ boot */

applyTheme(localStorage.getItem('kb_theme') || 'light');
$('#tabbar').classList.add('hidden');
buildSplash();
bindGlobal();
renderHome();
renderLibrary();
renderCounter();
renderSearch();
openDrawer(false);
renderEventHero();
renderEvents();
tryLiveFeed();
playSplash();
