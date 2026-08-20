# Backend proxy for King app

This Node.js (Express) backend provides secured endpoints to fetch public data from Wikipedia, RSS feeds, and generic web pages (via Cheerio scraping). It's designed to keep all fetching/parsing server-side so the Android client never needs to hold API keys or scraping logic.

Features
- /api/v1/search - simple search adapter (provider param: wiki|rss|scrape)
- /api/v1/fetch - fetch raw content or parsed summary for a given URL (allow-list recommended)
- Bearer token authentication (set CLIENT_BEARER_TOKEN in .env)
- Caching (in-memory) with TTL
- Rate limiting + basic security headers
- Dockerfile + .env.example

Security
- Do NOT commit backend/.env to git. Use backend/.env.example as template.
- Place sensitive keys and tokens in environment variables on your server.

Run locally
1. cd backend
2. cp .env.example .env  (then edit .env to set CLIENT_BEARER_TOKEN)
3. npm install
4. npm run dev

Deploy
- Provided Dockerfile for containerized deploy. Set env vars securely on your cloud provider (Cloud Run, ECS, etc.)

