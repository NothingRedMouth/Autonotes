import api from '../api/axios';

const getUsernameFromToken = () => {
  const token = localStorage.getItem('token');
  if (!token) return null;

  try {
    const payload = token.split('.')[1];
    const decodedPayload = atob(payload);
    const parsedPayload = JSON.parse(decodedPayload);
    return parsedPayload.sub;
  } catch (error) {
    console.error("Failed to decode token", error);
    return null;
  }
};

const handleError = (error) => {
  if (error.response?.data) {
    const message = error.response.data.message || error.response.data.error;
    const status = error.response.status;
    const customError = new Error(message);
    customError.status = status;
    throw customError;
  }
  throw error;
};

export const register = async (username, email, password) => {
  try {
    const response = await api.post('/auth/register', { username, email, password });
    return response.data.token;
  } catch (error) {
    handleError(error);
  }
};

export const login = async (username, password) => {
  try {
    const response = await api.post('/auth/login', { username, password });
    return response.data.token;
  } catch (error) {
    handleError(error);
  }
};

export const getProfile = async (username) => {
  try {
    const response = await api.get(`/users/${username}`);
    return response.data;
  } catch (error) {
    handleError(error);
  }
};

export const logout = () => {
  localStorage.removeItem('token');
  window.location.href = '/login';
};

export { getUsernameFromToken };