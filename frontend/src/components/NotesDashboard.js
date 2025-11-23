import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getAllNotes } from '../services/noteService';
import { STATUS_COLORS, STATUS_TEXTS, NOTE_STATUS } from '../utils/constants';

const NotesDashboard = () => {
  const [notes, setNotes] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    fetchNotes();
    
    // Авто-обновление для конспектов в обработке
    const interval = setInterval(() => {
      const hasProcessingNotes = notes.some(note => note.status === NOTE_STATUS.PROCESSING);
      if (hasProcessingNotes) {
        fetchNotes();
      }
    }, 10000); // Проверка каждые 10 секунд

    return () => clearInterval(interval);
  }, [notes]);

  const fetchNotes = async () => {
    try {
      const notesData = await getAllNotes();
      setNotes(notesData);
    } catch (err) {
      setError(err.message || 'Ошибка загрузки конспектов');
    } finally {
      setLoading(false);
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

  if (loading) return <div style={{ padding: 20, textAlign: 'center' }}>Загрузка конспектов...</div>;
  if (error) return <div style={{ padding: 20, color: 'red', textAlign: 'center' }}>{error}</div>;

  return (
    <div style={{ maxWidth: 800, margin: '0 auto', padding: 20 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 30 }}>
        <h1>Мои конспекты</h1>
        <Link 
          to="/upload" 
          style={{
            padding: '10px 20px',
            background: '#007bff',
            color: 'white',
            textDecoration: 'none',
            borderRadius: 4,
            fontWeight: 'bold'
          }}
        >
          + Новый конспект
        </Link>
      </div>

      {notes.length === 0 ? (
        <div style={{ 
          textAlign: 'center', 
          padding: 40, 
          background: '#f8f9fa', 
          borderRadius: 8,
          border: '2px dashed #dee2e6'
        }}>
          <h3>У вас пока нет конспектов</h3>
          <p>Начните с загрузки первого изображения!</p>
          <Link 
            to="/upload"
            style={{
              padding: '10px 20px',
              background: '#007bff',
              color: 'white',
              textDecoration: 'none',
              borderRadius: 4,
              display: 'inline-block',
              marginTop: 10
            }}
          >
            Загрузить конспект
          </Link>
        </div>
      ) : (
        <div style={{ display: 'grid', gap: 16 }}>
          {notes.map((note) => (
            <Link 
              key={note.id} 
              to={`/notes/${note.id}`}
              style={{
                display: 'block',
                padding: 20,
                background: 'white',
                border: '1px solid #e5e7eb',
                borderRadius: 8,
                textDecoration: 'none',
                color: 'inherit',
                transition: 'all 0.2s',
                boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
              }}
              onMouseEnter={(e) => {
                e.target.style.transform = 'translateY(-2px)';
                e.target.style.boxShadow = '0 4px 12px rgba(0,0,0,0.15)';
              }}
              onMouseLeave={(e) => {
                e.target.style.transform = 'translateY(0)';
                e.target.style.boxShadow = '0 1px 3px rgba(0,0,0,0.1)';
              }}
            >
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                <div style={{ flex: 1 }}>
                  <h3 style={{ margin: '0 0 8px 0', color: '#1f2937' }}>
                    {note.title}
                  </h3>
                  <p style={{ 
                    margin: '0 0 12px 0', 
                    color: '#6b7280',
                    fontSize: '14px'
                  }}>
                    Создан: {formatDate(note.createdAt)}
                  </p>
                  {note.recognizedText && (
                    <p style={{ 
                      margin: 0,
                      color: '#374151',
                      fontSize: '14px',
                      display: '-webkit-box',
                      WebkitLineClamp: 2,
                      WebkitBoxOrient: 'vertical',
                      overflow: 'hidden'
                    }}>
                      {note.recognizedText}
                    </p>
                  )}
                </div>
                <div style={{ 
                  padding: '4px 12px',
                  background: STATUS_COLORS[note.status] || '#6b7280',
                  color: 'white',
                  borderRadius: 20,
                  fontSize: '12px',
                  fontWeight: 'bold',
                  textTransform: 'uppercase'
                }}>
                  {STATUS_TEXTS[note.status] || note.status}
                </div>
              </div>
            </Link>
          ))}
        </div>
      )}
    </div>
  );
};

export default NotesDashboard;