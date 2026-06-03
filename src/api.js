const BASE = 'https://shoof-tv.net/api/v1';

let sessionToken = null;

export function setToken(t) { sessionToken = t; }
export function getToken() { return sessionToken; }

async function req(method, path, body = null, mobile = true) {
    const prefix = mobile ? '/mobile' : '';
    const headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
    if (sessionToken) headers['Authorization'] = `Bearer ${sessionToken}`;
    const opts = { method, headers };
    if (body) opts.body = JSON.stringify(body);
    const r = await fetch(BASE + prefix + path, opts);
    if (!r.ok) throw { status: r.status, data: await r.json().catch(() => ({})) };
    return r.json();
}

export const auth = {
    requestToken: () => req('GET', '/auth/request', null, false).then(d => {
        // Fix path - no /mobile prefix for public endpoints
        return fetch(`${BASE}/mobile/auth/request`).then(r => r.json());
    }),
    verifyToken: (token) => fetch(`${BASE}/mobile/auth/verify/${token}`, {
        headers: { 'Accept': 'application/json' }
    }).then(r => r.json()),
    logout: () => req('POST', '/auth/logout'),
};

export const api = {
    me:             () => req('GET', '/me'),
    browse:         () => req('GET', '/browse'),
    discover:       () => req('GET', '/browse/discover'),
    categories:     () => req('GET', '/browse/categories'),
    byCategory:     (k) => req('GET', `/browse/category/${k}`),
    searchWatch:    (q) => req('GET', `/browse/search-watch?q=${encodeURIComponent(q)}`),
    search:         (q) => req('GET', `/search?q=${encodeURIComponent(q)}`),
    file:           (uuid) => req('GET', `/f/${uuid}`),
    mediaUrl:       (uuid) => req('GET', `/media/${uuid}/url`),
    plans:          () => req('GET', '/platform/plans'),
    continue:       () => req('GET', '/watch/continue'),
    saveProgress:   (item_uuid, position, duration) => req('POST', '/watch/progress', { item_uuid, position, duration }),
    toggleList:     (uuid) => req('POST', `/files/${uuid}/list`),
    toggleLike:     (uuid) => req('POST', `/files/${uuid}/like`),
    myList:         () => req('GET', '/my/list'),
};
