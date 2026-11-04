import React, { useCallback, useEffect, useMemo, useState } from 'react';
import api, { getErrorMessage } from '../services/api';
import { SECTIONS, sectionOf } from '../utils/sections';
import { DIFFICULTY_LABEL } from '../utils/stats';
import QuestionForm from './QuestionForm';

const ROLES = ['USER', 'EDITOR', 'ADMIN'];
const ROLE_LABEL = { USER: 'User', EDITOR: 'Editor', ADMIN: 'Admin' };

const formatDate = (iso) => {
    if (!iso) return '–';
    const d = new Date(iso);
    return Number.isNaN(d.getTime()) ? '–' : d.toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
};

// ---------------------------------------------------------------- overview (admin)

const Overview = () => {
    const [data, setData] = useState(null);
    const [error, setError] = useState('');
    useEffect(() => {
        api.get('/api/admin/overview')
            .then((res) => setData(res.data))
            .catch((err) => setError(getErrorMessage(err, "Couldn't load the overview.")));
    }, []);
    if (error) return <div className="alert alert-error" role="alert">{error}</div>;
    if (!data) return <p className="loading">Loading…</p>;
    const tiles = [
        ['Users', data.totalUsers],
        ['Verified', data.verifiedUsers],
        ['Signed up with Google', data.googleUsers],
        ['New in the last 7 days', data.newUsersLast7Days],
        ['Active in the last 7 days', data.activeUsersLast7Days],
    ];
    return (
        <>
            <div className="tile-grid">
                {tiles.map(([label, value]) => (
                    <div key={label} className="card tile"><span className="tile-value">{value}</span><span className="tile-label">{label}</span></div>
                ))}
            </div>
            <div className="tile-row">
                <div className="card tile-list">
                    <h3 className="section-title">Questions</h3>
                    <ul>{Object.entries(data.questionsBySection || {}).map(([k, v]) => <li key={k}><span>{k}</span><strong>{v}</strong></li>)}</ul>
                </div>
                <div className="card tile-list">
                    <h3 className="section-title">Roles</h3>
                    <ul>{Object.entries(data.usersByRole || {}).map(([k, v]) => <li key={k}><span>{ROLE_LABEL[k] || k}</span><strong>{v}</strong></li>)}</ul>
                </div>
            </div>
        </>
    );
};

// ---------------------------------------------------------------- questions (editor + admin)

const Questions = ({ onChanged }) => {
    const [questions, setQuestions] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [section, setSection] = useState('DSA');
    const [search, setSearch] = useState('');
    const [editing, setEditing] = useState(null); // null | 'new' | question
    const [confirmDelete, setConfirmDelete] = useState(null);
    const [busy, setBusy] = useState(false);
    const [notice, setNotice] = useState('');

    const load = useCallback(async () => {
        setLoading(true);
        try {
            const { data } = await api.get('/api/questions');
            setQuestions(Array.isArray(data) ? data : []);
            setError('');
        } catch (err) {
            setError(getErrorMessage(err, "Couldn't load questions."));
        } finally {
            setLoading(false);
        }
    }, []);
    useEffect(() => { load(); }, [load]);

    const categories = useMemo(() => {
        const map = {};
        questions.forEach((q) => {
            const s = sectionOf(q);
            map[s] = map[s] || [];
            if (!map[s].includes(q.category)) map[s].push(q.category);
        });
        return map;
    }, [questions]);

    const shown = useMemo(() => {
        const term = search.trim().toLowerCase();
        return questions.filter((q) => sectionOf(q) === section
            && (!term || `${q.title} ${q.category}`.toLowerCase().includes(term)));
    }, [questions, section, search]);

    const saved = (q) => {
        setEditing(null);
        setNotice(`Saved "${q.title}".`);
        if (sectionOf(q) !== section) setSection(sectionOf(q));
        load();
        onChanged();
    };

    const remove = async (q) => {
        setBusy(true);
        try {
            await api.delete(`/api/questions/${q.id}`);
            setNotice(`Deleted "${q.title}".`);
            setConfirmDelete(null);
            load();
            onChanged();
        } catch (err) {
            setError(getErrorMessage(err, "Couldn't delete that question."));
        } finally {
            setBusy(false);
        }
    };

    return (
        <>
            <div className="card toolbar">
                <label className="select">
                    <span className="sr-only">Section</span>
                    <select value={section} onChange={(e) => setSection(e.target.value)}>
                        {SECTIONS.map((s) => <option key={s.id} value={s.id}>{s.label}</option>)}
                    </select>
                </label>
                <label className="search">
                    <span className="sr-only">Search questions</span>
                    <input type="search" placeholder="Search titles and topics…" value={search} onChange={(e) => setSearch(e.target.value)} />
                </label>
                <span className="toolbar-spacer" />
                <button type="button" className="btn btn-primary" onClick={() => setEditing('new')}>+ Add question</button>
            </div>
            {notice && <div className="alert alert-success" role="status">{notice}</div>}
            {error && <div className="alert alert-error" role="alert">{error}</div>}
            {loading ? <p className="loading">Loading…</p> : (
                <div className="card">
                    <p className="admin-count">{shown.length} in {section}</p>
                    <ul className="admin-list">
                        {shown.map((q) => (
                            <li key={q.id} className="admin-item">
                                <div className="admin-item-main">
                                    <a href={q.problemUrl} target="_blank" rel="noopener noreferrer" className="question-title">{q.title}</a>
                                    <span className="admin-item-meta">
                                        {q.category}
                                        <span className={`pill pill-${q.difficulty.toLowerCase()}`}>{DIFFICULTY_LABEL[q.difficulty]}</span>
                                    </span>
                                </div>
                                {confirmDelete === q.id ? (
                                    <div className="admin-actions">
                                        <span className="field-hint">Delete for everyone?</span>
                                        <button type="button" className="btn btn-small btn-danger" disabled={busy} onClick={() => remove(q)}>Delete</button>
                                        <button type="button" className="btn btn-small btn-ghost" onClick={() => setConfirmDelete(null)}>Cancel</button>
                                    </div>
                                ) : (
                                    <div className="admin-actions">
                                        <button type="button" className="btn btn-small" onClick={() => setEditing(q)}
                                                aria-label={`Edit ${q.title}`}>Edit</button>
                                        <button type="button" className="btn btn-small btn-ghost btn-danger" onClick={() => setConfirmDelete(q.id)}
                                                aria-label={`Delete ${q.title}`}>Delete</button>
                                    </div>
                                )}
                            </li>
                        ))}
                    </ul>
                </div>
            )}
            {editing && (
                <QuestionForm
                    question={editing === 'new' ? null : editing}
                    defaultSection={section}
                    categories={categories}
                    onSaved={saved}
                    onClose={() => setEditing(null)}
                />
            )}
        </>
    );
};

