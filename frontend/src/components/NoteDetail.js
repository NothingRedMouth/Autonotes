import React, { useEffect, useState } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { getNoteById, deleteNote } from '../services/noteService';

const NoteDetail = () => {
  const { noteId } = useParams();
  const navigate = useNavigate();
  const [note, setNote] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [deleteLoading, setDeleteLoading] = useState(false);

  useEffect(() => {
    fetchNote();
  }, [noteId]);

  const fetchNote = async () => {
    try {
      setLoading(true);
      const noteData = await getNoteById(noteId);
      setNote(noteData);
    } catch (err) {
      setError(err.message || 'Ошибка загрузки конспекта');
    } finally {
      setLoading(false);
    }
  };

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
      case 'COMPLETED':
        return '#10b981';
      case 'PROCESSING':
        return '#f59e0b';
      case 'FAILED':
        return '#ef4444';
      default:
        return '#6b7280';
    }
  };

  const getStatusText = (status) => {
    switch (status) {
      case 'COMPLETED':
        return 'Готов';
      case 'PROCESSING':
        return 'В обработке';
      case 'FAILED':
        return 'Ошибка обработки';
      default:
        return status;
    }
  };

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('ru-RU', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  if (loading) return <div style={{ padding: 20, textAlign: 'center' }}>Загрузка конспекта...</div>;
  if (error) return <div style={{ padding: 20, color: 'red', textAlign: 'center' }}>{error}</div>;
  if (!note) return <div style={{ padding: 20, textAlign: 'center' }}>Конспект не найден</div>;

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: 20 }}>
      <div style={{ marginBottom: 24 }}>
        <Link 
          to="/dashboard"
          style={{
            color: '#007bff',
            textDecoration: 'none',
            fontSize: '14px',
            marginBottom: 16,
            display: 'inline-block'
          }}
        >
          ← Назад к списку конспектов
        </Link>
        
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginTop: 8 }}>
          <div>
            <h1 style={{ margin: '0 0 8px 0' }}>{note.title}</h1>
            <p style={{ color: '#6b7280', margin: 0 }}>
              Создан: {formatDate(note.createdAt)}
              {note.updatedAt && ` • Обновлен: ${formatDate(note.updatedAt)}`}
            </p>
          </div>
          
          <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <div style={{ 
              padding: '6px 16px',
              background: getStatusColor(note.status),
              color: 'white',
              borderRadius: 20,
              fontSize: '14px',
              fontWeight: 'bold',
              textTransform: 'uppercase'
            }}>
              {getStatusText(note.status)}
            </div>
            
            <button
              onClick={handleDelete}
              style={{
                padding: '6px 12px',
                border: '1px solid #dc2626',
                borderRadius: 6,
                backgroundColor: 'white',
                color: '#dc2626',
                cursor: deleteLoading ? 'not-allowed' : 'pointer',
                fontSize: '14px'
              }}
              disabled={deleteLoading}
            >
              {deleteLoading ? 'Удаление...' : 'Удалить'}
            </button>
          </div>
        </div>
      </div>

      <div style={{ display: 'grid', gap: 24 }}>
        {/* Распознанный текст */}
        {note.recognizedText && (
          <div style={{ 
            background: 'white', 
            border: '1px solid #e5e7eb', 
            borderRadius: 8, 
            padding: 24 
          }}>
            <h3 style={{ margin: '0 0 16px 0', color: '#1f2937' }}>Распознанный текст</h3>
            <div style={{ 
              background: '#f8f9fa', 
              padding: 16, 
              borderRadius: 6,
              whiteSpace: 'pre-wrap',
              lineHeight: 1.6,
              fontSize: '15px'
            }}>
              {note.recognizedText}
            </div>
          </div>
        )}

        {/* Суммаризированный текст */}
        {note.summaryText && (
          <div style={{ 
            background: 'white', 
            border: '1px solid #e5e7eb', 
            borderRadius: 8, 
            padding: 24 
          }}>
            <h3 style={{ margin: '0 0 16px 0', color: '#1f2937' }}>Конспект</h3>
            <div style={{ 
              background: '#f0f9ff', 
              padding: 16, 
              borderRadius: 6,
              whiteSpace: 'pre-wrap',
              lineHeight: 1.6,
              fontSize: '15px',
              borderLeft: '4px solid #007bff'
            }}>
              {note.summaryText}
            </div>
          </div>
        )}

        {/* Сообщение о процессе обработки */}
        {note.status === 'PROCESSING' && (
          <div style={{ 
            background: '#fffbeb', 
            border: '1px solid #f59e0b', 
            borderRadius: 8, 
            padding: 24,
            textAlign: 'center'
          }}>
            <div style={{ fontSize: 48, marginBottom: 16 }}>⏳</div>
            <h3 style={{ margin: '0 0 8px 0', color: '#92400e' }}>Обработка конспекта</h3>
            <p style={{ color: '#b45309', margin: 0 }}>
              Ваш конспект обрабатывается. Это может занять несколько минут.<br />
              Обновите страницу позже, чтобы увидеть результат.
            </p>
          </div>
        )}

        {/* Сообщение об ошибке */}
        {note.status === 'FAILED' && (
          <div style={{ 
            background: '#fef2f2', 
            border: '1px solid #ef4444', 
            borderRadius: 8, 
            padding: 24,
            textAlign: 'center'
          }}>
            <div style={{ fontSize: 48, marginBottom: 16 }}>❌</div>
            <h3 style={{ margin: '0 0 8px 0', color: '#dc2626' }}>Ошибка обработки</h3>
            <p style={{ color: '#b91c1c', margin: 0 }}>
              Не удалось обработать конспект. Попробуйте загрузить изображение еще раз.
            </p>
          </div>
        )}
      </div>
    </div>
  );
};

export default NoteDetail;