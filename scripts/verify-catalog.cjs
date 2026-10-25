const fs = require('node:fs');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname, '../app/src/main/java/com/jinatra/wishplay/data/remote/api/WishPlayApiService.kt'), 'utf8');
const key = source.match(/rawgApiKey = "([^"]+)"/)[1];
async function get(resource) {
  const response = await fetch(`https://api.rawg.io/api/${resource}${resource.includes('?') ? '&' : '?'}key=${key}`, {signal:AbortSignal.timeout(30000)});
  if (!response.ok) throw new Error(`Catalog HTTP ${response.status}`);
  return response.json();
}
async function main() {
  const searches = [{platform:'Android',id:21,query:'Genshin Impact'}, {platform:'PC',id:4,query:'Elden Ring'}, {platform:'PS5',id:187,query:'Astro Bot'}];
  const results = await Promise.all(searches.map(async item => {
    const response = await get(`games?platforms=${item.id}&search=${encodeURIComponent(item.query)}&page_size=5`);
    if (!response.results.length || !response.results.some(game => game.name.toLowerCase().includes(item.query.toLowerCase()))) {
      throw new Error(`${item.platform} search did not return the expected title`);
    }
    const game = response.results.find(game => game.name.toLowerCase().includes(item.query.toLowerCase()));
    const detail = await get(`games/${game.id}`);
    if (!detail.platforms.some(port => port.platform.id === item.id)) throw new Error('Platform membership was lost');
    const image = await fetch(detail.background_image, {method:'HEAD',signal:AbortSignal.timeout(30000)});
    if (!image.ok || !image.headers.get('content-type')?.startsWith('image/')) throw new Error('Game artwork is unavailable');
    return {platform:item.platform,title:game.name,artwork:true,dates:detail.platforms.map(port => ({platform:port.platform.name,date:port.released_at || null}))};
  }));
  const pages = await Promise.all([1,2].map(page => get(`games?platforms=21&page_size=40&page=${page}&ordering=-added`)));
  if (!pages[0].next || !pages[1].results.some(game => !pages[0].results.some(first => first.id === game.id))) {
    throw new Error('Catalog pagination did not return additional games');
  }
  async function publicJson(url) {
    const response = await fetch(url, {signal:AbortSignal.timeout(30000)});
    if (!response.ok) throw new Error(`Store HTTP ${response.status}`);
    return response.json();
  }
  const [steam, apple] = await Promise.all([
    publicJson('https://store.steampowered.com/api/appdetails?appids=413150&l=english&cc=us'),
    publicJson('https://itunes.apple.com/lookup?id=1406710800&country=us&entity=software')
  ]);
  if (!steam['413150']?.success || steam['413150'].data.type !== 'game') throw new Error('Steam game lookup failed');
  if (apple.results?.[0]?.primaryGenreId !== 6014) throw new Error('App Store game lookup failed');
  console.log(JSON.stringify({searches:results,pagination:{page1:pages[0].results.length,page2:pages[1].results.length},
    stores:{steam:{title:steam['413150'].data.name,date:steam['413150'].data.release_date.date},
      appStore:{title:apple.results[0].trackName,date:apple.results[0].releaseDate}}},null,2));
}
main().catch(error => { console.error(error.message); process.exitCode=1; });
