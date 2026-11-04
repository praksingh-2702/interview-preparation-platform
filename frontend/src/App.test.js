import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App';
import api from './services/api';
import { applyListFilters, computeStats, ringGradient } from './utils/stats';
import { buildHeatmap, levelFor } from './utils/heatmap';
import { localDateKey } from './utils/time';

jest.mock('./services/api', () => {
    const mockApi = { get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn() };
    return {
        __esModule: true,
        default: mockApi,
        SESSION_EXPIRED_EVENT: 'freakcode:session-expired',
        getErrorMessage: (err, fallback = 'error') =>
            err?.response?.data?.detail || fallback,
        getFieldErrors: (err) => err?.response?.data?.errors || {},
        getErrorCode: (err) => err?.response?.data?.code || null,
        getRetryAfter: () => null,
    };
});

const QUESTIONS = [
    { id: 1, title: 'Two Sum', category: 'Arrays', difficulty: 'EASY', description: 'd', problemUrl: 'https://x/1', companies: ['Amazon', 'Google'] },
    { id: 2, title: '3Sum', category: 'Arrays', difficulty: 'MEDIUM', description: 'd', problemUrl: 'https://x/2', companies: ['Razorpay'] },
    { id: 3, title: 'Binary Search', category: 'Binary Search', difficulty: 'EASY', description: 'd', problemUrl: 'https://x/3', companies: ['Apple'] },
];

// A JWT whose payload only needs a future `exp` for the client-side expiry check.
const fakeToken = (expSecondsFromNow = 3600) => {
    const payload = btoa(JSON.stringify({ sub: 'om', exp: Math.floor(Date.now() / 1000) + expSecondsFromNow }));
    return `header.${payload}.sig`;
};

const mockGets = ({ progress = {}, notes = {}, stats = null } = {}) => {
    api.get.mockImplementation((url) => {
        if (url === '/api/questions') return Promise.resolve({ data: QUESTIONS });
        if (url === '/api/progress/my-status') return Promise.resolve({ data: progress });
        if (url === '/api/notes') return Promise.resolve({ data: notes });
        if (url === '/api/stats') {
            return stats ? Promise.resolve({ data: stats }) : Promise.reject({ response: { status: 404 } });
        }
        return Promise.reject(new Error(`unexpected GET ${url}`));
    });
};

const loggedIn = () => {
    localStorage.setItem('token', fakeToken());
    localStorage.setItem('username', 'om');
};

beforeEach(() => {
    localStorage.clear();
    jest.clearAllMocks();
    api.get.mockReset();
    api.post.mockReset();
    api.put.mockReset();
    window.history.replaceState(null, '', '/');
});

test('shows the login screen when there is no session', () => {
    render(<App />);
    expect(screen.getByRole('button', { name: 'Log in' })).toBeInTheDocument();
    expect(screen.getByLabelText('Username or email')).toBeInTheDocument();
});

test('an expired stored token starts at login instead of a broken dashboard', () => {
    localStorage.setItem('token', fakeToken(-60));
    render(<App />);
    expect(screen.getByRole('button', { name: 'Log in' })).toBeInTheDocument();
    expect(api.get).not.toHaveBeenCalled();
});

test('logs in, loads the dashboard and shows real progress numbers', async () => {
    api.post.mockResolvedValueOnce({ data: { token: fakeToken(), username: 'om' } });
    mockGets({ progress: { 1: { solved: true, revised: false } } });

    render(<App />);
    await userEvent.type(screen.getByLabelText('Username or email'), 'om');
    await userEvent.type(screen.getByLabelText('Password'), 'secret-password');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByText('@om')).toBeInTheDocument();
    expect(api.post).toHaveBeenCalledWith('/api/auth/login', { username: 'om', password: 'secret-password' });
    expect(await screen.findByText('Two Sum')).toBeInTheDocument();
    expect(screen.getByRole('progressbar', { name: 'Overall progress' })).toHaveAttribute('aria-valuenow', '33');
    // Company dropdown is built from the data, not a hard-coded list.
    expect(screen.getByRole('option', { name: 'Razorpay' })).toBeInTheDocument();
});

