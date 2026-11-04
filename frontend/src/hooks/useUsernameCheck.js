import { useEffect, useState } from 'react';
import api from '../services/api';
import { usernameProblem } from '../utils/usernames';

const IDLE = { status: 'idle', message: '' };

/**
 * Live "is this username free?" check while typing (debounced).
 * status: idle | same | invalid | checking | ok | taken | error
 * current: the user's present name, which doesn't need checking.
 */
const useUsernameCheck = (value, { enabled = true, current = null, delay = 400 } = {}) => {
    const [state, setState] = useState(IDLE);

    useEffect(() => {
        const name = (value || '').trim();
        if (!enabled || !name) {
            setState(IDLE);
            return undefined;
        }
        if (current && name === current) {
            setState({ status: 'same', message: '' });
            return undefined;
        }
        const problem = usernameProblem(name);
        if (problem) {
            setState({ status: 'invalid', message: problem });
            return undefined;
        }
        setState({ status: 'checking', message: 'Checking…' });
        let cancelled = false;
        const timer = setTimeout(async () => {
            try {
                const res = await api.get('/api/users/username-available', { params: { username: name } });
                if (cancelled) return;
                const data = res?.data || {};
                setState(data.available
                    ? { status: 'ok', message: '✓ Available' }
                    : { status: 'taken', message: data.message || 'That username is taken.' });
            } catch {
                if (!cancelled) setState({ status: 'error', message: '' }); // the server re-checks on submit
            }
        }, delay);
        return () => {
            cancelled = true;
            clearTimeout(timer);
        };
    }, [value, enabled, current, delay]);

    return state;
};

export default useUsernameCheck;
