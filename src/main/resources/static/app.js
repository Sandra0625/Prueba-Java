const baseUrl = 'http://localhost:8081';

const $ = id => document.getElementById(id);
const out = $('output');

function show(v){
  out.textContent = (new Date()).toLocaleTimeString() + ' - ' + JSON.stringify(v, null, 2) + '\n' + out.textContent;
}

async function postForm(url, body){
  const token = localStorage.getItem('bankinc_token');
  const headers = {'Content-Type':'application/json'};
  if(token) headers['Authorization'] = 'Bearer ' + token;
  const res = await fetch(url, body ? {method:'POST', headers, body: JSON.stringify(body)} : {method:'POST', headers});
  const txt = await res.text();
  let parsed;
  try{ parsed = JSON.parse(txt) }catch(e){ parsed = txt }
  show({url, status: res.status, body: parsed});
  return {status:res.status, body: parsed};
}

async function getJson(url){
  const token = localStorage.getItem('bankinc_token');
  const headers = {};
  if(token) headers['Authorization'] = 'Bearer ' + token;
  const res = await fetch(url, {headers});
  const txt = await res.text();
  let parsed;
  try{ parsed = JSON.parse(txt) }catch(e){ parsed = txt }
  show({url, status: res.status, body: parsed});
  return {status:res.status, body: parsed};
}

// Card handlers
document.addEventListener('DOMContentLoaded', ()=>{
  // Login handlers
  const loginBox = document.getElementById('loginBox');
  const userArea = document.getElementById('userArea');
  const btnLogin = document.getElementById('btnLogin');
  const btnLogout = document.getElementById('btnLogout');

  function setLoggedIn(name){
    localStorage.setItem('bankinc_user', name);
    document.getElementById('holderName').value = name;
    loginBox.style.display = 'none';
    userArea.style.display = 'block';
  }

  function setLoggedOut(){
    localStorage.removeItem('bankinc_user');
    localStorage.removeItem('bankinc_token');
    localStorage.removeItem('bankinc_card');
    document.getElementById('holderName').value = '';
    loginBox.style.display = 'block';
    userArea.style.display = 'none';
  }

  // initialize state
  const existing = localStorage.getItem('bankinc_user');
  if(existing){ setLoggedIn(existing); } else { setLoggedOut(); }
  const existingCard = localStorage.getItem('bankinc_card');
  if(existing){ updateProfileUI(existing, existingCard); }

  // toggle between login and register forms
  document.getElementById('showLogin').addEventListener('click', ()=>{
    document.getElementById('loginForm').style.display = 'flex';
    document.getElementById('registerForm').style.display = 'none';
  });
  document.getElementById('showRegister').addEventListener('click', ()=>{
    document.getElementById('loginForm').style.display = 'none';
    document.getElementById('registerForm').style.display = 'flex';
  });

  btnLogin.addEventListener('click', ()=>{
    // Call backend login
    const name = document.getElementById('loginName').value || '';
    const pass = document.getElementById('loginPassword').value || '';
    if(!name || !pass){ show('Ingrese usuario y contraseña'); return }
    postForm(`${baseUrl}/auth/login`, {username: name, password: pass}).then(res => {
      if(res.status === 200 && res.body && res.body.token){
        localStorage.setItem('bankinc_token', res.body.token);
        setLoggedIn(name);
        show({event:'login', user: name});
      } else {
        show({event:'login_failed', body: res.body});
      }
    });
  });

  btnLogout.addEventListener('click', ()=>{
    setLoggedOut();
    show('Sesión cerrada');
  });

  // register flow: create user and immediately create a card
  document.getElementById('btnRegister').addEventListener('click', async ()=>{
    const name = document.getElementById('regName').value || '';
    const pass = document.getElementById('regPassword').value || '';
    const pid = document.getElementById('regProductId').value || 'PROD01';
    if(!name || !pass){ show('Ingrese nombre y contraseña'); return }
    // register user and get token
    const reg = await postForm(`${baseUrl}/auth/register`, {username: name, password: pass});
    if(reg.status === 200 && reg.body && reg.body.token){
      localStorage.setItem('bankinc_token', reg.body.token);
      // now create card using authenticated token
      const res = await postForm(`${baseUrl}/cards/generate`, { productId: pid, holderName: name });
      if(res.status === 200){
        const cardId = (typeof res.body === 'string') ? res.body : (res.body && res.body.cardId) || res.body;
        localStorage.setItem('bankinc_user', name);
        localStorage.setItem('bankinc_card', cardId);
        setLoggedIn(name);
        updateProfileUI(name, cardId);
        show({event:'registered', user:name, card:cardId});
      } else {
        show({event:'register_card_failed', status: res.status, body: res.body});
      }
    } else {
      show({event:'register_failed', status: reg.status, body: reg.body});
    }
  });

  // Generate card (uses logged user as holderName if not provided)
  document.getElementById('btnGenerate').addEventListener('click', async ()=>{
    const pid = document.getElementById('productId').value || 'PROD01';
    let holder = document.getElementById('holderName').value || '';
    const logged = localStorage.getItem('bankinc_user');
    if(!holder && logged) holder = logged;
    await postForm(`${baseUrl}/cards/generate`, { productId: pid, holderName: holder });
  });

  document.getElementById('btnEnroll').addEventListener('click', async ()=>{
    const id = document.getElementById('cardId').value; if(!id){show('Ingrese cardId'); return}
    await postForm(`${baseUrl}/cards/${encodeURIComponent(id)}/enroll`);
  });

  document.getElementById('btnBlock').addEventListener('click', async ()=>{
    const id = document.getElementById('cardId').value; if(!id){show('Ingrese cardId'); return}
    await postForm(`${baseUrl}/cards/${encodeURIComponent(id)}/block`);
  });

  document.getElementById('btnRecharge').addEventListener('click', async ()=>{
    const id = document.getElementById('cardId').value; const amt = document.getElementById('rechargeAmount').value || '0';
    if(!id){show('Ingrese cardId'); return}
    await postForm(`${baseUrl}/cards/${encodeURIComponent(id)}/recharge?amount=${encodeURIComponent(amt)}`);
  });

  document.getElementById('btnBalance').addEventListener('click', async ()=>{
    const id = document.getElementById('cardId').value; if(!id){show('Ingrese cardId'); return}
    await getJson(`${baseUrl}/cards/${encodeURIComponent(id)}/balance`);
  });

  // Transaction handlers
  document.getElementById('btnPurchase').addEventListener('click', async ()=>{
    const id = document.getElementById('txCardId').value; const price = parseFloat(document.getElementById('txPrice').value || '0');
    if(!id){show('Ingrese cardId'); return}
    await postForm(`${baseUrl}/transaction/purchase`, {cardId: id, price});
  });

  document.getElementById('btnGetTx').addEventListener('click', async ()=>{
    const tx = document.getElementById('txId').value; if(!tx){show('Ingrese transactionId'); return}
    await getJson(`${baseUrl}/transaction/${encodeURIComponent(tx)}`);
  });

  document.getElementById('btnAnnul').addEventListener('click', async ()=>{
    const tx = document.getElementById('txId').value; if(!tx){show('Ingrese transactionId'); return}
    await postForm(`${baseUrl}/transaction/anulation`, {transactionId: tx});
  });

  document.getElementById('productId').focus();
});

function updateProfileUI(name, cardId){
  const pn = document.getElementById('profileName');
  const pc = document.getElementById('profileCard');
  pn.textContent = name || '-';
  pc.textContent = cardId || '(ninguna)';
}
