import React from 'react';

/** The line under a username input: live availability, or the server's error. */
const UsernameStatus = ({ id, check, error, hint }) => {
    if (error) return <span className="field-error" id={id}>{error}</span>;
    if (check.status === 'ok') return <span className="field-ok" id={id}>{check.message}</span>;
    if (check.status === 'taken' || check.status === 'invalid') {
        return <span className="field-error" id={id}>{check.message}</span>;
    }
    if (check.status === 'checking') return <span className="field-hint" id={id}>{check.message}</span>;
    return hint ? <span className="field-hint" id={id}>{hint}</span> : <span id={id} />;
};

export default UsernameStatus;
