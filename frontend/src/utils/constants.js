// Константы для статусов конспектов
export const NOTE_STATUS = {
  PROCESSING: 'PROCESSING',
  COMPLETED: 'COMPLETED',
  FAILED: 'FAILED'
};

// Цвета для статусов
export const STATUS_COLORS = {
  [NOTE_STATUS.PROCESSING]: '#f59e0b',
  [NOTE_STATUS.COMPLETED]: '#10b981',
  [NOTE_STATUS.FAILED]: '#ef4444'
};

// Тексты для статусов
export const STATUS_TEXTS = {
  [NOTE_STATUS.PROCESSING]: 'В обработке',
  [NOTE_STATUS.COMPLETED]: 'Готов',
  [NOTE_STATUS.FAILED]: 'Ошибка'
};

// Максимальный размер файла (Подняли до 50MB по задаче)
export const MAX_FILE_SIZE = 50 * 1024 * 1024;


export const ALLOWED_FILE_TYPES = [
  'image/jpeg',
  'image/jpg',
  'image/png',
  'image/gif',
  'application/pdf' 
];