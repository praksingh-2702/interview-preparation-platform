import React, { useEffect, useRef, useState } from 'react';
import api, { getErrorCode, getErrorMessage, getFieldErrors, getRetryAfter } from '../services/api';
import Brand from './Brand';
import GoogleButton from './GoogleButton';
import UsernameStatus from './UsernameStatus';
import { clearStartMode, peekStartMode } from '../utils/startMode';
import useUsernameCheck from '../hooks/useUsernameCheck';

const SLOW_SERVER_MS = 5000;
const RESEND_COOLDOWN = 60;

/**
 * Account screens:
 *   login ⇄ register → verify (6-digit code) → login
 *   login → forgot (email) → reset (code + new password) → login
 *   Sign in with Google → straight in (new Google users then pick a username)
 */
const Login = ({ onLoginSuccess, notice = '' }) => {
    const [mode, setMode] = useState(peekStartMode); // 'register' when the landing page's Get started was clicked
    const [form, setForm] = useState({ username: '', email: '', password: '', code: '', newPassword: '' });
    const [pendingEmail, setPendingEmail] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [slowServer, setSlowServer] = useState(false);
    const [error, setError] = useState('');
    const [fieldErrors, setFieldErrors] = useState({});
    const [success, setSuccess] = useState('');
    const [cooldown, setCooldown] = useState(0);
    const slowTimer = useRef(null);
    const usernameCheck = useUsernameCheck(form.username, { enabled: mode === 'register' });

    useEffect(() => () => clearTimeout(slowTimer.current), []);

    // The landing page's choice only applies to the first screen; later visits (e.g. after logout) start at login.
    useEffect(() => { clearStartMode(); }, []);

    useEffect(() => {
        if (cooldown <= 0) return undefined;
        const t = setTimeout(() => setCooldown((c) => c - 1), 1000);
        return () => clearTimeout(t);
    }, [cooldown]);

    const update = (field) => (e) => {
        const value = field === 'code' ? e.target.value.replace(/\D/g, '').slice(0, 6) : e.target.value;
        setForm((f) => ({ ...f, [field]: value }));
        setFieldErrors((fe) => ({ ...fe, [field]: undefined }));
    };

    const goTo = (next, { keepMessages = false } = {}) => {
        setMode(next);
        setFieldErrors({});
        if (!keepMessages) {
            setError('');
            setSuccess('');
        }
        setForm((f) => ({ ...f, password: '', code: '', newPassword: '' }));
    };

    /** Runs a request with the shared loading / slow-server / error handling. */
    const run = async (fn, fallbackError) => {
        if (submitting) return;
        setError('');
        setFieldErrors({});
        setSuccess('');
        setSubmitting(true);
        slowTimer.current = setTimeout(() => setSlowServer(true), SLOW_SERVER_MS);
        try {
            await fn();
        } catch (err) {
            const retry = getRetryAfter(err);
            if (retry) setCooldown(retry);
            setFieldErrors(getFieldErrors(err));
            setError(getErrorMessage(err, fallbackError));
        } finally {
            clearTimeout(slowTimer.current);
            setSlowServer(false);
            setSubmitting(false);
        }
    };

    const handleLogin = () => run(async () => {
        try {
            const { data } = await api.post('/api/auth/login', {
                username: form.username.trim(),
                password: form.password,
            });
            onLoginSuccess(data);
        } catch (err) {
            if (getErrorCode(err) === 'EMAIL_NOT_VERIFIED') {
                setPendingEmail(err.response.data.email || err.response.data.properties?.email || '');
                goTo('verify');
                setSuccess(getErrorMessage(err));
                setCooldown(RESEND_COOLDOWN);
                return;
            }
            throw err;
        }
    }, 'Login failed.');

    const handleGoogle = (credential) => run(async () => {
        const { data } = await api.post('/api/auth/google', { credential });
        onLoginSuccess(data);
    }, 'Google sign-in failed. Please try again.');

    const handleRegister = () => run(async () => {
        const { data } = await api.post('/api/auth/register', {
            username: form.username.trim(),
            email: form.email.trim(),
            password: form.password,
        });
        setPendingEmail(data.email || form.email.trim());
        goTo('verify');
        setSuccess(data.message || 'Check your email for a 6-digit code.');
        setCooldown(RESEND_COOLDOWN);
    }, 'Registration failed.');

    const handleVerify = () => run(async () => {
        await api.post('/api/auth/verify-email', { email: pendingEmail, code: form.code });
        goTo('login');
        setSuccess('Email verified! Log in to start tracking.');
    }, 'Verification failed.');

    const handleForgot = () => run(async () => {
        const email = form.email.trim();
        const { data } = await api.post('/api/auth/forgot-password', { email });
        setPendingEmail(email);
        goTo('reset');
        setSuccess(data.message || 'If an account exists for that email, we sent a code.');
        setCooldown(RESEND_COOLDOWN);
    }, 'Could not send the reset code.');

    const handleReset = () => run(async () => {
        await api.post('/api/auth/reset-password', {
            email: pendingEmail, code: form.code, newPassword: form.newPassword,
        });
        goTo('login');
        setSuccess('Password updated. Log in with your new password.');
    }, 'Could not reset your password.');

    const handleResend = () => run(async () => {
        const url = mode === 'reset' ? '/api/auth/forgot-password' : '/api/auth/resend-verification';
        await api.post(url, { email: pendingEmail });
        setSuccess(`A new code is on its way to ${pendingEmail}.`);
        setCooldown(RESEND_COOLDOWN);
    }, 'Could not resend the code.');

    const handlers = {
        login: handleLogin, register: handleRegister, verify: handleVerify, forgot: handleForgot, reset: handleReset,
    };
    const onSubmit = (e) => {
        e.preventDefault();
        handlers[mode]();
    };

    const isTabbed = mode === 'login' || mode === 'register';
    const titles = {
        verify: 'Verify your email',
        forgot: 'Reset your password',
        reset: 'Choose a new password',
    };
    const submitLabels = {
        login: ['Log in', 'Logging in…'],
        register: ['Create account', 'Creating account…'],
        verify: ['Verify email', 'Verifying…'],
        forgot: ['Send reset code', 'Sending…'],
        reset: ['Set new password', 'Saving…'],
    };

    const codeField = (
        <div className="field">
            <label className="field-label" htmlFor="auth-code">6-digit code</label>
            <input
                id="auth-code"
                className="otp-input"
                type="text"
                inputMode="numeric"
                autoComplete="one-time-code"
                pattern="\d{6}"
                maxLength={6}
                value={form.code}
                onChange={update('code')}
                required
                aria-invalid={!!fieldErrors.code}
                aria-describedby="auth-code-help"
            />
            <span id="auth-code-help">
                {fieldErrors.code
                    ? <span className="field-error">{fieldErrors.code}</span>
                    : <span className="field-hint">Sent to <strong>{pendingEmail}</strong>. It expires in 10 minutes.</span>}
            </span>
            <button type="button" className="link-button" onClick={handleResend} disabled={cooldown > 0 || submitting}>
                {cooldown > 0 ? `Resend code in ${cooldown}s` : 'Resend code'}
            </button>
        </div>
    );

    return (
        <div className="auth-page">
            <div className="auth-card">
                <Brand as="h1" className="auth-brand" />
                <p className="auth-tagline">DSA, OOPS, OS and CN: track everything placement interviews actually ask.</p>

                {isTabbed ? (
                    <div className="tabs" role="tablist" aria-label="Account">
                        <button type="button" role="tab" aria-selected={mode === 'login'}
                                className={`tab ${mode === 'login' ? 'tab-active' : ''}`} onClick={() => goTo('login')}>
                            Log in
                        </button>
                        <button type="button" role="tab" aria-selected={mode === 'register'}
                                className={`tab ${mode === 'register' ? 'tab-active' : ''}`} onClick={() => goTo('register')}>
                            Create account
                        </button>
                    </div>
                ) : (
                    <div className="auth-step-header">
                        <button type="button" className="link-button" onClick={() => goTo('login')}>← Back to log in</button>
                        <h2 className="auth-step-title">{titles[mode]}</h2>
                    </div>
                )}

                {notice && mode === 'login' && !error && !success && (
                    <div className="alert alert-warning" role="status">{notice}</div>
                )}
                {success && <div className="alert alert-success" role="status">{success}</div>}
                {error && <div className="alert alert-error" role="alert">{error}</div>}

                <form onSubmit={onSubmit}>
                    {(mode === 'login' || mode === 'register') && (
                        <div className="field">
                            <label className="field-label" htmlFor="auth-username">
                                {mode === 'login' ? 'Username or email' : 'Username'}
                            </label>
                            <input
                                id="auth-username"
                                aria-describedby={fieldErrors.username || mode === 'register' ? 'auth-username-error' : undefined}
                                type="text"
                                autoComplete="username"
                                value={form.username}
                                onChange={update('username')}
                                required
                                minLength={mode === 'register' ? 3 : undefined}
                                maxLength={mode === 'register' ? 30 : 254}
                                pattern={mode === 'register' ? '[A-Za-z0-9_.\\-]+' : undefined}
                                title={mode === 'register' ? 'Letters, digits, ".", "_" and "-" only' : undefined}
                                aria-invalid={!!fieldErrors.username}
                            />
                            {mode === 'register'
                                ? <UsernameStatus id="auth-username-error" check={usernameCheck} error={fieldErrors.username}
                                                  hint="Shown on the leaderboard." />
                                : fieldErrors.username && <span className="field-error" id="auth-username-error">{fieldErrors.username}</span>}
                        </div>
                    )}

                    {(mode === 'register' || mode === 'forgot') && (
                        <div className="field">
                            <label className="field-label" htmlFor="auth-email">Email</label>
                            <input
                                id="auth-email"
                                aria-describedby={fieldErrors.email ? 'auth-email-error' : undefined}
                                type="email"
                                autoComplete="email"
                                value={form.email}
                                onChange={update('email')}
                                required
                                aria-invalid={!!fieldErrors.email}
                            />
                            {fieldErrors.email && <span className="field-error" id="auth-email-error">{fieldErrors.email}</span>}
                        </div>
                    )}

                    {(mode === 'login' || mode === 'register') && (
                        <div className="field">
                            <label className="field-label" htmlFor="auth-password">Password</label>
                            <input
                                id="auth-password"
                                aria-describedby="auth-password-help"
                                type="password"
                                autoComplete={mode === 'register' ? 'new-password' : 'current-password'}
                                value={form.password}
                                onChange={update('password')}
                                required
                                minLength={mode === 'register' ? 8 : undefined}
                                maxLength={mode === 'register' ? 72 : undefined}
                                aria-invalid={!!fieldErrors.password}
                            />
                            <span id="auth-password-help">
                                {mode === 'register' && !fieldErrors.password && <span className="field-hint">At least 8 characters.</span>}
                                {fieldErrors.password && <span className="field-error">{fieldErrors.password}</span>}
                            </span>
                            {mode === 'login' && (
                                <button type="button" className="link-button forgot-link" onClick={() => goTo('forgot')}>
                                    Forgot password?
                                </button>
                            )}
                        </div>
                    )}

                    {(mode === 'verify' || mode === 'reset') && codeField}

                    {mode === 'reset' && (
                        <div className="field">
                            <label className="field-label" htmlFor="auth-new-password">New password</label>
                            <input
                                id="auth-new-password"
                                type="password"
                                autoComplete="new-password"
                                value={form.newPassword}
                                onChange={update('newPassword')}
                                required
                                minLength={8}
                                maxLength={72}
                                aria-invalid={!!fieldErrors.newPassword}
                                aria-describedby="auth-new-password-help"
                            />
                            <span id="auth-new-password-help">
                                {fieldErrors.newPassword
                                    ? <span className="field-error">{fieldErrors.newPassword}</span>
                                    : <span className="field-hint">At least 8 characters. This signs you out everywhere.</span>}
                            </span>
                        </div>
                    )}

                    {mode === 'forgot' && (
                        <p className="field-hint auth-explain">Enter your account's email and we'll send you a 6-digit code.</p>
                    )}

                    <button type="submit" className="btn btn-primary btn-block"
                            disabled={submitting || (mode === 'register' && usernameCheck.status === 'taken')}>
                        {submitLabels[mode][submitting ? 1 : 0]}
                    </button>

                    {slowServer && (
                        <p className="field-hint slow-hint" role="status">
                            Waking up the server. The first request after a quiet spell can take 2–3 minutes; please keep this tab open.
                        </p>
                    )}
                </form>

                {isTabbed && (
                    <GoogleButton onCredential={handleGoogle} text={mode === 'register' ? 'signup_with' : 'continue_with'} />
                )}

                <p className="auth-legal">
                    <a href="/privacy.html">Privacy</a> · <a href="/terms.html">Terms</a>
                </p>
            </div>
        </div>
    );
};

export default Login;
