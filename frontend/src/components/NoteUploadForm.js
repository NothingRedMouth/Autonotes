import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { createNote } from '../services/noteService';

const NoteUploadForm = () => {
  const [formData, setFormData] = useState({
    title: '',
    file: null
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [preview, setPreview] = useState('');
  const navigate = useNavigate();

  const handleInputChange = (e) => {
    const { name, value } = e.target;
    setFormData(prev => ({
      ...prev,
      [name]: value
    }));
  };

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      // Проверяем тип файла
      if (!file.type.startsWith('image/')) {
        setError('Пожалуйста, выберите файл изображения');
        return;
      }

      // Проверяем размер файла (максимум 10MB)
      if (file.size > 10 * 1024 * 1024) {
        setError('Размер файла не должен превышать 10MB');
        return;
      }

      setFormData(prev => ({
        ...prev,
        file: file
      }));

      // Создаем превью
      const reader = new FileReader();
      reader.onload = (e) => {
        setPreview(e.target.result);
      };
      reader.readAsDataURL(file);
      setError('');
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!formData.title.trim()) {
      setError('Введите название конспекта');
      return;
    }

    if (!formData.file) {
      setError('Выберите файл для загрузки');
      return;
    }

    setLoading(true);
    setError('');

    try {
      await createNote(formData.title, formData.file);
      // После успешной загрузки переходим на дашборд
      navigate('/dashboard');
    } catch (err) {
      setError(err.message || 'Ошибка при загрузке конспекта');
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    navigate('/dashboard');
  };

  return (
    <div style={{ maxWidth: 600, margin: '0 auto', padding: 20 }}>
      <div style={{ marginBottom: 30 }}>
        <h1>Загрузить новый конспект</h1>
        <p style={{ color: '#6b7280', marginTop: 8 }}>
          Загрузите фотографию учебной доски для автоматического создания конспекта
        </p>
      </div>

      <form onSubmit={handleSubmit}>
        <div style={{ marginBottom: 20 }}>
          <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>
            Название конспекта *
          </label>
          <input
            type="text"
            name="title"
            value={formData.title}
            onChange={handleInputChange}
            placeholder="Например: Лекция по математике от 10.11.2024"
            style={{
              width: '100%',
              padding: '10px 12px',
              border: '1px solid #d1d5db',
              borderRadius: 6,
              fontSize: '16px'
            }}
            disabled={loading}
          />
        </div>

        <div style={{ marginBottom: 20 }}>
          <label style={{ display: 'block', marginBottom: 8, fontWeight: 'bold' }}>
            Фотография доски *
          </label>
          <div
            style={{
              border: '2px dashed #d1d5db',
              borderRadius: 8,
              padding: 40,
              textAlign: 'center',
              backgroundColor: preview ? 'transparent' : '#f9fafb',
              cursor: loading ? 'not-allowed' : 'pointer',
              position: 'relative'
            }}
            onClick={() => !loading && document.getElementById('file-input').click()}
          >
            <input
              id="file-input"
              type="file"
              accept="image/*"
              onChange={handleFileChange}
              style={{ display: 'none' }}
              disabled={loading}
            />
            
            {preview ? (
              <div>
                <img 
                  src={preview} 
                  alt="Preview" 
                  style={{ 
                    maxWidth: '100%', 
                    maxHeight: 300, 
                    borderRadius: 4,
                    marginBottom: 16
                  }} 
                />
                <p style={{ color: '#6b7280', margin: 0 }}>
                  Нажмите для выбора другого файла
                </p>
              </div>
            ) : (
              <div>
                <div style={{ fontSize: 48, color: '#9ca3af', marginBottom: 16 }}>📷</div>
                <p style={{ color: '#6b7280', margin: 0 }}>
                  Нажмите для выбора файла или перетащите изображение сюда
                </p>
                <p style={{ color: '#9ca3af', fontSize: '14px', margin: '8px 0 0 0' }}>
                  Поддерживаются: JPG, PNG, JPEG (макс. 10MB)
                </p>
              </div>
            )}
          </div>
        </div>

        {error && (
          <div style={{ 
            padding: 12, 
            backgroundColor: '#fef2f2', 
            border: '1px solid #fecaca',
            borderRadius: 6,
            color: '#dc2626',
            marginBottom: 20
          }}>
            {error}
          </div>
        )}

        <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
          <button
            type="button"
            onClick={handleCancel}
            style={{
              padding: '10px 20px',
              border: '1px solid #d1d5db',
              borderRadius: 6,
              backgroundColor: 'white',
              color: '#374151',
              cursor: 'pointer'
            }}
            disabled={loading}
          >
            Отмена
          </button>
          <button
            type="submit"
            style={{
              padding: '10px 20px',
              border: 'none',
              borderRadius: 6,
              backgroundColor: loading ? '#9ca3af' : '#007bff',
              color: 'white',
              cursor: loading ? 'not-allowed' : 'pointer',
              fontWeight: 'bold'
            }}
            disabled={loading}
          >
            {loading ? 'Загрузка...' : 'Загрузить конспект'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default NoteUploadForm;