const axios = require('axios');
const cheerio = require('cheerio');
const NodeCache = require('node-cache');
const cache = new NodeCache({ stdTTL: Number(process.env.CACHE_TTL_SECONDS || 300) });

function normalizeUrl(u) {
  try { return new URL(u).toString(); } catch(e){ return null; }
}

async function fetchAndParse(url) {
  const norm = normalizeUrl(url);
  if (!norm) throw new Error('invalid_url');
  const cacheKey = `scrape:${norm}`;
  const cached = cache.get(cacheKey);
  if (cached) return cached;

  const resp = await axios.get(norm, { timeout: 10000, headers: { 'User-Agent': 'KingAppBackend/1.0 (+https://github.com/ShahRafu/King)' } });
  const html = resp.data;
  const $ = cheerio.load(html);

  // Simple extraction: title, description/meta, first paragraphs
  const title = $('head > title').text() || $('title').text() || '';
  const metaDesc = $('meta[name="description"]').attr('content') || $('meta[property="og:description"]').attr('content') || '';

  // gather first few paragraph texts
  const paragraphs = [];
  $('p').each((i, el) => {
    const text = $(el).text().trim();
    if (text.length > 40 && paragraphs.length < 5) paragraphs.push(text);
  });

  const result = {
    title,
    description: metaDesc,
    paragraphs,
    url: norm,
    source: 'scrape'
  };

  cache.set(cacheKey, result);
  return result;
}

module.exports = { fetchAndParse };
