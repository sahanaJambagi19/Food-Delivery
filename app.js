// ===== API Configuration =====
const API_BASE = 'http://localhost:8080/api';

// ===== Token Helpers =====
const Auth = {
    save(data) {
        localStorage.setItem('accessToken',  data.accessToken);
        localStorage.setItem('refreshToken', data.refreshToken);
        localStorage.setItem('userId',       data.userId);
        localStorage.setItem('userName',     data.name);
        localStorage.setItem('userEmail',    data.email);
        localStorage.setItem('userRole',     data.role);
    },
    getToken()   { return localStorage.getItem('accessToken'); },
    getRole()    { return localStorage.getItem('userRole'); },
    getUserId()  { return localStorage.getItem('userId'); },
    getName()    { return localStorage.getItem('userName'); },
    isLoggedIn() { return !!localStorage.getItem('accessToken'); },
    clear() {
        ['accessToken','refreshToken','userId','userName','userEmail','userRole']
            .forEach(k => localStorage.removeItem(k));
    },
    redirectByRole() {
        const role = Auth.getRole();
        if (role === 'ADMIN')              window.location.href = 'admin/dashboard.html';
        else if (role === 'RESTAURANT_OWNER') window.location.href = 'restaurant/dashboard.html';
        else if (role === 'DELIVERY_PARTNER') window.location.href = 'delivery/dashboard.html';
        else                               window.location.href = 'index.html';
    }
};

// ===== HTTP Client =====
async function apiFetch(path, options = {}, _retried = false) {
    const headers = { 'Content-Type': 'application/json', ...options.headers };
    if (Auth.isLoggedIn()) headers['Authorization'] = 'Bearer ' + Auth.getToken();

    let res;
    try {
        res = await fetch(API_BASE + path, { ...options, headers });
    } catch (e) {
        return { ok: false, status: 0, data: { message: 'Cannot reach the server. Is the backend running?' } };
    }
    const json = await res.json().catch(() => ({}));

    // Auth endpoints (login/register/refresh) return 401 for bad credentials: show that error, don't redirect.
    const isAuthCall = path.startsWith('/auth/');
    if (res.status === 401 && !isAuthCall) {
        if (!_retried && await tryRefresh()) return apiFetch(path, options, true); // retry once
        Auth.clear();
        const inSub = /\/(admin|restaurant|delivery)\//.test(location.pathname);
        window.location.href = (inSub ? '../' : '') + 'login.html';
        return { ok: false, status: 401, data: json };
    }
    return { ok: res.ok, status: res.status, data: json };
}

async function tryRefresh() {
    const rt = localStorage.getItem('refreshToken');
    if (!rt) return false;
    try {
        const res = await fetch(API_BASE + '/auth/refresh', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ refreshToken: rt })
        });
        if (!res.ok) return false;
        const json = await res.json();
        Auth.save(json.data);
        return true;
    } catch { return false; }
}

// ===== UI Helpers =====
function showAlert(elId, message, type = 'danger') {
    const el = document.getElementById(elId);
    if (!el) return;
    el.className = `alert-custom alert-${type}-custom`;
    el.textContent = message;
    el.style.display = 'block';
    setTimeout(() => el.style.display = 'none', 5000);
}

function setLoading(btnId, loading) {
    const btn = document.getElementById(btnId);
    if (!btn) return;
    btn.disabled = loading;
    btn.innerHTML = loading
        ? '<span class="spinner-border spinner-border-sm me-2"></span>Please wait...'
        : btn.dataset.label;
}


