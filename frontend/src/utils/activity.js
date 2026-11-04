import { localDateKey, parseTime } from './time';

const HEATMAP_DAYS = 365;

/** Local calendar day as a Date at noon (so stepping by days never trips over DST). */
const dayFromKey = (key) => {
    const [y, m, d] = key.split('-').map(Number);
    return new Date(y, m - 1, d, 12);
};
const shift = (date, days) => {
    const copy = new Date(date);
    copy.setDate(copy.getDate() + days);
    return copy;
};

/**
 * Heatmap + streaks straight from the tick state: one count per row currently ticked
 * Solved / Read, on the (local) day it was ticked. Unticking removes it immediately.
 * Same rules as the server's /api/stats, but with no request and no lag.
 */
export const computeActivity = (progressMap, now = Date.now()) => {
    const perDay = new Map();
    Object.values(progressMap || {}).forEach((p) => {
        if (!p?.solved) return;
        const t = parseTime(p.solvedAt);
        if (t === null) return;
        const key = localDateKey(new Date(t));
        perDay.set(key, (perDay.get(key) || 0) + 1);
    });

    const today = dayFromKey(localDateKey(new Date(now)));
    const todayKey = localDateKey(today);
    const fromKey = localDateKey(shift(today, -(HEATMAP_DAYS - 1)));

    const activity = [...perDay.entries()]
        .filter(([date]) => date >= fromKey && date <= todayKey)
        .sort(([a], [b]) => (a < b ? -1 : 1))
        .map(([date, count]) => ({ date, count }));

    // Current streak: counts back from today, or from yesterday if nothing yet today.
    let day = perDay.has(todayKey) ? today : shift(today, -1);
    let currentStreak = 0;
    while (perDay.has(localDateKey(day))) {
        currentStreak += 1;
        day = shift(day, -1);
    }

    let longestStreak = 0;
    let run = 0;
    let previous = null;
    [...perDay.keys()].sort().forEach((key) => {
        const d = dayFromKey(key);
        run = previous && localDateKey(shift(previous, 1)) === key ? run + 1 : 1;
        longestStreak = Math.max(longestStreak, run);
        previous = d;
    });

    return { activity, currentStreak, longestStreak, activeDays: perDay.size };
};
