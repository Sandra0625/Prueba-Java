const baseUrl = 'http://localhost:8081';

const $ = id => document.getElementById(id);
const out = $('output');

function show(v){
  if(!out) return; // output panel was removed from UI — avoid JS errors
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
  // Views
  const viewLogin = document.getElementById('view-login');
  const viewRegister = document.getElementById('view-register');
  const viewDashboard = document.getElementById('view-dashboard');

  const btnEntrar = document.getElementById('btnEntrar');
  const btnGoRegister = document.getElementById('btnGoRegister');
  const btnSubmitRegister = document.getElementById('btnSubmitRegister');
  const btnCancelRegister = document.getElementById('btnCancelRegister');
  const btnLogout = document.getElementById('btnLogout');

  const btnRecharge = document.getElementById('btnRecharge');
  const btnBalance = document.getElementById('btnBalance');
  const btnPurchase = document.getElementById('btnPurchase');

  function showView(v){
    viewLogin.classList.add('hidden');
    viewRegister.classList.add('hidden');
    viewDashboard.classList.add('hidden');
    v.classList.remove('hidden');
  }

  function setLoggedOut(){
    localStorage.removeItem('bankinc_user');
    localStorage.removeItem('bankinc_token');
    localStorage.removeItem('bankinc_card');
    updateProfileUI(null, null);
    showView(viewLogin);
  }

  async function fetchMyCardsAndShow(){
    const res = await getJson(`${baseUrl}/cards/me`);
    if(res.status === 200 && Array.isArray(res.body) && res.body.length>0){
      const first = res.body[0];
      localStorage.setItem('bankinc_card', first.cardId);
      updateProfileUI(localStorage.getItem('bankinc_user'), first.cardId);
    }
    showView(viewDashboard);
  }

  // init
  const existingUser = localStorage.getItem('bankinc_user');
  if(existingUser && localStorage.getItem('bankinc_token')){
    fetchMyCardsAndShow();
  } else {
    showView(viewLogin);
  }

  btnGoRegister.addEventListener('click', ()=> showView(viewRegister));
  btnCancelRegister.addEventListener('click', ()=> showView(viewLogin));

  // Login flow
  btnEntrar.addEventListener('click', async ()=>{
    const username = document.getElementById('loginUsername').value || '';
    const password = document.getElementById('loginPassword').value || '';
    if(!username || !password){ show('Ingrese usuario y contraseña'); return }
    const r = await postForm(`${baseUrl}/auth/login`, {username, password});
    if(r.status === 200 && r.body && r.body.token){
      localStorage.setItem('bankinc_token', r.body.token);
      localStorage.setItem('bankinc_user', username);
      await fetchMyCardsAndShow();
      show({event:'login', user: username});
    } else {
      show({event:'login_failed', body:r.body});
    }
  });

  // Register flow: collects name, document, email, celular
  btnSubmitRegister.addEventListener('click', async ()=>{
    const name = document.getElementById('reg_fullname').value || '';
    const doc = document.getElementById('reg_doc').value || '';
    const email = document.getElementById('reg_email').value || '';
    const cell = document.getElementById('reg_cell').value || '';
    if(!name || !doc || !email){ show('Complete nombre, documento y correo'); return }

    // Use email as username and document as initial password (demo). Adjust if you want a different policy.
    const username = email;
    const password = doc;

    const reg = await postForm(`${baseUrl}/auth/register`, {username, password});
    if(reg.status === 200 && reg.body && reg.body.token){
      localStorage.setItem('bankinc_token', reg.body.token);
      localStorage.setItem('bankinc_user', username);
      // create default card for user
      const gen = await postForm(`${baseUrl}/cards/generate`, {productId: 'PROD01', holderName: name});
      if(gen.status === 200){
        await fetchMyCardsAndShow();
        show({event:'registered', user: username, card: gen.body});
      } else {
        show({event:'registered_but_card_failed', body: gen.body});
      }
    } else {
      show({event:'register_failed', body: reg.body});
    }
  });

  // Logout
  btnLogout.addEventListener('click', ()=> setLoggedOut());

  // Dashboard actions
  btnRecharge.addEventListener('click', async ()=>{
    const id = document.getElementById('cardId').value || localStorage.getItem('bankinc_card');
    const amt = document.getElementById('rechargeAmount').value || '0';
    if(!id){ show('Ingrese cardId'); return }
    await postForm(`${baseUrl}/cards/${encodeURIComponent(id)}/recharge?amount=${encodeURIComponent(amt)}`);
  });

  btnBalance.addEventListener('click', async ()=>{
    const id = document.getElementById('cardId').value || localStorage.getItem('bankinc_card');
    if(!id){ show('Ingrese cardId'); return }
    await getJson(`${baseUrl}/cards/${encodeURIComponent(id)}/balance`);
  });

  btnPurchase.addEventListener('click', async ()=>{
    const id = document.getElementById('txCardId').value || localStorage.getItem('bankinc_card');
    const price = parseFloat(document.getElementById('txPrice').value || '0');
    if(!id){ show('Ingrese cardId'); return }
    await postForm(`${baseUrl}/transaction/purchase`, {cardId: id, price});
  });
});

function updateProfileUI(name, cardId){
  const pn = document.getElementById('profileName');
  const pc = document.getElementById('profileCard');
  pn.textContent = name || '-';
  pc.textContent = cardId || '(ninguna)';
}
