import React from 'react';
import { SECTIONS } from '../utils/sections';

/** Section switcher with a mini progress readout per section. */
const SectionTabs = ({ active, counts, onChange }) => (
    <nav className="section-tabs" role="tablist" aria-label="Study sections">
        {SECTIONS.map((s) => {
            const c = counts[s.id] || { done: 0, total: 0 };
            const pct = c.total ? Math.round((c.done / c.total) * 100) : 0;
            const selected = active === s.id;
            return (
                <button
                    key={s.id}
                    type="button"
                    role="tab"
                    aria-selected={selected}
                    className={`section-tab ${selected ? 'section-tab-active' : ''}`}
                    onClick={() => onChange(s.id)}
                    title={s.name}
                >
                    <span className="section-tab-label">{s.label}</span>
                    <span className="section-tab-meta">{c.done}/{c.total}</span>
                    <span className="bar bar-thin" aria-hidden="true">
                        <span className="bar-fill" style={{ width: `${pct}%` }} />
                    </span>
                </button>
            );
        })}
    </nav>
);

export default SectionTabs;
