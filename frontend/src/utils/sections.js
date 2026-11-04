// The four study sections. DSA links to LeetCode problems; OOPS, OS and CN link to GeeksforGeeks articles.
export const SECTIONS = [
    { id: 'DSA', label: 'DSA', name: 'Data Structures & Algorithms', verb: 'Solved', theory: false },
    { id: 'OOPS', label: 'OOPS', name: 'Object-Oriented Programming', verb: 'Read', theory: true },
    { id: 'OS', label: 'OS', name: 'Operating Systems', verb: 'Read', theory: true },
    { id: 'CN', label: 'CN', name: 'Computer Networks', verb: 'Read', theory: true },
];

export const SECTION_BY_ID = Object.fromEntries(SECTIONS.map((s) => [s.id, s]));

/** Questions from before v3 have no section; they are DSA. */
export const sectionOf = (q) => q.section || 'DSA';

const STORAGE_KEY = 'freakcode.section';

export const loadSection = () => {
    try {
        const s = window.localStorage.getItem(STORAGE_KEY);
        return SECTION_BY_ID[s] ? s : 'DSA';
    } catch {
        return 'DSA';
    }
};

export const saveSection = (id) => {
    try { window.localStorage.setItem(STORAGE_KEY, id); } catch { /* ignore */ }
};
