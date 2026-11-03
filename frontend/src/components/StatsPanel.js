import React from 'react';
import { DIFFICULTIES, DIFFICULTY_LABEL, ringGradient } from '../utils/stats';

const pct = (a, b) => (b ? Math.round((a / b) * 100) : 0);

const StatsPanel = ({ stats, filtered }) => (
    <section className="stats" aria-label="Progress summary">
        <div className="card stats-main">
            <p className="eyebrow">{filtered ? 'Progress (filtered view)' : 'Syllabus progress'}</p>
            <p className="stats-headline">
                <strong>{stats.solved}</strong> / {stats.total} solved
                <span className="stats-sub"> · {stats.revised} revised</span>
            </p>
            <div className="bar" role="progressbar" aria-valuemin={0} aria-valuemax={100} aria-valuenow={stats.percent}
                 aria-label="Overall progress">
                <div className="bar-fill" style={{ width: `${stats.percent}%` }} />
            </div>
            <ul className="diff-breakdown">
                {DIFFICULTIES.map((d) => {
                    const b = stats.byDifficulty[d];
                    return (
                        <li key={d} className={`diff-item diff-${d.toLowerCase()}`}>
                            <span className="diff-name">{DIFFICULTY_LABEL[d]}</span>
                            <span className="diff-count">{b.solved}/{b.total}</span>
                            <div className="bar bar-thin">
                                <div className="bar-fill" style={{ width: `${pct(b.solved, b.total)}%` }} />
                            </div>
                        </li>
                    );
                })}
            </ul>
        </div>

        <div className="card stats-ring-card">
            <div className="ring" style={{ background: ringGradient(stats) }} aria-hidden="true">
                <div className="ring-inner">
                    <span className="ring-value">{stats.percent}%</span>
                    <span className="ring-label">done</span>
                </div>
            </div>
            <span className="sr-only">{stats.percent}% of questions solved</span>
        </div>
    </section>
);

export default StatsPanel;
