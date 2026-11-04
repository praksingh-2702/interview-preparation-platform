import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import api, { getErrorMessage } from '../services/api';
import {
    applyListFilters, computeStats, DIFFICULTIES, DIFFICULTY_LABEL, filterQuestions, getCompanies, getTopics,
    statusFilters,
} from '../utils/stats';
import { loadSection, saveSection, SECTION_BY_ID, SECTIONS, sectionOf } from '../utils/sections';
import { computeActivity } from '../utils/activity';
import { isDue, parseTime } from '../utils/time';
import ActivityPanel from './ActivityPanel';
import NotesModal from './NotesModal';
import RevisionQueue from './RevisionQueue';
import SectionTabs from './SectionTabs';
import StatsPanel from './StatsPanel';
import TopicSection from './TopicSection';

const EMPTY_FILTERS = { company: '', difficulty: '', search: '', status: '' };

/** The practice sheet. The top bar lives in App; reloadKey changes when an editor edits questions. */
const Dashboard = ({ reloadKey = 0 }) => {
    const [questions, setQuestions] = useState([]);
    const [progressMap, setProgressMap] = useState({});
    const [notes, setNotes] = useState({});
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
    const [section, setSection] = useState(loadSection);
    const initializedSections = useRef(new Set());

    // Latest progress, readable from the stable callbacks without re-rendering every row.
    const progressRef = useRef(progressMap);
    progressRef.current = progressMap;
    // Per question: the state the user wants, the last state the server confirmed, and whether a save is running.
    const desiredRef = useRef({});
    const confirmedRef = useRef({});
    const inFlightRef = useRef({});

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
    }, []);

    useEffect(() => { load(); }, [load, reloadKey]);

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

    const setRow = useCallback((key, status) => {
        progressRef.current = { ...progressRef.current, [key]: status }; // visible to an immediate next click
        setProgressMap((m) => ({ ...m, [key]: status }));
    }, []);

    /**
     * Sends the latest wanted state for one question. Clicks made while a save is running only update
     * desiredRef; when the save returns, the newest state is sent (if it differs). So rapid
     * tick / untick never queues a pile of requests, and the last click always wins.
     */
    const flushProgress = useCallback(async (questionId) => {
        const key = String(questionId);
        inFlightRef.current[key] = true;
        setSaving((s) => ({ ...s, [key]: true }));
        try {
            for (;;) {
                const want = desiredRef.current[key];
                const res = await api.put(`/api/progress/${questionId}`, want);
                const server = res?.data && typeof res.data === 'object' ? res.data : want;
                confirmedRef.current[key] = { ...want, ...server };
                const latest = desiredRef.current[key];
                if (latest.solved === want.solved && latest.revised === want.revised) {
                    // Settled: adopt the server's timestamps and revision schedule.
                    setRow(key, { ...progressRef.current[key], ...server });
                    break;
                }
            }
        } catch (err) {
            if (err.response?.status !== 401) {
                setRow(key, confirmedRef.current[key]);
                setToast(getErrorMessage(err, "Couldn't save that change. Please try again."));
            }
        } finally {
            delete desiredRef.current[key];
            delete inFlightRef.current[key];
            setSaving((s) => {
                const copy = { ...s };
                delete copy[key];
                return copy;
            });
        }
    }, [setRow]);

    const handleToggleProgress = useCallback((questionId, field) => {
        const key = String(questionId);
        const current = progressRef.current[key] || { solved: false, revised: false };
        if (!inFlightRef.current[key]) confirmedRef.current[key] = current;
        const next = { solved: !!current.solved, revised: !!current.revised, [field]: !current[field] };

        // Optimistic: the checkbox, progress ring and heatmap all move on this click.
        const optimistic = { ...current, ...next };
        if (field === 'solved') {
            if (next.solved) {
                optimistic.solvedAt = new Date().toISOString();
            } else {
                optimistic.solvedAt = null;
                optimistic.nextReviewAt = null;
                optimistic.reviewStage = null;
            }
        }
        setRow(key, optimistic);
        desiredRef.current[key] = next;
        if (!inFlightRef.current[key]) flushProgress(questionId);
    }, [flushProgress, setRow]);

    const handleReview = useCallback(async (questionId, result) => {
        const key = String(questionId);
        setReviewing((r) => ({ ...r, [key]: true }));
        try {
            const { data } = await api.post(`/api/progress/${questionId}/review`, { result });
            setProgressMap((m) => ({ ...m, [key]: { ...m[key], ...data } }));
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
    }, []);

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

    const meta = SECTION_BY_ID[section] || SECTIONS[0];
    const sectionQuestions = useMemo(
        () => questions.filter((q) => sectionOf(q) === section),
        [questions, section]
    );
    const sectionCounts = useMemo(() => {
        const counts = {};
        SECTIONS.forEach((s) => { counts[s.id] = { done: 0, total: 0 }; });
        questions.forEach((q) => {
            const c = counts[sectionOf(q)];
            if (!c) return;
            c.total += 1;
            if (progressMap[String(q.id)]?.solved) c.done += 1;
        });
        return counts;
    }, [questions, progressMap]);
    const topics = useMemo(() => getTopics(sectionQuestions), [sectionQuestions]);
    const companies = useMemo(() => getCompanies(sectionQuestions), [sectionQuestions]);
    const scoped = useMemo(() => filterQuestions(sectionQuestions, filters), [sectionQuestions, filters]);
    const visible = useMemo(
        () => applyListFilters(scoped, filters, { progressMap, notes, now }),
        [scoped, filters, progressMap, notes, now]
    );
    const stats = useMemo(() => computeStats(scoped, progressMap), [scoped, progressMap]);
    // Heatmap + streaks for all sections, derived from the ticks themselves (instant, never out of sync).
    const activity = useMemo(() => computeActivity(progressMap, now), [progressMap, now]);
    const dueQuestions = useMemo(
        () => questions
            .filter((q) => isDue(progressMap[String(q.id)], now))
            .sort((a, b) => parseTime(progressMap[String(a.id)].nextReviewAt) - parseTime(progressMap[String(b.id)].nextReviewAt)),
        [questions, progressMap, now]
    );

    // The first time each section is shown, open its first topic so the page isn't a wall of headers.
    useEffect(() => {
        if (!topics.length || initializedSections.current.has(section)) return;
        initializedSections.current.add(section);
        setExpanded((prev) => (topics.some((t) => prev[t]) ? prev : { ...prev, [topics[0]]: true }));
    }, [section, topics]);

    const changeSection = useCallback((id) => {
        setSection(id);
        saveSection(id);
        setFilters(EMPTY_FILTERS);
    }, []);

    const scopeActive = !!(filters.company || filters.difficulty);
    const listActive = !!(filters.search.trim() || filters.status);
    const anyFilter = scopeActive || listActive;

    const setFilter = (field) => (e) => setFilters((f) => ({ ...f, [field]: e.target.value }));
    const setAllExpanded = (value) => setExpanded(Object.fromEntries(topics.map((t) => [t, value])));
    const notesQuestion = notesFor !== null ? questions.find((q) => q.id === notesFor) : null;

    return (
        <>
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
                        {/* One heatmap for all four sections, above the section switcher. */}
                        <div className="overview-block"><ActivityPanel stats={activity} /></div>

                        <SectionTabs active={section} counts={sectionCounts} onChange={changeSection} />

                        <StatsPanel stats={stats} filtered={scopeActive} sectionName={meta.name} verb={meta.verb} />

                        <div className="today-grid">
                            <RevisionQueue
                                dueQuestions={dueQuestions}
                                progressMap={progressMap}
                                reviewing={reviewing}
                                onReview={handleReview}
                                onShowAll={() => setFilters((f) => ({ ...f, status: 'due' }))}
                            />
                        </div>

                        <div className="card toolbar">
                            <label className="search">
                                <span className="sr-only">Search {meta.label}</span>
                                <input type="search"
                                       placeholder={meta.theory ? `Search ${meta.label} topics…` : 'Search problems, topics, companies…'}
                                       value={filters.search} onChange={setFilter('search')} />
                            </label>
                            <label className="select">
                                <span className="sr-only">Status</span>
                                <select value={filters.status} onChange={setFilter('status')}>
                                    {statusFilters(meta.verb).map((s) => <option key={s.value} value={s.value}>{s.label}</option>)}
                                </select>
                            </label>
                            {companies.length > 0 && (
                                <label className="select">
                                    <span className="sr-only">Company</span>
                                    <select value={filters.company} onChange={setFilter('company')}>
                                        <option value="">All companies</option>
                                        {companies.map((c) => <option key={c} value={c}>{c}</option>)}
                                    </select>
                                </label>
                            )}
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
                            <p className="result-count">
                                {visible.length} {meta.theory ? (visible.length === 1 ? 'question' : 'questions') : (visible.length === 1 ? 'problem' : 'problems')} match
                            </p>
                        )}

                        {visible.length === 0 ? (
                            <p className="empty">{sectionQuestions.length === 0
                                ? `No ${meta.label} questions yet.`
                                : 'Nothing matches these filters.'}</p>
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
                                            verb={meta.verb}
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
        </>
    );
};

export default Dashboard;
