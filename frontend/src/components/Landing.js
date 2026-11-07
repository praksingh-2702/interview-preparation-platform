import React, { useEffect } from 'react';
import './Landing.css';

const NAME = 'FreakCode';
const ACCENT_FROM = 5; // letters from index 5 ("Code") use the accent colour, like <Brand />

const TOPICS = ['DSA', 'OOPS', 'OS', 'CN'];

// Fixed values (not random) so the page renders the same every time and tests stay stable.
const GLYPHS = [
    { c: '{ }', left: 6, dur: 19, delay: 0, size: 22 },
    { c: '</>', left: 17, dur: 24, delay: 6, size: 18 },
    { c: ';', left: 27, dur: 17, delay: 3, size: 26 },
    { c: '[ ]', left: 38, dur: 22, delay: 9, size: 20 },
    { c: '=>', left: 49, dur: 26, delay: 1, size: 18 },
    { c: '( )', left: 59, dur: 20, delay: 12, size: 24 },
    { c: '#', left: 69, dur: 23, delay: 5, size: 20 },
    { c: '&&', left: 78, dur: 18, delay: 8, size: 18 },
    { c: '0 1', left: 88, dur: 25, delay: 2, size: 22 },
    { c: '{ }', left: 94, dur: 21, delay: 11, size: 16 },
];

// Render's free backend sleeps when idle (cold start can take minutes). Pinging its health
// endpoint while the visitor reads this page often means it is awake by the time they sign up.
const wakeBackend = () => {
    if (process.env.NODE_ENV === 'test' || typeof fetch !== 'function') return;
    const base = process.env.REACT_APP_API_URL || 'http://localhost:8080';
    try {
        // no-cors: we only need the request to reach the server, not to read the answer.
        fetch(`${base}/actuator/health`, { mode: 'no-cors' }).catch(() => {});
    } catch {
        /* best effort only */
    }
};

const Landing = ({ onGetStarted, onLogin }) => {
    useEffect(() => {
        wakeBackend();
    }, []);

    return (
        <div className="landing">
            <div className="landing-glyphs" aria-hidden="true">
                {GLYPHS.map((g, i) => (
                    <span key={i} className="landing-glyph"
                          style={{
                              left: `${g.left}%`,
                              fontSize: `${g.size}px`,
                              animationDuration: `${g.dur}s`,
                              animationDelay: `${g.delay}s`,
                          }}>
                        {g.c}
                    </span>
                ))}
            </div>

            <main className="landing-inner">
                {/* aria-label gives screen readers the whole word; the animated letters are hidden from them. */}
                <h1 className="landing-title" aria-label={NAME}>
                    {NAME.split('').map((ch, i) => (
                        <span key={i} aria-hidden="true"
                              className={`landing-letter ${i >= ACCENT_FROM ? 'landing-letter-accent' : ''}`}
                              style={{ '--i': i }}>
                            {ch}
                        </span>
                    ))}
                    <span className="landing-cursor" aria-hidden="true" />
                </h1>

                <p className="landing-tagline landing-fade" style={{ '--d': '2.6s' }}>
                    Your placement prep, tracked.
                </p>

                <p className="landing-desc landing-fade" style={{ '--d': '3s' }}>
                    FreakCode is a free study tracker for placement interviews. Work through curated
                    problems and core theory topics, tick them off, and let streaks, spaced-repetition
                    revision and a leaderboard keep you consistent.
                </p>

                <ul className="landing-topics landing-fade" style={{ '--d': '3.3s' }} aria-label="Topics covered">
                    {TOPICS.map((t) => <li key={t} className="landing-topic">{t}</li>)}
                </ul>

                <div className="landing-actions landing-fade" style={{ '--d': '3.6s' }}>
                    <button type="button" className="btn btn-primary landing-cta" onClick={onGetStarted}>
                        Get started
                    </button>
                    <button type="button" className="btn btn-ghost landing-cta" onClick={onLogin}>
                        I already have an account
                    </button>
                </div>

                <p className="landing-legal landing-fade" style={{ '--d': '4s' }}>
                    <a href="/privacy.html">Privacy Policy</a> · <a href="/terms.html">Terms</a>
                </p>
            </main>
        </div>
    );
};

export default Landing;
