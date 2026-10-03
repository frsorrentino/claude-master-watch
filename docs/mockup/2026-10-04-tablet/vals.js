// Esegue il Component di una tavola .dc.html e stampa i valori di renderVals() come JSON (le funzioni diventano null).
// Uso: node vals.js tavola.dc.html '{"stato":"facoltativo"}'
const fs = require('fs');
const src = fs.readFileSync(process.argv[2], 'utf8');
const m = src.match(/<script type="text\/x-dc"[^>]*>([\s\S]*?)<\/script>/);
if (!m) { console.log('{}'); process.exit(0); }
class DCLogic {
  constructor(props) { this.props = props || {}; this.state = {}; }
  setState(s) { Object.assign(this.state, typeof s === 'function' ? s(this.state) : s); }
  forceUpdate() {}
}
const Component = new Function('DCLogic', m[1] + '\nreturn Component;')(DCLogic);
const c = new Component({});
if (process.argv[3]) Object.assign(c.state, JSON.parse(process.argv[3]));
const vals = c.renderVals();
console.log(JSON.stringify(vals, (k, v) => (typeof v === 'function' ? null : v)));
