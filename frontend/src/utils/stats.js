export const DIFFICULTIES = ['EASY', 'MEDIUM', 'HARD'];

export const DIFFICULTY_LABEL = { EASY: 'Easy', MEDIUM: 'Medium', HARD: 'Hard' };

/** Topics in the order they first appear in the question list (the seed file groups them). */
export const getTopics = (questions) => {
    const seen = new Set();
    const topics = [];
    questions.forEach((q) => {
        if (q.category && !seen.has(q.category)) {
            seen.add(q.category);
            topics.push(q.category);
        }
    });
    return topics;
};

export const getCompanies = (questions) => {
    const set = new Set();
    questions.forEach((q) => (q.companies || []).forEach((c) => set.add(c)));
    return [...set].sort((a, b) => a.localeCompare(b));
};

export const filterQuestions = (questions, { company, difficulty }) =>
    questions.filter((q) =>
        (!company || (q.companies || []).includes(company)) &&
        (!difficulty || q.difficulty === difficulty)
    );

/** Totals and solved counts, overall and per difficulty. */
export const computeStats = (questions, progressMap) => {
    const stats = { total: questions.length, solved: 0, revised: 0, byDifficulty: {} };
    DIFFICULTIES.forEach((d) => { stats.byDifficulty[d] = { total: 0, solved: 0 }; });

    questions.forEach((q) => {
        const p = progressMap[String(q.id)];
        const bucket = stats.byDifficulty[q.difficulty];
        if (bucket) bucket.total += 1;
        if (p?.solved) {
            stats.solved += 1;
            if (bucket) bucket.solved += 1;
        }
        if (p?.revised) stats.revised += 1;
    });
    stats.percent = stats.total ? Math.round((stats.solved / stats.total) * 100) : 0;
    return stats;
};

/**
 * conic-gradient for the progress ring: solved Easy, Medium and Hard segments,
 * then the unsolved remainder in the neutral track colour.
 * (The old ring painted every unsolved question red, so 0% looked like "all Hard solved".)
 */
export const ringGradient = (stats) => {
    if (!stats.total) return 'conic-gradient(var(--track) 0deg 360deg)';
    const deg = (n) => +((n / stats.total) * 360).toFixed(2);
    const e = deg(stats.byDifficulty.EASY.solved);
    const m = +(e + deg(stats.byDifficulty.MEDIUM.solved)).toFixed(2);
    const h = +(m + deg(stats.byDifficulty.HARD.solved)).toFixed(2);
    return `conic-gradient(var(--easy) 0deg ${e}deg, var(--medium) ${e}deg ${m}deg, ` +
        `var(--hard) ${m}deg ${h}deg, var(--track) ${h}deg 360deg)`;
};
