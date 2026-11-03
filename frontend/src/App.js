import React, { useCallback, useEffect, useState } from 'react';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import api, { SESSION_EXPIRED_EVENT } from './services/api';
import { clearSession, getUsername, hasValidSession, saveSession } from './services/session';

function App() {
    const [isAuthenticated, setIsAuthenticated] = useState(() => hasValidSession());
    const [username, setUsername] = useState(() => getUsername());
    const [notice, setNotice] = useState('');

    // Expired token left over from a previous visit: start at the login screen.
    useEffect(() => {
        if (!hasValidSession()) clearSession();
    }, []);

    // Any 401 from the API (expired, revoked, logged in elsewhere) sends the user back to login.
    useEffect(() => {
        const onExpired = (e) => {
            setIsAuthenticated(false);
            setNotice(e.detail?.message || 'Your session has ended. Please log in again.');
        };
        window.addEventListener(SESSION_EXPIRED_EVENT, onExpired);
        return () => window.removeEventListener(SESSION_EXPIRED_EVENT, onExpired);
    }, []);

    const handleLoginSuccess = useCallback(({ token, username: name }) => {
        saveSession(token, name);
        setUsername(name);
        setNotice('');
        setIsAuthenticated(true);
    }, []);

    const handleLogout = useCallback(async () => {
        try {
            await api.post('/api/auth/logout'); // revokes the token server-side
        } catch {
            // Logging out locally is enough if the server can't be reached.
        }
        clearSession();
        setIsAuthenticated(false);
        setNotice('');
    }, []);

    return isAuthenticated
        ? <Dashboard username={username} onLogout={handleLogout} />
        : <Login onLoginSuccess={handleLoginSuccess} notice={notice} />;
}

export default App;
