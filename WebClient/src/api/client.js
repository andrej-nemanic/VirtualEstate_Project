import axios from 'axios';

const API_URL = '/api';

const client = axios.create({
  baseURL: API_URL
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const path = error.config?.url || '';
      const isAuthEndpoint = path.includes('/users/login') || path.includes('/users/register');
      if (!isAuthEndpoint) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        window.dispatchEvent(new Event('auth:unauthorized'));
      }
    }
    return Promise.reject(error);
  }
);

export const authApi = {
  login: (email, password) => client.post('/users/login', { email, password }),
  register: (data) => client.post('/users/register', data),
  me: () => client.get('/users/me')
};

export const propertyApi = {
  list: (params) => client.get('/properties', { params }),
  get: (id) => client.get(`/properties/${id}`),
  create: (data) => client.post('/properties', data),
  update: (id, data) => client.put(`/properties/${id}`, data),
  remove: (id) => client.delete(`/properties/${id}`),
  search: (lat, lng, distance) => client.get('/properties/search', { params: { lat, lng, distance } }),
  stats: (params) => client.get('/properties/stats', { params })
};

export default client;
