const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const wiki = require('../adapters/wikiAdapter');
const rssAdapter = require('../adapters/rssAdapter');
const scrape = require('../adapters/scrapeAdapter');

// Protected endpoints: require Bearer token
router.get('/search', auth, async (req, res) => {
  const provider = req.query.provider || 'wiki';
  const q = req.query.q;
  if (!q) return res.status(400).json({ error: 'q query param required' });

  try {
    let results = [];
    if (provider === 'wiki') {
      results = await wiki.search(q);
    } else if (provider === 'rss') {
      const feedUrl = req.query.feed; // optional
      if (!feedUrl) return res.status(400).json({ error: 'feed param required for rss provider' });
      results = await rssAdapter.fetchFeed(feedUrl);
    } else if (provider === 'scrape') {
      const url = req.query.url;
      if (!url) return res.status(400).json({ error: 'url param required for scrape provider' });
      const parsed = await scrape.fetchAndParse(url);
      results = [parsed];
    } else {
      return res.status(400).json({ error: 'unsupported provider' });
    }

    res.json({ provider, q, results });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'fetch_failed', detail: err.message });
  }
});

// Deep fetch endpoint (raw or parsed) - allowlist recommended
router.get('/fetch', auth, async (req, res) => {
  const url = req.query.url;
  if (!url) return res.status(400).json({ error: 'url query param required' });

  try {
    const parsed = await scrape.fetchAndParse(url);
    res.json({ url, parsed });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'fetch_failed', detail: err.message });
  }
});

module.exports = router;
