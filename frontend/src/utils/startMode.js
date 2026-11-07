// Remembers which account screen the landing page asked for ('register' or 'login'),
// so <Login /> can open on it. Lives in memory only; <Login /> clears it once it has mounted.
let startMode = 'login';

export const setStartMode = (mode) => { startMode = mode; };
export const peekStartMode = () => startMode;
export const clearStartMode = () => { startMode = 'login'; };
