import React, { useEffect, useState } from 'react';
import { getProfile, logout, getUsernameFromToken } from '../services/authService';
import { getAllNotes } from '../services/noteService';

const Profile = () => {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [stats, setStats] = useState({
    totalNotes: 0,
    processedNotes: 0,
    totalSize: 0
  });

  useEffect(() => {
    const fetchProfileAndStats = async () => {
      try {
        // Получаем имя пользователя динамически из токена!
        const username = getUsernameFromToken();
        if (!username) {
            throw new Error('Не удалось получить имя пользователя из токена.');
        }

        // Получаем профиль
        const profileData = await getProfile(username);
        setProfile(profileData);

        // Получаем конспекты для статистики
        const notes = await getAllNotes();

        // Считаем статистику
        const totalNotes = notes.length;
        const processedNotes = notes.filter(note => note.status === 'COMPLETED').length;

        // Считаем общий размер (предполагаем, что размер можно получить из images)
        // Если размер недоступен, оставляем 0
        const totalSize = notes.reduce((size, note) => {
          const noteSize = note.images?.reduce((imgSize, img) => {
            // Если размер указан в байтах, конвертируем в MB
            return imgSize + (img.size || 0);
          }, 0) || 0;
          return size + noteSize;
        }, 0);

        setStats({
          totalNotes,
          processedNotes,
          totalSize: Math.round(totalSize / (1024 * 1024)) // в MB
        });

      } catch (err) {
        setError(err.message || 'Ошибка загрузки профиля');
      } finally {
        setLoading(false);
      }
    };

    fetchProfileAndStats();
  }, []);

  const handleLogout = () => {
    logout();
  };

  if (loading) {
    return (
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '50vh',
        gap: 'var(--spacing-4)'
      }}>
        <div className="loading-spinner" style={{ width: '2rem', height: '2rem' }}></div>
        <p style={{ color: 'var(--text-secondary)', fontSize: 'var(--font-size-lg)' }}>
          Загрузка профиля...
        </p>
      </div>
    );
  }

  if (error) {
    return (
      <div style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '50vh',
        gap: 'var(--spacing-4)'
      }}>
        <div style={{
          fontSize: '3rem',
          color: 'var(--error-color)',
          marginBottom: 'var(--spacing-4)'
        }}>
          ⚠️
        </div>
        <h2 style={{ color: 'var(--text-primary)', margin: 0 }}>Ошибка загрузки</h2>
        <p style={{ color: 'var(--text-secondary)', textAlign: 'center' }}>{error}</p>
      </div>
    );
  }

  return (
    <div className="slide-up">
      {/* Header */}
      <div style={{
        textAlign: 'center',
        marginBottom: 'var(--spacing-8)',
        maxWidth: 600,
        margin: '0 auto var(--spacing-8) auto'
      }}>
        <div style={{
          width: 80,
          height: 80,
          backgroundColor: 'var(--primary-light)',
          borderRadius: '50%',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          margin: '0 auto var(--spacing-6)',
          fontSize: '2.5rem'
        }}>
          👤
        </div>
        <h1 style={{
          fontSize: 'var(--font-size-3xl)',
          fontWeight: '700',
          color: 'var(--text-primary)',
          margin: '0 0 var(--spacing-3) 0'
        }}>
          Профиль
        </h1>
        <p style={{
          color: 'var(--text-secondary)',
          fontSize: 'var(--font-size-lg)',
          margin: 0
        }}>
          Информация о вашем аккаунте
        </p>
      </div>

      <div style={{ maxWidth: 600, margin: '0 auto' }}>
        {profile && (
          <div className="card" style={{ padding: 'var(--spacing-8)' }}>
            <div style={{
              display: 'grid',
              gap: 'var(--spacing-6)',
              gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))'
            }}>
              <div>
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 'var(--spacing-3)',
                  marginBottom: 'var(--spacing-2)'
                }}>
                  <span style={{ fontSize: '1.25rem' }}>🆔</span>
                  <h3 style={{
                    fontSize: 'var(--font-size-base)',
                    fontWeight: '600',
                    color: 'var(--text-primary)',
                    margin: 0
                  }}>
                    ID пользователя
                  </h3>
                </div>
                <p style={{
                  fontSize: 'var(--font-size-sm)',
                  color: 'var(--text-secondary)',
                  margin: 0,
                  fontFamily: 'monospace',
                  backgroundColor: 'var(--background-color)',
                  padding: 'var(--spacing-2) var(--spacing-3)',
                  borderRadius: 'var(--radius-md)',
                  border: '1px solid var(--border-color)'
                }}>
                  {profile.id}
                </p>
              </div>

              <div>
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 'var(--spacing-3)',
                  marginBottom: 'var(--spacing-2)'
                }}>
                  <span style={{ fontSize: '1.25rem' }}>👤</span>
                  <h3 style={{
                    fontSize: 'var(--font-size-base)',
                    fontWeight: '600',
                    color: 'var(--text-primary)',
                    margin: 0
                  }}>
                    Имя пользователя
                  </h3>
                </div>
                <p style={{
                  fontSize: 'var(--font-size-lg)',
                  fontWeight: '500',
                  color: 'var(--text-primary)',
                  margin: 0
                }}>
                  {profile.username}
                </p>
              </div>

              <div>
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 'var(--spacing-3)',
                  marginBottom: 'var(--spacing-2)'
                }}>
                  <span style={{ fontSize: '1.25rem' }}>📧</span>
                  <h3 style={{
                    fontSize: 'var(--font-size-base)',
                    fontWeight: '600',
                    color: 'var(--text-primary)',
                    margin: 0
                  }}>
                    Email адрес
                  </h3>
                </div>
                <p style={{
                  fontSize: 'var(--font-size-base)',
                  color: 'var(--text-secondary)',
                  margin: 0
                }}>
                  {profile.email}
                </p>
              </div>

              <div>
                <div style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 'var(--spacing-3)',
                  marginBottom: 'var(--spacing-2)'
                }}>
                  <span style={{ fontSize: '1.25rem' }}>📅</span>
                  <h3 style={{
                    fontSize: 'var(--font-size-base)',
                    fontWeight: '600',
                    color: 'var(--text-primary)',
                    margin: 0
                  }}>
                    Дата регистрации
                  </h3>
                </div>
                <p style={{
                  fontSize: 'var(--font-size-base)',
                  color: 'var(--text-secondary)',
                  margin: 0
                }}>
                  {new Date(profile.createdAt).toLocaleDateString('ru-RU', {
                    year: 'numeric',
                    month: 'long',
                    day: 'numeric'
                  })}
                </p>
              </div>
            </div>

            {/* Stats Section */}
            <div style={{
              marginTop: 'var(--spacing-8)',
              paddingTop: 'var(--spacing-6)',
              borderTop: '1px solid var(--border-color)'
            }}>
              <div style={{
                display: 'flex',
                alignItems: 'center',
                gap: 'var(--spacing-3)',
                marginBottom: 'var(--spacing-4)'
              }}>
                <span style={{ fontSize: '1.25rem' }}>📊</span>
                <h3 style={{
                  fontSize: 'var(--font-size-lg)',
                  fontWeight: '600',
                  color: 'var(--text-primary)',
                  margin: 0
                }}>
                  Статистика
                </h3>
              </div>
              <div style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))',
                gap: 'var(--spacing-4)'
              }}>
                <div style={{
                  textAlign: 'center',
                  padding: 'var(--spacing-4)',
                  backgroundColor: 'var(--background-color)',
                  borderRadius: 'var(--radius-lg)',
                  border: '1px solid var(--border-color)'
                }}>
                  <div style={{
                    fontSize: 'var(--font-size-2xl)',
                    fontWeight: '700',
                    color: 'var(--primary-color)',
                    marginBottom: 'var(--spacing-1)'
                  }}>
                    {stats.totalNotes}
                  </div>
                  <div style={{
                    fontSize: 'var(--font-size-sm)',
                    color: 'var(--text-secondary)'
                  }}>
                    Конспектов
                  </div>
                </div>
                <div style={{
                  textAlign: 'center',
                  padding: 'var(--spacing-4)',
                  backgroundColor: 'var(--background-color)',
                  borderRadius: 'var(--radius-lg)',
                  border: '1px solid var(--border-color)'
                }}>
                  <div style={{
                    fontSize: 'var(--font-size-2xl)',
                    fontWeight: '700',
                    color: 'var(--success-color)',
                    marginBottom: 'var(--spacing-1)'
                  }}>
                    {stats.processedNotes}
                  </div>
                  <div style={{
                    fontSize: 'var(--font-size-sm)',
                    color: 'var(--text-secondary)'
                  }}>
                    Обработано
                  </div>
                </div>
                <div style={{
                  textAlign: 'center',
                  padding: 'var(--spacing-4)',
                  backgroundColor: 'var(--background-color)',
                  borderRadius: 'var(--radius-lg)',
                  border: '1px solid var(--border-color)'
                }}>
                  <div style={{
                    fontSize: 'var(--font-size-2xl)',
                    fontWeight: '700',
                    color: 'var(--text-primary)',
                    marginBottom: 'var(--spacing-1)'
                  }}>
                    {stats.totalSize} MB
                  </div>
                  <div style={{
                    fontSize: 'var(--font-size-sm)',
                    color: 'var(--text-secondary)'
                  }}>
                    Загружено
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}

        {/* Actions */}
        <div style={{
          marginTop: 'var(--spacing-6)',
          display: 'flex',
          gap: 'var(--spacing-3)',
          justifyContent: 'center'
        }}>
          <button
            onClick={handleLogout}
            className="btn btn-danger"
            style={{
              fontSize: 'var(--font-size-base)',
              padding: 'var(--spacing-3) var(--spacing-6)'
            }}
          >
            🚪 Выйти из аккаунта
          </button>
        </div>
      </div>
    </div>
  );
};

export default Profile;