import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { getUsernameFromToken, logout } from '../services/authService';

const Layout = ({ children }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const username = getUsernameFromToken();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isActive = (path) => {
    return location.pathname === path;
  };

  return (
    <div style={{ minHeight: '100vh', backgroundColor: '#f8f9fa' }}>
      {/* Header */}
      <header style={{
        backgroundColor: 'white',
        borderBottom: '1px solid #e5e7eb',
        padding: '0 20px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
      }}>
        <div style={{
          maxWidth: 1200,
          margin: '0 auto',
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          height: 64
        }}>
          {/* Logo and Navigation */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 40 }}>
            <Link 
              to="/dashboard" 
              style={{
                fontSize: '20px',
                fontWeight: 'bold',
                color: '#007bff',
                textDecoration: 'none'
              }}
            >
              🎓 Autonotes
            </Link>
            
            <nav style={{ display: 'flex', gap: 24 }}>
              <Link 
                to="/dashboard"
                style={{
                  padding: '8px 16px',
                  borderRadius: 6,
                  textDecoration: 'none',
                  color: isActive('/dashboard') ? '#007bff' : '#6b7280',
                  backgroundColor: isActive('/dashboard') ? '#eff6ff' : 'transparent',
                  fontWeight: isActive('/dashboard') ? '600' : '400'
                }}
              >
                Конспекты
              </Link>
              <Link 
                to="/upload"
                style={{
                  padding: '8px 16px',
                  borderRadius: 6,
                  textDecoration: 'none',
                  color: isActive('/upload') ? '#007bff' : '#6b7280',
                  backgroundColor: isActive('/upload') ? '#eff6ff' : 'transparent',
                  fontWeight: isActive('/upload') ? '600' : '400'
                }}
              >
                + Новый конспект
              </Link>
            </nav>
          </div>

          {/* User Menu */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
            <Link 
              to="/profile"
              style={{
                padding: '8px 16px',
                borderRadius: 6,
                textDecoration: 'none',
                color: isActive('/profile') ? '#007bff' : '#6b7280',
                backgroundColor: isActive('/profile') ? '#eff6ff' : 'transparent',
                fontWeight: '500'
              }}
            >
              👤 {username}
            </Link>
            <button
              onClick={handleLogout}
              style={{
                padding: '8px 16px',
                border: '1px solid #d1d5db',
                borderRadius: 6,
                backgroundColor: 'white',
                color: '#6b7280',
                cursor: 'pointer',
                fontSize: '14px'
              }}
            >
              Выйти
            </button>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main style={{ padding: '20px 0' }}>
        {children}
      </main>

      {/* Footer */}
      <footer style={{
        backgroundColor: 'white',
        borderTop: '1px solid #e5e7eb',
        padding: '20px',
        marginTop: 'auto'
      }}>
        <div style={{
          maxWidth: 1200,
          margin: '0 auto',
          textAlign: 'center',
          color: '#6b7280',
          fontSize: '14px'
        }}>
          <p>Autonotes — автоматическое создание конспектов лекций © 2024</p>
        </div>
      </footer>
    </div>
  );
};

export default Layout;