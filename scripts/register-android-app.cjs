// Register the renamed Android package using the project's existing Firebase credentials.
// Never print credentials, tokens, signing passwords, or the downloaded configuration.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const {spawnSync} = require('node:child_process');
const root = path.resolve(__dirname, '..');
const packageName = 'com.jinatra.wishplay';

async function main() {
  const credential = JSON.parse(fs.readFileSync(path.join(root, 'firebase/serviceAccountKey.json'), 'utf8'));
  const encode = data => Buffer.from(JSON.stringify(data)).toString('base64url');
  const now = Math.floor(Date.now() / 1000);
  const unsigned = encode({alg:'RS256',typ:'JWT'}) + '.' + encode({
    iss:credential.client_email, scope:'https://www.googleapis.com/auth/cloud-platform',
    aud:'https://oauth2.googleapis.com/token', iat:now, exp:now+3600
  });
  const jwt = unsigned + '.' + crypto.sign('RSA-SHA256', Buffer.from(unsigned), credential.private_key).toString('base64url');
  const authResponse = await fetch('https://oauth2.googleapis.com/token', {
    method:'POST', headers:{'Content-Type':'application/x-www-form-urlencoded'},
    body:new URLSearchParams({grant_type:'urn:ietf:params:oauth:grant-type:jwt-bearer',assertion:jwt}),
    signal:AbortSignal.timeout(30000)
  });
  const auth = await authResponse.json();
  if (!authResponse.ok) throw new Error(`Authentication failed (${authResponse.status}).`);
  const base = 'https://firebase.googleapis.com/v1beta1/';
  async function request(resource, body) {
    const response = await fetch(base + resource, {
      method:body === undefined ? 'GET' : 'POST',
      headers:{Authorization:`Bearer ${auth.access_token}`,'Content-Type':'application/json'},
      body:body === undefined ? undefined : JSON.stringify(body), signal:AbortSignal.timeout(30000)
    });
    const data = await response.json();
    if (!response.ok) throw new Error(`Firebase ${response.status}: ${data.error?.message || 'Request failed'}`);
    return data;
  }
  const parent = `projects/${credential.project_id}`;
  const apps = await request(parent + '/androidApps?pageSize=100');
  let app = apps.apps?.find(item => item.packageName === packageName);
  if (!process.argv.includes('--apply')) {
    console.log(JSON.stringify({project:credential.project_id,packageName,registered:Boolean(app)}));
    return;
  }
  if (!app) {
    const operation = await request(parent + '/androidApps', {packageName,displayName:'Jinatra WishPlay'});
    let result = operation;
    for (let attempt = 0; !result.done && attempt < 45; attempt++) {
      await new Promise(resolve => setTimeout(resolve, 2000));
      result = await request(operation.name);
    }
    if (!result.done || result.error) throw new Error('Android registration did not complete; rerun to resume.');
    app = result.response;
    console.log('Registered ' + packageName);
  }
  const props = fs.readFileSync(path.join(root,'local.properties'),'utf8');
  const property = (key, fallback) => props.match(new RegExp('^'+key+'=(.*)$','m'))?.[1].trim() || fallback;
  const keystores = [{file:path.resolve(root,property('WISHPLAY_KEYSTORE_FILE','keystore/wishplay-jinatra-release.jks')),
    password:property('WISHPLAY_KEYSTORE_PASSWORD',''),alias:property('WISHPLAY_KEY_ALIAS','wishplay')}];
  const debugFile = path.join(process.env.USERPROFILE || '', '.android/debug.keystore');
  if (fs.existsSync(debugFile)) keystores.push({file:debugFile,password:'android',alias:'androiddebugkey'});
  const sha = await request(app.name + '/sha');
  const known = new Set(sha.certificates?.map(item => item.shaHash.replaceAll(':','').toUpperCase()) || []);
  for (const store of keystores) {
    const output = spawnSync('keytool', ['-exportcert','-keystore',store.file,'-storepass',store.password,'-alias',store.alias],{windowsHide:true});
    if (output.status !== 0) throw new Error('Could not read signing certificate for Firebase registration.');
    const cert = new crypto.X509Certificate(output.stdout);
    for (const [certType, fingerprint] of [['SHA_1',cert.fingerprint],['SHA_256',cert.fingerprint256]]) {
      const shaHash = fingerprint.replaceAll(':','');
      if (!known.has(shaHash.toUpperCase())) {
        await request(app.name + '/sha', {certType,shaHash});
        known.add(shaHash.toUpperCase());
      }
    }
  }
  const config = await request(app.name + '/config');
  const json = JSON.parse(Buffer.from(config.configFileContents,'base64').toString('utf8'));
  json.client = json.client.filter(item => item.client_info.android_client_info.package_name === packageName);
  if (!json.client.length) throw new Error('Downloaded Firebase config is missing the requested package.');
  fs.writeFileSync(path.join(root,'app/google-services.json'), JSON.stringify(json,null,2)+'\n');
  console.log('Updated Firebase config and signing certificates for ' + packageName);
}
main().catch(error => { console.error(error.message); process.exitCode=1; });
