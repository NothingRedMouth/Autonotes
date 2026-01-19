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
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'linear-gradient(135deg, var(--primary-color) 0%, var(--primary-hover) 100%)',
      padding: 'var(--spacing-4)'
    }}>
      <div className="card fade-in" style={{
        width: '100%',
        maxWidth: 420,
        padding: 'var(--spacing-8)',
        boxShadow: 'var(--shadow-lg)'
      }}>
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: 'var(--spacing-8)' }}>
          <div style={{
            width: 64,
            height: 64,
            backgroundColor: 'var(--primary-light)',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            margin: '0 auto var(--spacing-4)',
            fontSize: '2rem'
          }}>
            {isRegister ? '👤' : '🔐'}
          </div>
          <h1 style={{
            fontSize: 'var(--font-size-2xl)',
            fontWeight: '700',
            color: 'var(--text-primary)',
            margin: 0
          }}>
            {isRegister ? 'Создать аккаунт' : 'Добро пожаловать'}
          </h1>
          <p style={{
            color: 'var(--text-secondary)',
            margin: 'var(--spacing-2) 0 0 0',
            fontSize: 'var(--font-size-sm)'
          }}>
            {isRegister ? 'Присоединяйтесь к сообществу Autonotes' : 'Войдите в свой аккаунт'}
          </p>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} style={{ marginBottom: 'var(--spacing-6)' }}>
          <div className="form-group">
            <label className="form-label">Имя пользователя</label>
            <input
              type="text"
              name="username"
              value={formData.username}
              onChange={handleChange}
              required
              className="form-input"
              placeholder="Введите имя пользователя"
            />
          </div>

          {isRegister && (
            <div className="form-group">
              <label className="form-label">Email адрес</label>
              <input
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                required
                className="form-input"
                placeholder="your@email.com"
              />
            </div>
          )}

          <div className="form-group">
            <label className="form-label">Пароль</label>
            <input
              type="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              required
              className="form-input"
              placeholder="••••••••"
              minLength={6}
            />
          </div>
          <button
            type="submit"
            disabled={loading}
            className="btn btn-primary"
            style={{
              width: '100%',
              padding: 'var(--spacing-4)',
              fontSize: 'var(--font-size-base)',
              fontWeight: '600'
            }}
          >
            {loading ? (
              <>
                <span className="loading-spinner" style={{ marginRight: 'var(--spacing-2)' }}></span>
                {isRegister ? 'Создание аккаунта...' : 'Вход...'}
              </>
            ) : (
              isRegister ? 'Создать аккаунт' : 'Войти'
            )}
          </button>
        </form>

        {/* Error Message */}
        {error && (
          <div style={{
            padding: 'var(--spacing-4)',
            backgroundColor: '#fef2f2',
            border: '1px solid var(--error-color)',
            borderRadius: 'var(--radius-md)',
            color: 'var(--error-color)',
            fontSize: 'var(--font-size-sm)',
            marginBottom: 'var(--spacing-6)'
          }}>
            <span style={{ fontWeight: '500' }}>Ошибка:</span> {error}
          </div>
        )}

        {/* Footer */}
        <div style={{ textAlign: 'center', paddingTop: 'var(--spacing-4)', borderTop: '1px solid var(--border-color)' }}>
          <p style={{ color: 'var(--text-secondary)', fontSize: 'var(--font-size-sm)', margin: 0 }}>
            {isRegister ? 'Уже есть аккаунт?' : 'Нет аккаунта?'}
            <Link
              to={isRegister ? '/login' : '/register'}
              style={{
                color: 'var(--primary-color)',
                textDecoration: 'none',
                fontWeight: '600',
                marginLeft: 'var(--spacing-1)'
              }}
            >
              {isRegister ? 'Войти' : 'Зарегистрироваться'}
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default AuthForm;