import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ThemeProvider, createTheme, CssBaseline } from '@mui/material';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './components/Login';
import AdminPanel from './components/AdminPanel';
import ClientPanel from './components/ClientPanel';
import EmployeePanel from './components/EmployeePanel';
import Layout from './components/Layout';
import { UserRole } from './types';

const theme = createTheme({
  palette: {
    primary: {
      main: '#1976d2',
    },
    secondary: {
      main: '#dc004e',
    },
  },
});

const AppRoutes: React.FC = () => {
  const { isAuthenticated, user } = useAuth();

  return (
    <Routes>
      <Route path="/login" element={isAuthenticated ? <Navigate to="/" replace /> : <Login />} />
      
      <Route
        path="/admin/*"
        element={
          <ProtectedRoute allowedRoles={[UserRole.ADMIN]}>
            <Layout>
              <AdminPanel />
            </Layout>
          </ProtectedRoute>
        }
      />
      
      <Route
        path="/client"
        element={
          <ProtectedRoute allowedRoles={[UserRole.CLIENT]}>
            <Layout>
              <ClientPanel />
            </Layout>
          </ProtectedRoute>
        }
      />
      
      <Route
        path="/employee"
        element={
          <ProtectedRoute allowedRoles={[UserRole.WORKER]}>
            <Layout>
              <EmployeePanel />
            </Layout>
          </ProtectedRoute>
        }
      />
      
      <Route
        path="/"
        element={
          isAuthenticated ? (
            user?.role === UserRole.ADMIN ? (
              <Navigate to="/admin" replace />
            ) : user?.role === UserRole.CLIENT ? (
              <Navigate to="/client" replace />
            ) : user?.role === UserRole.WORKER ? (
              <Navigate to="/employee" replace />
            ) : (
              <Navigate to="/login" replace />
            )
          ) : (
            <Navigate to="/login" replace />
          )
        }
      />
    </Routes>
  );
};

const App: React.FC = () => {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <BrowserRouter>
        <AuthProvider>
          <AppRoutes />
        </AuthProvider>
      </BrowserRouter>
    </ThemeProvider>
  );
};

export default App;

