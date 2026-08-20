const dotenv = require('dotenv');
dotenv.config();

module.exports = function(req, res, next) {
  const authHeader = req.headers['authorization'] || '';
  const token = (authHeader.startsWith('Bearer ')) ? authHeader.slice(7) : null;
  if (!token || token !== process.env.CLIENT_BEARER_TOKEN) {
    return res.status(401).json({ error: 'unauthorized' });
  }
  next();
};
