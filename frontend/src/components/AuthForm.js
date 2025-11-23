import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { register, login } from '../services/authService';

const AuthForm = ({ mode = 'login' }) => {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: ''
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const isRegister = mode === 'register';

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      let token;
      if (isRegister) {
        token = await register(formData.username, formData.email, formData.password);
      } else {
        token = await login(formData.username, formData.password);
      }
      
      localStorage.setItem('token', token);
      navigate('/profile');
    } catch (err) {
      setError(err.message || 'Произошла ошибка');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ maxWidth: 400, margin: '50px auto', padding: 20 }}>
      <h2>{isRegister ? 'Регистрация' : 'Вход'}</h2>
      
      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 15 }}>
          <label>Имя пользователя:</label>
          <input
            type="text"
            name="username"
            value={formData.username}
            onChange={handleChange}
            required
            style={{ width: '100%', padding: 8 }}
          />
        </div>

        {isRegister && (
          <div style={{ marginBottom: 15 }}>
            <label>Email:</label>
            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              required
              style={{ width: '100%', padding: 8 }}
            />
          </div>
        )}

        <div style={{ marginBottom: 15 }}>
          <label>Пароль:</label>
          <input
            type="password"
            name="password"
            value={formData.password}
            onChange={handleChange}
            required
            style={{ width: '100%', padding: 8 }}
          />
        </div>

        <button 
          type="submit" 
          disabled={loading}
          style={{ width: '100%', padding: 10, background: '#007bff', color: 'white', border: 'none' }}
        >
          {loading ? 'Загрузка...' : (isRegister ? 'Зарегистрироваться' : 'Войти')}
        </button>
      </form>

      {error && (
        <div style={{ color: 'red', marginTop: 15, padding: 10, background: '#ffeaea' }}>
          {error}
        </div>
      )}

      <div style={{ marginTop: 20, textAlign: 'center' }}>
        {isRegister ? (
          <p>Уже есть аккаунт? <Link to="/login">Войдите</Link></p>
        ) : (
          <p>Нет аккаунта? <Link to="/register">Зарегистрируйтесь</Link></p>
        )}
      </div>
    </div>
  );
};

export default AuthForm;