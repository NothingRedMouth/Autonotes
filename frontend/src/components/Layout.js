import React, { useState } from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { getUsernameFromToken, logout } from '../services/authService';

const Layout = ({ children }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const username = getUsernameFromToken();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isActive = (path) => {
    return location.pathname === path;
  };

  const toggleMobileMenu = () => {
    setMobileMenuOpen(!mobileMenuOpen);
  };

  return (
    <div style={{ minHeight: '100vh', backgroundColor: 'var(--background-color)', display: 'flex', flexDirection: 'column' }}>

      <header style={{
        backgroundColor: 'var(--surface-color)',
        borderBottom: '1px solid var(--border-color)',
        boxShadow: 'var(--shadow-sm)',
        position: 'sticky',
        top: 0,
        zIndex: 50
      }}>
        <div className="container" style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          height: 72,
          padding: 0
        }}>

          <Link
            to="/dashboard"
            style={{
              fontSize: 'var(--font-size-xl)',
              fontWeight: 'bold',
              color: 'var(--primary-color)',
              textDecoration: 'none',
              display: 'flex',
              alignItems: 'center',
              gap: 'var(--spacing-2)'
            }}
          >
            <span style={{ fontSize: '1.5em' }}>🎓</span>
            <span>Autonotes</span>
          </Link>

          <nav style={{
            display: 'none',
            alignItems: 'center',
            gap: 'var(--spacing-6)'
          }} className="desktop-nav">
            <Link
              to="/dashboard"
              style={{
                padding: 'var(--spacing-2) var(--spacing-4)',
                borderRadius: 'var(--radius-md)',
                textDecoration: 'none',
                color: isActive('/dashboard') ? 'var(--primary-color)' : 'var(--text-secondary)',
                backgroundColor: isActive('/dashboard') ? 'var(--primary-light)' : 'transparent',
                fontWeight: isActive('/dashboard') ? '600' : '500',
                transition: 'all var(--transition-fast)'
              }}
            >
              📚 Конспекты
            </Link>
            <Link
              to="/upload"
              className="btn btn-primary"
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: 'var(--spacing-2)'
              }}
            >
              ➕ Новый конспект
            </Link>
          </nav>

          {/* User Menu */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--spacing-4)' }}>
            <Link
              to="/profile"
              style={{
                padding: 'var(--spacing-2) var(--spacing-4)',
                borderRadius: 'var(--radius-md)',
                textDecoration: 'none',
                color: isActive('/profile') ? 'var(--primary-color)' : 'var(--text-secondary)',
                backgroundColor: isActive('/profile') ? 'var(--primary-light)' : 'transparent',
                fontWeight: '500',
                display: 'flex',
                alignItems: 'center',
                gap: 'var(--spacing-2)',
                transition: 'all var(--transition-fast)'
              }}
            >
              <span>👤</span>
              <span style={{ display: 'none' }} className="desktop-username">{username}</span>
            </Link>
            <button
              onClick={handleLogout}
              className="btn btn-ghost"
              style={{
                padding: 'var(--spacing-2) var(--spacing-4)',
                fontSize: 'var(--font-size-sm)'
              }}
            >
              🚪 Выйти
            </button>
            <button
              onClick={toggleMobileMenu}
              style={{
                display: 'none',
                background: 'none',
                border: 'none',
                cursor: 'pointer',
                padding: 'var(--spacing-2)',
                borderRadius: 'var(--radius-md)',
                color: 'var(--text-secondary)'
              }}
              className="mobile-menu-btn"
            >
              {mobileMenuOpen ? '✕' : '☰'}
            </button>
          </div>
        </div>

        {mobileMenuOpen && (
          <div style={{
            display: 'none',
            backgroundColor: 'var(--surface-color)',
            borderTop: '1px solid var(--border-color)',
            padding: 'var(--spacing-4) 0'
          }} className="mobile-nav">
            <div className="container" style={{ padding: 0 }}>
              <nav style={{ display: 'flex', flexDirection: 'column', gap: 'var(--spacing-2)' }}>
                <Link
                  to="/dashboard"
                  onClick={() => setMobileMenuOpen(false)}
                  style={{
                    padding: 'var(--spacing-3) var(--spacing-4)',
                    borderRadius: 'var(--radius-md)',
                    textDecoration: 'none',
                    color: isActive('/dashboard') ? 'var(--primary-color)' : 'var(--text-secondary)',
                    backgroundColor: isActive('/dashboard') ? 'var(--primary-light)' : 'transparent',
                    fontWeight: isActive('/dashboard') ? '600' : '500'
                  }}
                >
                  📚 Конспекты
                </Link>
                <Link
                  to="/upload"
                  onClick={() => setMobileMenuOpen(false)}
                  style={{
                    padding: 'var(--spacing-3) var(--spacing-4)',
                    borderRadius: 'var(--radius-md)',
                    textDecoration: 'none',
                    color: 'var(--primary-color)',
                    backgroundColor: 'var(--primary-light)',
                    fontWeight: '600'
                  }}
                >
                  ➕ Новый конспект
                </Link>
              </nav>
            </div>
          </div>
        )}
      </header>

      <main style={{ flex: 1, padding: 'var(--spacing-8) 0' }}>
        <div className="container">
          {children}
        </div>
      </main>

      <footer style={{
        backgroundColor: 'var(--surface-color)',
        borderTop: '1px solid var(--border-color)',
        padding: 'var(--spacing-8) 0',
        marginTop: 'auto'
      }}>
        <div className="container" style={{ textAlign: 'center', color: 'var(--text-secondary)', fontSize: 'var(--font-size-sm)' }}>
          <p>✨ Autonotes — автоматическое создание конспектов лекций с помощью ИИ</p>
          <p style={{ marginTop: 'var(--spacing-2)', fontSize: 'var(--font-size-xs)' }}>
            Создавайте конспекты из фотографий досок за секунды
          </p>
        </div>
      </footer>
    </div>
  );
};

export default Layout;