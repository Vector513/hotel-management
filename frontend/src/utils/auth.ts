import { jwtDecode } from 'jwt-decode';
import type { User, UserRole } from '../types';

interface JWTPayload {
  userId?: number;
  id?: number;
  username: string;
  role: UserRole;
  clientId?: number;
  employeeId?: number;
  fullName?: string;
  exp: number;
  iat: number;
}

export const decodeToken = (token: string): User | null => {
  try {
    const decoded = jwtDecode<JWTPayload>(token);
    return {
      id: decoded.userId || decoded.id || 0,
      username: decoded.username,
      role: decoded.role,
      clientId: decoded.clientId,
      employeeId: decoded.employeeId,
      fullName: decoded.fullName,
    };
  } catch (error) {
    console.error('Failed to decode token:', error);
    return null;
  }
};

export const isTokenExpired = (token: string): boolean => {
  try {
    const decoded = jwtDecode<JWTPayload>(token);
    const currentTime = Date.now() / 1000;
    return decoded.exp < currentTime;
  } catch {
    return true;
  }
};

export const getStoredUser = (): User | null => {
  const token = localStorage.getItem('token');
  if (!token) return null;
  
  if (isTokenExpired(token)) {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    return null;
  }
  
  const user = decodeToken(token);
  if (user) {
    localStorage.setItem('user', JSON.stringify(user));
  }
  return user;
};

