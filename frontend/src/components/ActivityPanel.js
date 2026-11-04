import React, { useEffect, useMemo, useRef } from 'react';
import { buildHeatmap } from '../utils/heatmap';

const DAY_LABELS = ['', 'Mon', '', 'Wed', '', 'Fri', ''];

const formatDate = (key) => {
    const [y, m, d] = key.split('-').map(Number);
    return new Date(y, m - 1, d).toLocaleDateString(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
};

const ActivityPanel = ({ stats }) => {
    const { columns, monthLabels } = useMemo(() => buildHeatmap(stats?.activity || []), [stats]);
    const scrollRef = useRef(null);

    // On narrow screens the grid scrolls; start at the most recent weeks.
    useEffect(() => {
        const el = scrollRef.current;
        if (el) el.scrollLeft = el.scrollWidth;
    }, [columns]);
    const total = (stats?.activity || []).reduce((sum, a) => sum + a.count, 0);
    const current = stats?.currentStreak ?? 0;

    return (
        <section className="card activity" aria-label="Activity">
            <div className="streaks">
                <div className="streak streak-current">
                    <span className="streak-value">{current}</span>
                    <span className="streak-label">day streak{current > 0 ? ' 🔥' : ''}</span>
                </div>
                <div className="streak">
                    <span className="streak-value">{stats?.longestStreak ?? 0}</span>
                    <span className="streak-label">longest streak</span>
                </div>
                <div className="streak">
                    <span className="streak-value">{stats?.activeDays ?? 0}</span>
                    <span className="streak-label">active days</span>
                </div>
                <p className="activity-total">{total} {total === 1 ? 'activity' : 'activities'} in the last year</p>
            </div>

            <div className="heatmap-scroll" ref={scrollRef}>
                <div className="heatmap" role="img" aria-label={`Activity heatmap: ${total} activities in the last year`}>
                    <div className="heatmap-months" aria-hidden="true">
                        {monthLabels.map((m) => (
                            <span key={`${m.week}-${m.label}`} style={{ gridColumnStart: m.week + 1 }}>{m.label}</span>
                        ))}
                    </div>
                    <div className="heatmap-days" aria-hidden="true">
                        {DAY_LABELS.map((d, i) => <span key={i}>{d}</span>)}
                    </div>
                    <div className="heatmap-grid">
                        {columns.map((col, w) => (
                            <div className="heatmap-col" key={w}>
                                {col.map((cell) => (
                                    <span
                                        key={cell.date}
                                        className={`cell level-${cell.level} ${cell.future ? 'cell-future' : ''}`}
                                        title={cell.future ? '' : `${cell.count} ${cell.count === 1 ? 'activity' : 'activities'} on ${formatDate(cell.date)}`}
                                    />
                                ))}
                            </div>
                        ))}
                    </div>
                </div>
            </div>
            <div className="heatmap-legend" aria-hidden="true">
                Less {[0, 1, 2, 3, 4].map((l) => <span key={l} className={`cell level-${l}`} />)} More
            </div>
        </section>
    );
};

export default ActivityPanel;