// ---------------------------------------------------------------- users (admin)

const Users = ({ currentUsername }) => {
    const [users, setUsers] = useState([]);
    const [query, setQuery] = useState('');
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [saving, setSaving] = useState({});

    useEffect(() => {
        let cancelled = false;
        const t = setTimeout(async () => {
            setLoading(true);
            try {
                const { data } = await api.get('/api/admin/users', { params: query.trim() ? { q: query.trim() } : {} });
                if (!cancelled) { setUsers(Array.isArray(data) ? data : []); setError(''); }
            } catch (err) {
                if (!cancelled) setError(getErrorMessage(err, "Couldn't load users."));
            } finally {
                if (!cancelled) setLoading(false);
            }
        }, 300);
        return () => { cancelled = true; clearTimeout(t); };
    }, [query]);

    const changeRole = async (user, role) => {
        setSaving((s) => ({ ...s, [user.id]: true }));
        setError('');
        try {
            const { data } = await api.put(`/api/admin/users/${user.id}/role`, { role });
            setUsers((list) => list.map((u) => (u.id === user.id ? data : u)));
        } catch (err) {
            setError(getErrorMessage(err, "Couldn't change that role."));
        } finally {
            setSaving((s) => { const c = { ...s }; delete c[user.id]; return c; });
        }
    };

    return (
        <>
            <div className="card toolbar">
                <label className="search">
                    <span className="sr-only">Search users</span>
                    <input type="search" placeholder="Search username or email…" value={query} onChange={(e) => setQuery(e.target.value)} />
                </label>
                <span className="field-hint">Editors can add, edit and delete questions. Admins can also manage users.</span>
            </div>
            {error && <div className="alert alert-error" role="alert">{error}</div>}
            <div className="card board-table-wrap">
                <table className="board-table admin-users">
                    <thead>
                        <tr>
                            <th scope="col">User</th>
                            <th scope="col">Email</th>
                            <th scope="col">Role</th>
                            <th scope="col" className="num">Done</th>
                            <th scope="col" className="num">Points</th>
                            <th scope="col">Joined</th>
                        </tr>
                    </thead>
                    <tbody>
                        {users.map((u) => {
                            const self = u.username === currentUsername;
                            return (
                                <tr key={u.id}>
                                    <td>
                                        @{u.username}
                                        {!u.verified && <span className="pill pill-muted">unverified</span>}
                                        {u.google && <span className="pill pill-muted">Google</span>}
                                        {!u.showOnLeaderboard && <span className="pill pill-muted">hidden</span>}
                                    </td>
                                    <td className="cell-email">{u.email}</td>
                                    <td>
                                        <select aria-label={`Role for ${u.username}`} className="input-select input-small" value={u.role}
                                                disabled={self || !!saving[u.id]}
                                                title={self ? "You can't change your own role" : undefined}
                                                onChange={(e) => changeRole(u, e.target.value)}>
                                            {ROLES.map((r) => <option key={r} value={r}>{ROLE_LABEL[r]}</option>)}
                                        </select>
                                    </td>
                                    <td className="num">{u.completed}</td>
                                    <td className="num">{u.points}</td>
                                    <td>{formatDate(u.createdAt)}</td>
                                </tr>
                            );
                        })}
                    </tbody>
                </table>
                {!loading && users.length === 0 && <p className="empty">No users match.</p>}
                {loading && users.length === 0 && <p className="loading">Loading…</p>}
            </div>
        </>
    );
};

// ---------------------------------------------------------------- panel

const AdminPanel = ({ role, username, onQuestionsChanged }) => {
    const tabs = role === 'ADMIN'
        ? [['overview', 'Overview'], ['questions', 'Questions'], ['users', 'Users']]
        : [['questions', 'Questions']];
    const [tab, setTab] = useState(tabs[0][0]);

    return (
        <main className="container main">
            <div className="admin-head">
                <h2 className="page-title">{role === 'ADMIN' ? 'Admin' : 'Editor'}</h2>
                <div className="tabs admin-tabs" role="tablist" aria-label="Admin sections">
                    {tabs.map(([id, label]) => (
                        <button key={id} type="button" role="tab" aria-selected={tab === id}
                                className={`tab ${tab === id ? 'tab-active' : ''}`} onClick={() => setTab(id)}>
                            {label}
                        </button>
                    ))}
                </div>
            </div>
            {tab === 'overview' && <Overview />}
            {tab === 'questions' && <Questions onChanged={onQuestionsChanged} />}
            {tab === 'users' && <Users currentUsername={username} />}
        </main>
    );
};

export default AdminPanel;
