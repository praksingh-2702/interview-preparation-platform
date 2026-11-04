// Session persistence. localStorage can throw (private mode, blocked storage), so every access is guarded.
const TOKEN_KEY = 'token';
const USER_KEY = 'username';
const ROLE_KEY = 'role';
const NEEDS_USERNAME_KEY = 'needsUsername';

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

/** Cached so the right menu shows instantly; the server re-checks the role on every request anyway. */
export const getRole = () => safeGet(ROLE_KEY) || 'USER';

export const getNeedsUsername = () => safeGet(NEEDS_USERNAME_KEY) === 'true';

/** Stores whichever of username / role / needsUsername are given. */
export const saveProfile = ({ username, role, needsUsername } = {}) => {
    if (username) safeSet(USER_KEY, username);
    if (role) safeSet(ROLE_KEY, role);
    if (typeof needsUsername === 'boolean') safeSet(NEEDS_USERNAME_KEY, String(needsUsername));
};

export const saveSession = (token, username, extras = {}) => {
    safeSet(TOKEN_KEY, token);
    saveProfile({ username, ...extras });
};

export const clearSession = () => {
    [TOKEN_KEY, USER_KEY, ROLE_KEY, NEEDS_USERNAME_KEY].forEach(safeRemove);
};

/** True when a token exists and is not known to be expired. */
export const hasValidSession = () => {
    const token = getToken();
    if (!token) return false;
    const exp = getTokenExpiry(token);
    return exp === null || exp > Date.now();
};
