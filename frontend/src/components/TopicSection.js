import React from 'react';
import QuestionRow from './QuestionRow';

const TopicSection = ({ topic, questions, progressMap, saving, expanded, onToggleExpanded, onToggleProgress }) => {
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
                    {questions.map((q) => (
                        <QuestionRow
                            key={q.id}
                            question={q}
                            status={progressMap[String(q.id)]}
                            saving={!!saving[String(q.id)]}
                            onToggle={onToggleProgress}
                        />
                    ))}
                </ul>
            )}
        </section>
    );
};

export default TopicSection;