test('toggling a checkbox saves with PUT and rolls back on failure', async () => {
    loggedIn();
    mockGets();
    api.put.mockResolvedValueOnce({ data: { solved: true, revised: false } });

    render(<App />);
    const row = (await screen.findByText('Two Sum')).closest('li');
    const solved = within(row).getByLabelText('Solved');

    await userEvent.click(solved);
    expect(api.put).toHaveBeenCalledWith('/api/progress/1', { solved: true, revised: false });
    await waitFor(() => expect(solved).toBeChecked());
    // Checkboxes stay clickable while saving; let this save settle before the next one.
    await waitFor(() => expect(api.put).toHaveBeenCalledTimes(1));
    await act(async () => {});

    api.put.mockRejectedValueOnce({ response: { status: 500, data: { detail: 'Server exploded' } } });
    await userEvent.click(within(row).getByLabelText('Revised'));
    await waitFor(() => expect(within(row).getByLabelText('Revised')).not.toBeChecked());
    expect(await screen.findByRole('alert')).toHaveTextContent('Server exploded');
});

test('a session-expired event returns the user to login with an explanation', async () => {
    loggedIn();
    mockGets();

    render(<App />);
    await screen.findByText('Two Sum');
    act(() => {
        window.dispatchEvent(new CustomEvent('freakcode:session-expired', {
            detail: { message: 'You were signed out because this account logged in somewhere else.' },
        }));
    });
    expect(await screen.findByText(/logged in somewhere else/)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Log in' })).toBeInTheDocument();
});

test('progress ring leaves unsolved questions neutral (not red)', () => {
    const stats = computeStats(QUESTIONS, {});
    expect(ringGradient(stats)).toContain('var(--track) 0deg 360deg');

    const some = computeStats(QUESTIONS, { 1: { solved: true } });
    expect(ringGradient(some)).toContain('var(--easy) 0deg 120deg');
    expect(ringGradient(some)).toContain('var(--track) 120deg 360deg');
});


// ---------- v2 ----------

test('search narrows the list and expands matching topics', async () => {
    loggedIn();
    mockGets();
    render(<App />);
    await screen.findByText('Two Sum');
    // Binary Search topic starts collapsed
    expect(screen.queryByText('https://x/3')).not.toBeInTheDocument();

    await userEvent.type(screen.getByPlaceholderText(/Search problems/), 'apple');
    expect(await screen.findByRole('link', { name: 'Binary Search' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Two Sum' })).not.toBeInTheDocument();
    expect(screen.getByText('1 problem match')).toBeInTheDocument();
});

test('due problems show in the revision queue and can be reviewed', async () => {
    loggedIn();
    const yesterday = new Date(Date.now() - 86400000).toISOString();
    mockGets({ progress: { 2: { solved: true, revised: false, nextReviewAt: yesterday, reviewStage: 0 } } });
    api.post.mockResolvedValueOnce({
        data: { solved: true, revised: true, nextReviewAt: new Date(Date.now() + 3 * 86400000).toISOString(), reviewStage: 1 },
    });

    render(<App />);
    const queue = await screen.findByRole('region', { name: 'Revision queue' });
    expect(within(queue).getByText('1 due')).toBeInTheDocument();
    expect(within(queue).getByRole('link', { name: '3Sum' })).toBeInTheDocument();

    await userEvent.click(within(queue).getByRole('button', { name: 'Got it' }));
    expect(api.post).toHaveBeenCalledWith('/api/progress/2/review', { result: 'GOOD' });
    await waitFor(() => expect(within(queue).getByText('0 due')).toBeInTheDocument());
});

test('notes can be added from a problem row', async () => {
    loggedIn();
    mockGets();
    api.put.mockResolvedValueOnce({
        status: 200, data: { questionId: 1, content: 'two pointers', solutionUrl: null, updatedAt: '2026-11-04T00:00:00Z' },
    });

    render(<App />);
    await userEvent.click(await screen.findByRole('button', { name: 'Add notes for Two Sum' }));
    const dialog = screen.getByRole('dialog');
    await userEvent.type(within(dialog).getByLabelText('Your notes'), 'two pointers');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Save' }));

    expect(api.put).toHaveBeenCalledWith('/api/notes/1', { content: 'two pointers', solutionUrl: '' });
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(screen.getByRole('button', { name: 'Edit notes for Two Sum' })).toBeInTheDocument();
});

test('activity panel is built from ticked rows', async () => {
    loggedIn();
    const at = (daysAgo) => new Date(Date.now() - daysAgo * 86400000).toISOString();
    mockGets({
        progress: {
            1: { solved: true, revised: false, solvedAt: at(0) },
            2: { solved: true, revised: true, solvedAt: at(1) },
            3: { solved: false, revised: true, solvedAt: null }, // Revised alone doesn't count
        },
    });
    render(<App />);
    const panel = await screen.findByRole('region', { name: 'Activity' });
    expect(within(panel).getByText('2 questions completed in the last year')).toBeInTheDocument();
    const streak = within(panel).getByText(/day streak/).closest('.streak');
    expect(streak).toHaveTextContent('2');
    expect(api.get).not.toHaveBeenCalledWith('/api/stats', expect.anything());
});

test('tick then untick updates the heatmap instantly, and rapid clicks only save the final state', async () => {
    loggedIn();
    mockGets();
    let resolveFirst;
    api.put
        .mockImplementationOnce(() => new Promise((r) => { resolveFirst = r; }))
        .mockResolvedValue({ data: { solved: false, revised: false } });

    render(<App />);
    const row = (await screen.findByText('Two Sum')).closest('li');
    const panel = screen.getByRole('region', { name: 'Activity' });
    const solved = within(row).getByLabelText('Solved');

    await userEvent.click(solved);                   // tick: counted right away
    expect(solved).toBeChecked();
    expect(within(panel).getByText('1 question completed in the last year')).toBeInTheDocument();

    await userEvent.click(solved);                   // untick while the first save is still running
    await userEvent.click(solved);                   // tick
    await userEvent.click(solved);                   // untick: this is the state that must win
    expect(solved).not.toBeChecked();
    expect(within(panel).getByText('0 questions completed in the last year')).toBeInTheDocument();
    expect(api.put).toHaveBeenCalledTimes(1);        // nothing queued up behind the running save

    await act(async () => { resolveFirst({ data: { solved: true, revised: false, solvedAt: new Date().toISOString() } }); });
    await waitFor(() => expect(api.put).toHaveBeenCalledTimes(2));
    expect(api.put).toHaveBeenLastCalledWith('/api/progress/1', { solved: false, revised: false });
    await waitFor(() => expect(within(panel).getByText('0 questions completed in the last year')).toBeInTheDocument());
    expect(solved).not.toBeChecked();
});

test('heatmap grid ends today and buckets counts into levels', () => {
    const today = new Date(2026, 10, 4); // Wed 4 Nov 2026
    const { columns } = buildHeatmap([{ date: '2026-11-04', count: 4 }, { date: '2026-11-02', count: 1 }], today);
    expect(columns).toHaveLength(53);
    const last = columns[52];
    expect(last[0].date).toBe('2026-11-01'); // Sunday
    expect(last[3]).toMatchObject({ date: '2026-11-04', count: 4, level: 3, future: false });
    expect(last[1].level).toBe(1);
    expect(last[4].future).toBe(true);
    expect([0, 1, 2, 3, 6].map(levelFor)).toEqual([0, 1, 2, 2, 4]);
});

test('status filters use progress and notes', () => {
    const progress = { 1: { solved: true }, 2: { solved: true, nextReviewAt: '2000-01-01T00:00:00Z' } };
    const ids = (status) => applyListFilters(QUESTIONS, { search: '', status }, { progressMap: progress, notes: { 3: {} } })
        .map((q) => q.id);
    expect(ids('solved')).toEqual([1, 2]);
    expect(ids('unsolved')).toEqual([3]);
    expect(ids('due')).toEqual([2]);
    expect(ids('notes')).toEqual([3]);
});


// ---------- v2 phase 2: accounts ----------

test('register → enter code → log in', async () => {
    api.post
        .mockResolvedValueOnce({ status: 201, data: { message: 'We emailed a code to om@example.com.', email: 'om@example.com' } })
        .mockResolvedValueOnce({ data: { message: 'Email verified.' } });
    render(<App />);

    await userEvent.click(screen.getByRole('tab', { name: 'Create account' }));
    await userEvent.type(screen.getByLabelText('Username'), 'om');
    await userEvent.type(screen.getByLabelText('Email'), 'om@example.com');
    await userEvent.type(screen.getByLabelText('Password'), 'secret-password');
    await userEvent.click(screen.getByRole('button', { name: 'Create account' }));

    expect(await screen.findByText('Verify your email')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Resend code in \d+s/ })).toBeDisabled();
    await userEvent.type(screen.getByLabelText('6-digit code'), '12a3456');
    expect(screen.getByLabelText('6-digit code')).toHaveValue('123456');
    await userEvent.click(screen.getByRole('button', { name: 'Verify email' }));

    expect(api.post).toHaveBeenLastCalledWith('/api/auth/verify-email', { email: 'om@example.com', code: '123456' });
    expect(await screen.findByText(/Email verified! Log in/)).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Log in' })).toHaveAttribute('aria-selected', 'true');
});

test('logging in to an unverified account jumps to the code step', async () => {
    api.post.mockRejectedValueOnce({
        response: { status: 403, data: { code: 'EMAIL_NOT_VERIFIED', detail: 'Please verify your email first.', email: 'om@example.com' } },
    });
    render(<App />);
    await userEvent.type(screen.getByLabelText('Username or email'), 'om');
    await userEvent.type(screen.getByLabelText('Password'), 'secret-password');
    await userEvent.click(screen.getByRole('button', { name: 'Log in' }));

    expect(await screen.findByText('Verify your email')).toBeInTheDocument();
    expect(screen.getByText('om@example.com')).toBeInTheDocument();
});

test('forgot password → code + new password → back to login', async () => {
    api.post
        .mockResolvedValueOnce({ data: { message: 'If an account exists for that email, we sent a code.' } })
        .mockResolvedValueOnce({ data: { message: 'Password updated.' } });
    render(<App />);

    await userEvent.click(screen.getByRole('button', { name: 'Forgot password?' }));
    await userEvent.type(screen.getByLabelText('Email'), 'om@example.com');
    await userEvent.click(screen.getByRole('button', { name: 'Send reset code' }));

    expect(await screen.findByText('Choose a new password')).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText('6-digit code'), '654321');
    await userEvent.type(screen.getByLabelText('New password'), 'new-password-1');
    await userEvent.click(screen.getByRole('button', { name: 'Set new password' }));

    expect(api.post).toHaveBeenLastCalledWith('/api/auth/reset-password',
        { email: 'om@example.com', code: '654321', newPassword: 'new-password-1' });
    expect(await screen.findByText(/Password updated/)).toBeInTheDocument();
});

// ---------- v3 ----------

test('section tabs switch between DSA and theory sections', async () => {
    loggedIn();
    const os = { id: 9, section: 'OS', title: 'Mutex vs semaphore', category: 'Synchronization', difficulty: 'MEDIUM',
        problemUrl: 'https://www.geeksforgeeks.org/operating-systems/mutex-vs-semaphore/', companies: [] };
    mockGets({ progress: { 9: { solved: true, revised: false } } });
    const base = api.get.getMockImplementation();
    api.get.mockImplementation((url, cfg) =>
        url === '/api/questions' ? Promise.resolve({ data: [...QUESTIONS, os] }) : base(url, cfg));

    render(<App />);
    await screen.findByText('Two Sum');
    expect(screen.queryByText('Mutex vs semaphore')).not.toBeInTheDocument();

    const osTab = screen.getByRole('tab', { name: /^OS/ });
    expect(osTab).toHaveTextContent('1/1');
    await userEvent.click(osTab);

    const link = await screen.findByRole('link', { name: 'Mutex vs semaphore' });
    expect(link).toHaveAttribute('href', os.problemUrl);
    expect(screen.queryByText('Two Sum')).not.toBeInTheDocument();
    expect(screen.getByLabelText('Read')).toBeChecked();
    expect(screen.getByRole('tab', { name: /^OS/ })).toHaveAttribute('aria-selected', 'true');
});

/** Routes GET requests by URL; values may be data or a function of the axios config. */
const routeGets = (routes) => {
    api.get.mockImplementation((url, cfg) => {
        if (url in routes) {
            const r = routes[url];
            return Promise.resolve({ data: typeof r === 'function' ? r(cfg) : r });
        }
        if (url === '/api/questions') return Promise.resolve({ data: QUESTIONS });
        if (url === '/api/progress/my-status' || url === '/api/notes') return Promise.resolve({ data: {} });
        return Promise.reject({ response: { status: 404 } });
    });
};

test('Google sign-in: new users pick a unique username before the dashboard', async () => {
    process.env.REACT_APP_GOOGLE_CLIENT_ID = 'test-client.apps.googleusercontent.com';
    let googleCallback;
    window.google = { accounts: { id: {
        initialize: jest.fn(({ callback }) => { googleCallback = callback; }),
        renderButton: jest.fn(),
    } } };
    try {
        api.post.mockImplementation((url) => {
            if (url === '/api/auth/google') {
                return Promise.resolve({ data: { token: fakeToken(), username: 'google-1a2b', needsUsername: true, role: 'USER' } });
            }
            return Promise.resolve({ data: {} });
        });
        routeGets({
            '/api/users/me': { username: 'google-1a2b', email: 'Om.Sanjeet@gmail.com', role: 'USER', needsUsername: true },
            '/api/users/username-available': (cfg) => ({ available: cfg.params.username !== 'taken_name' }),
        });
        api.put.mockResolvedValueOnce({ data: { token: fakeToken(), username: 'om.sanjeet', needsUsername: false, role: 'USER' } });

        render(<App />);
        await waitFor(() => expect(window.google.accounts.id.renderButton).toHaveBeenCalled());
        await act(async () => { googleCallback({ credential: 'google-id-token' }); });
        expect(api.post).toHaveBeenCalledWith('/api/auth/google', { credential: 'google-id-token' });

        // picker, pre-filled from the Google email
        const input = await screen.findByLabelText('Username');
        await waitFor(() => expect(input).toHaveValue('om.sanjeet'));
        expect(await screen.findByText('✓ Available')).toBeInTheDocument();

        await userEvent.clear(input);
        await userEvent.type(input, 'taken_name');
        expect(await screen.findByText('That username is taken.')).toBeInTheDocument();
        expect(screen.getByRole('button', { name: 'Continue' })).toBeDisabled();

        await userEvent.clear(input);
        await userEvent.type(input, 'om.sanjeet');
        await screen.findByText('✓ Available');
        await userEvent.click(screen.getByRole('button', { name: 'Continue' }));

        expect(api.put).toHaveBeenCalledWith('/api/users/me/username', { username: 'om.sanjeet' });
        expect(await screen.findByText('@om.sanjeet')).toBeInTheDocument();
        expect(await screen.findByText('Two Sum')).toBeInTheDocument();
    } finally {
        delete process.env.REACT_APP_GOOGLE_CLIENT_ID;
        delete window.google;
    }
});

test('no Google button when no client id is configured', () => {
    render(<App />);
    expect(screen.queryByTestId('google-button')).not.toBeInTheDocument();
});

test('sign-up checks username availability while typing', async () => {
    api.get.mockResolvedValue({ data: { available: false, message: 'That username is taken.' } });
    render(<App />);
    await userEvent.click(screen.getByRole('tab', { name: 'Create account' }));
    await userEvent.type(screen.getByLabelText('Username'), 'om');
    expect(await screen.findByText(/must be 3-30 characters/)).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText('Username'), 'kar');
    expect(await screen.findByText('That username is taken.')).toBeInTheDocument();
    expect(api.get).toHaveBeenCalledWith('/api/users/username-available', { params: { username: 'omkar' } });
});

