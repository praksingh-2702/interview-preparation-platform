/** Accepts ISO strings or epoch numbers (seconds or ms) and returns ms, or null. */
export const parseTime = (value) => {
    if (value === null || value === undefined || value === '') return null;
    if (typeof value === 'number') return value < 1e12 ? value * 1000 : value;
    const ms = Date.parse(value);
    return Number.isNaN(ms) ? null : ms;
};

/** yyyy-mm-dd for a Date in the browser's local timezone. */
export const localDateKey = (date) => {
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, '0');
    const d = String(date.getDate()).padStart(2, '0');
    return `${y}-${m}-${d}`;
};

export const browserTimeZone = () => {
    try {
        return Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
    } catch {
        return 'UTC';
    }
};

/** A solved problem whose next review time has arrived. */
export const isDue = (status, now = Date.now()) => {
    if (!status?.solved) return false;
    const next = parseTime(status.nextReviewAt);
    return next !== null && next <= now;
};

const RTF = typeof Intl !== 'undefined' && Intl.RelativeTimeFormat
    ? new Intl.RelativeTimeFormat('en', { numeric: 'auto' })
    : null;

/** "today", "tomorrow", "in 3 days", "2 days ago". */
export const relativeDay = (ms, now = Date.now()) => {
    if (ms === null) return '';
    const startOf = (t) => { const d = new Date(t); d.setHours(0, 0, 0, 0); return d.getTime(); };
    const days = Math.round((startOf(ms) - startOf(now)) / 86400000);
    if (RTF) return RTF.format(days, 'day');
    return days === 0 ? 'today' : days > 0 ? `in ${days} days` : `${-days} days ago`;
};
