import React, { useCallback, useEffect, useState } from 'react';
import api, { getErrorMessage } from '../services/api';
import { browserTimeZone } from '../utils/time';

const MEDALS = { 1: '🥇', 2: '🥈', 3: '🥉' };

const Leaderboard = ({ onOpenSettings }) => {
    const [board, setBoard] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const load = useCallback(async () => {
        setLoading(true);
        setError('');
        try {
            const { data } = await api.get('/api/leaderboard', { params: { tz: browserTimeZone(), limit: 50 } });
            setBoard(data);
        } catch (err) {
            if (err?.response?.status !== 401) setError(getErrorMessage(err, "Couldn't load the leaderboard."));
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { load(); }, [load]);

    const me = board?.me;
    const entries = board?.entries || [];

    return (
        <main className="container main">
            <section className="card board-head">
                <div>
                    <h2 className="page-title">Leaderboard</h2>
                    <p className="field-hint board-rules">
                        Every row you tick (Solved or Read) in any section scores <strong>Easy 1</strong>, <strong>Medium 2</strong>,{' '}
                        <strong>Hard 3</strong>. Ties go to the longer current streak.
                    </p>
                </div>
                <button type="button" className="btn btn-ghost btn-small" onClick={load} disabled={loading}>
                    {loading ? 'Loading…' : 'Refresh'}
                </button>
            </section>

            {error && (
                <div className="alert alert-error" role="alert">
                    {error} <button type="button" className="btn btn-small" onClick={load}>Retry</button>
                </div>
            )}

            {me && (
                <section className="card board-me" aria-label="Your standing">
                    {me.rank ? (
                        <p>You're <strong className="board-rank">#{me.rank}</strong> of {board.totalRanked}</p>
                    ) : me.listed ? (
                        <p>Tick your first question to get on the board.</p>
                    ) : (
                        <p>
                            You're hidden from the leaderboard.{' '}
                            <button type="button" className="link-button" onClick={onOpenSettings}>Change in Settings</button>
                        </p>
                    )}
                    <div className="board-me-stats">
                        <span><strong>{me.points}</strong> points</span>
                        <span><strong>{me.completed}</strong> done</span>
                        <span><strong>{me.currentStreak}</strong> day streak</span>
                    </div>
                </section>
            )}

            {!loading && !error && entries.length === 0 && (
                <p className="empty">No one is on the board yet. Tick a question and be the first!</p>
            )}

            {entries.length > 0 && (
                <div className="card board-table-wrap">
                    <table className="board-table">
                        <thead>
                            <tr>
                                <th scope="col">#</th>
                                <th scope="col">User</th>
                                <th scope="col" className="num">Points</th>
                                <th scope="col" className="num">Done</th>
                                <th scope="col" className="num">Streak</th>
                            </tr>
                        </thead>
                        <tbody>
                            {entries.map((e) => (
                                <tr key={e.username} className={e.you ? 'board-you' : ''}>
                                    <td className="board-rank-cell">{MEDALS[e.rank] || e.rank}</td>
                                    <td>@{e.username}{e.you && <span className="pill pill-you">you</span>}</td>
                                    <td className="num"><strong>{e.points}</strong></td>
                                    <td className="num">{e.completed}</td>
                                    <td className="num">{e.currentStreak > 0 ? `${e.currentStreak} 🔥` : '0'}</td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                </div>
            )}
        </main>
    );
};

export default Leaderboard;
