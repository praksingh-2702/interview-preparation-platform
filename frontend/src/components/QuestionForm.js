import React, { useEffect, useMemo, useState } from 'react';
import api, { getErrorMessage, getFieldErrors } from '../services/api';
import { SECTIONS } from '../utils/sections';
import { DIFFICULTIES, DIFFICULTY_LABEL } from '../utils/stats';

const blank = (section) => ({
    section, title: '', category: '', difficulty: 'EASY', problemUrl: '', companies: '', description: '',
});

/** Add or edit one question (editors and admins). */
const QuestionForm = ({ question, defaultSection = 'DSA', categories, onSaved, onClose }) => {
    const [form, setForm] = useState(() => (question
        ? { ...blank(question.section || 'DSA'), ...question, companies: (question.companies || []).join(', '), description: question.description || '' }
        : blank(defaultSection)));
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');
    const [fieldErrors, setFieldErrors] = useState({});
    const isDsa = form.section === 'DSA';
    const sectionCategories = useMemo(() => categories[form.section] || [], [categories, form.section]);

    useEffect(() => {
        const onKey = (e) => { if (e.key === 'Escape') onClose(); };
        window.addEventListener('keydown', onKey);
        return () => window.removeEventListener('keydown', onKey);
    }, [onClose]);

    const set = (field) => (e) => {
        setForm((f) => ({ ...f, [field]: e.target.value }));
        setFieldErrors((fe) => ({ ...fe, [field]: undefined }));
    };

    const submit = async (e) => {
        e.preventDefault();
        setSaving(true);
        setError('');
        setFieldErrors({});
        const body = {
            section: form.section,
            title: form.title.trim(),
            category: form.category.trim(),
            difficulty: form.difficulty,
            problemUrl: form.problemUrl.trim(),
            companies: isDsa ? form.companies.split(',').map((c) => c.trim()).filter(Boolean) : [],
            description: isDsa ? form.description.trim() : '',
        };
        try {
            const res = question
                ? await api.put(`/api/questions/${question.id}`, body)
                : await api.post('/api/questions', body);
            onSaved(res.data);
        } catch (err) {
            setFieldErrors(getFieldErrors(err));
            setError(getErrorMessage(err, "Couldn't save the question."));
        } finally {
            setSaving(false);
        }
    };

    const err = (field) => fieldErrors[field] && <span className="field-error">{fieldErrors[field]}</span>;

    return (
        <div className="modal-backdrop" onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}>
            <div className="modal card" role="dialog" aria-modal="true" aria-labelledby="qform-title">
                <form onSubmit={submit}>
                    <div className="modal-header">
                        <h2 id="qform-title" className="section-title">{question ? 'Edit question' : 'Add question'}</h2>
                        <button type="button" className="btn btn-ghost btn-small" onClick={onClose} aria-label="Close">✕</button>
                    </div>
                    {error && <div className="alert alert-error" role="alert">{error}</div>}

                    <div className="form-grid">
                        <div className="field">
                            <label className="field-label" htmlFor="qf-section">Section</label>
                            <select id="qf-section" className="input-select" value={form.section} onChange={set('section')}>
                                {SECTIONS.map((s) => <option key={s.id} value={s.id}>{s.label}</option>)}
                            </select>
                        </div>
                        <div className="field">
                            <label className="field-label" htmlFor="qf-difficulty">Difficulty</label>
                            <select id="qf-difficulty" className="input-select" value={form.difficulty} onChange={set('difficulty')}>
                                {DIFFICULTIES.map((d) => <option key={d} value={d}>{DIFFICULTY_LABEL[d]}</option>)}
                            </select>
                        </div>
                    </div>

                    <div className="field">
                        <label className="field-label" htmlFor="qf-title">{isDsa ? 'Problem' : 'Topic'}</label>
                        <input id="qf-title" required maxLength={200} value={form.title} onChange={set('title')} />
                        {err('title')}
                    </div>
                    <div className="field">
                        <label className="field-label" htmlFor="qf-category">Topic group</label>
                        <input id="qf-category" required maxLength={100} list="qf-categories" value={form.category}
                               onChange={set('category')} placeholder={sectionCategories[0] || 'e.g. Arrays'} />
                        <datalist id="qf-categories">
                            {sectionCategories.map((c) => <option key={c} value={c} />)}
                        </datalist>
                        {err('category')}
                    </div>
                    <div className="field">
                        <label className="field-label" htmlFor="qf-url">{isDsa ? 'Practice link (LeetCode…)' : 'Article link (GeeksforGeeks…)'}</label>
                        <input id="qf-url" type="url" required maxLength={500} value={form.problemUrl} onChange={set('problemUrl')}
                               placeholder={isDsa ? 'https://leetcode.com/problems/…' : 'https://www.geeksforgeeks.org/…'} />
                        {err('problemUrl')}
                    </div>
                    {isDsa && (
                        <>
                            <div className="field">
                                <label className="field-label" htmlFor="qf-companies">Companies (comma-separated)</label>
                                <input id="qf-companies" value={form.companies} onChange={set('companies')} placeholder="Amazon, Google" />
                            </div>
                            <div className="field">
                                <label className="field-label" htmlFor="qf-description">One-line description</label>
                                <input id="qf-description" maxLength={2000} value={form.description} onChange={set('description')} />
                            </div>
                        </>
                    )}

                    <div className="modal-actions">
                        <span className="toolbar-spacer" />
                        <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
                        <button type="submit" className="btn btn-primary" disabled={saving}>
                            {saving ? 'Saving…' : question ? 'Save changes' : 'Add question'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default QuestionForm;
