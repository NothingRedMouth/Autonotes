import React, { useEffect, useState, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { getNoteById, deleteNote } from '../services/noteService';
import { STATUS_TEXTS } from '../utils/constants';

const NoteDetail = () => {
  const { noteId } = useParams();
  const navigate = useNavigate();
  const [note, setNote] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [deleteLoading, setDeleteLoading] = useState(false);

  const fetchNote = useCallback(async (isSilent = false) => {
    try {
      if (!isSilent) setLoading(true);
      const noteData = await getNoteById(noteId);
      setNote(noteData);
    } catch (err) {
      setError(err.message || 'Ошибка загрузки конспекта');
    } finally {
      if (!isSilent) setLoading(false);
    }
  }, [noteId]);

  useEffect(() => {
    fetchNote();
  }, [fetchNote]);

  useEffect(() => {
    let interval;
    if (note && note.status === 'PROCESSING') {
      interval = setInterval(() => {
        fetchNote(true); // Обновляем данные в фоне
      }, 5000);
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [note?.status, fetchNote]);

  const handleDelete = async () => {
    if (!window.confirm('Вы уверены, что хотите удалить этот конспект?')) {
      return;
    }
    setDeleteLoading(true);
    try {
      await deleteNote(noteId);
      navigate('/dashboard');
    } catch (err) {
      setError(err.message || 'Ошибка при удалении конспекта');
    } finally {
      setDeleteLoading(false);
    }
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'COMPLETED': return '#10b981';
      case 'PROCESSING': return '#f59e0b';
      case 'FAILED': return '#ef4444';
      default: return '#6b7280';
    }
  };

  const getStatusText = (status) => {
    switch (status) {
      case 'COMPLETED': return 'Готов';
      case 'PROCESSING': return 'В обработке';
      case 'FAILED': return 'Ошибка обработки';
      default: return status;
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('ru-RU', {
      year: 'numeric', month: 'long', day: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  };

  if (loading && !note) {
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
          Загрузка конспекта...
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
        <Link to="/dashboard" className="btn btn-primary">
          Вернуться к списку
        </Link>
      </div>
    );
  }

  if (!note) {
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
          color: 'var(--text-secondary)',
          marginBottom: 'var(--spacing-4)'
        }}>
          📄
        </div>
        <h2 style={{ color: 'var(--text-primary)', margin: 0 }}>Конспект не найден</h2>
        <p style={{ color: 'var(--text-secondary)', textAlign: 'center' }}>
          Возможно, он был удален или у вас нет доступа к нему
        </p>
        <Link to="/dashboard" className="btn btn-primary">
          Вернуться к списку
        </Link>
      </div>
    );
  }

  return (
    <div className="slide-up">
      <nav style={{
        marginBottom: 'var(--spacing-6)',
        fontSize: 'var(--font-size-sm)'
      }}>
        <Link
          to="/dashboard"
          style={{
            color: 'var(--text-secondary)',
            textDecoration: 'none',
            display: 'inline-flex',
            alignItems: 'center',
            gap: 'var(--spacing-1)'
          }}
        >
          <span>←</span>
          Назад к списку конспектов
        </Link>
      </nav>

      <div style={{
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'flex-start',
        marginBottom: 'var(--spacing-8)',
        flexWrap: 'wrap',
        gap: 'var(--spacing-4)'
      }}>
        <div style={{ flex: 1, minWidth: 300 }}>
          <h1 style={{
            fontSize: 'var(--font-size-3xl)',
            fontWeight: '700',
            color: 'var(--text-primary)',
            margin: '0 0 var(--spacing-2) 0',
            lineHeight: 1.2
          }}>
            {note.title}
          </h1>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: 'var(--spacing-4)',
            flexWrap: 'wrap'
          }}>
            <p style={{
              color: 'var(--text-secondary)',
              margin: 0,
              fontSize: 'var(--font-size-sm)'
            }}>
              📅 {formatDate(note.createdAt)}
            </p>
            {note.updatedAt && note.updatedAt !== note.createdAt && (
              <p style={{
                color: 'var(--text-muted)',
                margin: 0,
                fontSize: 'var(--font-size-sm)'
              }}>
                ✏️ Обновлено {formatDate(note.updatedAt)}
              </p>
            )}
          </div>
        </div>

        <div style={{
          display: 'flex',
          gap: 'var(--spacing-3)',
          alignItems: 'center',
          flexWrap: 'wrap'
        }}>
          <div className={`status-badge status-${note.status.toLowerCase()}`} style={{
            fontSize: 'var(--font-size-sm)',
            padding: 'var(--spacing-2) var(--spacing-4)'
          }}>
            {STATUS_TEXTS[note.status] || note.status}
          </div>
          <button
            onClick={handleDelete}
            disabled={deleteLoading}
            className="btn btn-danger"
            style={{
              padding: 'var(--spacing-2) var(--spacing-4)',
              fontSize: 'var(--font-size-sm)'
            }}
          >
            {deleteLoading ? (
              <>
                <span className="loading-spinner" style={{ marginRight: 'var(--spacing-2)' }}></span>
                Удаление...
              </>
            ) : (
              <>
                <span>🗑️</span>
                Удалить
              </>
            )}
          </button>
        </div>
      </div>

      <div style={{ display: 'grid', gap: 'var(--spacing-6)' }}>
        <div className="card" style={{ padding: 'var(--spacing-6)' }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: 'var(--spacing-2)',
            marginBottom: 'var(--spacing-4)'
          }}>
            <span style={{ fontSize: '1.25rem' }}>📎</span>
            <h3 style={{
              fontSize: 'var(--font-size-lg)',
              fontWeight: '600',
              color: 'var(--text-primary)',
              margin: 0
            }}>
              Прикрепленные файлы
            </h3>
          </div>
          {note.images && note.images.length > 0 ? (
            <div style={{
              display: 'grid',
              gap: 'var(--spacing-3)',
              gridTemplateColumns: 'repeat(auto-fill, minmax(200px, 1fr))'
            }}>
              {note.images
                .sort((a, b) => a.orderIndex - b.orderIndex)
                .map((image, idx) => (
                  <div
                    key={image.id || idx}
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: 'var(--spacing-3)',
                      padding: 'var(--spacing-3)',
                      backgroundColor: 'var(--background-color)',
                      borderRadius: 'var(--radius-md)',
                      border: '1px solid var(--border-color)'
                    }}
                  >
                    <span style={{ fontSize: '1.25rem' }}>📄</span>
                    <div style={{ flex: 1, minWidth: 0 }}>
                      <p style={{
                        fontSize: 'var(--font-size-sm)',
                        fontWeight: '500',
                        color: 'var(--text-primary)',
                        margin: 0,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap'
                      }}>
                        {image.originalFileName || `Изображение ${idx + 1}`}
                      </p>
                      <p style={{
                        fontSize: 'var(--font-size-xs)',
                        color: 'var(--text-muted)',
                        margin: 'var(--spacing-1) 0 0 0'
                      }}>
                        #{idx + 1} в последовательности
                      </p>
                    </div>
                  </div>
                ))}
            </div>
          ) : (
            <div style={{
              textAlign: 'center',
              padding: 'var(--spacing-8)',
              color: 'var(--text-muted)',
              fontSize: 'var(--font-size-sm)'
            }}>
              <span style={{ fontSize: '2rem', marginBottom: 'var(--spacing-2)', display: 'block' }}>📭</span>
              Файлы не найдены
            </div>
          )}
        </div>

        {note.summaryText && (
          <div className="card" style={{ padding: 'var(--spacing-8)' }}>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: 'var(--spacing-2)',
              marginBottom: 'var(--spacing-6)'
            }}>
              <span style={{ fontSize: '1.25rem' }}>📝</span>
              <h3 style={{
                fontSize: 'var(--font-size-xl)',
                fontWeight: '600',
                color: 'var(--text-primary)',
                margin: 0
              }}>
                Конспект
              </h3>
            </div>
            <div style={{
              backgroundColor: 'var(--primary-light)',
              padding: 'var(--spacing-6)',
              borderRadius: 'var(--radius-lg)',
              borderLeft: '4px solid var(--primary-color)',
              lineHeight: 1.7,
              fontSize: 'var(--font-size-base)',
              color: 'var(--text-primary)'
            }}>
              {note.summaryText.split('\n').map((paragraph, idx) => (
                <p key={idx} style={{ margin: 0, marginBottom: idx < note.summaryText.split('\n').length - 1 ? 'var(--spacing-4)' : 0 }}>
                  {paragraph}
                </p>
              ))}
            </div>
          </div>
        )}

        {note.status === 'PROCESSING' && (
          <div style={{
            backgroundColor: '#fefce8',
            border: '1px solid var(--warning-color)',
            borderRadius: 'var(--radius-lg)',
            padding: 'var(--spacing-8)',
            textAlign: 'center'
          }}>
            <div style={{
              fontSize: '3rem',
              marginBottom: 'var(--spacing-4)',
              animation: 'pulse 2s ease-in-out infinite'
            }}>
              ⏳
            </div>
            <h3 style={{
              fontSize: 'var(--font-size-xl)',
              fontWeight: '600',
              color: '#92400e',
              margin: '0 0 var(--spacing-3) 0'
            }}>
              Идет анализ...
            </h3>
            <p style={{
              color: '#b45309',
              margin: 0,
              fontSize: 'var(--font-size-base)'
            }}>
              ИИ обрабатывает ваши фотографии. Результат появится здесь автоматически.
            </p>
            <div style={{
              marginTop: 'var(--spacing-4)',
              display: 'flex',
              justifyContent: 'center',
              gap: 'var(--spacing-2)'
            }}>
              <div className="loading-spinner"></div>
              <span style={{ color: 'var(--text-secondary)', fontSize: 'var(--font-size-sm)' }}>
                Обработка может занять несколько минут
              </span>
            </div>
          </div>
        )}

        {note.status === 'FAILED' && (
          <div style={{
            backgroundColor: '#fef2f2',
            border: '1px solid var(--error-color)',
            borderRadius: 'var(--radius-lg)',
            padding: 'var(--spacing-8)',
            textAlign: 'center'
          }}>
            <div style={{
              fontSize: '3rem',
              marginBottom: 'var(--spacing-4)',
              color: 'var(--error-color)'
            }}>
              ❌
            </div>
            <h3 style={{
              fontSize: 'var(--font-size-xl)',
              fontWeight: '600',
              color: 'var(--error-color)',
              margin: '0 0 var(--spacing-3) 0'
            }}>
              Ошибка обработки
            </h3>
            <p style={{
              color: '#dc2626',
              margin: '0 0 var(--spacing-6) 0',
              fontSize: 'var(--font-size-base)'
            }}>
              Не удалось обработать фотографии. Попробуйте загрузить их снова с лучшим качеством.
            </p>
            <Link to="/upload" className="btn btn-primary">
              Загрузить заново
            </Link>
          </div>
        )}
      </div>
    </div>
  );
};

export default NoteDetail;