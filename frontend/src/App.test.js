import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import App from './App';
import api from './services/api';
import { computeStats, ringGradient } from './utils/stats';

jest.mock('./services/api', () => {
    const mockApi = { get: jest.fn(), post: jest.fn(), put: jest.fn() };
    return {
        __esModule: true,
        default: mockApi,
        SESSION_EXPIRED_EVENT: 'freakcode:session-expired',
        getErrorMessage: (err, fallback = 'error') =>
            err?.response?.data?.detail || fallback,
        getFieldErrors: (err) => err?.response?.data?.errors || {},
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

beforeEach(() => {
    localStorage.clear();
    jest.clearAllMocks();
});

test('shows the login screen when there is no session', () => {
    render(<App />);
    expect(screen.getByRole('button', { name: 'Log in' })).toBeInTheDocument();
    expect(screen.getByLabelText('Username')).toBeInTheDocument();
});

test('an expired stored token starts at login instead of a broken dashboard', () => {
    localStorage.setItem('token', fakeToken(-60));
    render(<App />);
    expect(screen.getByRole('button', { name: 'Log in' })).toBeInTheDocument();
    expect(api.get).not.toHaveBeenCalled();
});

test('logs in, loads the dashboard and shows real progress numbers', async () => {
    api.post.mockResolvedValueOnce({ data: { token: fakeToken(), username: 'om' } });
    api.get.mockImplementation((url) => Promise.resolve(
        url === '/api/questions' ? { data: QUESTIONS } : { data: { 1: { solved: true, revised: false } } }
    ));

    render(<App />);
    await userEvent.type(screen.getByLabelText('Username'), 'om');
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
    localStorage.setItem('token', fakeToken());
    localStorage.setItem('username', 'om');
    api.get.mockImplementation((url) => Promise.resolve(url === '/api/questions' ? { data: QUESTIONS } : { data: {} }));
    api.put.mockResolvedValueOnce({ data: { solved: true, revised: false } });

    render(<App />);
    const row = (await screen.findByText('Two Sum')).closest('li');
    const solved = within(row).getByLabelText('Solved');

    await userEvent.click(solved);
    expect(api.put).toHaveBeenCalledWith('/api/progress/1', { solved: true, revised: false });
    await waitFor(() => expect(solved).toBeChecked());
    // Checkboxes are disabled while a save is in flight; wait for it to finish.
    await waitFor(() => expect(within(row).getByLabelText('Revised')).not.toBeDisabled());

    api.put.mockRejectedValueOnce({ response: { status: 500, data: { detail: 'Server exploded' } } });
    await userEvent.click(within(row).getByLabelText('Revised'));
    await waitFor(() => expect(within(row).getByLabelText('Revised')).not.toBeChecked());
    expect(await screen.findByRole('alert')).toHaveTextContent('Server exploded');
});

test('a session-expired event returns the user to login with an explanation', async () => {
    localStorage.setItem('token', fakeToken());
    api.get.mockImplementation((url) => Promise.resolve(url === '/api/questions' ? { data: QUESTIONS } : { data: {} }));

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
