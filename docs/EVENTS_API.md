# Next-events feed

The app shows your website's upcoming events using the same Notte function. This
file explains how it is wired, what happens when the payload changes, and how to
tighten it once we see a real response.

---

## 1. The call

```http
POST {NOTTE_BASE_URL}/functions/{NOTTE_FUNCTION_ID}/runs/start
x-notte-api-key: <NOTTE_API_KEY>
Authorization: Bearer <NOTTE_API_KEY>
Content-Type: application/json

{
  "function_id": "635cfdc6-667e-453c-bc57-bce54f35724b",
  "variables": { "query": "", "highlighted_only": false, "limit": 50 }
}
```

Implementation: `app/src/main/java/com/khajabad/app/data/events/EventApi.kt`

- Plain `HttpURLConnection` + `org.json` — no Retrofit/OkHttp/Moshi, nothing added to the APK.
- 15 s connect / 30 s read timeouts, everything on `Dispatchers.IO`.
- **Async runs are handled**: if the first response contains a `run_id` (or nested
  `run.id` / `data.id`) and no event data, the client polls `GET {base}/runs/{run_id}`
  every 1.2 s until the payload appears or the run reaches a terminal status
  (40 s hard ceiling → the UI falls back to cache instead of hanging).

## 2. Reading the payload

`EventParser` is deliberately schema-agnostic, because the function output can evolve:

1. Parses the body as an object (or array).
2. Walks the tree up to 8 levels deep, skipping envelope keys (`variables`, `input`, `request`, `params`, `config`, `function`).
3. Collects every array that yields event-looking objects, then keeps the **largest** one and sorts by date.
4. Maps each object through alias tables:

| Field | Aliases tried |
|---|---|
| title | `title, name, event_name, event, heading, label, subject, program, programme, caption, text` |
| start | `start, start_date, start_time, datetime, date, event_date, when, begins, from, startDate, Date` |
| end | `end, end_date, end_time, until, to, ends` |
| location | `location, venue, place, address, where, city, hall, masjid, mosque` |
| description | `description, details, summary, body, note, notes, info, about, content` |
| link | `url, link, website, registration_url, href, permalink` |
| image | `image, image_url, cover, thumbnail, photo, poster` |
| highlighted | `highlighted, featured, is_highlighted, pin, pinned, star` |

Nested objects are unwrapped (`{"start": {"iso": "…"}}` works), and dates are parsed
without `java.time` (so it runs on API 23) — ISO-8601 with/without zone, epoch
seconds or millis, plus common human formats (`24 Oct 2026`, `October 24, 2026`,
`Fri, 24 Oct 2026`, `24–26 Oct 2026` ranges, `24/10/2026`, `20261024`).

Anything that is not mapped is kept in `Event.raw` — the Events screen has a
**“Show raw fields”** toggle that prints the keys of the first event, which is the
fastest way to tell me which alias to add.

> If you can paste one real response (or just the top-level keys + one event object),
> I will pin the mapping exactly and delete the guessing.

## 3. Where it shows up

| Surface | Behaviour |
|---|---|
| **Home → Next events** | Deep-ink hero card: date block, title, time · venue, per-second countdown, live/saved dot, FEATURED ribbon, tap → Events |
| **Events screen** | Month-grouped list, “today” highlight, infinite-refresh control, offline banner, detail sheet |
| **Detail sheet** | Location, end time, description, **Add to calendar** (`ACTION_INSERT` → `CalendarContract`), share, open link |
| **Drawer** | “Events” row shows the next event title / count |

States are explicit (`EventsState`): `Loading` (shimmer), `Live`, `Stale` (cached +
“offline · 2 h ago”), `Failed` (retry card). The cache lives in Preferences DataStore
(`events_cache` + `events_fetched_at`), so a cold start with no network still renders
the last known schedule.

Refresh policy: fetched once at startup, then on demand; automatic refetch only if the
cache is older than **10 minutes**.

## 4. Configuration

Order of precedence — first non-blank wins:

1. environment variable `NOTTE_API_KEY` / `NOTTE_FUNCTION_ID` / `NOTTE_BASE_URL`
2. `local.properties` (gitignored — for your machine)
3. `notte.properties` (committed, so CI/published APKs work)
4. defaults baked in the build script

```properties
NOTTE_API_KEY=sk-notte-…
NOTTE_FUNCTION_ID=635cfdc6-667e-453c-bc57-bce54f35724b
NOTTE_BASE_URL=https://us-prod.notte.cc
```

These become `BuildConfig.NOTTE_API_KEY`, `BuildConfig.NOTTE_FUNCTION_ID`,
`BuildConfig.NOTTE_BASE_URL`. If the key is empty the app degrades cleanly: the card
shows a retry state and the Events screen explains that the feed is not configured —
books, reader and counter are unaffected (they never touch the network).

### About the key

A key shipped inside an APK can be extracted — the website exposes it too, so this is
the same exposure you already have. If you want it locked down:

1. Put a tiny proxy in front of Notte (Cloudflare Worker / Vercel function) that adds
   the header server-side and allows only your domain/app,
2. point `NOTTE_BASE_URL` at the proxy and blank the key,
3. rotate the current key in the Notte dashboard.

Also worth doing regardless: rate-limit the function, and make it cache its scrape
for a couple of minutes so a few hundred app refreshes don't hammer the source site.

## 5. Privacy / permissions

The app declares only `INTERNET` + `ACCESS_NETWORK_STATE`, and they are used solely
for this feed. No analytics, no identifiers sent, no background sync, no wake locks.

## 6. When the feed looks wrong

The sandbox that wrote this client could not reach `us-prod.notte.cc`, so the exact
response shape has never been observed — the parser is deliberately tolerant, and
everything below is verified against fixtures rather than the live function.

- **Empty card, no error** → the run succeeded but the parser found no array. Open
  Events → *raw response* toggle to see the untouched payload, then add the field
  name to the alias tables in `data/events/Event.kt`.
- **Card stuck on "offline"** → a stale cached payload is being shown; the state
  chip says how old it is. Pull to refresh from the Events screen.
- **Card says "not configured"** → `BuildConfig.NOTTE_API_KEY` is empty; see §4.
- **Did the app itself break, or just the feed?** The `JVM launch test` job
  (`smoke.yml`) renders the home screen headlessly on every push, so a red build
  there means the UI broke — not the network. Its log lands in
  [`docs/ci/jvm-test.log`](ci/README.md); the demo feed in `preview/` renders the
  same components without any network at all.
