import axios from 'axios';
import { clearSession, getToken } from './session';

export const SESSION_EXPIRED_EVENT = 'freakcode:session-expired';

const api = axios.create({
    baseURL: process.env.REACT_APP_API_URL || 'http://localhost:8080',
    timeout: 180000, // a sleeping Render free instance needs ~2.5 min to boot the JVM
});

// Public auth endpoints (login, register, verify, reset...). Logout is excluded: it needs the token.
const isAuthEndpoint = (url = '') => url.includes('/api/auth/') && !url.includes('/api/auth/logout');

// Attach the JWT to every request except the public auth endpoints (a stale token there is pointless).
api.interceptors.request.use((config) => {
    const token = getToken();
    if (token && !isAuthEndpoint(config.url)) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// A 401 on a protected endpoint means the session is gone (expired, revoked, logged in elsewhere).
api.interceptors.response.use(
    (response) => response,
    (error) => {
        const status = error.response?.status;
        if (status === 401 && !isAuthEndpoint(error.config?.url)) {
            clearSession();
            window.dispatchEvent(new CustomEvent(SESSION_EXPIRED_EVENT, {
                detail: { message: getErrorMessage(error) },
            }));
        }
        return Promise.reject(error);
    }
);

/** Turns any axios error into a human-readable sentence. */
export const getErrorMessage = (error, fallback = 'Something went wrong. Please try again.') => {
    if (!error) return fallback;
    if (error.code === 'ECONNABORTED') return 'The server took too long to respond. Please try again.';
    if (!error.response) return 'Cannot reach the server. Check your connection and try again.';
    const data = error.response.data;
    if (typeof data === 'string' && data.trim()) return data;
    if (data && typeof data === 'object') {
        return data.detail || data.message || data.properties?.detail || fallback;
    }
    return fallback;
};

/** Stable machine-readable error code from the API (e.g. EMAIL_NOT_VERIFIED), if any. */
export const getErrorCode = (error) => {
    const data = error?.response?.data;
    return (data && typeof data === 'object' && (data.code || data.properties?.code)) || null;
};

/** Seconds from a 429's Retry-After header, or null. */
export const getRetryAfter = (error) => {
    const raw = error?.response?.headers?.['retry-after'];
    const n = Number.parseInt(raw, 10);
    return Number.isFinite(n) && n > 0 ? n : null;
};

/** Field-level validation errors from a 400 response, e.g. { password: "..." }. */
export const getFieldErrors = (error) => {
    const data = error?.response?.data;
    return (data && typeof data === 'object' && (data.errors || data.properties?.errors)) || {};
};

export default api;
