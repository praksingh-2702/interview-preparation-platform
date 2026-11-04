import React from 'react';
import { DIFFICULTY_LABEL } from '../utils/stats';
import { parseTime, relativeDay } from '../utils/time';
import { sectionOf } from '../utils/sections';

const VISIBLE = 5;

const RevisionQueue = ({ dueQuestions, progressMap, reviewing, onReview, onShowAll }) => {
    const shown = dueQuestions.slice(0, VISIBLE);
    const remaining = dueQuestions.length - shown.length;

    return (
        <section className="card revision" aria-label="Revision queue">
            <div className="revision-header">
                <h2 className="section-title">Revise today</h2>
                <span className="badge">{dueQuestions.length} due</span>
            </div>

            {dueQuestions.length === 0 ? (
                <p className="revision-empty">
                    Nothing due. Solved problems and topics you've read come back here after 1, 3, 7, 14, 30 and 60 days.
                </p>
            ) : (
                <ul className="revision-list">
                    {shown.map((q) => {
                        const status = progressMap[String(q.id)];
                        const busy = !!reviewing[String(q.id)];
                        return (
                            <li key={q.id} className="revision-item">
                                <div className="revision-info">
                                    <a href={q.problemUrl} target="_blank" rel="noopener noreferrer" className="question-title">
                                        {q.title}
                                    </a>
                                    <span className="revision-meta">
                                        <span className="pill pill-section">{sectionOf(q)}</span>
                                        <span className={`pill pill-${q.difficulty.toLowerCase()}`}>{DIFFICULTY_LABEL[q.difficulty]}</span>
                                        due {relativeDay(parseTime(status?.nextReviewAt))}
                                    </span>
                                </div>
                                <div className="revision-actions">
                                    <button type="button" className="btn btn-small btn-good" disabled={busy}
                                            onClick={() => onReview(q.id, 'GOOD')}>Got it</button>
                                    <button type="button" className="btn btn-small btn-again" disabled={busy}
                                            onClick={() => onReview(q.id, 'AGAIN')}>Again</button>
                                </div>
                            </li>
                        );
                    })}
                </ul>
            )}
            {remaining > 0 && (
                <button type="button" className="btn btn-ghost btn-small revision-more" onClick={onShowAll}>
                    Show all {dueQuestions.length} due
                </button>
            )}
        </section>
    );
};

export default RevisionQueue;