test('leaderboard shows ranks, points and highlights you', async () => {
    loggedIn();
    routeGets({
        '/api/users/me': { username: 'om', role: 'USER', needsUsername: false, showOnLeaderboard: true },
        '/api/leaderboard': {
            entries: [
                { rank: 1, username: 'priya', points: 42, completed: 20, currentStreak: 5, you: false, listed: true },
                { rank: 2, username: 'om', points: 30, completed: 18, currentStreak: 0, you: true, listed: true },
            ],
            me: { rank: 2, username: 'om', points: 30, completed: 18, currentStreak: 0, you: true, listed: true },
            totalRanked: 2,
        },
    });
    render(<App />);
    await screen.findByText('Two Sum');
    expect(screen.queryByRole('button', { name: 'Admin' })).not.toBeInTheDocument(); // plain users

    await userEvent.click(screen.getByRole('button', { name: 'Leaderboard' }));
    expect(await screen.findByText('@priya')).toBeInTheDocument();
    expect(screen.getByText(/of 2/)).toHaveTextContent("You're #2 of 2");
    const meRow = screen.getByText('you').closest('tr');
    expect(within(meRow).getByText('30')).toBeInTheDocument();
    expect(window.location.hash).toBe('#leaderboard');
});

test('admins get the Admin panel with an overview', async () => {
    loggedIn();
    routeGets({
        '/api/users/me': { username: 'om', role: 'ADMIN', needsUsername: false },
        '/api/admin/overview': {
            totalUsers: 12, verifiedUsers: 10, googleUsers: 4, newUsersLast7Days: 3, activeUsersLast7Days: 6,
            usersByRole: { USER: 10, EDITOR: 1, ADMIN: 1 }, questionsBySection: { DSA: 100, OOPS: 39, OS: 44, CN: 38 },
        },
    });
    render(<App />);
    const adminLink = await screen.findByRole('button', { name: 'Admin' });
    await userEvent.click(adminLink);
    expect(await screen.findByText('Active in the last 7 days')).toBeInTheDocument();
    expect(screen.getByText('12')).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Users' })).toBeInTheDocument();
});

