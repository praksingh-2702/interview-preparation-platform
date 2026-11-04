import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import api, { getErrorMessage } from '../services/api';
import {
    applyListFilters, computeStats, DIFFICULTIES, DIFFICULTY_LABEL, filterQuestions, getCompanies, getTopics,
    STATUS_FILTERS,
} from '../utils/stats';
import { browserTimeZone, isDue, parseTime } from '../utils/time';
import ActivityPanel from './ActivityPanel';
import Brand from './Brand';
import NotesModal from './NotesModal';
import RevisionQueue from './RevisionQueue';
import StatsPanel from './StatsPanel';
import TopicSection from './TopicSection';

const EMPTY_FILTERS = { company: '', difficulty: '', search: '', status: '' };

const Dashboard = ({ username, onLogout }) => {
    const [questions, setQuestions] = useState([]);
    const [progressMap, setProgressMap] = useState({});
    const [notes, setNotes] = useState({});
    const [activity, setActivity] = useState(null);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState('');
    const [toast, setToast] = useState('');
    const [saving, setSaving] = useState({});
    const [reviewing, setReviewing] = useState({});
    const [filters, setFilters] = useState(EMPTY_FILTERS);
    const [expanded, setExpanded] = useState({});
    const [notesFor, setNotesFor] = useState(null); // question id whose notes dialog is open
    const [notesSaving, setNotesSaving] = useState(false);
    const [notesError, setNotesError] = useState('');
    const [now, setNow] = useState(() => Date.now());
    const [slowLoad, setSlowLoad] = useState(false);

    // Latest progress, readable from the stable callbacks without re-rendering every row.
    const progressRef = useRef(progressMap);
    progressRef.current = progressMap;

    const refreshStats = useCallback(async () => {
        try {
            const { data } = await api.get('/api/stats', { params: { tz: browserTimeZone() } });
            setActivity(data);
        } catch {
            // Optional panel: the rest of the dashboard works without it.
        }
    }, []);

    // Questions + progress are required; notes and stats are optional extras.
    const load = useCallback(async () => {
        setLoading(true);
        setLoadError('');
        setSlowLoad(false);
        const slowTimer = setTimeout(() => setSlowLoad(true), 5000);
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
            setNow(Date.now());
        } catch (err) {
            if (err.response?.status !== 401) {
                setLoadError(getErrorMessage(err, 'Could not load your problems.'));
            }
        } finally {
            clearTimeout(slowTimer);
            setLoading(false);
        }
        api.get('/api/notes')
            .then(({ data }) => setNotes(data && typeof data === 'object' ? data : {}))
            .catch(() => {});
        refreshStats();
    }, [refreshStats]);

    useEffect(() => { load(); }, [load]);

    // Keep "due" badges honest if the tab stays open for a long time.
    useEffect(() => {
        const t = setInterval(() => setNow(Date.now()), 60000);
        return () => clearInterval(t);
    }, []);

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
        setProgressMap((m) => ({ ...m, [key]: { ...previous, ...next } }));
        setSaving((s) => ({ ...s, [key]: true }));

        try {
            const res = await api.put(`/api/progress/${questionId}`, next);
            if (res?.data && typeof res.data === 'object') {
                setProgressMap((m) => ({ ...m, [key]: { ...m[key], ...res.data } }));
            }
            refreshStats();
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
    }, [refreshStats]);

    const handleReview = useCallback(async (questionId, result) => {
        const key = String(questionId);
        setReviewing((r) => ({ ...r, [key]: true }));
        try {
            const { data } = await api.post(`/api/progress/${questionId}/review`, { result });
            setProgressMap((m) => ({ ...m, [key]: { ...m[key], ...data } }));
            refreshStats();
        } catch (err) {
            if (err.response?.status !== 401) {
                setToast(getErrorMessage(err, "Couldn't record that review. Please try again."));
            }
        } finally {
            setReviewing((r) => {
                const copy = { ...r };
                delete copy[key];
                return copy;
            });
        }
    }, [refreshStats]);

    const openNotes = useCallback((questionId) => {
        setNotesError('');
        setNotesFor(questionId);
    }, []);
    const closeNotes = useCallback(() => setNotesFor(null), []);

    const saveNotes = async ({ content, solutionUrl }) => {
        const key = String(notesFor);
        setNotesSaving(true);
        setNotesError('');
        try {
            const res = await api.put(`/api/notes/${notesFor}`, { content, solutionUrl });
            setNotes((n) => {
                const copy = { ...n };
                if (res.status === 204 || !res.data) delete copy[key];
                else copy[key] = res.data;
                return copy;
            });
            setNotesFor(null);
        } catch (err) {
            if (err.response?.status !== 401) {
                setNotesError(getErrorMessage(err, "Couldn't save your notes."));
            }
        } finally {
            setNotesSaving(false);
        }
    };

    const deleteNotes = async () => {
        const key = String(notesFor);
        setNotesSaving(true);
        try {
            await api.delete(`/api/notes/${notesFor}`);
            setNotes((n) => {
                const copy = { ...n };
                delete copy[key];
                return copy;
            });
            setNotesFor(null);
        } catch (err) {
            if (err.response?.status !== 401) setNotesError(getErrorMessage(err, "Couldn't delete your notes."));
        } finally {
            setNotesSaving(false);
        }
    };

    const toggleExpanded = useCallback((topic) => {
        setExpanded((prev) => ({ ...prev, [topic]: !prev[topic] }));
    }, []);

    const topics = useMemo(() => getTopics(questions), [questions]);
    const companies = useMemo(() => getCompanies(questions), [questions]);
    const scoped = useMemo(() => filterQuestions(questions, filters), [questions, filters]);
    const visible = useMemo(
        () => applyListFilters(scoped, filters, { progressMap, notes, now }),
        [scoped, filters, progressMap, notes, now]
    );
    const stats = useMemo(() => computeStats(scoped, progressMap), [scoped, progressMap]);
    const dueQuestions = useMemo(
        () => questions
            .filter((q) => isDue(progressMap[String(q.id)], now))
            .sort((a, b) => parseTime(progressMap[String(a.id)].nextReviewAt) - parseTime(progressMap[String(b.id)].nextReviewAt)),
        [questions, progressMap, now]
    );

    const scopeActive = !!(filters.company || filters.difficulty);
    const listActive = !!(filters.search.trim() || filters.status);
    const anyFilter = scopeActive || listActive;

    const setFilter = (field) => (e) => setFilters((f) => ({ ...f, [field]: e.target.value }));
    const setAllExpanded = (value) => setExpanded(Object.fromEntries(topics.map((t) => [t, value])));
    const notesQuestion = notesFor !== null ? questions.find((q) => q.id === notesFor) : null;

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
                {loading && (
                    <div className="loading" role="status">
                        <p>Loading your problems…</p>
                        {slowLoad && (
                            <p className="field-hint">
                                The server is waking up after being idle. This can take up to 2–3 minutes; please keep this tab open.
                            </p>
                        )}
                    </div>
                )}

                {!loading && loadError && (
                    <div className="alert alert-error" role="alert">
                        {loadError}
                        <button type="button" className="btn btn-small" onClick={load}>Retry</button>
                    </div>
                )}

                {!loading && !loadError && (
                    <>
                        <StatsPanel stats={stats} filtered={scopeActive} />

                        <div className="today-grid">
                            <RevisionQueue
                                dueQuestions={dueQuestions}
                                progressMap={progressMap}
                                reviewing={reviewing}
                                onReview={handleReview}
                                onShowAll={() => setFilters((f) => ({ ...f, status: 'due' }))}
                            />
                            {activity && <ActivityPanel stats={activity} />}
                        </div>

                        <div className="card toolbar">
                            <label className="search">
                                <span className="sr-only">Search problems</span>
                                <input type="search" placeholder="Search problems, topics, companies…"
                                       value={filters.search} onChange={setFilter('search')} />
                            </label>
                            <label className="select">
                                <span className="sr-only">Status</span>
                                <select value={filters.status} onChange={setFilter('status')}>
                                    {STATUS_FILTERS.map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
                                </select>
                            </label>
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
                            {anyFilter && (
                                <button type="button" className="btn btn-ghost" onClick={() => setFilters(EMPTY_FILTERS)}>
                                    Clear
                                </button>
                            )}
                            <span className="toolbar-spacer" />
                            <button type="button" className="btn btn-ghost" onClick={() => setAllExpanded(true)}>Expand all</button>
                            <button type="button" className="btn btn-ghost" onClick={() => setAllExpanded(false)}>Collapse all</button>
                        </div>

                        {listActive && visible.length > 0 && (
                            <p className="result-count">{visible.length} {visible.length === 1 ? 'problem' : 'problems'} match</p>
                        )}

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
                                            notes={notes}
                                            saving={saving}
                                            now={now}
                                            // While searching or filtering by status, show every match.
                                            expanded={listActive || !!expanded[topic]}
                                            onToggleExpanded={toggleExpanded}
                                            onToggleProgress={handleToggleProgress}
                                            onOpenNotes={openNotes}
                                        />
                                    );
                                })}
                            </div>
                        )}
                    </>
                )}
            </main>

            {notesQuestion && (
                <NotesModal
                    question={notesQuestion}
                    note={notes[String(notesQuestion.id)]}
                    saving={notesSaving}
                    error={notesError}
                    onSave={saveNotes}
                    onDelete={deleteNotes}
                    onClose={closeNotes}
                />
            )}

            {toast && <div className="toast" role="alert">{toast}</div>}
        </div>
    );
};

export default Dashboard;
