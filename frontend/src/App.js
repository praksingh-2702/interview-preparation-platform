import React, { useCallback, useEffect, useState } from 'react';
import Login from './components/Login';
import Dashboard from './components/Dashboard';
import Leaderboard from './components/Leaderboard';
import AdminPanel from './components/AdminPanel';
import ChooseUsername from './components/ChooseUsername';
import SettingsModal from './components/SettingsModal';
import TopBar from './components/TopBar';
import api, { SESSION_EXPIRED_EVENT } from './services/api';
import {
    clearSession, getNeedsUsername, getRole, getUsername, hasValidSession, saveProfile, saveSession,
} from './services/session';
import { suggestFromEmail } from './utils/usernames';

const VIEWS = ['dashboard', 'leaderboard', 'admin'];
const viewFromHash = () => {
    const v = window.location.hash.replace(/^#\/?/, '');
    return VIEWS.includes(v) ? v : 'dashboard';
};

const storedProfile = () => ({
    username: getUsername(),
    role: getRole(),
    needsUsername: getNeedsUsername(),
    email: null,
    showOnLeaderboard: true,
    googleLinked: false,
});

function App() {
    const [isAuthenticated, setIsAuthenticated] = useState(() => hasValidSession());
    const [profile, setProfile] = useState(storedProfile);
    const [notice, setNotice] = useState('');
    const [view, setView] = useState(viewFromHash);
    const [settingsOpen, setSettingsOpen] = useState(false);
    const [questionsVersion, setQuestionsVersion] = useState(0);

    // Expired token left over from a previous visit: start at the login screen.
    useEffect(() => {
        if (!hasValidSession()) clearSession();
    }, []);

    // Any 401 from the API (expired, revoked, logged in elsewhere) sends the user back to login.
    useEffect(() => {
        const onExpired = (e) => {
            setIsAuthenticated(false);
            setSettingsOpen(false);
            setNotice(e.detail?.message || 'Your session has ended. Please log in again.');
        };
        window.addEventListener(SESSION_EXPIRED_EVENT, onExpired);
        return () => window.removeEventListener(SESSION_EXPIRED_EVENT, onExpired);
    }, []);

    // Back/forward between Practice, Leaderboard and Admin.
    useEffect(() => {
        const onNav = () => setView(viewFromHash());
        window.addEventListener('hashchange', onNav);
        window.addEventListener('popstate', onNav);
        return () => {
            window.removeEventListener('hashchange', onNav);
            window.removeEventListener('popstate', onNav);
        };
    }, []);

    const updateProfile = useCallback((partial, token) => {
        setProfile((p) => {
            const next = { ...p, ...partial };
            if (token) saveSession(token, next.username, { role: next.role, needsUsername: next.needsUsername });
            else saveProfile(next);
            return next;
        });
    }, []);

    // Fresh role / username / settings from the server (the cached copy is only for a fast first paint).
    const refreshProfile = useCallback(async () => {
        try {
            const res = await api.get('/api/users/me');
            if (res?.data && typeof res.data === 'object' && res.data.username) updateProfile(res.data);
        } catch {
            // Not fatal: the cached profile keeps working, and a 401 is handled globally.
        }
    }, [updateProfile]);

    useEffect(() => {
        if (isAuthenticated) refreshProfile();
    }, [isAuthenticated, refreshProfile]);

    const handleLoginSuccess = useCallback((data) => {
        const next = { username: data.username, role: data.role || 'USER', needsUsername: !!data.needsUsername };
        saveSession(data.token, next.username, { role: next.role, needsUsername: next.needsUsername });
        setProfile((p) => ({ ...p, ...next }));
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
        setSettingsOpen(false);
        setProfile(storedProfile());
        setIsAuthenticated(false);
        setNotice('');
    }, []);

    const navigate = useCallback((next) => {
        if (next === 'dashboard') {
            window.history.pushState(null, '', window.location.pathname + window.location.search);
        } else {
            window.history.pushState(null, '', `#${next}`);
        }
        setView(next);
        window.scrollTo?.(0, 0);
    }, []);

    if (!isAuthenticated) {
        return <Login onLoginSuccess={handleLoginSuccess} notice={notice} />;
    }

    if (profile.needsUsername) {
        return (
            <ChooseUsername
                key={profile.email || 'pending'}
                initial={suggestFromEmail(profile.email)}
                email={profile.email}
                onDone={(data) => updateProfile({ username: data.username, needsUsername: false }, data.token)}
                onLogout={handleLogout}
            />
        );
    }

    const canEdit = profile.role === 'EDITOR' || profile.role === 'ADMIN';
    const current = view === 'admin' && !canEdit ? 'dashboard' : view;

    return (
        <div className="app-shell">
            <TopBar
                username={profile.username}
                role={profile.role}
                view={current}
                onNavigate={navigate}
                onOpenSettings={() => setSettingsOpen(true)}
                onLogout={handleLogout}
            />
            {/* Kept mounted so switching tabs doesn't reload the whole sheet. */}
            <div hidden={current !== 'dashboard'}>
                <Dashboard reloadKey={questionsVersion} />
            </div>
            {current === 'leaderboard' && <Leaderboard onOpenSettings={() => setSettingsOpen(true)} />}
            {current === 'admin' && (
                <AdminPanel role={profile.role} username={profile.username}
                            onQuestionsChanged={() => setQuestionsVersion((v) => v + 1)} />
            )}
            {settingsOpen && (
                <SettingsModal profile={profile} onClose={() => setSettingsOpen(false)} onProfileChange={updateProfile} />
            )}
        </div>
    );
}

export default App;