test('admins can remove unverified accounts and delete a user', async () => {
    loggedIn();
    let users = [
        { id: 1, username: 'om', email: 'om@x.com', role: 'ADMIN', verified: true, google: false, showOnLeaderboard: true, createdAt: null, completed: 1, points: 1 },
        { id: 2, username: 'ghost', email: 'g@x.com', role: 'USER', verified: false, google: false, showOnLeaderboard: true, createdAt: null, completed: 0, points: 0 },
        { id: 3, username: 'real', email: 'r@x.com', role: 'USER', verified: true, google: false, showOnLeaderboard: true, createdAt: null, completed: 2, points: 3 },
    ];
    routeGets({
        '/api/users/me': { username: 'om', role: 'ADMIN', needsUsername: false },
        '/api/admin/overview': { totalUsers: 3, verifiedUsers: 2, googleUsers: 0, newUsersLast7Days: 0, activeUsersLast7Days: 0, usersByRole: {}, questionsBySection: {} },
        '/api/admin/users': () => users,
    });
    api.post.mockImplementation((url) => {
        if (url === '/api/admin/users/remove-unverified') {
            users = users.filter((u) => u.verified);
            return Promise.resolve({ data: { removed: 1 } });
        }
        return Promise.resolve({ data: {} });
    });
    api.delete.mockResolvedValue({ status: 204 });

    render(<App />);
    await userEvent.click(await screen.findByRole('button', { name: 'Admin' }));
    await userEvent.click(screen.getByRole('tab', { name: 'Users' }));
    expect(await screen.findByText('@ghost')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Delete @om' })).not.toBeInTheDocument(); // not yourself

    await userEvent.click(screen.getByRole('button', { name: 'Remove unverified (1)' }));
    await userEvent.click(screen.getByRole('button', { name: 'Remove' }));
    expect(await screen.findByText('Removed 1 unverified account.')).toBeInTheDocument();
    await waitFor(() => expect(screen.queryByText('@ghost')).not.toBeInTheDocument());

    await userEvent.click(screen.getByRole('button', { name: 'Delete @real' }));
    await userEvent.click(screen.getByRole('button', { name: 'Delete' }));
    expect(api.delete).toHaveBeenCalledWith('/api/admin/users/3');
    await waitFor(() => expect(screen.queryByText('@real')).not.toBeInTheDocument());
});
