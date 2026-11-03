import React, { useEffect, useRef, useState } from 'react';
import api, { getErrorMessage, getFieldErrors } from '../services/api';
import Brand from './Brand';

const SLOW_SERVER_MS = 5000;

const Login = ({ onLoginSuccess, notice = '' }) => {
    const [mode, setMode] = useState('login'); // 'login' | 'register'
    const [form, setForm] = useState({ username: '', email: '', password: '' });
    const [submitting, setSubmitting] = useState(false);
    const [slowServer, setSlowServer] = useState(false);
    const [error, setError] = useState('');
    const [fieldErrors, setFieldErrors] = useState({});
    const [success, setSuccess] = useState('');
    const slowTimer = useRef(null);

    const isRegister = mode === 'register';

    useEffect(() => () => clearTimeout(slowTimer.current), []);

    const update = (field) => (e) => {
        setForm((f) => ({ ...f, [field]: e.target.value }));
        setFieldErrors((fe) => ({ ...fe, [field]: undefined }));
    };

    const switchMode = (next) => {
        setMode(next);
        setError('');
        setFieldErrors({});
        setSuccess('');
        setForm((f) => ({ ...f, password: '' }));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (submitting) return;
        setError('');
        setFieldErrors({});
        setSuccess('');
        setSubmitting(true);
        slowTimer.current = setTimeout(() => setSlowServer(true), SLOW_SERVER_MS);

        try {
            if (isRegister) {
                await api.post('/api/auth/register', {
                    username: form.username.trim(),
                    email: form.email.trim(),
                    password: form.password,
                });
                setMode('login');
                setForm((f) => ({ ...f, password: '' }));
                setSuccess('Account created. Log in to start tracking.');
            } else {
                const { data } = await api.post('/api/auth/login', {
                    username: form.username.trim(),
                    password: form.password,
                });
                onLoginSuccess({ token: data.token, username: data.username });
                return;
            }
        } catch (err) {
            setFieldErrors(getFieldErrors(err));
            setError(getErrorMessage(err, isRegister ? 'Registration failed.' : 'Login failed.'));
        } finally {
            clearTimeout(slowTimer.current);
            setSlowServer(false);
            setSubmitting(false);
        }
    };

    return (
        <div className="auth-page">
            <div className="auth-card">
                <Brand as="h1" className="auth-brand" />
                <p className="auth-tagline">Track your way through the 100 DSA problems interviews actually ask.</p>

                <div className="tabs" role="tablist" aria-label="Account">
                    <button type="button" role="tab" aria-selected={!isRegister}
                            className={`tab ${!isRegister ? 'tab-active' : ''}`} onClick={() => switchMode('login')}>
                        Log in
                    </button>
                    <button type="button" role="tab" aria-selected={isRegister}
                            className={`tab ${isRegister ? 'tab-active' : ''}`} onClick={() => switchMode('register')}>
                        Create account
                    </button>
                </div>

                {notice && !error && !success && <div className="alert alert-warning" role="status">{notice}</div>}
                {success && <div className="alert alert-success" role="status">{success}</div>}
                {error && <div className="alert alert-error" role="alert">{error}</div>}

                <form onSubmit={handleSubmit}>
                    <label className="field">
                        <span className="field-label">Username</span>
                        <input
                            type="text"
                            autoComplete="username"
                            value={form.username}
                            onChange={update('username')}
                            required
                            minLength={isRegister ? 3 : undefined}
                            maxLength={isRegister ? 30 : 100}
                            pattern={isRegister ? '[A-Za-z0-9_.\\-]+' : undefined}
                            title={isRegister ? 'Letters, digits, ".", "_" and "-" only' : undefined}
                            aria-invalid={!!fieldErrors.username}
                        />
                        {fieldErrors.username && <span className="field-error">{fieldErrors.username}</span>}
                    </label>

                    {isRegister && (
                        <label className="field">
                            <span className="field-label">Email</span>
                            <input
                                type="email"
                                autoComplete="email"
                                value={form.email}
                                onChange={update('email')}
                                required
                                aria-invalid={!!fieldErrors.email}
                            />
                            {fieldErrors.email && <span className="field-error">{fieldErrors.email}</span>}
                        </label>
                    )}

                    <label className="field">
                        <span className="field-label">Password</span>
                        <input
                            type="password"
                            autoComplete={isRegister ? 'new-password' : 'current-password'}
                            value={form.password}
                            onChange={update('password')}
                            required
                            minLength={isRegister ? 8 : undefined}
                            maxLength={isRegister ? 72 : undefined}
                            aria-invalid={!!fieldErrors.password}
                        />
                        {isRegister && !fieldErrors.password && <span className="field-hint">At least 8 characters.</span>}
                        {fieldErrors.password && <span className="field-error">{fieldErrors.password}</span>}
                    </label>

                    <button type="submit" className="btn btn-primary btn-block" disabled={submitting}>
                        {submitting ? (isRegister ? 'Creating account…' : 'Logging in…') : (isRegister ? 'Create account' : 'Log in')}
                    </button>

                    {slowServer && (
                        <p className="field-hint slow-hint" role="status">
                            Waking up the server. The first request after a quiet spell can take up to a minute.
                        </p>
                    )}
                </form>
            </div>
        </div>
    );
};

export default Login;
