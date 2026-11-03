// Session persistence. localStorage can throw (private mode, blocked storage), so every access is guarded.
const TOKEN_KEY = 'token';
const USER_KEY = 'username';

const safeGet = (key) => {
    try { return window.localStorage.getItem(key); } catch { return null; }
};
const safeSet = (key, value) => {
    try { window.localStorage.setItem(key, value); } catch { /* ignore */ }
};
const safeRemove = (key) => {
    try { window.localStorage.removeItem(key); } catch { /* ignore */ }
};

/** Reads the `exp` claim (seconds) from a JWT without verifying it. Returns ms, or null if unknown. */
export const getTokenExpiry = (token) => {
    try {
        const payload = token.split('.')[1];
        const json = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
        return typeof json.exp === 'number' ? json.exp * 1000 : null;
    } catch {
        return null;
    }
};

export const getToken = () => safeGet(TOKEN_KEY);

export const getUsername = () => safeGet(USER_KEY);

export const saveSession = (token, username) => {
    safeSet(TOKEN_KEY, token);
    if (username) safeSet(USER_KEY, username);
};

export const clearSession = () => {
    safeRemove(TOKEN_KEY);
    safeRemove(USER_KEY);
};

/** True when a token exists and is not known to be expired. */
export const hasValidSession = () => {
    const token = getToken();
    if (!token) return false;
    const exp = getTokenExpiry(token);
    return exp === null || exp > Date.now();
};