// Curated real-food photography used whenever a restaurant/menu item has no uploaded image.
const FoodImages = (() => {
 const photos = {
  biryani:'photo-1563379091339-03246963d96c', indian:'photo-1585937421612-70a008356fbe', curry:'photo-1603894584373-5ac82b2ae398',
  dosa:'photo-1589301760014-d929f3979dbc', pizza:'photo-1513104890138-7c749659a591', burger:'photo-1568901346375-23c9450c58cd',
  salad:'photo-1512621776951-a57141f2eefd', bowl:'photo-1511690743698-d9d85f2fbf38', pasta:'photo- pasta',
  noodles:'photo-1585032226651-759b368d7246', sushi:'photo-1579871494447-9811cf80d66c', dessert:'photo-1578985545062-69928b1d9587',
  coffee:'photo-150,', sandwich:'photo-1528735602780-2552fd46c7af', chicken:'photo-153,', fries:'photo-1573080496219-bb080dd4f877',
  paneer:'photo-1631452180519-c014fe946bc7', soup:'photo-1547592180-85f173990554', wrap:'photo-1626700051175-6818013e1d4f',
  default:'photo-1546069901-ba9599a7e63c'
 };
 const ids={biryani:'photo-1563379091339-03246963d96c',indian:'photo-1585937421612-70a008356fbe',curry:'photo-1603894584373-5ac82b2ae398',dosa:'photo-1589301760014-d929f3979dbc',pizza:'photo-1513104890138-7c749659a591',burger:'photo-1568901346375-23c9450c58cd',salad:'photo-1512621776951-a57141f2eefd',bowl:'photo-1511690743698-d9d85f2fbf38',noodles:'photo-1585032226651-759b368d7246',sushi:'photo-1579871494447-9811cf80d66c',dessert:'photo-1578985545062-69928b1d9587',coffee:'photo-1509042239860-f550ce710b93',sandwich:'photo-1528735602780-2552fd46c7af',fries:'photo-1573080496219-bb080dd4f877',paneer:'photo-1631452180519-c014fe946bc7',soup:'photo-1547592180-85f173990554',wrap:'photo-1626700051175-6818013e1d4f',default:'photo-1546069901-ba9599a7e63c'};
 function key(value){const s=String(value||'').toLowerCase();if(/biryani|dum/.test(s))return'biryani';if(/dosa|idli|vada|south indian/.test(s))return'dosa';if(/pizza/.test(s))return'pizza';if(/burger/.test(s))return'burger';if(/salad|healthy|green|fresh/.test(s))return'salad';if(/pasta|italian|lasagna/.test(s))return'noodles';if(/noodle|wok|chinese|manchurian|fried rice/.test(s))return'noodles';if(/cake|pastry|dessert|sweet|ice cream/.test(s))return'dessert';if(/coffee|cafe|café|brew/.test(s))return'coffee';if(/sandwich/.test(s))return'sandwich';if(/fries|fry/.test(s))return'fries';if(/paneer/.test(s))return'paneer';if(/soup/.test(s))return'soup';if(/wrap|roll/.test(s))return'wrap';if(/chicken|tandoor|kebab|mughlai|punjabi|north indian|indian|curry|thali|masala|chaat/.test(s))return'indian';return'default';}
 function url(value){return 'https://images.unsplash.com/'+ids[key(value)]+'?auto=format&fit=crop&w=1000&q=85';}
 function restaurant(name,cuisine){return url((name||'')+' '+(cuisine||''));}
 function logo(name){const initials=String(name||'').split(/\\s+/).filter(Boolean).slice(0,2).map(x=>x[0].toUpperCase()).join('')||'F';const svg=`<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120"><rect width="120" height="120" rx="34" fill="#00473c"/><circle cx="60" cy="60" r="42" fill="none" stroke="#e6ff55" stroke-width="3"/><text x="60" y="72" text-anchor="middle" font-family="Arial,sans-serif" font-size="38" font-weight="700" fill="#f4f3e7">${initials}</text></svg>`;return 'data:image/svg+xml;charset=UTF-8,'+encodeURIComponent(svg);}
 function isPlaceholder(value){return !value || /\.svg(?:[?#].*)?$/i.test(String(value)) || /default-(?:food|restaurant|logo)/i.test(String(value));}
 return {url,restaurant,logo,isPlaceholder};
})();
