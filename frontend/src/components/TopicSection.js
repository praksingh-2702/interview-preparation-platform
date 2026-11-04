import React from 'react';
import QuestionRow from './QuestionRow';
import { isDue } from '../utils/time';

const TopicSection = ({
    topic, questions, progressMap, notes, saving, now, expanded, verb,
    onToggleExpanded, onToggleProgress, onOpenNotes,
}) => {
    const solved = questions.filter((q) => progressMap[String(q.id)]?.solved).length;
    const percent = questions.length ? Math.round((solved / questions.length) * 100) : 0;
    const bodyId = `topic-${topic.replace(/[^a-z0-9]+/gi, '-').toLowerCase()}`;

    return (
        <section className="card topic">
            <button type="button" className="topic-header" aria-expanded={expanded} aria-controls={bodyId}
                    onClick={() => onToggleExpanded(topic)}>
                <span className={`chevron ${expanded ? 'chevron-open' : ''}`} aria-hidden="true">▸</span>
                <span className="topic-name">{topic}</span>
                <span className="badge">{solved}/{questions.length}</span>
                <span className="bar bar-thin topic-bar" aria-hidden="true">
                    <span className="bar-fill" style={{ width: `${percent}%` }} />
                </span>
            </button>

            {expanded && (
                <ul className="question-list" id={bodyId}>
                    {questions.map((q) => {
                        const key = String(q.id);
                        return (
                            <QuestionRow
                                key={q.id}
                                question={q}
                                status={progressMap[key]}
                                hasNote={!!notes[key]}
                                due={isDue(progressMap[key], now)}
                                saving={!!saving[key]}
                                verb={verb}
                                onToggle={onToggleProgress}
                                onOpenNotes={onOpenNotes}
                            />
                        );
                    })}
                </ul>
            )}
        </section>
    );
};

export default TopicSection;
