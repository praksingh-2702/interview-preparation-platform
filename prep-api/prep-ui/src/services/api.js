import axios from 'axios';

// Create an instance pointing to your Spring Boot backend
const api = axios.create({
    baseURL: 'http://localhost:8080',
});

// Automatically intercept requests and attach the JWT token if it exists
api.interceptors.request.use(config => {
    const token = localStorage.getItem('token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export default api;