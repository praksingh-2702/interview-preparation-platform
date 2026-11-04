import React, { useState } from 'react';
import api, { getErrorMessage, getFieldErrors } from '../services/api';
import useUsernameCheck from '../hooks/useUsernameCheck';
import Brand from './Brand';
import UsernameStatus from './UsernameStatus';

/** Shown once after a first Google sign-in: pick the (unique) name used on the leaderboard. */
const ChooseUsername = ({ initial = '', email, onDone, onLogout }) => {
    const [name, setName] = useState(initial);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [fieldError, setFieldError] = useState('');
    const check = useUsernameCheck(name);
    const blocked = ['invalid', 'taken', 'checking'].includes(check.status) || !name.trim();

    const submit = async (e) => {
        e.preventDefault();
        if (saving || blocked) return;
        setSaving(true);
        setError('');
        setFieldError('');
        try {
            const { data } = await api.put('/api/users/me/username', { username: name.trim() });
            onDone(data);
        } catch (err) {
            const fe = getFieldErrors(err).username;
            if (fe) setFieldError(fe);
            else setError(getErrorMessage(err, "Couldn't save your username."));
        } finally {
            setSaving(false);
        }
    };

    return (
        <div className="auth-page">
            <div className="auth-card">
                <Brand as="h1" className="auth-brand" />
                <h2 className="auth-step-title choose-title">Pick your username</h2>
                <p className="auth-tagline">
                    This is how you'll appear on the leaderboard. It has to be unique, and you can change it later in Settings.
                </p>
                {email && <p className="field-hint choose-email">Signed in with Google as <strong>{email}</strong></p>}
                {error && <div className="alert alert-error" role="alert">{error}</div>}
                <form onSubmit={submit}>
                    <div className="field">
                        <label className="field-label" htmlFor="choose-username">Username</label>
                        <input
                            id="choose-username"
                            type="text"
                            autoComplete="username"
                            autoFocus
                            maxLength={30}
                            value={name}
                            onChange={(e) => { setName(e.target.value); setFieldError(''); }}
                            aria-invalid={check.status === 'taken' || check.status === 'invalid' || !!fieldError}
                            aria-describedby="choose-username-status"
                        />
                        <UsernameStatus id="choose-username-status" check={check} error={fieldError}
                                        hint="3-30 characters: letters, digits, '.', '_' or '-'." />
                    </div>
                    <button type="submit" className="btn btn-primary btn-block" disabled={saving || blocked}>
                        {saving ? 'Saving…' : 'Continue'}
                    </button>
                </form>
                <button type="button" className="link-button choose-logout" onClick={onLogout}>Log out</button>
            </div>
        </div>
    );
};

export default ChooseUsername;
