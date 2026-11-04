// Same rules as the backend (service/Usernames.java), so most problems show before any request.
export const USERNAME_MIN = 3;
export const USERNAME_MAX = 30;
const ALLOWED = /^[A-Za-z0-9_.-]+$/;
const PLACEHOLDER_PREFIX = 'google-';

export const usernameProblem = (raw) => {
    const name = (raw || '').trim();
    if (name.length < USERNAME_MIN || name.length > USERNAME_MAX) {
        return `Username must be ${USERNAME_MIN}-${USERNAME_MAX} characters.`;
    }
    if (!ALLOWED.test(name)) return "Use only letters, digits, '.', '_' and '-'.";
    if (name.toLowerCase().startsWith(PLACEHOLDER_PREFIX)) return `Usernames can't start with '${PLACEHOLDER_PREFIX}'.`;
    return null;
};

/** "Om.Sanjeet+x@gmail.com" -> "om.sanjeet" */
export const suggestFromEmail = (email) => {
    let local = (email || '').split('@')[0].split('+')[0].toLowerCase().replace(/[^a-z0-9_.-]/g, '');
    local = local.slice(0, USERNAME_MAX);
    while (local && local.length < USERNAME_MIN) local += '_';
    return local;
};
