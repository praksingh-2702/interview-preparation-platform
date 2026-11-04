import React, { useEffect, useRef, useState } from 'react';

const SCRIPT_SRC = 'https://accounts.google.com/gsi/client';
let scriptPromise = null;

/** Read at call time (not import time) so tests can switch it on. Set REACT_APP_GOOGLE_CLIENT_ID on Vercel. */
export const googleClientId = () => process.env.REACT_APP_GOOGLE_CLIENT_ID || '';

const loadScript = () => {
    if (window.google?.accounts?.id) return Promise.resolve();
    if (!scriptPromise) {
        scriptPromise = new Promise((resolve, reject) => {
            const s = document.createElement('script');
            s.src = SCRIPT_SRC;
            s.async = true;
            s.defer = true;
            s.onload = resolve;
            s.onerror = () => {
                scriptPromise = null;
                reject(new Error('Google script failed to load'));
            };
            document.head.appendChild(s);
        });
    }
    return scriptPromise;
};

/**
 * Google's own "Sign in with Google" button (Google Identity Services). Hands the ID token
 * (credential) to onCredential; the backend verifies it. Renders nothing when no client id is set.
 */
const GoogleButton = ({ onCredential, text = 'continue_with' }) => {
    const ref = useRef(null);
    const callback = useRef(onCredential);
    callback.current = onCredential;
    const [failed, setFailed] = useState(false);
    const clientId = googleClientId();

    useEffect(() => {
        if (!clientId) return undefined;
        let active = true;
        loadScript()
            .then(() => {
                if (!active || !ref.current || !window.google?.accounts?.id) return;
                window.google.accounts.id.initialize({
                    client_id: clientId,
                    callback: (response) => callback.current(response.credential),
                    ux_mode: 'popup',
                });
                window.google.accounts.id.renderButton(ref.current, {
                    theme: 'filled_black',
                    size: 'large',
                    shape: 'pill',
                    text,
                    width: Math.max(200, Math.min(380, ref.current.offsetWidth || 320)),
                });
            })
            .catch(() => { if (active) setFailed(true); });
        return () => { active = false; };
    }, [clientId, text]);

    if (!clientId) return null;
    return (
        <div className="google-signin">
            <div className="divider"><span>or</span></div>
            {failed
                ? <p className="field-hint">Couldn't load Google sign-in. Check your connection, or use your password.</p>
                : <div ref={ref} className="google-button" data-testid="google-button" />}
        </div>
    );
};

export default GoogleButton;
