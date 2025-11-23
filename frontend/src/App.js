import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import AuthForm from './components/AuthForm';
import Profile from './components/Profile';
import ProtectedRoute from './components/ProtectedRoute';
import NotesDashboard from './components/NotesDashboard';
import NoteUploadForm from './components/NoteUploadForm';
import NoteDetail from './components/NoteDetail';
import Layout from './components/Layout';
import './App.css';

// Компонент-обертка для защищенных маршрутов с Layout
const ProtectedLayout = ({ children }) => (
  <ProtectedRoute>
    <Layout>
      {children}
    </Layout>
  </ProtectedRoute>
);

function App() {
  return (
    <Router>
      <div className="App">
        <Routes>
          {/* Публичные маршруты (без Layout) */}
          <Route path="/login" element={<AuthForm mode="login" />} />
          <Route path="/register" element={<AuthForm mode="register" />} />
          
          {/* Защищенные маршруты (с Layout) */}
          <Route path="/" element={<Navigate to="/dashboard" replace />} />
          <Route 
            path="/dashboard" 
            element={
              <ProtectedLayout>
                <NotesDashboard />
              </ProtectedLayout>
            } 
          />
          <Route 
            path="/profile" 
            element={
              <ProtectedLayout>
                <Profile />
              </ProtectedLayout>
            } 
          />
          <Route 
            path="/upload" 
            element={
              <ProtectedLayout>
                <NoteUploadForm />
              </ProtectedLayout>
            } 
          />
          <Route 
            path="/notes/:noteId" 
            element={
              <ProtectedLayout>
                <NoteDetail />
              </ProtectedLayout>
            } 
          />
        </Routes>
      </div>
    </Router>
  );
}

export default App;