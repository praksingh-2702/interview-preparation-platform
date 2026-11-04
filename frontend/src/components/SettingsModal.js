import React, { useEffect, useState } from 'react';
import api, { getErrorMessage, getFieldErrors } from '../services/api';
import useUsernameCheck from '../hooks/useUsernameCheck';
import UsernameStatus from './UsernameStatus';

const ROLE_LABEL = { USER: 'User', EDITOR: 'Editor', ADMIN: 'Admin' };

/** Username, leaderboard visibility and account info. */
const SettingsModal = ({ profile, onClose, onProfileChange }) => {
    const [name, setName] = useState(profile.username || '');
    const [savingName, setSavingName] = useState(false);
    const [savingVisibility, setSavingVisibility] = useState(false);
    const [error, setError] = useState('');
    const [fieldError, setFieldError] = useState('');
    const [saved, setSaved] = useState('');
    const check = useUsernameCheck(name, { current: profile.username });
    const nameChanged = name.trim() && name.trim() !== profile.username;
    const canSaveName = nameChanged && !['invalid', 'taken', 'checking'].includes(check.status);

    useEffect(() => {
        const onKey = (e) => { if (e.key === 'Escape') onClose(); };
        window.addEventListener('keydown', onKey);
        return () => window.removeEventListener('keydown', onKey);
    }, [onClose]);

    const saveName = async (e) => {
        e.preventDefault();
        if (!canSaveName || savingName) return;
        setSavingName(true);
        setError('');
        setFieldError('');
        setSaved('');
        try {
            const { data } = await api.put('/api/users/me/username', { username: name.trim() });
            onProfileChange({ username: data.username, needsUsername: false }, data.token);
            setSaved('Username updated.');
        } catch (err) {
            const fe = getFieldErrors(err).username;
            if (fe) setFieldError(fe);
            else setError(getErrorMessage(err, "Couldn't change your username."));
        } finally {
            setSavingName(false);
        }
    };

    const toggleVisibility = async (e) => {
        const show = e.target.checked;
        setSavingVisibility(true);
        setError('');
        setSaved('');
        try {
            const { data } = await api.put('/api/users/me/preferences', { showOnLeaderboard: show });
            onProfileChange({ showOnLeaderboard: !!data.showOnLeaderboard });
            setSaved(show ? "You're on the leaderboard." : "You're hidden from the leaderboard.");
        } catch (err) {
            setError(getErrorMessage(err, "Couldn't save that setting."));
        } finally {
            setSavingVisibility(false);
        }
    };

    return (
        <div className="modal-backdrop" onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}>
            <div className="modal card" role="dialog" aria-modal="true" aria-labelledby="settings-title">
                <div className="modal-header">
                    <h2 id="settings-title" className="section-title">Settings</h2>
                    <button type="button" className="btn btn-ghost btn-small" onClick={onClose} aria-label="Close">✕</button>
                </div>

                {error && <div className="alert alert-error" role="alert">{error}</div>}
                {saved && <div className="alert alert-success" role="status">{saved}</div>}

                <form onSubmit={saveName} className="settings-row">
                    <div className="field settings-field">
                        <label className="field-label" htmlFor="settings-username">Username</label>
                        <input
                            id="settings-username"
                            type="text"
                            maxLength={30}
                            value={name}
                            onChange={(e) => { setName(e.target.value); setFieldError(''); }}
                            aria-invalid={check.status === 'taken' || check.status === 'invalid' || !!fieldError}
                            aria-describedby="settings-username-status"
                        />
                        <UsernameStatus id="settings-username-status" check={check} error={fieldError}
                                        hint="Unique. Shown on the leaderboard." />
                    </div>
                    <button type="submit" className="btn btn-primary" disabled={!canSaveName || savingName}>
                        {savingName ? 'Saving…' : 'Save'}
                    </button>
                </form>

                <label className="switch-row">
                    <input type="checkbox" checked={profile.showOnLeaderboard !== false} disabled={savingVisibility}
                           onChange={toggleVisibility} />
                    <span>
                        <strong>Show me on the leaderboard</strong>
                        <span className="field-hint">Turn off to hide your name and points from everyone else.</span>
                    </span>
                </label>

                <dl className="account-info">
                    {profile.email && (<><dt>Email</dt><dd>{profile.email}</dd></>)}
                    <dt>Role</dt><dd>{ROLE_LABEL[profile.role] || profile.role}</dd>
                    <dt>Google</dt><dd>{profile.googleLinked ? 'Linked' : 'Not linked'}</dd>
                </dl>
            </div>
        </div>
    );
};

export default SettingsModal;
