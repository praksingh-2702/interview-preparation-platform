import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import api, { getErrorMessage } from '../services/api';
import {
    computeStats, DIFFICULTIES, DIFFICULTY_LABEL, filterQuestions, getCompanies, getTopics,
} from '../utils/stats';
import Brand from './Brand';
import StatsPanel from './StatsPanel';
import TopicSection from './TopicSection';

const Dashboard = ({ username, onLogout }) => {
    const [questions, setQuestions] = useState([]);
    const [progressMap, setProgressMap] = useState({});
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState('');
    const [toast, setToast] = useState('');
    const [saving, setSaving] = useState({});
    const [filters, setFilters] = useState({ company: '', difficulty: '' });
    const [expanded, setExpanded] = useState({});

    // Latest progress, readable from the stable toggle callback without re-rendering every row.
    const progressRef = useRef(progressMap);
    progressRef.current = progressMap;

    // Questions are fetched once; filtering happens client-side (no refetch on every dropdown change).
    const load = useCallback(async () => {
        setLoading(true);
        setLoadError('');
        try {
            const [qRes, pRes] = await Promise.all([
                api.get('/api/questions'),
                api.get('/api/progress/my-status'),
            ]);
            const qs = Array.isArray(qRes.data) ? qRes.data : [];
            setQuestions(qs);
            setProgressMap(pRes.data && typeof pRes.data === 'object' ? pRes.data : {});
            const first = getTopics(qs)[0];
            setExpanded((prev) => (Object.keys(prev).length || !first ? prev : { [first]: true }));
        } catch (err) {
            if (err.response?.status !== 401) {
                setLoadError(getErrorMessage(err, 'Could not load your problems.'));
            }
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { load(); }, [load]);

    useEffect(() => {
        if (!toast) return undefined;
        const t = setTimeout(() => setToast(''), 4000);
        return () => clearTimeout(t);
    }, [toast]);

    const handleToggleProgress = useCallback(async (questionId, field) => {
        const key = String(questionId);
        const previous = progressRef.current[key] || { solved: false, revised: false };
        const next = { solved: !!previous.solved, revised: !!previous.revised, [field]: !previous[field] };
        // Optimistic update; the checkbox stays disabled until the server answers, so no racing requests.
        setProgressMap((m) => ({ ...m, [key]: next }));
        setSaving((s) => ({ ...s, [key]: true }));

        try {
            await api.put(`/api/progress/${questionId}`, next);
        } catch (err) {
            if (err.response?.status !== 401) {
                setProgressMap((m) => ({ ...m, [key]: previous }));
                setToast(getErrorMessage(err, "Couldn't save that change. Please try again."));
            }
        } finally {
            setSaving((s) => {
                const copy = { ...s };
                delete copy[key];
                return copy;
            });
        }
    }, []);

    const toggleExpanded = useCallback((topic) => {
        setExpanded((prev) => ({ ...prev, [topic]: !prev[topic] }));
    }, []);

    const topics = useMemo(() => getTopics(questions), [questions]);
    const companies = useMemo(() => getCompanies(questions), [questions]);
    const visible = useMemo(() => filterQuestions(questions, filters), [questions, filters]);
    const stats = useMemo(() => computeStats(visible, progressMap), [visible, progressMap]);
    const filtersActive = !!(filters.company || filters.difficulty);

    const setFilter = (field) => (e) => setFilters((f) => ({ ...f, [field]: e.target.value }));
    const setAllExpanded = (value) => setExpanded(Object.fromEntries(topics.map((t) => [t, value])));

    return (
        <div className="app-shell">
            <header className="topbar">
                <div className="container topbar-inner">
                    <Brand as="h1" />
                    <div className="topbar-user">
                        {username && <span className="username">@{username}</span>}
                        <button type="button" className="btn btn-ghost" onClick={onLogout}>Log out</button>
                    </div>
                </div>
            </header>

            <main className="container main">
                {loading && <p className="loading" role="status">Loading your problems…</p>}

                {!loading && loadError && (
                    <div className="alert alert-error" role="alert">
                        {loadError}
                        <button type="button" className="btn btn-small" onClick={load}>Retry</button>
                    </div>
                )}

                {!loading && !loadError && (
                    <>
                        <StatsPanel stats={stats} filtered={filtersActive} />

                        <div className="card toolbar">
                            <label className="select">
                                <span className="sr-only">Company</span>
                                <select value={filters.company} onChange={setFilter('company')}>
                                    <option value="">All companies</option>
                                    {companies.map((c) => <option key={c} value={c}>{c}</option>)}
                                </select>
                            </label>
                            <label className="select">
                                <span className="sr-only">Difficulty</span>
                                <select value={filters.difficulty} onChange={setFilter('difficulty')}>
                                    <option value="">All difficulties</option>
                                    {DIFFICULTIES.map((d) => <option key={d} value={d}>{DIFFICULTY_LABEL[d]}</option>)}
                                </select>
                            </label>
                            {filtersActive && (
                                <button type="button" className="btn btn-ghost"
                                        onClick={() => setFilters({ company: '', difficulty: '' })}>
                                    Clear filters
                                </button>
                            )}
                            <span className="toolbar-spacer" />
                            <button type="button" className="btn btn-ghost" onClick={() => setAllExpanded(true)}>Expand all</button>
                            <button type="button" className="btn btn-ghost" onClick={() => setAllExpanded(false)}>Collapse all</button>
                        </div>

                        {visible.length === 0 ? (
                            <p className="empty">No problems match these filters.</p>
                        ) : (
                            <div className="topics">
                                {topics.map((topic) => {
                                    const topicQuestions = visible.filter((q) => q.category === topic);
                                    if (topicQuestions.length === 0) return null;
                                    return (
                                        <TopicSection
                                            key={topic}
                                            topic={topic}
                                            questions={topicQuestions}
                                            progressMap={progressMap}
                                            saving={saving}
                                            expanded={!!expanded[topic]}
                                            onToggleExpanded={toggleExpanded}
                                            onToggleProgress={handleToggleProgress}
                                        />
                                    );
                                })}
                            </div>
                        )}
                    </>
                )}
            </main>

            {toast && <div className="toast" role="alert">{toast}</div>}
        </div>
    );
};

export default Dashboard;
