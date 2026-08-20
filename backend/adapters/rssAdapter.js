const Parser = require('rss-parser');
const parser = new Parser();
const NodeCache = require('node-cache');
const cache = new NodeCache({ stdTTL: Number(process.env.CACHE_TTL_SECONDS || 300) });

async function fetchFeed(feedUrl) {
  const cacheKey = `rss:${feedUrl}`;
  const cached = cache.get(cacheKey);
  if (cached) return cached;

  const feed = await parser.parseURL(feedUrl);
  const items = (feed.items || []).slice(0, 30).map(item => ({
    title: item.title,
    link: item.link,
    pubDate: item.pubDate,
    contentSnippet: item.contentSnippet || item.content || '',
    source: 'rss'
  }));

  cache.set(cacheKey, items);
  return items;
}

module.exports = { fetchFeed };
