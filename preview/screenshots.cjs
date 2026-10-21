/* Renders the interactive preview to JPEG screenshots (used by CI + docs). */
const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

const OUT = path.join(__dirname, '..', 'docs', 'screens');
fs.mkdirSync(OUT, { recursive: true });

(async () => {
  const browser = await chromium.launch();
  const page = await browser.newPage({
    viewport: { width: 390, height: 844 },
    deviceScaleFactor: 2,
  });
  await page.goto('http://127.0.0.1:8080/index.html', { waitUntil: 'networkidle' });

  // Strip the preview chrome so shots show only the app surface.
  await page.evaluate(() => {
    document.querySelector('.topbar').style.display = 'none';
    document.querySelector('.notes').style.display = 'none';
    document.querySelector('.device-caption').style.display = 'none';
    document.querySelector('.stage').style.cssText = 'padding:0;margin:0;max-width:none';
    document.querySelector('.stage-body').style.cssText = 'display:block;gap:0';
    document.querySelector('.device-wrap').style.cssText = 'width:390px;height:844px;margin:0';
    const device = document.getElementById('device');
    device.style.cssText = 'width:390px;height:844px;border-radius:0;padding:0;box-shadow:none;transform:none;background:transparent';
    device.querySelector('.hardware-notch').style.display = 'none';
    document.querySelector('.viewport').style.cssText = 'border-radius:0;width:390px;height:844px';
  });

  const shot = async (name, wait = 700) => {
    await page.waitForTimeout(wait);
    await page.screenshot({ path: path.join(OUT, `${name}.jpg`), type: 'jpeg', quality: 92 });
  };

  // splash (replayed so it is fully visible)
  await page.evaluate(() => showScreen('splash'));
  await shot('01-splash', 1250);

  await page.evaluate(() => showScreen('home'));
  await shot('02-home');

  await page.evaluate(() => openDrawer(true));
  await shot('03-drawer');

  await page.evaluate(() => { openDrawer(false); showScreen('library'); });
  await shot('04-library');

  await page.evaluate(() => openReader('shajarath', 2));
  await shot('05-reader');

  await page.evaluate(() => document.getElementById('thumbsBtn').click());
  await shot('06-reader-pages');

  await page.evaluate(() => {
    document.getElementById('sheetScrim').click();
    showScreen('counter');
  });
  await shot('07-counter');

  await page.evaluate(() => {
    showScreen('search');
    const input = document.getElementById('searchInput');
    input.value = 'സ്വലാത്ത്';
    input.dispatchEvent(new Event('input', { bubbles: true }));
  });
  await shot('08-search');

  await page.evaluate(() => showScreen('bookmarks'));
  await shot('09-saved');

  // dark home
  await page.evaluate(() => {
    document.documentElement.classList.add('dark');
    showScreen('home');
  });
  await shot('10-home-dark');
  await page.evaluate(() => document.documentElement.classList.remove('dark'));

  // font diagnostics
  const diag = await browser.newPage({ viewport: { width: 420, height: 420 }, deviceScaleFactor: 2 });
  await diag.goto('http://127.0.0.1:8080/font-test.html', { waitUntil: 'networkidle' });
  await diag.waitForTimeout(400);
  await diag.screenshot({ path: path.join(OUT, 'zz-font-test.jpg'), type: 'jpeg', quality: 92 });
  await diag.close();

  await browser.close();
  console.log('screenshots written to', OUT);
})();
