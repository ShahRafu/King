# Backend proxy design notes

This backend proxy uses scraping (axios + cheerio) and RSS parsing to provide free public search/fetch for the Android client. Key design principles:

- No paid APIs or API keys stored on client.
- Backend-only approach: any scraping, parsing, or optional keys live only on the server-side.
- Simple bearer-token auth for client → backend calls.
- Caching and rate-limiting to be friendly to upstream sites.

Files added in repo under `backend/`:
- package.json
- index.js
- routes/search.js
- middleware/auth.js
- adapters/wikiAdapter.js
- adapters/rssAdapter.js
- adapters/scrapeAdapter.js
- Dockerfile
- .env.example

How to run locally (quick):
1. cd backend
2. cp .env.example .env
3. edit .env and set CLIENT_BEARER_TOKEN
4. npm install
5. npm run dev

Security notes: Do not commit backend/.env. Use strong bearer token and host the backend behind HTTPS when deploying.
