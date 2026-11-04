import React, { useEffect, useRef, useState } from 'react';

const TEMPLATE = 'Approach:\n\nTime: O()\nSpace: O()\n\nEdge cases:\n';

const NotesModal = ({ question, note, saving, error, onSave, onDelete, onClose }) => {
    const [content, setContent] = useState(note?.content || '');
    const [solutionUrl, setSolutionUrl] = useState(note?.solutionUrl || '');
    const textareaRef = useRef(null);

    useEffect(() => {
        textareaRef.current?.focus();
        const onKey = (e) => { if (e.key === 'Escape') onClose(); };
        window.addEventListener('keydown', onKey);
        return () => window.removeEventListener('keydown', onKey);
    }, [onClose]);

    const submit = (e) => {
        e.preventDefault();
        onSave({ content, solutionUrl: solutionUrl.trim() });
    };

    return (
        <div className="modal-backdrop" onMouseDown={(e) => { if (e.target === e.currentTarget) onClose(); }}>
            <div className="modal card" role="dialog" aria-modal="true" aria-labelledby="notes-title">
                <form onSubmit={submit}>
                    <div className="modal-header">
                        <h2 id="notes-title" className="section-title">Notes · {question.title}</h2>
                        <button type="button" className="btn btn-ghost btn-small" onClick={onClose} aria-label="Close">✕</button>
                    </div>

                    {error && <div className="alert alert-error" role="alert">{error}</div>}

                    <div className="field">
                        <label className="field-label" htmlFor="notes-content">Your notes</label>
                        <textarea
                            id="notes-content"
                            aria-describedby="notes-hint"
                            ref={textareaRef}
                            rows={10}
                            maxLength={5000}
                            value={content}
                            placeholder={TEMPLATE}
                            onChange={(e) => setContent(e.target.value)}
                        />
                        <span className="field-hint" id="notes-hint">{content.length}/5000 · only you can see this</span>
                    </div>

                    <div className="field">
                        <label className="field-label" htmlFor="notes-url">Link to your solution (optional)</label>
                        <input
                            id="notes-url"
                            type="url"
                            value={solutionUrl}
                            placeholder="https://github.com/you/dsa/blob/main/two-sum.cpp"
                            onChange={(e) => setSolutionUrl(e.target.value)}
                        />
                    </div>

                    <div className="modal-actions">
                        {note && (
                            <button type="button" className="btn btn-ghost btn-danger" disabled={saving} onClick={onDelete}>
                                Delete note
                            </button>
                        )}
                        <span className="toolbar-spacer" />
                        <button type="button" className="btn btn-ghost" onClick={onClose}>Cancel</button>
                        <button type="submit" className="btn btn-primary" disabled={saving}>
                            {saving ? 'Saving…' : 'Save'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default NotesModal;
