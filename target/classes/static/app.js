const baseUrl = 'http://localhost:8081';

const $ = id => document.getElementById(id);
const out = $('output');

function show(v){
  out.textContent = (new Date()).toLocaleTimeString() + ' - ' + JSON.stringify(v, null, 2) + '\n' + out.textContent;
}

async function postForm(url, body){
  const res = await fetch(url, body ? {method:'POST', headers:{'Content-Type':'application/json'}, body: JSON.stringify(body)} : {method:'POST'});
  const txt = await res.text();
  let parsed;
  try{ parsed = JSON.parse(txt) }catch(e){ parsed = txt }
  show({url, status: res.status, body: parsed});
  return {status:res.status, body: parsed};
}

async function getJson(url){
  const res = await fetch(url);
  const txt = await res.text();
  let parsed;
  try{ parsed = JSON.parse(txt) }catch(e){ parsed = txt }
  show({url, status: res.status, body: parsed});
  return {status:res.status, body: parsed};
}

// Card handlers
document.addEventListener('DOMContentLoaded', ()=>{
  document.getElementById('btnGenerate').addEventListener('click', async ()=>{
    const pid = document.getElementById('productId').value || 'PROD01';
    await postForm(`${baseUrl}/cards/generate?productId=${encodeURIComponent(pid)}`);
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
