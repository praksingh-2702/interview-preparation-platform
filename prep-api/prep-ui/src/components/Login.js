import React, { useState } from 'react';
import api from '../services/api';

const Login = ({ onLoginSuccess }) => { // Retained callback prop
    const [isSignUp, setIsSignUp] = useState(false); // Toggle state
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState(''); // Tracking state for dynamic signup field
    const [password, setPassword] = useState('');
    const [message, setMessage] = useState('');

    const handleSubmit = async (e) => {
        e.preventDefault();
        setMessage('');

        try {
            if (isSignUp) {
                // 1. Fire registration request
                await api.post('/api/auth/register', { 
                    username: username, 
                    email: email, 
                    password: password 
                });
                setMessage('Registration successful! Please log in.');
                setIsSignUp(false); // Flip UI view back to login mode automatically
                setPassword('');
            } else {
                // 2. Fire authenticating request
                const response = await api.post('/api/auth/login', {
                    username: username,
                    password: password
                });
                
                const token = response.data.token;
                if (token) {
                    localStorage.setItem('token', token);
                    setMessage('Login successful! Token stored.');
                    setTimeout(() => {
                        onLoginSuccess(); // Fires view alteration block
                    }, 500);
                }
            }
        } catch (error) {
            // Keep your exact error string format intact
            setMessage((isSignUp ? 'Registration failed: ' : 'Login failed: ') + 
                (error.response?.data?.message || error.response?.data || error.message));
        }
    };

    return (
        <div style={{ maxWidth: '300px', margin: '50px auto', textAlign: 'center' }}>
            <h2>{isSignUp ? 'PrepMind Register' : 'PrepMind Login'}</h2>
            <form onSubmit={handleSubmit}>
                <div style={{ marginBottom: '10px' }}>
                    <input 
                        type="text" 
                        placeholder="Username" 
                        value={username} 
                        onChange={(e) => setUsername(e.target.value)} 
                        required 
                        style={{ width: '100%', padding: '8px', boxSizing: 'border-box' }}
                    />
                </div>

                {/* Conditionally render Email field only when signing up */}
                {isSignUp && (
                    <div style={{ marginBottom: '10px' }}>
                        <input 
                            type="email" 
                            placeholder="Email Address" 
                            value={email} 
                            onChange={(e) => setEmail(e.target.value)} 
                            required 
                            style={{ width: '100%', padding: '8px', boxSizing: 'border-box' }}
                        />
                    </div>
                )}

                <div style={{ marginBottom: '10px' }}>
                    <input 
                        type="password" 
                        placeholder="Password" 
                        value={password} 
                        onChange={(e) => setPassword(e.target.value)} 
                        required 
                        style={{ width: '100%', padding: '8px', boxSizing: 'border-box' }}
                    />
                </div>

                <button type="submit" style={{ width: '100%', padding: '10px', cursor: 'pointer' }}>
                    {isSignUp ? 'Sign Up' : 'Login'}
                </button>
            </form>

            {message && <p style={{ marginTop: '15px', color: 'blue' }}>{message}</p>}

            {/* Toggle trigger link to switch views dynamically */}
            <p style={{ marginTop: '20px', fontSize: '14px', fontFamily: 'sans-serif' }}>
                {isSignUp ? "Already have an account? " : "Don't have an account? "}
                <span 
                    onClick={() => {
                        setIsSignUp(!isSignUp);
                        setMessage('');
                    }} 
                    style={{ color: '#0066cc', cursor: 'pointer', textDecoration: 'underline' }}
                >
                    {isSignUp ? 'Login here' : 'Register here'}
                </span>
            </p>
        </div>
    );
};

export default Login;