import React from 'react';
import { DIFFICULTY_LABEL } from '../utils/stats';

/** One question. The title links out: LeetCode for DSA, a GeeksforGeeks article for OOPS / OS / CN. */
const QuestionRow = ({ question, status, hasNote, due, saving, verb = 'Solved', onToggle, onOpenNotes }) => {
    const solved = !!status?.solved;
    const revised = !!status?.revised;
    const companies = question.companies || [];

    return (
        <li className={`question ${solved ? 'question-solved' : ''}`}>
            <div className="question-main">
                <a href={question.problemUrl} target="_blank" rel="noopener noreferrer" className="question-title">
                    {question.title}
                </a>
                {due && <span className="pill pill-due">Due</span>}
                {question.description && <p className="question-desc">{question.description}</p>}
            </div>

            <div className="question-meta">
                <span className={`pill pill-${question.difficulty.toLowerCase()}`}>
                    {DIFFICULTY_LABEL[question.difficulty] || question.difficulty}
                </span>
                {companies.length > 0 && (
                    <span className="companies">
                        {companies.map((c) => <span key={c} className="chip">{c}</span>)}
                    </span>
                )}
            </div>

            <div className="question-actions">
                <button type="button" className={`btn btn-small btn-notes ${hasNote ? 'btn-notes-filled' : ''}`}
                        onClick={() => onOpenNotes(question.id)}
                        aria-label={`${hasNote ? 'Edit' : 'Add'} notes for ${question.title}`}>
                    {hasNote ? '📝 Notes' : '+ Note'}
                </button>
                <label className="check check-solved">
                    <input type="checkbox" checked={solved} disabled={saving}
                           onChange={() => onToggle(question.id, 'solved')} />
                    {verb}
                </label>
                <label className="check check-revised">
                    <input type="checkbox" checked={revised} disabled={saving}
                           onChange={() => onToggle(question.id, 'revised')} />
                    Revised
                </label>
            </div>
        </li>
    );
};

export default React.memo(QuestionRow);
