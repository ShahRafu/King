const axios = require('axios');
const NodeCache = require('node-cache');

const cache = new NodeCache({ stdTTL: Number(process.env.CACHE_TTL_SECONDS || 300) });

async function search(query) {
  // Use Wikipedia REST search (Opensearch fallback)
  const cacheKey = `wiki:${query}`;
  const cached = cache.get(cacheKey);
  if (cached) return cached;

  // Use the Wikipedia opensearch API for simple results
  const url = 'https://en.wikipedia.org/w/api.php';
  const params = {
    action: 'opensearch',
    search: query,
    limit: 10,
    namespace: 0,
    format: 'json'
  };

  const resp = await axios.get(url, { params, timeout: 8000 });
  // resp.data: [searchterm, titles[], descriptions[], links[]]
  const data = resp.data;
  const results = [];
  if (Array.isArray(data) && data.length >= 4) {
    const titles = data[1] || [];
    const descs = data[2] || [];
    const links = data[3] || [];
    for (let i = 0; i < titles.length; i++) {
      results.push({
        title: titles[i],
        snippet: descs[i] || '',
        url: links[i],
        source: 'wikipedia'
      });
    }
  }

  cache.set(cacheKey, results);
  return results;
}

module.exports = { search };
