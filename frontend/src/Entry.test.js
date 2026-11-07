import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Entry from './components/Entry';
import api from './services/api';
import { clearStartMode } from './utils/startMode';

jest.mock('./services/api', () => {
    const mockApi = { get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn() };
    return {
        __esModule: true,
        default: mockApi,
        SESSION_EXPIRED_EVENT: 'freakcode:session-expired',
        getErrorMessage: (err, fallback = 'error') => err?.response?.data?.detail || fallback,
        getFieldErrors: (err) => err?.response?.data?.errors || {},
        getErrorCode: (err) => err?.response?.data?.code || null,
        getRetryAfter: () => null,
    };
});

const fakeToken = () => {
    const payload = btoa(JSON.stringify({ sub: 'om', exp: Math.floor(Date.now() / 1000) + 3600 }));
    return `header.${payload}.sig`;
};

beforeEach(() => {
    localStorage.clear();
    sessionStorage.clear();
    clearStartMode();
    window.history.replaceState(null, '', '/');
});

test('first visit shows the landing page, not the login form', () => {
    render(<Entry />);
    expect(screen.getByRole('heading', { name: 'FreakCode' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Get started' })).toBeInTheDocument();
    expect(screen.queryByLabelText('Username or email')).not.toBeInTheDocument();
});

test('Get started opens the create-account screen', async () => {
    render(<Entry />);
    await userEvent.click(screen.getByRole('button', { name: 'Get started' }));
    expect(screen.getByRole('tab', { name: 'Create account' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByLabelText('Email')).toBeInTheDocument();
});

test('"I already have an account" opens the log-in screen', async () => {
    render(<Entry />);
    await userEvent.click(screen.getByRole('button', { name: 'I already have an account' }));
    expect(screen.getByRole('tab', { name: 'Log in' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByLabelText('Username or email')).toBeInTheDocument();
});

test('someone with a valid session skips the landing page', async () => {
    api.get.mockImplementation((url) => {
        if (url === '/api/questions') return Promise.resolve({ data: [] });
        if (url === '/api/progress/my-status' || url === '/api/notes') return Promise.resolve({ data: {} });
        return Promise.reject({ response: { status: 404 } });
    });
    localStorage.setItem('token', fakeToken());
    localStorage.setItem('username', 'om');
    render(<Entry />);
    expect(await screen.findByText('@om')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Get started' })).not.toBeInTheDocument();
});

test('after the landing page was seen in this tab, a reload starts at login', () => {
    sessionStorage.setItem('freakcode:landing-seen', '1');
    render(<Entry />);
    expect(screen.queryByRole('button', { name: 'Get started' })).not.toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Log in' })).toHaveAttribute('aria-selected', 'true');
});
