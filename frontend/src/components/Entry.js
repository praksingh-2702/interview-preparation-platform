import React, { useState } from 'react';
import App from '../App';
import Landing from './Landing';
import { hasValidSession } from '../services/session';
import { setStartMode } from '../utils/startMode';

// The landing page is shown once per browser tab, and never to someone who is already logged in.
// Everything after it is the unchanged <App />. sessionStorage can throw (private mode), so it is guarded.
const LANDING_SEEN_KEY = 'freakcode:landing-seen';
const landingSeen = () => {
    try { return window.sessionStorage.getItem(LANDING_SEEN_KEY) === '1'; } catch { return false; }
};
const markLandingSeen = () => {
    try { window.sessionStorage.setItem(LANDING_SEEN_KEY, '1'); } catch { /* ignore */ }
};

const Entry = () => {
    const [onLanding, setOnLanding] = useState(() => !hasValidSession() && !landingSeen());

    if (!onLanding) return <App />;

    const enter = (mode) => {
        setStartMode(mode);
        markLandingSeen();
        setOnLanding(false);
    };
    return <Landing onGetStarted={() => enter('register')} onLogin={() => enter('login')} />;
};

export default Entry;
