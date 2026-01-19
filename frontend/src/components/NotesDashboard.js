import React, { useEffect, useState, useCallback, useMemo } from 'react';
import { Link } from 'react-router-dom';
import { getAllNotes } from '../services/noteService';
import { STATUS_COLORS, STATUS_TEXTS, NOTE_STATUS } from '../utils/constants';

const NotesDashboard = () => {
  const [notes, setNotes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [sortBy, setSortBy] = useState('createdAt');
  const [sortOrder, setSortOrder] = useState('desc');

  const fetchNotes = useCallback(async (isSilent = false) => {
    try {
      if (!isSilent) setLoading(true);
      const notesData = await getAllNotes();
      setNotes(notesData);
    } catch (err) {
      setError(err.message || 'Ошибка загрузки конспектов');
    } finally {
      if (!isSilent) setLoading(false);
    }
  }, []);

  //Исправление бесконечных запросов
  useEffect(() => {
    fetchNotes();
  }, [fetchNotes]);

  //Авто-обновление только если есть заметки в обработке
  useEffect(() => {
    const hasProcessingNotes = notes.some(note => note.status === NOTE_STATUS.PROCESSING);
    if (!hasProcessingNotes) return;

    const interval = setInterval(() => {
      fetchNotes(true);
    }, 5000);

    return () => clearInterval(interval);
  }, [notes, fetchNotes]);

  // Фильтрация и сортировка заметок
  const filteredAndSortedNotes = useMemo(() => {
    let filtered = notes.filter(note =>
      note.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (note.summaryText && note.summaryText.toLowerCase().includes(searchQuery.toLowerCase()))
    );

    filtered.sort((a, b) => {
      let aValue, bValue;

      switch (sortBy) {
        case 'title':
          aValue = a.title.toLowerCase();
          bValue = b.title.toLowerCase();
          break;
        case 'status':
          aValue = a.status;
          bValue = b.status;
          break;
        case 'createdAt':
        default:
          aValue = new Date(a.createdAt);
          bValue = new Date(b.createdAt);
          break;
      }

      if (sortOrder === 'asc') {
        return aValue > bValue ? 1 : -1;
      } else {
        return aValue < bValue ? 1 : -1;
      }
    });

    return filtered;
  }, [notes, searchQuery, sortBy, sortOrder]);

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('ru-RU', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getTimeAgo = (dateString) => {
    const now = new Date();
    const date = new Date(dateString);
    const diffInHours = Math.floor((now - date) / (1000 * 60 * 60));

    if (diffInHours < 1) return 'только что';
    if (diffInHours < 24) return `${diffInHours} ч назад`;

    const diffInDays = Math.floor(diffInHours / 24);
    if (diffInDays < 7) return `${diffInDays} д назад`;

    return formatDate(dateString);
  };

  if (loading && notes.length === 0) {
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
          Загрузка конспектов...
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
        <button
          onClick={() => window.location.reload()}
          className="btn btn-primary"
        >
          Попробовать снова
        </button>
      </div>
    );
  }

  return (
    <div className="slide-up">
      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        marginBottom: 'var(--spacing-8)',
        flexWrap: 'wrap',
        gap: 'var(--spacing-4)'
      }}>
        <div>
          <h1 style={{
            fontSize: 'var(--font-size-3xl)',
            fontWeight: '700',
            color: 'var(--text-primary)',
            margin: 0
          }}>
            Мои конспекты
          </h1>
          <p style={{
            color: 'var(--text-secondary)',
            margin: 'var(--spacing-1) 0 0 0',
            fontSize: 'var(--font-size-base)'
          }}>
            {notes.length === 0 ? 'Начните с загрузки первой фотографии доски' : `${notes.length} конспект${notes.length === 1 ? '' : notes.length < 5 ? 'а' : 'ов'}`}
          </p>
        </div>
        <Link to="/upload" className="btn btn-primary" style={{
          display: 'flex',
          alignItems: 'center',
          gap: 'var(--spacing-2)',
          fontSize: 'var(--font-size-base)',
          padding: 'var(--spacing-3) var(--spacing-5)'
        }}>
          <span>➕</span>
          Новый конспект
        </Link>
      </div>

      {notes.length > 0 && (
        <div style={{
          display: 'flex',
          gap: 'var(--spacing-4)',
          marginBottom: 'var(--spacing-6)',
          flexWrap: 'wrap',
          alignItems: 'center'
        }}>
          <div style={{ flex: 1, minWidth: 250 }}>
            <input
              type="text"
              placeholder="Поиск по названию или содержимому..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="form-input"
              style={{
                width: '100%',
                padding: 'var(--spacing-3) var(--spacing-4)',
                borderRadius: 'var(--radius-lg)'
              }}
            />
          </div>

          <div style={{ display: 'flex', gap: 'var(--spacing-2)', alignItems: 'center' }}>
            <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              className="form-input"
              style={{ padding: 'var(--spacing-3)', minWidth: 120 }}
            >
              <option value="createdAt">По дате</option>
              <option value="title">По названию</option>
              <option value="status">По статусу</option>
            </select>
            <button
              onClick={() => setSortOrder(sortOrder === 'asc' ? 'desc' : 'asc')}
              className="btn btn-ghost"
              style={{ padding: 'var(--spacing-3)' }}
              title={sortOrder === 'asc' ? 'По убыванию' : 'По возрастанию'}
            >
              {sortOrder === 'asc' ? '↑' : '↓'}
            </button>
          </div>
        </div>
      )}

      {filteredAndSortedNotes.length === 0 ? (
        notes.length === 0 ? (
          <div style={{
            textAlign: 'center',
            padding: 'var(--spacing-16)',
            background: 'linear-gradient(135deg, var(--surface-color) 0%, var(--surface-hover) 100%)',
            borderRadius: 'var(--radius-xl)',
            border: '2px dashed var(--border-color)'
          }}>
            <div style={{
              fontSize: '4rem',
              marginBottom: 'var(--spacing-6)',
              opacity: 0.6
            }}>
              📚
            </div>
            <h3 style={{
              fontSize: 'var(--font-size-xl)',
              fontWeight: '600',
              color: 'var(--text-primary)',
              margin: '0 0 var(--spacing-3) 0'
            }}>
              У вас пока нет конспектов
            </h3>
            <p style={{
              color: 'var(--text-secondary)',
              margin: '0 0 var(--spacing-6) 0',
              fontSize: 'var(--font-size-lg)'
            }}>
              Начните с загрузки фотографий досок для автоматического создания конспектов
            </p>
            <Link to="/upload" className="btn btn-primary" style={{
              display: 'inline-flex',
              alignItems: 'center',
              gap: 'var(--spacing-2)',
              padding: 'var(--spacing-4) var(--spacing-6)',
              fontSize: 'var(--font-size-lg)'
            }}>
              <span>📷</span>
              Загрузить первый конспект
            </Link>
          </div>
        ) : (
          <div style={{
            textAlign: 'center',
            padding: 'var(--spacing-12)',
            color: 'var(--text-secondary)'
          }}>
            <div style={{ fontSize: '3rem', marginBottom: 'var(--spacing-4)' }}>🔍</div>
            <h3 style={{ margin: '0 0 var(--spacing-2) 0' }}>Ничего не найдено</h3>
            <p style={{ margin: 0 }}>
              Попробуйте изменить поисковый запрос или сбросить фильтры
            </p>
          </div>
        )
      ) : (
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
          gap: 'var(--spacing-6)'
        }}>
          {filteredAndSortedNotes.map((note) => (
            <Link
              key={note.id}
              to={`/notes/${note.id}`}
              className="card note-card"
              style={{
                display: 'block',
                padding: 'var(--spacing-6)',
                textDecoration: 'none',
                color: 'inherit',
                cursor: 'pointer'
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 'var(--spacing-4)' }}>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <h3 style={{
                    fontSize: 'var(--font-size-lg)',
                    fontWeight: '600',
                    color: 'var(--text-primary)',
                    margin: '0 0 var(--spacing-2) 0',
                    overflow: 'hidden',
                    textOverflow: 'ellipsis',
                    whiteSpace: 'nowrap'
                  }}>
                    {note.title}
                  </h3>
                  <p style={{
                    color: 'var(--text-secondary)',
                    fontSize: 'var(--font-size-sm)',
                    margin: 0
                  }}>
                    {getTimeAgo(note.createdAt)}
                  </p>
                </div>
                <div className={`status-badge status-${note.status.toLowerCase()}`}>
                  {STATUS_TEXTS[note.status] || note.status}
                </div>
              </div>

              {note.summaryText && (
                <p style={{
                  color: 'var(--text-primary)',
                  fontSize: 'var(--font-size-sm)',
                  lineHeight: 1.5,
                  margin: 0,
                  display: '-webkit-box',
                  WebkitLineClamp: 3,
                  WebkitBoxOrient: 'vertical',
                  overflow: 'hidden'
                }}>
                  {note.summaryText}
                </p>
              )}

              {note.images && note.images.length > 0 && (
                <div style={{
                  marginTop: 'var(--spacing-4)',
                  paddingTop: 'var(--spacing-4)',
                  borderTop: '1px solid var(--border-color)',
                  fontSize: 'var(--font-size-xs)',
                  color: 'var(--text-muted)'
                }}>
                  📎 {note.images.length} файл{note.images.length === 1 ? '' : note.images.length < 5 ? 'а' : 'ов'}
                </div>
              )}
            </Link>
          ))}
        </div>
      )}
    </div>
  );
};

export default NotesDashboard;