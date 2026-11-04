import React from 'react';
import Brand from './Brand';

const NAV = [
    { id: 'dashboard', label: 'Practice' },
    { id: 'leaderboard', label: 'Leaderboard' },
    { id: 'admin', label: 'Admin', roles: ['EDITOR', 'ADMIN'] },
];

const TopBar = ({ username, role, view, onNavigate, onOpenSettings, onLogout }) => (
    <header className="topbar">
        <div className="container topbar-inner">
            <Brand as="h1" />
            <nav className="topnav" aria-label="Main">
                {NAV.filter((n) => !n.roles || n.roles.includes(role)).map((n) => (
                    <button key={n.id} type="button"
                            className={`nav-link ${view === n.id ? 'nav-active' : ''}`}
                            aria-current={view === n.id ? 'page' : undefined}
                            onClick={() => onNavigate(n.id)}>
                        {n.label}
                    </button>
                ))}
            </nav>
            <div className="topbar-user">
                <button type="button" className="btn btn-ghost user-button" onClick={onOpenSettings}
                        aria-label={`Settings for @${username}`} title="Settings">
                    {username && <span className="username">@{username}</span>}
                    <span aria-hidden="true">⚙</span>
                </button>
                <button type="button" className="btn btn-ghost" onClick={onLogout}>Log out</button>
            </div>
        </div>
    </header>
);

export default TopBar;
