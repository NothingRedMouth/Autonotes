import api from '../api/axios';
import { ALLOWED_FILE_TYPES, MAX_FILE_SIZE } from '../utils/constants';

const handleError = (error) => {
  if (error.response?.data) {
    const message = error.response.data.message || error.response.data.error;
    const status = error.response.status;
    const customError = new Error(message);
    customError.status = status;
    throw customError;
  }
  throw error;
};

// Валидация файла
const validateFile = (file) => {
  if (!ALLOWED_FILE_TYPES.includes(file.type)) {
    throw new Error('Неподдерживаемый формат файла. Используйте JPG, PNG или GIF.');
  }

  if (file.size > MAX_FILE_SIZE) {
    throw new Error('Размер файла не должен превышать 10MB.');
  }

  return true;
};

// Получение всех конспектов пользователя
export const getAllNotes = async () => {
  try {
    const response = await api.get('/notes');
    return response.data;
  } catch (error) {
    handleError(error);
  }
};

// Создание нового конспекта
export const createNote = async (title, file) => {
  try {
    // Валидация файла
    validateFile(file);

    const formData = new FormData();
    formData.append('title', title);
    formData.append('file', file);

    const response = await api.post('/notes', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  } catch (error) {
    handleError(error);
  }
};

// Получение конспекта по ID
export const getNoteById = async (id) => {
  try {
    const response = await api.get(`/notes/${id}`);
    return response.data;
  } catch (error) {
    handleError(error);
  }
};

// Удаление конспекта
export const deleteNote = async (id) => {
  try {
    await api.delete(`/notes/${id}`);
  } catch (error) {
    handleError(error);
  }
};

export default {
  getAllNotes,
  createNote,
  getNoteById,
  deleteNote
};